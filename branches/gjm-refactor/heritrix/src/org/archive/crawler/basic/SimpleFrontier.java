/*
 * SimpleFrontier.java
 * Created on Oct 1, 2003
 *
 * $Header$
 */
package org.archive.crawler.basic;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Logger;

import org.archive.crawler.datamodel.CandidateURI;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.FatalConfigurationException;
import org.archive.crawler.datamodel.MemUURISet;
import org.archive.crawler.datamodel.UURI;
import org.archive.crawler.datamodel.UURISet;
import org.archive.crawler.framework.CrawlController;
import org.archive.crawler.framework.URIFrontier;

/**
 * A basic in-memory mostly breadth-first frontier, which 
 * refrains from emitting more than one CrawlURI of the same 
 * 'key' (host) at once, and respects minimum-delay and 
 * delay-factor specifications for politeness
 * 
 * @author gojomo
 *
 */
public class SimpleFrontier implements URIFrontier {
	private static Logger logger = Logger.getLogger("org.archive.crawler.basic.SimpleFrontier");
	CrawlController controller;
	
	// HashMap allCuris = new HashMap(); // of UURI -> CrawlURI 
	
	// TODO update to use fingerprints only
	UURISet alreadyIncluded = new MemUURISet();
	
	// every CandidateURI not yet in process or another queue; 
	// all seeds start here; may contain duplicates
	LinkedList pendingQueue = new LinkedList(); // of CandidateURIs 

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

	// CrawlURIs held until some specific other CrawlURI is emitted
	HashMap heldCuris = new HashMap(); // of UURI -> CrawlURI




	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.URIFrontier#initialize(org.archive.crawler.framework.CrawlController)
	 */
	public void initialize(CrawlController c)
		throws FatalConfigurationException {
			
		this.controller = c;
		Iterator iter = c.getScope().getSeeds().iterator();
		while (iter.hasNext()) {
			UURI u = (UURI) iter.next();
			CandidateURI caUri = new CandidateURI(u);
			caUri.setIsSeed(true);
			schedule(caUri);
		}

	}

	/** 
	 * 
	 * @see org.archive.crawler.framework.URIFrontier#schedule(org.archive.crawler.datamodel.CandidateURI)
	 */
	public synchronized void schedule(CandidateURI caUri) {
		pendingQueue.addLast(caUri);
	}

	/** 
	 * 
	 * @see org.archive.crawler.framework.URIFrontier#next(int)
	 */
	public CrawlURI next(int timeout) {
		
		long now = System.currentTimeMillis();
		long waitMax = 0;
		CrawlURI curi = null;

		// if enough time has passed to wake any snoozing queues, do it
		wakeReadyQueues(now);
		
		// first, see if any holding queues are ready with a CrawlURI
		if (!readyClassQueues.isEmpty()) {
			curi = dequeueFromReady();
			return emitCuri(curi);
		}
		
		// if that fails, check the pending queue
		CandidateURI caUri; 
		while ((caUri = dequeueFromPending()) != null) {
			if(alreadyIncluded.contains(caUri)) {
				continue;
			}
			curi = new CrawlURI(caUri);
			if (!enqueueIfNecessary(curi)) {
				// OK to emit
				return emitCuri(curi);
			}
		}
		
		// consider if URIs exhausted
		if(isEmpty()) {
			// nothing left to crawl
			logger.info("nothing left to crawl");
			// TODO halt/spread the word???
			return null;
		}
		
		// nothing to return, but there are still URIs
		// held for the future
		
		// block until something changes, or timeout occurs
		waitMax = Math.min(earliestWakeTime()-now,timeout);
		try {
			if(waitMax<0) {
				logger.warning("negative wait "+waitMax+" ignored");
			} else {
				wait(waitMax);
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.URIFrontier#finished(org.archive.crawler.datamodel.CrawlURI)
	 */
	public void finished(CrawlURI curi) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.URIFrontier#isEmpty()
	 */
	public boolean isEmpty() {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.URIFrontier#size()
	 */
	public long size() {
		// TODO Auto-generated method stub
		return 0;
	}
	
	
	
	/**
	 * 
	 */
	protected void wakeReadyQueues(long now) {
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
				pushToPending((CrawlURI)awoken);
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
	private CrawlURI dequeueFromReady() {
		KeyedQueue firstReadyQueue = (KeyedQueue)readyClassQueues.getFirst();
		CrawlURI readyCuri = (CrawlURI) firstReadyQueue.removeFirst();
		return readyCuri;
	}

	/**
	 * @param crawlURI
	 * @return
	 */
	private CrawlURI emitCuri(CrawlURI curi) {
		if(curi != null) {
			if (curi.getStoreState() == URIStoreable.FINISHED) {
				System.out.println("break here");
			}
			assert curi.getStoreState() != URIStoreable.FINISHED : "state "+curi.getStoreState()+" instead of ready for "+ curi; 
			//assert curi.getAList() != null : "null alist in curi " + curi + " state "+ curi.getStoreState();
			noteInProcess(curi);
			curi.setServer(controller.getHostCache().getServerFor(curi));
		}
		return curi;
	}

	/**
	 * @param curi
	 */
	protected void noteInProcess(CrawlURI curi) {
		assert inProcessMap.get(curi.getClassKey()) == null : "two CrawlURIs with same classKey in process";
		
		inProcessMap.put(curi.getClassKey(), curi);
		curi.setStoreState(URIStoreable.IN_PROCESS);
		
		KeyedQueue classQueue = (KeyedQueue) allClassQueuesMap.get(curi.getClassKey());
		if (classQueue == null) {
			releaseHeld(curi); 
			return;
		}
		assert classQueue.getStoreState() == URIStoreable.READY : "odd state "+ classQueue.getStoreState() + " for classQueue "+ classQueue + "of to-be-emitted CrawlURI";
		readyClassQueues.remove(classQueue);
		enqueueToHeld(classQueue);
		releaseHeld(curi);
	}

	/**
	 * @param classQueue
	 */
	private void enqueueToHeld(KeyedQueue classQueue) {
		heldClassQueues.add(classQueue);
		classQueue.setStoreState(URIStoreable.HELD);
	}

	/**
	 * @param curi
	 */
	private void releaseHeld(CrawlURI curi) {
		CrawlURI released = (CrawlURI) heldCuris.get(curi.getUURI());
		if(released!=null) {
			heldCuris.remove(curi.getUURI());
			reinsert(released);
		}
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
	 * 
	 */
	protected synchronized CandidateURI dequeueFromPending() {
		if (pendingQueue.isEmpty()) {
			return null;
		}
		return (CandidateURI)pendingQueue.removeFirst();
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
	 * @return
	 */
	public long earliestWakeTime() {
		if (!snoozeQueues.isEmpty()) {
			return ((URIStoreable)snoozeQueues.first()).getWakeTime();
		}
		return Long.MAX_VALUE;
	}

	/**
	 * @param curi
	 */
	private synchronized void pushToPending(CrawlURI curi) {
		pendingQueue.addFirst(curi);
		curi.setStoreState(URIStoreable.PENDING);
	}

	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.URIFrontier#discoveredUriCount()
	 */
	public int discoveredUriCount() {
		// TODO Auto-generated method stub
		return 0;
	}

	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.URIFrontier#successfullyFetchedCount()
	 */
	public int successfullyFetchedCount() {
		// TODO Auto-generated method stub
		return 0;
	}

	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.URIFrontier#failedFetchCount()
	 */
	public int failedFetchCount() {
		// TODO Auto-generated method stub
		return 0;
	}

}
