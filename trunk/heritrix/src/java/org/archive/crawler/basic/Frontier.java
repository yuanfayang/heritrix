/* Copyright (C) 2003 Internet Archive.
 *
 * This file is part of the Heritrix web crawler (crawler.archive.org).
 *
 * Heritrix is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * any later version.
 *
 * Heritrix is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser Public License for more details.
 *
 * You should have received a copy of the GNU Lesser Public License
 * along with Heritrix; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * SimpleFrontier.java
 * Created on Oct 1, 2003
 *
 * $Header$
 */
package org.archive.crawler.basic;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.AttributeNotFoundException;

import org.archive.crawler.datamodel.CandidateURI;
import org.archive.crawler.datamodel.CoreAttributeConstants;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.FetchStatusCodes;
import org.archive.crawler.datamodel.UURI;
import org.archive.crawler.datamodel.UURISet;
import org.archive.crawler.datamodel.settings.CrawlerModule;
import org.archive.crawler.datamodel.settings.SimpleType;
import org.archive.crawler.framework.CrawlController;
import org.archive.crawler.event.CrawlStatusListener;
import org.archive.crawler.framework.URIFrontier;
import org.archive.crawler.framework.exceptions.FatalConfigurationException;
import org.archive.crawler.util.FPUURISet;
import org.archive.util.ArchiveUtils;
import org.archive.util.DiskBackedQueue;
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
 *f
 */
public class Frontier
	extends CrawlerModule 
	implements URIFrontier, FetchStatusCodes, CoreAttributeConstants, CrawlStatusListener {


    private static final int DEFAULT_CLASS_QUEUE_MEMORY_HEAD = 200;
	// how many multiples of last fetch elapsed time to wait before recontacting same server
	private static String ATTR_DELAY_FACTOR = "delay-factor";
	// always wait this long after one completion before recontacting same server, regardless of multiple
	private static String ATTR_MIN_DELAY = "min-delay-ms";
	// never wait more than this long, regardless of multiple
	private static String ATTR_MAX_DELAY = "max-delay-ms";
	// always wait at least this long between request *starts*
	// (contrasted with min-delay: if min-interval time has already elapsed during last
	// fetch, then next fetch may occur immediately; it constrains starts not off-cycles)
	private static String ATTR_MIN_INTERVAL = "min-interval-ms";
	private static Float DEFAULT_DELAY_FACTOR = new Float(5);
	private static Integer DEFAULT_MIN_DELAY = new Integer(500);
	private static Integer DEFAULT_MAX_DELAY = new Integer(5000);
	private static Integer DEFAULT_MIN_INTERVAL = new Integer(1000);
	
	private static Logger logger =
		Logger.getLogger("org.archive.crawler.basic.Frontier");
		
	private static String F_ADD = "\nF+ ";
	private static String F_EMIT = "\nFe ";
	private static String F_RESCHEDULE = "\nFr ";
	private static String F_SUCCESS = "\nFs ";
	private static String F_FAILURE = "\nFf ";
		
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
	private int maxRetries = 30;
	private int retryDelay = 900000; // 15 minutes
//	private long minDelay;
//	private float delayFactor;
//	private long maxDelay;
//	private int minInterval;

	// top-level stats
	long completionCount = 0;
	long failedCount = 0;
	long disregardedCount = 0; //URI's that are disregarded (for example because of robot.txt rules)

	long totalProcessedBytes = 0;
	
	// increments for every URI ever queued up (even dups)
	long totalUrisScheduled = 0;
	// increments for every URI ever queued up (even dups); decrements when retired
	long netUrisScheduled = 0; 
	// increments for every URI that turned out to be alreadyIncluded after queuing
	// scheduledDuplicates/totalUrisScheduled is running estimate of rate to discount pendingQueues
	long scheduledDuplicates = 0;


    /**
     * @param name
     */
    public Frontier(String name) {
        // The frontier should always have the same name.
        // the argument to this constructor is just ignored.
        super(URIFrontier.ATTR_NAME, "Frontier");
        addElementToDefinition(new SimpleType(ATTR_DELAY_FACTOR, "How many multiples of last fetch elapsed time to wait before recontacting same server", DEFAULT_DELAY_FACTOR));
        addElementToDefinition(new SimpleType(ATTR_MAX_DELAY, "Never wait more than this long, regardless of multiple", DEFAULT_MAX_DELAY));
        addElementToDefinition(new SimpleType(ATTR_MIN_DELAY, "Always wait this long after one completion before recontacting same server, regardless of multiple", DEFAULT_MIN_DELAY));
        addElementToDefinition(new SimpleType(ATTR_MIN_INTERVAL, "always wait at least this long between request *starts* (contrasted with min-delay: if min-interval time has already elapsed during last fetch, then next fetch may occur immediately; it constrains starts not off-cycles)", DEFAULT_MIN_INTERVAL));
    }
    
    public Frontier() {
        this(null);
    }

	/**
	 * Initializes the Frontier, given the supplied CrawlController. 
	 * 
	 * @see org.archive.crawler.framework.URIFrontier#initialize(org.archive.crawler.framework.CrawlController)
	 */
	public void initialize(CrawlController c)
		throws FatalConfigurationException, IOException {
		
//		delayFactor = getFloatAt(XP_DELAY_FACTOR,DEFAULT_DELAY_FACTOR);
//		minDelay = getIntAt(XP_MIN_DELAY,DEFAULT_MIN_DELAY);
//		maxDelay = getIntAt(XP_MAX_DELAY,DEFAULT_MAX_DELAY);
//		minInterval = getIntAt(XP_MIN_INTERVAL,DEFAULT_MIN_INTERVAL);
		
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
		controller.addCrawlStatusListener(this);
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
		 * @return Queue
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
		Queue q = threadWaitingHigh.getQueue();
		while(!q.isEmpty()) {
			scheduleHigh((CandidateURI) q.dequeue());
		}
		q = threadWaiting.getQueue();
		while(!q.isEmpty()) {
			schedule((CandidateURI) q.dequeue());
		}
	}


	/** 
	 * Arrange for the given CandidateURI to be visited, if it is not
	 * already scheduled/completed. 
	 * 
	 * @see org.archive.crawler.framework.URIFrontier#schedule(org.archive.crawler.datamodel.CandidateURI)
	 */
	public synchronized void schedule(CandidateURI caUri) {
		if(!caUri.forceFetch() && alreadyIncluded.quickContains(caUri)) {
			logger.finer("Disregarding alreadyIncluded "+caUri);
			return;
		}
		pendingQueue.enqueue(caUri);
		incrementScheduled();
		controller.recover.info(F_ADD+caUri.getURIString());
	}
	
	/** 
	 * Arrange for the given CandidateURI to be visited, with top
	 * priority (before anything else), if it is not already 
	 * scheduled/completed. 
	 * 
	 * @see org.archive.crawler.framework.URIFrontier#scheduleHigh(org.archive.crawler.datamodel.CandidateURI)
	 */
	public synchronized void scheduleHigh(CandidateURI caUri) {
		if(!caUri.forceFetch() && alreadyIncluded.quickContains(caUri)) {
			logger.finer("Disregarding alreadyIncluded "+caUri);
			return;
		}
		pendingHighQueue.enqueue(caUri);
		incrementScheduled();
		controller.recover.info(F_ADD+caUri.getURIString());
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
				if (!caUri.forceFetch() && alreadyIncluded.contains(caUri)) {
					// TODO: potentially up-prioritize URI
					logger.finer("Disregarding alreadyContained "+caUri);
					noteScheduledDuplicate();
					continue;
				}
				logger.finer("Scheduling "+caUri);
				alreadyIncluded.add(caUri);
				curi = new CrawlURI(caUri);
			}
			
			// If URI should be forced it has to be done
			// before everything that might be in a queue.
			// Since this only aplies to refetching expired robots.txt
			// and dns lookups it should be ok to be impolite.
			if (caUri.forceFetch()) {
				return emitCuri(curi);
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
				if (!caUri.forceFetch() && alreadyIncluded.contains(caUri)) {
					logger.finer("Disregarding alreadyContained "+caUri);
					noteScheduledDuplicate();
					continue;
				}
				logger.finer("Scheduling "+caUri);
				alreadyIncluded.add(caUri);
				curi = new CrawlURI(caUri);
			}
			
			// If URI should be forced it has to be done
			// before everything that might be in a queue.
			// Since this only aplies to refetching expired robots.txt
			// and dns lookups it should be ok to be impolite.
			if (caUri.forceFetch()) {
				return emitCuri(curi);
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
		logger.fine("Frontier.finished: "+curi.getURIString());
		
		// catch up on scheduling
		batchFlush();
		
		curi.incrementFetchAttempts();
		
		try {
			noteProcessingDone(curi);
			// snooze queues as necessary
			updateScheduling(curi);
			notify(); // new items might be available
			
			logLocalizedErrors(curi);
			
			// Regard any status larger then 0 as meaning success.
			if(curi.getFetchStatus() > 0){
				// SUCCESS: note & log
				successDisposition(curi);
				return;
			}
			
			// consider errors which can be retried
			if (needsRetrying(curi)) {
				scheduleForRetry(curi);
				return;
			}

			// Check for codes that mean that while we the crawler did manage to get it it 
			// must be disregarded for any reason.			
			if(isDisregarded(curi)){
				disregardDisposition(curi);
				return;
			}
					
			// In that case FAILURE, note & log
			failureDisposition(curi);
		} catch (RuntimeException e) {
			curi.setFetchStatus(S_RUNTIME_EXCEPTION);
			// store exception temporarily for logging
			curi.getAList().putObject(A_RUNTIME_EXCEPTION,(Object)e);
			failureDisposition(curi);
		} catch (AttributeNotFoundException e) {
            logger.severe(e.getMessage());
        }	
	} 
			
	/**
	 * @param curi
	 */
	private void disregardDisposition(CrawlURI curi) {
		disregardedCount++;

		// release any other curis that were waiting for this to finish
		releaseHeld(curi);	
		
		controller.throwCrawledURIFailureEvent(curi); //Let interested listeners know of disregard disposition.

		// send to basic log 
		Object array[] = { curi };
		controller.uriProcessing.log(
			Level.INFO,
			curi.getUURI().getUriString(),
			array);

		// if exception, also send to crawlErrors
		if (curi.getFetchStatus() == S_RUNTIME_EXCEPTION) {
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

	private boolean isDisregarded(CrawlURI curi) {
		switch (curi.getFetchStatus()) {
			case S_ROBOTS_PRECLUDED :
				 // they don't want us to have it	
			case S_OUT_OF_SCOPE :
				 // filtered out
			case S_TOO_MANY_EMBED_HOPS :
				 // too far from last true link
			case S_TOO_MANY_LINK_HOPS :
				 // too far from seeds
				return true;
			default:
				return false;
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
		totalProcessedBytes += curi.getContentSize();
		
		if ( (completionCount % 500) == 0) {
			logger.info("==========> " +
				completionCount+" <========== HTTP URIs completed");
		}
		
		// release any other curis that were waiting for this to finish
		releaseHeld(curi);	
		
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
		controller.throwCrawledURISuccessfulEvent(curi); //Let everyone know in case they want to do something before we strip the curi.
		curi.stripToMinimal();
		decrementScheduled();
		controller.recover.info(F_SUCCESS+curi.getURIString());
	}



	/**
	 * Store is empty only if all queues are empty and 
	 * no URIs are in-process
	 * 
	 * @return True if queues are empty.
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
	
	private CrawlURI dequeueFromReady() {
		KeyedQueue firstReadyQueue = (KeyedQueue)readyClassQueues.getFirst();
		CrawlURI readyCuri = (CrawlURI) firstReadyQueue.dequeue();
		return readyCuri;
	}

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
		logger.finer(this+".emitCuri("+curi+")");
		controller.recover.info(F_EMIT+curi.getURIString());
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
			//releaseHeld(curi); 
			return;
		}
		assert classQueue.getStoreState() == URIStoreable.READY : "odd state "+ classQueue.getStoreState() + " for classQueue "+ classQueue + "of to-be-emitted CrawlURI";
		readyClassQueues.remove(classQueue);
		enqueueToHeld(classQueue);
		//releaseHeld(curi);
	}

	/**
	 * @param classQueue
	 */
	private void enqueueToHeld(KeyedQueue classQueue) {
		heldClassQueues.add(classQueue);
		classQueue.setStoreState(URIStoreable.HELD);
	}

	/**
	 * Defer curi until another curi with the given uuri is
	 * completed.
	 * 
	 * @param curi the curi to held
	 * @param uuri the uuri to wait for
	 */
	private void addAsHeld(CrawlURI curi, UURI uuri) {
		List heldsForUuri = (List) heldCuris.get(uuri);
		if(heldsForUuri ==null) {
			heldsForUuri = new ArrayList();
		}
		heldsForUuri.add(curi);
		heldCuris.put(uuri,heldsForUuri);
		curi.setStoreState(URIStoreable.HELD);
	}
	
	/**
	 * @param curi
	 */
	private void releaseHeld(CrawlURI curi) {
		List heldsForUuri = (List) heldCuris.get(curi.getUURI());
		if(heldsForUuri!=null) {
			heldCuris.remove(curi.getUURI());
			Iterator iter = heldsForUuri.iterator();
			while(iter.hasNext()) {
				reinsert((CrawlURI) iter.next());
			} 
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
	 * Place curi on a queue for its class (server), if either (1) such a queue
	 * already exists; or (2) another curi of the same class is in progress.
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
	 * @param curi
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
	protected void updateScheduling(CrawlURI curi) throws AttributeNotFoundException {
		long durationToWait = 0;
		if (curi.getAList().containsKey(A_FETCH_BEGAN_TIME)
			&& curi.getAList().containsKey(A_FETCH_COMPLETED_TIME)) {
				
			long completeTime = curi.getAList().getLong(A_FETCH_COMPLETED_TIME);
			long durationTaken = (completeTime - curi.getAList().getLong(A_FETCH_BEGAN_TIME));
//			durationToWait =
//				(long) (getFloatAt(XP_DELAY_FACTOR,DEFAULT_DELAY_FACTOR)
//					* durationTaken);
            durationToWait =
                    (long) (((Float) getAttribute(ATTR_DELAY_FACTOR, curi)).floatValue()
                        * durationTaken);
            

			//long minDelay = getIntAt(XP_MIN_DELAY,DEFAULT_MIN_DELAY);
            long minDelay = ((Integer) getAttribute(ATTR_MIN_DELAY, curi)).longValue();
			if (minDelay > durationToWait) {
				// wait at least the minimum
				durationToWait = minDelay;
			}
			
			//long maxDelay = getIntAt(XP_MAX_DELAY,DEFAULT_MAX_DELAY);
            long maxDelay = ((Integer) getAttribute(ATTR_MAX_DELAY, curi)).longValue();
			if (durationToWait > maxDelay) {
				// wait no more than the maximum
				durationToWait = maxDelay;
			}
			
			//long minInterval = getIntAt(XP_MIN_INTERVAL,DEFAULT_MIN_INTERVAL);
            long minInterval = ((Integer) getAttribute(ATTR_MIN_INTERVAL, curi)).longValue();
			if (durationToWait < (minInterval - durationTaken) ) {
				// wait at least as long as necessary to space off from last fetch begin
				durationToWait = minInterval - durationTaken;
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

		// release any other curis that were waiting for this to finish
		releaseHeld(curi);	
		
		controller.throwCrawledURIFailureEvent(curi); //Let interested listeners know of failed disposition.
		
		// send to basic log 
		Object array[] = { curi };
		controller.uriProcessing.log(
			Level.INFO,
			curi.getUURI().getUriString(),
			array);

		// if exception, also send to crawlErrors
		if (curi.getFetchStatus() == S_RUNTIME_EXCEPTION) {
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
		controller.recover.info(F_FAILURE+curi.getURIString());
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
	 * @return True if failure.
	 */
	private boolean isDispositiveFailure(CrawlURI curi) {
		switch (curi.getFetchStatus()) {

			case S_DOMAIN_UNRESOLVABLE :
				// network errors; perhaps some of these 
				// should be scheduled for retries
			case S_RUNTIME_EXCEPTION :
				// something unexpectedly bad happened
			case S_UNFETCHABLE_URI :
				// no chance to fetch
			case S_TOO_MANY_RETRIES :
				// no success after configurable number of retries
			case S_UNATTEMPTED :
				// nothing happened to this URI: don't send it through again

				return true;

			default :
				return false;
		}
	}
	
	/**
	 * @param curi
	 * @return True if we need to retry.
	 */
	private boolean needsRetrying(CrawlURI curi) {
		//
		if (curi.getFetchAttempts()>=maxRetries) {
			return false;
		}
		switch (curi.getFetchStatus()) {
			case S_CONNECT_FAILED:					
			case S_CONNECT_LOST:
			case S_DEFERRED: 
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
		if(curi.getAList().containsKey(A_PREREQUISITE_URI)) {
			// schedule as a function of other URI's progress
			UURI prereq = (UURI) curi.getPrerequisiteUri();
			addAsHeld(curi,prereq);
			curi.getAList().remove(A_PREREQUISITE_URI);
			controller.recover.info(F_RESCHEDULE+curi.getURIString());
			return;
		}
		if(curi.getAList().containsKey(A_RETRY_DELAY)) {
			delay = curi.getAList().getInt(A_RETRY_DELAY);
		} else {
			// use overall default
			delay = retryDelay; 
		}
		if (delay>0) {
			// snooze to future
			logger.finer("inserting snoozed "+curi+" for "+delay);
			insertSnoozed(curi,retryDelay);
		} else {
			// eligible for retry asap
			pushToPending(curi);
		}
		controller.throwCrawledURINeedRetryEvent(curi); // Let everyone interested know that it will be retried.
		controller.recover.info(F_RESCHEDULE+curi.getURIString());
	}
	
	/**
	 * Snoozes a queue until a fixed point in time has passed.
	 * 
	 * @param classKey The key (in the allClassqueuesMap) to the queue that we want to snooze
	 * @param wake Time (in millisec.) when we want the queue to stop snoozing.
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
	 * @return True if curi should be forgotten.
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
	 * @return The number of URIs successfully completed to date
	 */
	public long successfullyFetchedCount(){
		return completionCount;
	}
	
	/** 
     * @return Return the number of URIs that failed to date.
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
	 * This methods compiles a human readable report on the status of the frontier
	 * at the time of the call. 
	 * 
	 * @return A report on the current status of the frontier.
	 */
	public synchronized String report()
	{
		StringBuffer rep = new StringBuffer();
		
		rep.append("Frontier report - " + ArchiveUtils.TIMESTAMP12.format(new Date()) + "\n");
		rep.append(" Job being crawled:         " + controller.getOrder().getName() + "\n");
		rep.append("\n -----===== QUEUES =====-----\n");
		rep.append(" Pending queue length:      " + pendingQueue.length()+ "\n");
		rep.append(" Pending high queue length: " + pendingHighQueue.length() + "\n");
		rep.append("\n All class queues map size: " + allClassQueuesMap.size() + "\n");
		if(allClassQueuesMap.size()!=0)
		{
			Iterator q = allClassQueuesMap.keySet().iterator();
			int i = 1;
			while(q.hasNext())
			{
				KeyedQueue kq = (KeyedQueue)allClassQueuesMap.get(q.next());
				rep.append("   All class keyqueue " + (i++) + " - " + kq.getClassKey() + "\n");
				rep.append("     Length:   " + kq.length() + "\n");
				rep.append("     Is ready: " + kq.isReady() + "\n");
				rep.append("     Status:   " + kq.state.toString() + "\n");
			}
		}
		rep.append("\n Ready class queues size:   " + readyClassQueues.size() + "\n");
		for(int i=0 ; i < readyClassQueues.size() ; i++)
		{
			KeyedQueue kq = (KeyedQueue)readyClassQueues.get(i);			
			rep.append("   Ready class keyqueue " + (i+1) + " - " + kq.getClassKey() + "\n");
			rep.append("     Length:   " + kq.length() + "\n");
			rep.append("     Is ready: " + kq.isReady() + "\n");
			rep.append("     Status:   " + kq.state.toString() + "\n");
		}
		rep.append("\n Held class queues size:    " + heldClassQueues.size() + "\n");
		for(int i=0 ; i < heldClassQueues.size() ; i++)
		{
			KeyedQueue kq = (KeyedQueue)heldClassQueues.get(i);			
			rep.append("   Held class keyqueue " + (i+1) + " - " + kq.getClassKey() + "\n");
			rep.append("     Length:   " + kq.length() + "\n");
			rep.append("     Is ready: " + kq.isReady() + "\n");
			rep.append("     Status:   " + kq.state.toString() + "\n");
		}
		rep.append("\n Snooze queues size:        " + snoozeQueues.size() + "\n");
		if(snoozeQueues.size()!=0)
		{
			Object[] q = ((TreeSet)snoozeQueues).toArray();
			for(int i=0 ; i < q.length ; i++)
			{
				if(q[i] instanceof KeyedQueue)
				{
					KeyedQueue kq = (KeyedQueue)q[i];
					rep.append("   Snooze item " + (i+1) + " - keyqueue " + kq.getClassKey() + "\n");
					rep.append("     Length:   " + kq.length() + "\n");
					rep.append("     Is ready: " + kq.isReady() + "\n");
					rep.append("     Status:   " + kq.state.toString() + "\n");
				}
				else if(q[i] instanceof CrawlURI)
				{
					CrawlURI cu = (CrawlURI)q[i];
					rep.append("   Snooze item " + (i+1) + " - " + "CrawlUri" + "\n");
					rep.append("     UURI:           " + cu.getUURI().getUriString() + "\n");
					rep.append("     Fetch attempts: " + cu.getFetchAttempts () + "\n");
				}
			}
		}
		rep.append("\n -----===== CrawlURI MAPS =====-----\n");
		rep.append(" In process map size: " + inProcessMap.size() + "\n");
		if(inProcessMap.size()!=0)
		{
			Iterator q = inProcessMap.keySet().iterator();
			int i = 1;
			while(q.hasNext())
			{
				CrawlURI cu = (CrawlURI)inProcessMap.get(q.next());
				rep.append("   In process CrawlUri " + (i++) + "\n");
				rep.append("     UURI:           " + cu.getUURI().getUriString() + "\n");
				rep.append("     Fetch attempts: " + cu.getFetchAttempts () + "\n");
			}
		}
		rep.append("\n Held curis map size: " + heldCuris.size() + "\n");
		if(heldCuris.size()!=0)
		{
			Iterator q = heldCuris.keySet().iterator();
			int i = 1;
			while(q.hasNext())
			{
				Object qn = q.next();
				if(qn instanceof CrawlURI)
				{
					CrawlURI cu = (CrawlURI)qn;
					rep.append("   Held item " + (i++) + " is a CrawlURI\n");
					rep.append("     UURI:           " + cu.getUURI().getUriString() + "\n");
					rep.append("     Fetch attempts: " + cu.getFetchAttempts () + "\n");
				}
				else if(qn instanceof UURI)
				{
					UURI uu = (UURI)qn;
					rep.append("   Held item " + (i++) + " is a UURI\n");
					rep.append("     UURI:           " + uu.getUriString() + "\n");
				}
				else
				{
					rep.append("   Held item " + (i++) + " is not a CrawlURI or a UURI\n");
					rep.append("     Item: " + qn.toString() + "\n");
				}
			}
		}
		
		return rep.toString();
	}
	
	
	public long getAllClassQueuesMap()
	{
		long total = 0;
		return total;		
	}

	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.CrawlListener#crawlPausing(java.lang.String)
	 */
	public void crawlPausing(String statusMessage) {
		// We are not interested in the crawlPausing event
	}



	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.CrawlStatusListener#crawlPaused(java.lang.String)
	 */
	public void crawlPaused(String statusMessage) {
		// We are not interested in the crawlPaused event
	}



	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.CrawlStatusListener#crawlResuming(java.lang.String)
	 */
	public void crawlResuming(String statusMessage) {
		// We are not interested in the crawlResuming event
	}


	/*	(non-Javadoc) 
	 *  @see org.archive.crawler.framework.CrawlStatusListener#crawlEnding(java.lang.String)
	 */
	public void crawlEnding(String sExitMessage) {
		// We are not interested in the crawlEnding event
	}
	

	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.CrawlStatusListener#crawlEnded(java.lang.String)
	 */
	public void crawlEnded(String sExitMessage) {
		// Ok, if the CrawlController is exiting we delete our reference to it to facilitate gc.
		controller = null;			
	}
	
	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.URIFrontier#getTotalBytesWritten()
	 */
	public long totalBytesWritten() {
		return totalProcessedBytes;
	}
}
