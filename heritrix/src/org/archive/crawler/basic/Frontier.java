/*
 * SimpleFrontier.java
 * Created on Oct 1, 2003
 *
 * $Header$
 */
package org.archive.crawler.basic;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.archive.crawler.datamodel.CandidateURI;
import org.archive.crawler.datamodel.CoreAttributeConstants;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.FetchStatusCodes;
import org.archive.crawler.datamodel.UURI;
import org.archive.crawler.datamodel.UURISet;
import org.archive.crawler.framework.CrawlController;
import org.archive.crawler.framework.CrawlListener;
import org.archive.crawler.framework.URIFrontier;
import org.archive.crawler.framework.XMLConfig;
import org.archive.crawler.framework.exceptions.FatalConfigurationException;
import org.archive.crawler.util.FPUURISet;
import org.archive.util.CachingDiskLongFPSet;
import org.archive.util.DiskBackedQueue;
import org.archive.util.DiskLongFPSet;
import org.archive.util.MemLongFPSet;
import org.archive.util.MemQueue;
import org.archive.util.Queue;

/**
 * A basic mostly breadth-first frontier, which refrains from 
 * emitting more than one CrawlURI of the same 'key' (host) at 
 * once, and respects minimum-delay and delay-factor specifications 
 * for politeness
 * 
 * @author gojomo
 *
 */
public class Frontier
	extends XMLConfig 
	implements URIFrontier, FetchStatusCodes, CoreAttributeConstants, CrawlListener {
	private static final int DEFAULT_CLASS_QUEUE_MEMORY_HEAD = 200;
	private static String XP_DELAY_FACTOR = "@delay-factor";
	private static String XP_MIN_DELAY = "@min-delay-ms";
	private static String XP_MAX_DELAY = "@max-delay-ms";
	private static int DEFAULT_DELAY_FACTOR = 5;
	private static int DEFAULT_MIN_DELAY = 1000;
	private static int DEFAULT_MAX_DELAY = 5000;

	private static Logger logger =
		Logger.getLogger("org.archive.crawler.basic.Frontier");
	CrawlController controller;
	
	// those UURIs which are already in-process (or processed), and
	// thus should not be rescheduled	
	UURISet alreadyIncluded;
	
	private ThreadLocalQueue threadWaiting = new ThreadLocalQueue();
	private ThreadLocalQueue threadWaitingHigh = new ThreadLocalQueue();

	// every CandidateURI not yet in process or another queue; 
	// all seeds start here; may contain duplicates
	Queue pendingQueue; // of CandidateURIs 
	
	// every CandidateURI not yet in process or another queue; 
	// all seeds start here; may contain duplicates
	Queue pendingHighQueue; // of CandidateURIs 

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

    // limits on retries TODO: separate into retryPolicy? 
	private int maxRetries = 3;
	private int retryDelay = 15000;
	private long minDelay;
	private long delayFactor;
	private long maxDelay;

	// top-level stats
	long completionCount = 0;
	long failedCount = 0;
	// increments for every URI ever queued up (even dups)
	long totalUrisScheduled = 0;
	// increments for every URI ever queued up (even dups); decrements when retired
	long netUrisScheduled = 0; 
	// increments for every URI that turned out to be alreadyIncluded after queuing
	// scheduledDuplicates/totalUrisScheduled is running estimate of rate to discount pendingQueues
	long scheduledDuplicates = 0;


	/**
	 * Initializes the Frontier, given the supplied CrawlController. 
	 * 
	 * @see org.archive.crawler.framework.URIFrontier#initialize(org.archive.crawler.framework.CrawlController)
	 */
	public void initialize(CrawlController c)
		throws FatalConfigurationException, IOException {
		
		delayFactor = getIntAt(XP_DELAY_FACTOR,DEFAULT_DELAY_FACTOR);
		minDelay = getIntAt(XP_MIN_DELAY,DEFAULT_MIN_DELAY);
		maxDelay = getIntAt(XP_MAX_DELAY,DEFAULT_MAX_DELAY);
		
		pendingQueue = new DiskBackedQueue(c.getScratchDisk(),"pendingQ",10000);
	    pendingHighQueue = new DiskBackedQueue(c.getScratchDisk(),"pendingHighQ",10000);
		
		
		alreadyIncluded = new FPUURISet(new MemLongFPSet(20,0.75f));
		
		// alternative: pure disk-based set 
//		alreadyIncluded = new FPUURISet(new DiskLongFPSet(c.getScratchDisk(),"alreadyIncluded",3,0.5f));

		// alternative: disk-based set with in-memory cache supporting quick positive contains() checks
//		alreadyIncluded = new FPUURISet(
//			new CachingDiskLongFPSet(
//				c.getScratchDisk(),
//				"alreadyIncluded",
//				23, // 8 million slots on disk (for start)
//				0.75f,
//				20, // 1 million slots in cache (always)
//				0.75f));
		
		this.controller = c;
		controller.addListener(this);
		Iterator iter = c.getScope().getSeedsIterator();
		while (iter.hasNext()) {
			UURI u = (UURI) iter.next();
			CandidateURI caUri = new CandidateURI(u);
			caUri.setIsSeed(true);
			schedule(caUri);
		}
	}

	private static class ThreadLocalQueue extends ThreadLocal {
		/* (non-Javadoc)
		 * @see java.lang.ThreadLocal#initialValue()
		 */
		protected Object initialValue() {
			return new MemQueue();
		}

		/**
		 * 
		 */
		public Queue getQueue() {
			return (Queue)super.get();
		}

}
	
	
	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.URIFrontier#batchSchedule(org.archive.crawler.datamodel.CandidateURI)
	 */
	public void batchSchedule(CandidateURI caUri) {
		// initially just pass-through
		// schedule(caUri);
		threadWaiting.getQueue().enqueue(caUri);
	}



	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.URIFrontier#batchScheduleHigh(org.archive.crawler.datamodel.CandidateURI)
	 */
	public void batchScheduleHigh(CandidateURI caUri) {
		// initially just pass-through
		//scheduleHigh(caUri);
		threadWaitingHigh.getQueue().enqueue(caUri);
	}



	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.URIFrontier#batchFlush()
	 */
	public synchronized void batchFlush() {
		Queue q = threadWaiting.getQueue();
		while(!q.isEmpty()) {
			schedule((CandidateURI) q.dequeue());
		}
		q = threadWaitingHigh.getQueue();
		while(!q.isEmpty()) {
			scheduleHigh((CandidateURI) q.dequeue());
		}
	}


	/** 
	 * Arrange for the given CandidateURI to be visited, if it is not
	 * already scheduled/completed. 
	 * 
	 * @see org.archive.crawler.framework.URIFrontier#schedule(org.archive.crawler.datamodel.CandidateURI)
	 */
	public synchronized void schedule(CandidateURI caUri) {
		if(alreadyIncluded.quickContains(caUri)) {
			logger.finer("Disregarding alreadyIncluded "+caUri);
			return;
		}
		pendingQueue.enqueue(caUri);
		incrementScheduled();
	}
	
	/** 
	 * Arrange for the given CandidateURI to be visited, with top
	 * priority (before anything else), if it is not already 
	 * scheduled/completed. 
	 * 
	 * @see org.archive.crawler.framework.URIFrontier#scheduleHigh(org.archive.crawler.datamodel.CandidateURI)
	 */
	public synchronized void scheduleHigh(CandidateURI caUri) {
		if(alreadyIncluded.quickContains(caUri)) {
			logger.finer("Disregarding alreadyIncluded "+caUri);
			return;
		}
		pendingHighQueue.enqueue(caUri);
		incrementScheduled();
	}

	/**
	 * 
	 */
	private void incrementScheduled() {
		totalUrisScheduled++;
		netUrisScheduled++;
	}

	/** 
	 * Return the next CrawlURI to be processed (and presumably
	 * visited/fetched) by a a worker thread. 
	 * 
	 * First checks the global pendingHigh queue, then any "Ready"
	 * per-host queues, then the global pending queue. 
	 * 
	 * @see org.archive.crawler.framework.URIFrontier#next(int)
	 */
	public synchronized CrawlURI next(int timeout) {
		
		long now = System.currentTimeMillis();
		long waitMax = 0;
		CrawlURI curi = null;

		// first, empty the high-priority queue
		CandidateURI caUri; 
		while ((caUri = dequeueFromPendingHigh()) != null) {
			if( caUri instanceof CrawlURI ) {
				curi = (CrawlURI) caUri;
			} else {
				if (alreadyIncluded.contains(caUri)) {
					// TODO: potentially up-prioritize URI
					logger.finer("Disregarding alreadyContained "+caUri);
					noteScheduledDuplicate();
					continue;
				}
				logger.finer("Scheduling "+caUri);
				alreadyIncluded.add(caUri);
				curi = new CrawlURI(caUri);
			}
			if (!enqueueIfNecessary(curi)) {
				// OK to emit
				return emitCuri(curi);
			}
		} // if reached, the pendingHighQueue is empty

		// if enough time has passed to wake any snoozing queues, do it
		wakeReadyQueues(now);
		
		// now, see if any holding queues are ready with a CrawlURI
		if (!readyClassQueues.isEmpty()) {
			curi = dequeueFromReady();
			return emitCuri(curi);
		}
		
		// if that fails, check the pending queue
		while ((caUri = dequeueFromPending()) != null) {
			if( caUri instanceof CrawlURI ) {
				curi = (CrawlURI) caUri;
			} else {
				if (alreadyIncluded.contains(caUri)) {
					logger.finer("Disregarding alreadyContained "+caUri);
					noteScheduledDuplicate();
					continue;
				}
				logger.finer("Scheduling "+caUri);
				alreadyIncluded.add(caUri);
				curi = new CrawlURI(caUri);
			}
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
			if(waitMax<1) {
				// ignore
				// logger.warning("negative or zero wait "+waitMax+" ignored");
			} else {
				synchronized(this) {
					wait(waitMax);
				}
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Note that a URI that was queued up turned out to be a 
	 * duplicate. 
	 */
	private void noteScheduledDuplicate() {
		netUrisScheduled--;
		scheduledDuplicates++;
	}

	/**
	 * Note that the previously emitted CrawlURI has completed 
	 * its processing (for now). 
	 * 
	 * The CrawlURI may be scheduled to retry, if appropriate,
	 * and other related URIs may become eligible for release
	 * via the next next() call, as a result of finished(). 
	 * 
	 *  (non-Javadoc)
	 * @see org.archive.crawler.framework.URIFrontier#finished(org.archive.crawler.datamodel.CrawlURI)
	 */
	public synchronized void finished(CrawlURI curi) {
		logger.fine(this+".finished("+curi+")");
		
		// catch up on scheduling
		batchFlush();
		
		curi.incrementFetchAttempts();
		
		try {
			noteProcessingDone(curi);
			// snooze queues as necessary
			updateScheduling(curi);
			notify(); // new items might be available
			
			logLocalizedErrors(curi);
			
			// consider errors which halt further processing
			if (isDispositiveFailure(curi)) {
				failureDisposition(curi);
				return;
			}
				
			// consider errors which can be retried
			if (needsRetrying(curi)) {
				scheduleForRetry(curi);
				return;
			}
								
			// SUCCESS: note & log
			successDisposition(curi);
		} catch (RuntimeException e) {
			curi.setFetchStatus(S_INTERNAL_ERROR);
			// store exception temporarily for logging
			curi.getAList().putObject(A_RUNTIME_EXCEPTION,(Object)e);
			failureDisposition(curi);
		}	
	} 
			
	/**
	 * Take note of any processor-local errors that have
	 * been entered into the CrawlURI. 
	 * 
	 */
	private void logLocalizedErrors(CrawlURI curi) {
		if(curi.getAList().containsKey(A_LOCALIZED_ERRORS)) {
			List localErrors = (List)curi.getAList().getObject(A_LOCALIZED_ERRORS);
			Iterator iter = localErrors.iterator();
			while(iter.hasNext()) {
				Object array[] = { curi, iter.next() };
				controller.localErrors.log(
					Level.WARNING,
					curi.getUURI().getUriString(),
					array);
			}
			// once logged, discard
			curi.getAList().remove(A_LOCALIZED_ERRORS);
		}
	}

	/**
	 * The CrawlURI has been successfully crawled, and will be
	 * attempted no more. 
	 * 
	 * @param curi
	 */
	protected void successDisposition(CrawlURI curi) {
		completionCount++;
		if ( (completionCount % 500) == 0) {
			logger.info("==========> " +
				completionCount+" <========== HTTP URIs completed");
		}
				
		Object array[] = { curi };
		controller.uriProcessing.log(
			Level.INFO,
			curi.getUURI().getUriString(),
			array);
		
		// note that CURI has passed out of scheduling
		if (shouldBeForgotten(curi)) {
			// curi is dismissed without prejudice: it can be reconstituted
			forget(curi);
		} else {
			curi.setStoreState(URIStoreable.FINISHED);
			if (curi.getDontRetryBefore() < 0) {
				// if not otherwise set, retire this URI forever
				curi.setDontRetryBefore(Long.MAX_VALUE);
			}
		}
		curi.stripToMinimal();
		decrementScheduled();
	}



	/**
	 * Store is empty only if all queues are empty and 
	 * no URIs are in-process
	 * 
	 * @return
	 */
	public boolean isEmpty() {
		return pendingQueue.isEmpty()
			    && pendingHighQueue.isEmpty()
				&& readyClassQueues.isEmpty()
				&& heldClassQueues.isEmpty() 
				&& snoozeQueues.isEmpty()
				&& inProcessMap.isEmpty();
	}	
	
	
	/**
	 * Wake any snoozed queues whose snooze time is up.
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

	private void discardQueue(URIStoreable q) {
		allClassQueuesMap.remove(((KeyedQueue)q).getClassKey());
		q.setStoreState(URIStoreable.FINISHED);
		((KeyedQueue)q).release();
		assert !heldClassQueues.contains(q) : "heldClassQueues holding dead q";
		assert !readyClassQueues.contains(q) : "readyClassQueues holding dead q";
		assert !snoozeQueues.contains(q) : "snoozeQueues holding dead q";
		//assert heldClassQueues.size()+readyClassQueues.size()+snoozeQueues.size() <= allClassQueuesMap.size() : "allClassQueuesMap discrepancy";
	}
	
	/**
	 * @return
	 */
	private CrawlURI dequeueFromReady() {
		KeyedQueue firstReadyQueue = (KeyedQueue)readyClassQueues.getFirst();
		CrawlURI readyCuri = (CrawlURI) firstReadyQueue.dequeue();
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
			curi.setServer(controller.getServerCache().getServerFor(curi));
		}
		logger.fine(this+".emitCuri("+curi+")");
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
	protected void reinsert(CrawlURI curi) {

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
	protected CandidateURI dequeueFromPendingHigh() {
		if (pendingHighQueue.isEmpty()) {
			return null;
		}
		return (CandidateURI)pendingHighQueue.dequeue();
	}
	/**
	 * 
	 */
	protected CandidateURI dequeueFromPending() {
		if (pendingQueue.isEmpty()) {
			return null;
		}
		return (CandidateURI)pendingQueue.dequeue();
	}

	/**
	 * 
	 * @param curi
	 * @return true if enqueued
	 */
	protected boolean enqueueIfNecessary(CrawlURI curi) {
		KeyedQueue classQueue = (KeyedQueue) allClassQueuesMap.get(curi.getClassKey());
		if (classQueue != null) {
			// must enqueue
			classQueue.enqueue(curi);
			curi.setStoreState(classQueue.getStoreState());
			return true;
		}
		CrawlURI classmateInProgress = (CrawlURI) inProcessMap.get(curi.getClassKey());
		if (classmateInProgress != null) {
			// must create queue, and enqueue
			classQueue = new KeyedQueue(curi.getClassKey(),controller.getScratchDisk(),DEFAULT_CLASS_QUEUE_MEMORY_HEAD);
			allClassQueuesMap.put(classQueue.getClassKey(), classQueue);
			enqueueToHeld(classQueue);
			classQueue.enqueue(curi);
			curi.setStoreState(classQueue.getStoreState());
			return true;
		}
		
		return false;	
	}

	/**
	 * @return
	 */
	protected long earliestWakeTime() {
		if (!snoozeQueues.isEmpty()) {
			return ((URIStoreable)snoozeQueues.first()).getWakeTime();
		}
		return Long.MAX_VALUE;
	}

	/**
	 * @param curi
	 */
	private synchronized void pushToPending(CrawlURI curi) {
		pendingHighQueue.enqueue(curi);
		curi.setStoreState(URIStoreable.PENDING);
	}
	
	/**
	 * 
	 * @return
	 */
	protected void noteProcessingDone(CrawlURI curi) {
		assert inProcessMap.get(curi.getClassKey())
			== curi : "CrawlURI returned not in process";

		inProcessMap.remove(curi.getClassKey());

		KeyedQueue classQueue =
			(KeyedQueue) allClassQueuesMap.get(curi.getClassKey());
		if (classQueue == null) {
			return;
		}
		assert classQueue.getStoreState()
			== URIStoreable.HELD : "odd state for classQueue of remitted CrawlURI";
		heldClassQueues.remove(classQueue);
		if (classQueue.isEmpty()) {
			// just drop it
			discardQueue(classQueue);
			return;
		}
		readyClassQueues.add(classQueue);
		classQueue.setStoreState(URIStoreable.READY);
		// TODO: since usually, queue will be snoozed, this juggling is often superfluous
	}

	/**
	 * Update any scheduling structures with the new information
	 * in this CrawlURI. Chiefly means make necessary arrangements
	 * for no other URIs at the same host to be visited within the
	 * appropriate politeness window. 
	 * 
	 * @param curi
	 */
	protected void updateScheduling(CrawlURI curi) {
		long durationToWait = 0;
		if (curi.getAList().containsKey(A_FETCH_BEGAN_TIME)
			&& curi.getAList().containsKey(A_FETCH_COMPLETED_TIME)) {
				
			long completeTime = curi.getAList().getLong(A_FETCH_COMPLETED_TIME);
			durationToWait =
				delayFactor
					* (completeTime
						- curi.getAList().getLong(A_FETCH_BEGAN_TIME));

			if (minDelay > durationToWait) {
				durationToWait = minDelay;
			}
			if (durationToWait > maxDelay) {
				durationToWait = maxDelay;
			}
			
			if(durationToWait>0) {
				snoozeQueueUntil(curi.getClassKey(), completeTime + durationToWait);
			} 
		}
	}
	
	/**
	 * The CrawlURI has encountered a problem, and will not
	 * be retried. 
	 * 
	 * @param curi
	 */
	protected void failureDisposition(CrawlURI curi) {

		failedCount++;

		// send to basic log 
		Object array[] = { curi };
		controller.uriProcessing.log(
			Level.INFO,
			curi.getUURI().getUriString(),
			array);

		// if exception, also send to crawlErrors
		if (curi.getFetchStatus() == S_INTERNAL_ERROR) {
			controller.runtimeErrors.log(
				Level.WARNING,
				curi.getUURI().getUriString(),
				array);
		}
		if (shouldBeForgotten(curi)) {
			// curi is dismissed without prejudice: it can be reconstituted
			forget(curi);
		} else {
			curi.setStoreState(URIStoreable.FINISHED);
			if (curi.getDontRetryBefore() < 0) {
				// if not otherwise set, retire this URI forever
				curi.setDontRetryBefore(Long.MAX_VALUE);
			}
			curi.stripToMinimal();
		}
		decrementScheduled();
	}

	/**
	 * 
	 */
	private void decrementScheduled() {
		netUrisScheduled--;
	}

	/**
	 * Has the CrawlURI suffered a failure which completes
	 * its processing?
	 * 
	 * @param curi
	 * @return
	 */
	private boolean isDispositiveFailure(CrawlURI curi) {
		switch (curi.getFetchStatus()) {

			case S_DOMAIN_UNRESOLVABLE :
				// network errors; perhaps some of these 
				// should be scheduled for retries
			case S_INTERNAL_ERROR :
				// something unexpectedly bad happened
			case S_UNFETCHABLE_URI :
				// no chance to fetch

// THESE NEXT FOUR AREN'T TRULY FAILURES
//			case S_ROBOTS_PRECLUDED :
//				// they don't want us to have it	
//			case S_OUT_OF_SCOPE :
//				// filtered out
//			case S_TOO_MANY_EMBED_HOPS :
//				// too far from last true link
//			case S_TOO_MANY_LINK_HOPS :
//				// too far from seeds
				return true;

			case S_UNATTEMPTED :
				// this uri is virgin, let it carry on
			default :
				return false;
		}
	}
	
	/**
	 * @param curi
	 * @return
	 */
	private boolean needsRetrying(CrawlURI curi) {
		//
		if (curi.getFetchAttempts()>=maxRetries) {
			return false;
		}
		switch (curi.getFetchStatus()) {
			case S_CONNECT_FAILED:					
			case S_CONNECT_LOST:
			case S_UNATTEMPTED:
			case S_TIMEOUT:
				// these are all worth a retry
				return true;
			default:
				return false;
		}
	}

	/**
	 * @param curi
	 */
	private void scheduleForRetry(CrawlURI curi) {
		int delay;
		if(curi.getAList().containsKey(A_RETRY_DELAY)) {
			delay = curi.getAList().getInt(A_RETRY_DELAY);
		} else {
			// use overall default
			delay = retryDelay; 
		}
		if (delay>0) {
			// snooze to future
			logger.fine("inserting snoozed "+curi+" for "+delay);
			insertSnoozed(curi,retryDelay);
		} else {
			// eligible for retry asap
			pushToPending(curi);
		}
	}
	
	/**
	 * @param object
	 * @param l
	 */
	protected void snoozeQueueUntil(Object classKey, long wake) {
		KeyedQueue classQueue = (KeyedQueue) allClassQueuesMap.get(classKey);
		if ( classQueue == null ) {
			classQueue = new KeyedQueue(classKey,controller.getScratchDisk(),DEFAULT_CLASS_QUEUE_MEMORY_HEAD);
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
	 * Some URIs, if they recur,  deserve another
	 * chance at consideration: they might not be too
	 * many hops away via another path, or the scope
	 * may have been updated to allow them passage.
	 * 
	 * @param curi
	 * @return
	 */
	private boolean shouldBeForgotten(CrawlURI curi) {
		switch(curi.getFetchStatus()) {
			case S_OUT_OF_SCOPE:
			case S_TOO_MANY_EMBED_HOPS:
			case S_TOO_MANY_LINK_HOPS:
				return true;
			default:
				return false;
		}
	}

	/**
	 * Forget the given CrawlURI. This allows a new instance
	 * to be created in the future, if it is reencountered under 
	 * different circumstances. 
	 * 
	 * @param curi
	 */
	protected void forget(CrawlURI curi) {
		logger.finer("Forgetting "+curi);
		alreadyIncluded.remove(curi.getUURI());
		curi.setStoreState(URIStoreable.FORGOTTEN);
	}

	/**
	 * Revisit the CrawlURI -- but not before delay time has passed.
	 * @param curi
	 * @param retryDelay
	 */
	protected void insertSnoozed(CrawlURI curi, long retryDelay) {
		curi.setWakeTime(System.currentTimeMillis()+retryDelay );
		curi.setStoreState(URIStoreable.SNOOZED);
		snoozeQueues.add(curi);
	}

	/** Return the number of URIs successfully completed to date.
	 * 
	 * @return
	 */
	public long successfullyFetchedCount(){
		return completionCount;
	}
	
	/** Return the number of URIs that failed to date.
	 * 
	 * @return
	 */
	public long failedFetchCount(){
		return failedCount;
	}
	
	/** Return the size of the URI store.
	 * @return storeSize
	 */
	public long discoveredUriCount(){
		return alreadyIncluded.size();	
	}

	/** 
	 * Estimate of the number of URIs waiting to be fetched.
	 * scheduled
	 * 
	 * @see org.archive.crawler.framework.URIFrontier#pendingUriCount()
	 */
	public long pendingUriCount() {
		float duplicatesInPendingEstimate = (totalUrisScheduled == 0) ? 0 : scheduledDuplicates / totalUrisScheduled;
		return netUrisScheduled - (long)(alreadyIncluded.count() * duplicatesInPendingEstimate );
	}



	/** 
	 *  The crawl controller invokes this method when it is exiting.
	 *  The frontier should do cleanup to facilitate gc.
	 *  No other object then the crawl controller should invoke this method. 
	 */
	public void crawlEnding(String sExitMessage) {
		controller = null;			
	}

}
