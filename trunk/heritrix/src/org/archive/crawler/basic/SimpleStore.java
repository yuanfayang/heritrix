/*
 * SimpleStore.java
 * Created on May 27, 2003
 *
 * $Header$
 */
package org.archive.crawler.basic;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.TreeSet;

import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.UURI;
import org.archive.crawler.framework.CrawlController;
import org.archive.crawler.framework.URIStore;

/**
 * A minimal in-memory URIStore. Keeps "full" CrawlURI instances
 * around, because it can. 
 * 
 * @author gojomo
 *
 */
public class SimpleStore implements URIStore {
	protected final Object ReadyChangeSemaphore = new Object();
	
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
	TreeSet snoozeQueues = new TreeSet(); // of KeyedQueue, sorted by wakeTime


	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.URIStore#initialize(org.archive.crawler.framework.CrawlController)
	 */
	public void initialize(CrawlController c) {
		// TODO load state from disk 

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
		curi.getAList().setInt("distance-from-seed",0);
	}

	/**
	 * 
	 */
	public void wakeReadyQueues(long now) {
		while(((URIStoreable)snoozeQueues.first()).getWakeTime()<now) {
			URIStoreable awoken = (URIStoreable)snoozeQueues.first();
			snoozeQueues.remove(awoken);
			if (awoken instanceof KeyedQueue) {
				assert inProcessMap.get(awoken.getClassKey()) == null : "false ready: class peer still in process";
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
		CrawlURI readyCuri = (CrawlURI) firstReadyQueue.getFirst();
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
		heldClassQueues.add(classQueue);
		classQueue.setStoreState(URIStoreable.HELD);
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
	 * @param curi
	 * @return
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

}
