/*
 * SimpleStore.java
 * Created on May 27, 2003
 *
 * $Header$
 */
package org.archive.crawler.basic;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Iterator;

import org.archive.crawler.datamodel.CoreAttributeConstants;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.FetchStatusCodes;
import org.archive.crawler.datamodel.UURI;
import org.archive.crawler.framework.CrawlController;
import org.archive.crawler.framework.URIStore;

import java.util.logging.Logger;

/**
 * A minimal in-memory URIStore. Keeps "full" CrawlURI instances
 * around, because it can. 
 * 
 * @author gojomo
 *
 */
public class SimpleStore implements URIStore, FetchStatusCodes, CoreAttributeConstants {
	private static Logger logger = Logger.getLogger("org.archive.crawler.basic.SimpleStore");
	
	HashMap allCuris = new HashMap(); // of UURI -> CrawlURI 
	
	// every CrawlURI not yet in process or another queue; all seeds start here
	LinkedList pendingQueue = new LinkedList(); // of CrawlURIs 
	
	
	// every CrawlURI handed out for processing but not yet returned
	HashMap inProcessMap = new HashMap(); // of String (classKey) -> CrawlURI
	
	// all active per-class queues
	HashMap allClassQueuesMap = new HashMap(); // of String (classKey) -> KeyedQueue
	
	// all per-class queues whose first item may be handed out (that is, no CrawlURI
	// of the same class is currently in-process)
	LinkedList readyClassQueues = new LinkedList(); // of String (queueKey) -> KeyedQueue 
	
	// all per-class queues who are on hold because a CrawlURI of their class
	// is already in process 
	LinkedList heldClassQueues = new LinkedList(); // of String (queueKey) -> KeyedQueue 

    // all per-class queues who are on hold until a certain time
	SortedSet snoozeQueues = new TreeSet(new SchedulingComparator()); // of KeyedQueue, sorted by wakeTime


	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.URIStore#initialize(org.archive.crawler.framework.CrawlController)
	 */
	public void initialize(CrawlController c) {
		// TODO load state from disk 

	}
	
	/** Return the number of pending (encountered but not visited) URIs
	 * @return
	 */
	public int urisInFrontier(){
		int uris = pendingQueue.size() ;
		
		Iterator i = allClassQueuesMap.values().iterator();
		
		while(i.hasNext()){
			KeyedQueue queue = (KeyedQueue)i.next();
			uris += queue.size();
		}
		
		return uris;
	}
	
	
	/** Return the size of the URI store.
	 * @return storeSize
	 */
	public int discoveredUriCount(){
		return allCuris.size();	
	}
	

	/**
	 * @param uuri
	 */
	public void insertAsSeed(UURI uuri) {
		if(allCuris.get(uuri)!=null) {
			// already inserted
			return;
		}
		CrawlURI curi = new CrawlURI(uuri);
		curi.getAList().putInt("distance-from-seed",0);
		allCuris.put(uuri,curi);
		pendingQueue.addLast(curi);
		curi.setStoreState(URIStoreable.PENDING);
	}

	/**
	 * 
	 */
	public synchronized void wakeReadyQueues(long now) {
		while(!snoozeQueues.isEmpty()&&((URIStoreable)snoozeQueues.first()).getWakeTime()<=now) {
			URIStoreable awoken = (URIStoreable)snoozeQueues.first();
			if (!snoozeQueues.remove(awoken)) {
				logger.severe("first() item couldn't be remove()d!");
			}
			if (awoken instanceof KeyedQueue) {
				assert inProcessMap.get(awoken.getClassKey()) == null : "false ready: class peer still in process";
				if(((KeyedQueue)awoken).isEmpty()) {
					// just drop queue
					discardQueue(awoken);
					return;
				}
				readyClassQueues.add(awoken);
				awoken.setStoreState(URIStoreable.READY);
			} else if (awoken instanceof CrawlURI) {
				// TODO think about whether this is right
				pendingQueue.addFirst(awoken);
				awoken.setStoreState(URIStoreable.PENDING);
			} else {
				assert false : "something evil has awoken!";
			}
		}
	}

	private void discardQueue(URIStoreable awoken) {
		allClassQueuesMap.remove(((KeyedQueue)awoken).getClassKey());
		awoken.setStoreState(URIStoreable.FINISHED);
	}

	/**
	 * @return
	 */
	public LinkedList getReadyClassQueues() {
		return readyClassQueues;
	}

	/**
	 * @return
	 */
	public CrawlURI dequeueFromReady() {
		KeyedQueue firstReadyQueue = (KeyedQueue)readyClassQueues.getFirst();
		CrawlURI readyCuri = (CrawlURI) firstReadyQueue.removeFirst();
		return readyCuri;
	}

	/**
	 * @param curi
	 */
	public void noteInProcess(CrawlURI curi) {
		assert inProcessMap.get(curi.getClassKey()) == null : "two CrawlURIs with same classKey in process";
		
		inProcessMap.put(curi.getClassKey(), curi);
		curi.setStoreState(URIStoreable.IN_PROCESS);
		
		KeyedQueue classQueue = (KeyedQueue) allClassQueuesMap.get(curi.getClassKey());
		if (classQueue == null) {
			return;
		}
		assert classQueue.getStoreState() == URIStoreable.READY : "odd state for classQueue of to-be-emitted CrawlURI";
		readyClassQueues.remove(classQueue);
		enqueueToHeld(classQueue);
	}

	/**
	 * 
	 * @return
	 */
	public void noteProcessingDone(CrawlURI curi) {
		assert inProcessMap.get(curi.getClassKey()) == curi : "CrawlURI returned not in process";
		
		inProcessMap.remove(curi.getClassKey());
		
		KeyedQueue classQueue = (KeyedQueue) allClassQueuesMap.get(curi.getClassKey());
		if ( classQueue == null ) {
			return;
		}
		assert classQueue.getStoreState() == URIStoreable.HELD : "odd state for classQueue of remitted CrawlURI";
		heldClassQueues.remove(classQueue);
		if(classQueue.isEmpty()) {
			// just drop it
			discardQueue(classQueue);
			return; 
		}
		readyClassQueues.add(classQueue);
		classQueue.setStoreState(URIStoreable.READY);
		// TODO: since usually, queue will be snoozed, this juggling is often superfluous
	}
	
	/**
	 * 
	 */
	protected CrawlURI dequeueFromPending() {
		if (pendingQueue.isEmpty()) {
			return null;
		}
		return (CrawlURI)pendingQueue.removeFirst();
	}

	/**
	 * 
	 * @param curi
	 * @return true if enqueued
	 */
	public boolean enqueueIfNecessary(CrawlURI curi) {
		KeyedQueue classQueue = (KeyedQueue) allClassQueuesMap.get(curi.getClassKey());
		if (classQueue != null) {
			// must enqueue
			classQueue.add(curi);
			curi.setStoreState(classQueue.getStoreState());
			return true;
		}
		CrawlURI classmateInProgress = (CrawlURI) inProcessMap.get(curi.getClassKey());
		if (classmateInProgress != null) {
			// must create queue, and enqueue
			classQueue = new KeyedQueue(curi.getClassKey());
			allClassQueuesMap.put(classQueue.getClassKey(), classQueue);
			enqueueToHeld(classQueue);
			classQueue.add(curi);
			curi.setStoreState(classQueue.getStoreState());
			return true;
		}
		
		return false;	
	}

	/**
	 * @param classQueue
	 */
	private void enqueueToHeld(KeyedQueue classQueue) {
		heldClassQueues.add(classQueue);
		classQueue.setStoreState(URIStoreable.HELD);
	}

	/**
	 * @return
	 */
	public long earliestWakeTime() {
		if (!snoozeQueues.isEmpty()) {
			return ((URIStoreable)snoozeQueues.first()).getWakeTime();
		}
		return Long.MAX_VALUE;
	}

	/**
	 * @param prereq
	 */
	public void insertAtHead(UURI uuri, int dist) {
		if(filteredOut(uuri)){
			if(uuri != null){
				logger.info("filtering " + uuri.toString() );
			}
			return; 
		}
		
		CrawlURI curi = (CrawlURI)allCuris.get(uuri);
		if(curi == null) {
			// newly scheduled
			curi = new CrawlURI(uuri);
			allCuris.put(uuri,curi);
		} else {
			// curi already exists;
			
			// if curi is still locked out, ignore request to schedule
			if(curi.dontFetchYet()){
				return;
			} 
			// yank URI back into scheduling if necessary
			curi.reconstitute();
		}
		
		int newDist = dist;
		if(curi.getAList().containsKey(A_DISTANCE_FROM_SEED)) {
			newDist = Math.max(dist,curi.getAList().getInt(A_DISTANCE_FROM_SEED));
		}
		curi.getAList().putInt(A_DISTANCE_FROM_SEED,newDist);
		
		KeyedQueue classQueue = (KeyedQueue) allClassQueuesMap.get(curi.getClassKey());
		if ( classQueue == null ) {
			pendingQueue.addFirst(curi);
			curi.setStoreState(URIStoreable.PENDING);
			notify();
			return;
		}
		classQueue.addFirst(curi);
		curi.setStoreState(classQueue.getStoreState());
	}

	/**
	 * @param uuri
	 * @return
	 */
	private boolean filteredOut(UURI uuri) {
		// for now discard all non-http non-dns schemes
		if (uuri==null) return true;
		
		return !(uuri.getUri().getScheme().equals("http")
						|| uuri.getUri().getScheme().equals("dns"));
	}

	/**
	 * @param curi
	 */
	public void reinsert(CrawlURI curi) {

		if(enqueueIfNecessary(curi)) {
			// added to classQueue
			return;
		}
		// no classQueue
		pushToPending(curi);
	}

	/**
	 * @param curi
	 */
	private void pushToPending(CrawlURI curi) {
		pendingQueue.addFirst(curi);
		curi.setStoreState(URIStoreable.PENDING);
	}

	/**
	 * @param object
	 * @param l
	 */
	public void snoozeQueueUntil(Object classKey, long wake) {
		KeyedQueue classQueue = (KeyedQueue) allClassQueuesMap.get(classKey);
		if ( classQueue == null ) {
			classQueue = new KeyedQueue(classKey);
			allClassQueuesMap.put(classQueue.getClassKey(),classQueue);
		} else {
			assert classQueue.getStoreState() == URIStoreable.READY : "snoozing queue should have been READY";
			readyClassQueues.remove(classQueue);
		}
		classQueue.setWakeTime(wake);
		snoozeQueues.add(classQueue);
		classQueue.setStoreState(URIStoreable.SNOOZED);
	}

	/**
	 * add, without necessarily adding to front of queues
	 * (in fact, currently adds at back
	 * 
	 * @param embed
	 * @param i
	 */
	public void insert(UURI uuri, int dist) {
		if(filteredOut(uuri)) return;
		CrawlURI curi = (CrawlURI)allCuris.get(uuri);
		if(curi!=null) {
			// already inserted
			// TODO: perhaps yank to front?
			// if curi is still locked out, ignore request to schedule
			if(curi.dontFetchYet()){
				return;
			} 
			// yank URI back into scheduling if necessary
			curi.reconstitute();
		} else {
			curi = new CrawlURI(uuri);
		}
		curi.getAList().putInt("distance-from-seed",dist);
		allCuris.put(uuri,curi);
		KeyedQueue classQueue = (KeyedQueue) allClassQueuesMap.get(curi.getClassKey());
		if ( classQueue == null ) {
			pendingQueue.addLast(curi);
			curi.setStoreState(URIStoreable.PENDING);
			notify();
			return;
		}
		classQueue.addLast(curi);
		curi.setStoreState(classQueue.getStoreState());
	}

	/**
	 * Store is empty only if all queues are empty and 
	 * no URIs are in-process
	 * 
	 * @return
	 */
	public boolean isEmpty() {
		return pendingQueue.isEmpty()
		        && readyClassQueues.isEmpty()
		        && heldClassQueues.isEmpty() 
				&& snoozeQueues.isEmpty()
				&& inProcessMap.isEmpty();
	}

	/**
	 * @param u
	 * @return
	 */
	public CrawlURI getExistingCrawlURI(UURI u) {
		return (CrawlURI) allCuris.get(u);
	}

	/**
	 * Revisit the CrawlURI -- but not before delay time has passed.
	 * @param curi
	 * @param retryDelay
	 */
	public void insertSnoozed(CrawlURI curi, long retryDelay) {
		curi.setWakeTime(System.currentTimeMillis()+retryDelay );
		curi.setStoreState(URIStoreable.SNOOZED);
		snoozeQueues.add(curi);
	}
}
