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
package org.archive.crawler.frontier;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.AttributeNotFoundException;

import org.apache.commons.collections.Predicate;
import org.apache.commons.httpclient.HttpStatus;
import org.archive.crawler.datamodel.CandidateURI;
import org.archive.crawler.datamodel.CoreAttributeConstants;
import org.archive.crawler.datamodel.CrawlServer;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.FetchStatusCodes;
import org.archive.crawler.datamodel.UURI;
import org.archive.crawler.datamodel.UriUniqFilter;
import org.archive.crawler.datamodel.UriUniqFilter.HasUriReceiver;
import org.archive.crawler.framework.CrawlController;
import org.archive.crawler.framework.URIFrontier;
import org.archive.crawler.framework.URIFrontierMarker;
import org.archive.crawler.framework.exceptions.EndedException;
import org.archive.crawler.framework.exceptions.FatalConfigurationException;
import org.archive.crawler.framework.exceptions.InvalidURIFrontierMarkerException;
import org.archive.crawler.util.FPUriUniqFilter;
import org.archive.queue.TieredQueue;
import org.archive.util.ArchiveUtils;
import org.archive.util.MemLongFPSet;
import org.archive.util.PaddingStringBuffer;

import EDU.oswego.cs.dl.util.concurrent.ConcurrentReaderHashMap;

/**
 * A basic mostly breadth-first frontier, which refrains from
 * emitting more than one CrawlURI of the same 'key' (host) at
 * once, and respects minimum-delay and delay-factor specifications
 * for politeness.
 *
 * There is one generic 'pendingQueue', and then an arbitrary
 * number of other 'KeyedQueues' each representing a certain
 * 'key' class of URIs -- effectively, a single host (by hostname).
 *
 * KeyedQueues may have an item in-process -- in which case they
 * do not provide any other items for processing. KeyedQueues may
 * also be 'snoozed' -- when they should be kept inactive for a
 * period of time, to either enforce politeness policies or allow
 * a configurable amount of time between error retries.
 *
 *
 * @author Gordon Mohr
 */
public class NewFrontier
    extends AbstractFrontier
    implements URIFrontier, FetchStatusCodes, CoreAttributeConstants,
        HasUriReceiver {
    // be robust against trivial implementation changes
    private static final long serialVersionUID = ArchiveUtils.classnameBasedUID(NewFrontier.class,1);

    private static final Logger logger =
        Logger.getLogger(NewFrontier.class.getName());


    // those UURIs which are already in-process (or processed), and
    // thus should not be rescheduled
    protected UriUniqFilter alreadyIncluded;

    // default 
    TieredQueue mainQueue;
    
    //
    int maxWorkQueues = 100; // TODO: be configurable
    
    // all per-class queues
    ConcurrentReaderHashMap allClassQueuesMap = new ConcurrentReaderHashMap(); // of String (classKey) -> KeyedQueue

    // all per-class queues whose first item may be handed out 
    LinkedList readyClassQueues = new LinkedList(); // of KeyedQueues

    // all per-class queues who are on hold until a certain time
    SortedSet snoozeQueues = new TreeSet(new SchedulingComparator()); // of KeyedQueue, sorted by wakeTime     

    public NewFrontier(String name){
        this(name,"NewFrontier. \nMaintains the internal" +
                " state of the crawl. It dictates the order in which URIs" +
                " will be scheduled. \nThis frontier is mostly a breadth-first"+
                " frontier, which refrains from emitting more than one" +
                " CrawlURI of the same \'key\' (host) at once, and respects" +
                " minimum-delay and delay-factor specifications for" +
                " politeness.");
    }

    /**
     * @param name
     */
    public NewFrontier(String name, String description) {
        // The 'name' of all frontiers should be the same (URIFrontier.ATTR_NAME)
        // therefore we'll ignore the supplied parameter.
        super(URIFrontier.ATTR_NAME, description);
    }

    /**
     * Initializes the Frontier, given the supplied CrawlController.
     *
     * @see org.archive.crawler.framework.URIFrontier#initialize(org.archive.crawler.framework.CrawlController)
     */
    public void initialize(CrawlController c)
        throws FatalConfigurationException, IOException {
        this.controller = c;
        mainQueue = createMainQueue(c.getStateDisk(),"mainQ");
        alreadyIncluded = createAlreadyIncluded(c.getStateDisk(),
                "alreadyIncluded");
        loadSeeds();
    }
    
    
    
    /* (non-Javadoc)
     * @see org.archive.crawler.framework.URIFrontier#unpause()
     */
    public synchronized void unpause() {
        // TODO Auto-generated method stub
        super.unpause();
        synchronized(readyClassQueues) {
            readyClassQueues.notifyAll();
        }
    }
    /**
     * Create the main queue which holds items until they are assigned
     * to per-host queues. 
     * 
     * @param stateDisk
     * @param string
     * @return
     * @throws IOException
     */
    protected TieredQueue createMainQueue(File stateDisk, String string) throws IOException {
        TieredQueue tq = new TieredQueue(3);
        // SETTING TO -1 SO ITS OBVIOUSLY BROKEN.  SOMETHING
        // MISSING HERE.  MAKING THIS COMMIT SO BUILD SUCCEEDS.
        tq.initializeDiskBackedQueues(stateDisk,string, -1);
        return tq;
    }

    /**
     * Create a memory-based UriUniqFilter that will serve as record 
     * of already seen URIs.
     *
     * @param dir Directory where the set's files should be written
     * @param filePrefix Prefix to names of the set's files
     * @return A UURISet that will serve as a record of already seen URIs
     * @throws IOException If problems occur creating files on disk
     */
    protected UriUniqFilter createAlreadyIncluded(File dir, String filePrefix)
            throws IOException, FatalConfigurationException {
        UriUniqFilter uuf = new FPUriUniqFilter(new MemLongFPSet(23,0.75f));
        uuf.setDestination(this);
        return uuf;
    }


    
    /**
     * Arrange for the given CandidateURI to be visited, if it is not
     * already scheduled/completed.
     *
     * @see org.archive.crawler.framework.URIFrontier#schedule(org.archive.crawler.datamodel.CandidateURI)
     */
    public void schedule(CandidateURI caUri) {
        synchronized(alreadyIncluded) {
            if(caUri.forceFetch()) {
                alreadyIncluded.addForce(caUri);
            } else {
                alreadyIncluded.add(caUri);
            }
        }
    }
    
    /**
     * @param huri
     */
    public void receive(UriUniqFilter.HasUri huri) {
        CandidateURI caUri = (CandidateURI) huri;
        CrawlURI curi = asCrawlUri(caUri);

        applySpecialHandling(curi);

        synchronized(mainQueue) {
            if (curi.needsImmediateScheduling()) {
                mainQueue.enqueue(curi,0);
            } else if (curi.needsSoonScheduling()) {
                mainQueue.enqueue(curi,1);
            } else {
                mainQueue.enqueue(curi);
            }
        }
    }

    /**
     * @param curi
     */
    protected void applySpecialHandling(CrawlURI curi) {
        if(curi.isSeed() && curi.getVia() != null
                && curi.flattenVia().length() > 0){
            // The only way a seed can have a non-empty via is if it is the
            // result of a seed redirect.  Add it to the seeds list.
            //
            // This is a feature.  This is handling for case where a seed
            // gets immediately redirected to another page.  What we're doing is
            // treating the immediate redirect target as a seed.
            List seeds = this.controller.getScope().getSeedlist();
            synchronized(seeds) {
                seeds.add(curi.getUURI());
            }
            // And it needs rapid scheduling.
            curi.setSchedulingDirective(CandidateURI.MEDIUM);
        }
        
        // optionally preferencing embeds up to MEDIUM
        int prefHops = 1; // TODO: restore as configurable setting
        if (prefHops > 0) {
            int embedHops = curi.getTransHops();
            if (embedHops > 0 && embedHops <= prefHops
                    && curi.getSchedulingDirective() == CandidateURI.NORMAL) {
                // number of embed hops falls within the preferenced range, and
                // uri is not already MEDIUM -- so promote it
                curi.setSchedulingDirective(CandidateURI.MEDIUM);
            }
        }
    }

    /**
     * Return the next CrawlURI to be processed (and presumably
     * visited/fetched) by a a worker thread.
     *
     * First checks any "Ready" per-host queues, then the global
     * pending queue.
     *
     * @return next CrawlURI to be processed. Or null if none is available.
     *
     * @see org.archive.crawler.framework.URIFrontier#next(int)
     */
    public CrawlURI next() throws InterruptedException, EndedException {
        while(true) {
            long now = System.currentTimeMillis();

            // do common checks for pause, terminate, bandwidth-hold
            preNext(now);

            synchronized(readyClassQueues) {
                // Check for snoozing queues who are ready to wake up.
                wakeSnoozedQueues(now);

                if(!readyClassQueues.isEmpty()) {
                    NewWorkQueue q = (NewWorkQueue) readyClassQueues.removeFirst();
                    CrawlURI curi = q.peek();
                    noteAboutToEmit(curi,q);
                    // TODO: replace q on readyClassQueues if still able to provide items (valence>1)
                    return curi; 
                }
            }
            
            // if reach here, nothing was ready; try to make or wait for
            // something to be ready, avoiding hold of frontier lock as 
            // much as possible      
            if(fillReadyQueues()) {
                continue; // the while(true) with fresh URIs
            }
            
            // ensure any piled-up scheduled URIs are considered
            synchronized(alreadyIncluded) {
                if(alreadyIncluded.pending()>0) {
                    if(alreadyIncluded.flush()>0) {
                        continue; // the while(true) with fresh URIs
                    }
                }
                
                // consider if URIs exhausted
                if(isEmpty()) {
                    // TODO: notify controller?
                    // nothing left to crawl
                    throw new EndedException("exhausted");
                } 
            }
            
            // wait until something changes
            synchronized(readyClassQueues) {
                long wait = 0;
                if(!snoozeQueues.isEmpty()) {
                    wait = ((NewWorkQueue)snoozeQueues.first()).getWakeTime() - now;
                }
                readyClassQueues.wait(wait);
            }
        }
    }

    /**
     * 
     */
    private boolean fillReadyQueues() {
        boolean retVal = false;
        synchronized(mainQueue) {
        
            while (allClassQueuesMap.size()<maxWorkQueues 
                    && !mainQueue.isEmpty()) {
                CrawlURI curi = (CrawlURI) mainQueue.dequeue();
                NewWorkQueue wq = (NewWorkQueue) allClassQueuesMap.get(curi.getClassKey());
                if (wq==null) {
                    wq = newWorkQueueFor(curi);
                    allClassQueuesMap.put(curi.getClassKey(),wq);
                    wq.enqueue(curi);
                    synchronized(readyClassQueues) {
                        // new queue is always ready
                        readyClassQueues.addLast(wq);
                        readyClassQueues.notify();
                    }
                    retVal = true; // the overall while, since there's now a ready queue
                } else {
                    // old queue is never made ready by addition
                    // TODO: is this true? esp. if valence>1?
                    wq.enqueue(curi);
                }
            }
        }
        return retVal; 
    }

    /**
     * @param curi
     * @return
     */
    private NewWorkQueue newWorkQueueFor(CrawlURI curi) {
        NewWorkQueue kq = null;

        String key = curi.getClassKey();
        // the creation of disk directories makes this a potentially
        // lengthy operation we don't want to hold full-frontier lock
        // for 
        try {
            kq = new NewWorkQueue(key,
            this.controller.getServerCache().getServerFor(curi),
            scratchDirFor(key),
            100);
        } catch (IOException e) {
            // An IOException occured trying to make new KeyedQueue.
            curi.getAList().putObject(A_RUNTIME_EXCEPTION,e);
            Object array[] = { curi };
            this.controller.runtimeErrors.log(
                    Level.SEVERE,
                    curi.getUURI().toString(),
                    array);
        }
        return kq;    
    }

    /**
     * @param curi
     */
    protected void noteAboutToEmit(CrawlURI curi, NewWorkQueue q) {
        curi.setHolder(q);
        CrawlServer cs = this.controller.getServerCache().getServerFor(curi);
        if (cs != null) {
            curi.setServer(cs);
        }
        this.controller.recover.emitted(curi);        
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
    public void finished(CrawlURI curi) {
        long now = System.currentTimeMillis();
        
        curi.incrementFetchAttempts();
        logLocalizedErrors(curi);
        NewWorkQueue wq = (NewWorkQueue) curi.getHolder();
        assert (wq.peek() == curi) : "unexpected peek "+wq;

        if (needsRetrying(curi)) {
            // Consider errors which can be retried, leaving uri atop queue
            long delay_sec = retryDelayFor(curi);
            synchronized(wq) {
                wq.unpeek();
                if (delay_sec>0) {
                    wq.setWakeTime(now+(delay_sec*1000));
                    snoozeQueues.add(wq);
                } else {
                    readyClassQueues.add(wq);
                }
            }
            controller.fireCrawledURINeedRetryEvent(curi); // Let everyone interested know that it will be retried.
            controller.recover.rescheduled(curi);
            return;
        } 
        
        // curi will definitely be disposed of without retry, so remove from q
        wq.dequeue();
        log(curi);
        
        if (curi.isSuccess()) {
            totalProcessedBytes += curi.getContentSize();
            successCount++;
            controller.fireCrawledURISuccessfulEvent(curi); //Let everyone know in case they want to do something before we strip the curi.
            controller.recover.finishedSuccess(curi);        
        } else if(isDisregarded(curi)) {
            // Check for codes that mean that while we the crawler did
            // manage to try it, it must be disregarded for some reason.
            disregardedCount++;
            //Let interested listeners know of disregard disposition.
            controller.fireCrawledURIDisregardEvent(curi);
            // if exception, also send to crawlErrors
            if (curi.getFetchStatus() == S_RUNTIME_EXCEPTION) {
                Object[] array = { curi };
                controller.runtimeErrors.log(
                    Level.WARNING,
                    curi.getUURI().toString(),
                    array );
            }
            // TODO: consider reinstating forget-uri
        } else {
            // In that case FAILURE, note & log
            //Let interested listeners know of failed disposition.
            this.controller.fireCrawledURIFailureEvent(curi);
            // if exception, also send to crawlErrors
            if (curi.getFetchStatus() == S_RUNTIME_EXCEPTION) {
                Object[] array = { curi };
                this.controller.runtimeErrors.log(
                    Level.WARNING,
                    curi.getUURI().toString(),
                    array);
            }

            this.failedCount++;
            this.controller.recover.finishedFailure(curi);
        }
        
        long delay_ms = politenessDelayFor(curi);
        synchronized(readyClassQueues) { // TODO: rethink this sync
            if (delay_ms>0) {
                wq.setWakeTime(now+delay_ms);
                snoozeQueues.add(wq);
            } else {
                readyClassQueues.add(wq);
                readyClassQueues.notify(); // new items might be available, let waiting threads know
            }
        }
        
        fillReadyQueues();
        
        curi.stripToMinimal();
        curi.processingCleanup();

    }


    protected boolean isDisregarded(CrawlURI curi) {
        switch (curi.getFetchStatus()) {
            case S_ROBOTS_PRECLUDED :     // they don't want us to have it
            case S_OUT_OF_SCOPE :         // filtered out by scope
            case S_BLOCKED_BY_USER :      // filtered out by user
            case S_TOO_MANY_EMBED_HOPS :  // too far from last true link
            case S_TOO_MANY_LINK_HOPS :   // too far from seeds
            case S_DELETED_BY_USER :      // user deleted
                return true;
            default:
                return false;
        }
    }

    /**
     * Frontier is empty only if all queues are empty and
     * no URIs are in-process
     *
     * @return True if queues are empty.
     */
    public boolean isEmpty() {
        synchronized(mainQueue) {
            return 
                mainQueue.isEmpty() &&
                alreadyIncluded.pending()==0 &&
                allClassQueuesMap.isEmpty();
        }
    }


    /**
     * Wake any snoozed queues whose snooze time is up.
     * @param now Current time in millisec.
     * @throws InterruptedException
     */
    protected void wakeSnoozedQueues(long now) throws InterruptedException {
        while(!snoozeQueues.isEmpty()&&((NewWorkQueue)snoozeQueues.first()).getWakeTime()<=now) {
            NewWorkQueue awoken = (NewWorkQueue)snoozeQueues.first();
            if (!snoozeQueues.remove(awoken)) {
                logger.severe("first() item couldn't be remove()d! - "+awoken+" - " + snoozeQueues.contains(awoken));
                logger.severe(report());
            }
            if (awoken.isEmpty()) {
                // delete it
                discardQueue(awoken);
            } else {
                // ready it
                readyClassQueues.add(awoken);
            }
        }
    }

    private void discardQueue(NewWorkQueue q) {
        allClassQueuesMap.remove(q.getClassKey());
        assert !snoozeQueues.contains(q) : "snoozeQueues holding dead q "+q;
        assert !readyClassQueues.contains(q) : "readyClassQueues holding dead q "+q;
    }

    protected long earliestWakeTime() {
        if (!snoozeQueues.isEmpty()) {
            return ((NewWorkQueue)snoozeQueues.first()).getWakeTime();
        }
        return Long.MAX_VALUE;
    }

    /**
     * @param curi
     * @param array
     */
    protected void log(CrawlURI curi) {
        curi.aboutToLog();
        Object array[] = { curi };
        this.controller.uriProcessing.log(
            Level.INFO,
            curi.getUURI().toString(),
            array );
    }

    /**
     * Checks if a recently completed CrawlURI that did not finish successfully
     * needs to be retried (processed again after some time elapses)
     *
     * @param curi The CrawlURI to check
     * @return True if we need to retry.
     * @throws AttributeNotFoundException If problems occur trying to read the
     *            maximum number of retries from the settings framework.
     */
    private boolean needsRetrying(CrawlURI curi) {
        if (overMaxRetries(curi)) {
            return false; 
        }
        
        switch (curi.getFetchStatus()) {
            case HttpStatus.SC_UNAUTHORIZED:
                // We can get here though usually a positive status code is
                // a success.  We get here if there is rfc2617 credential data
                // loaded and we're supposed to go around again.  See if any
                // rfc2617 credential present and if there, assume it got
                // loaded in FetchHTTP on expectation that we're to go around
                // again.  If no rfc2617 loaded, we should not be here.
                boolean loaded = curi.hasRfc2617CredentialAvatar();
                if (!loaded) {
                    logger.severe("Have 401 but no creds loaded " + curi);
                }
                return loaded;
            case S_DEFERRED:
            case S_CONNECT_FAILED:
            case S_CONNECT_LOST:
                // these are all worth a retry
                // TODO: consider if any others (S_TIMEOUT in some cases?) deserve retry
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
     * @param curi The CrawlURI to forget
     */
    protected void forget(CrawlURI curi) {
        logger.finer("Forgetting "+curi);
        alreadyIncluded.forget(curi.getUURI());
    }

    /**  (non-Javadoc)
     * @see org.archive.crawler.framework.URIFrontier#discoveredUriCount()
     */
    public long discoveredUriCount(){
        return alreadyIncluded.count();
    }


    
    /** (non-Javadoc)
     * @see org.archive.crawler.framework.URIFrontier#getInitialMarker(java.lang.String, boolean)
     */
    public URIFrontierMarker getInitialMarker(String regexpr, boolean inCacheOnly) {
        ArrayList keyqueueKeys = new ArrayList();
        if(allClassQueuesMap.size()!=0)
        {
            Iterator q = allClassQueuesMap.keySet().iterator();
            while(q.hasNext())
            {
                keyqueueKeys.add(q.next());
            }
        }
        return new FrontierMarker(regexpr,inCacheOnly,keyqueueKeys);
    }

    /** (non-Javadoc)
     *
     * @param marker
     * @param numberOfMatches
     * @param verbose
     * @return List of URIS.
     * @throws InvalidURIFrontierMarkerException
     */
    public ArrayList getURIsList(URIFrontierMarker marker, int numberOfMatches,
            boolean verbose) throws InvalidURIFrontierMarkerException {
        if(marker instanceof FrontierMarker == false){
            throw new InvalidURIFrontierMarkerException();
        }

        FrontierMarker mark = (FrontierMarker)marker;
        ArrayList list = new ArrayList(numberOfMatches);

        // inspect the KeyedQueues
        while( numberOfMatches > 0 && mark.getCurrentQueue() != -1){
            String queueKey = (String)mark.getKeyQueues().
                get(mark.getCurrentQueue());
            KeyedQueue keyq = (KeyedQueue)allClassQueuesMap.get(queueKey);
            if(keyq==null){
                throw new InvalidURIFrontierMarkerException();
            }

            numberOfMatches -= inspectQueue(keyq,"hostQueue("+queueKey+")",list,mark,verbose, numberOfMatches);
            if(numberOfMatches>0){
                mark.nextQueue();
            }
        }
        return list;
    }

    /**
     * Adds any applicable URIs from a given queue to the given list.
     *
     * @param queue
     *            The queue to inspect
     * @param queueName
     * @param list
     *            The list to add matched URIs to.
     * @param marker
     *            Where to start accepting matches from.
     * @param verbose
     *            List items are verbose
     * @param numberOfMatches
     *            maximum number of matches to add to list
     * @return the number of matches found
     * @throws InvalidURIFrontierMarkerException
     */
    private int inspectQueue( KeyedQueue queue,
                              String queueName,
                              ArrayList list,
                              FrontierMarker marker,
                              boolean verbose,
                              int numberOfMatches)
                          throws InvalidURIFrontierMarkerException{
        if(queue.length() < marker.getAbsolutePositionInCurrentQueue()) {
            // Not good. Invalid marker.
            throw new InvalidURIFrontierMarkerException();
        }

        if(queue.length()==0){
            return 0;
        }

        Iterator it = queue.getIterator(marker.isInCacheOnly());
        int foundMatches = 0;
        long itemsScanned = 0;
        while(it.hasNext() && foundMatches < numberOfMatches){
            Object o = it.next();
            if( itemsScanned >= marker.getAbsolutePositionInCurrentQueue()
                    && o instanceof CandidateURI ){
                // Ignore items that are in front of current position
                // and those that are not CandidateURIs.
                CandidateURI caURI = (CandidateURI)o;
                if(marker.match(caURI)){
                    // Found match.
                    String text;
                    if(verbose){
                        // A verbose description
                        PaddingStringBuffer verb = new PaddingStringBuffer();
                        verb.append(caURI.getURIString());
                        verb.append(" ("+queueName+":" + itemsScanned + ")");
                        verb.newline();
                        verb.padTo(2);
                        verb.append(caURI.getPathFromSeed());
                        if(caURI.getVia() != null
                                && caURI.getVia() instanceof CandidateURI){
                            verb.append(" ");
                            verb.append(((CandidateURI)caURI.getVia()).getURIString());
                        }
                        text = verb.toString();
                    } else {
                        text = caURI.getURIString();
                    }
                    list.add(text);
                    foundMatches++;
                    marker.incrementNextItemNumber();
                }
            }
            itemsScanned++;
        }
        marker.setAbsolutePositionInCurrentQueue(itemsScanned);
        return foundMatches;
    }

    /**
     * @param match String to  match.
     * @return Number of items deleted.
     */
    public long deleteURIs(String match) {
        long numberOfDeletes = 0;
        // Create QueueItemMatcher
        Predicate mat = new URIQueueMatcher(match, true, this);
        // Delete from all KeyedQueues
        if(allClassQueuesMap.size()!=0)
        {
            Iterator q = allClassQueuesMap.keySet().iterator();
            while(q.hasNext())
            {
                NewWorkQueue kq = (NewWorkQueue)allClassQueuesMap.get(q.next());
                numberOfDeletes += kq.deleteMatchedItems(mat);

                // If our deleting has emptied the KeyedQueue then update it's
                // state.
//                kq.checkEmpty();
            }
        }
        // Delete from pendingQueue
//        numberOfDeletes += pendingQueue.deleteMatchedItems(mat);
        queuedCount -= numberOfDeletes;
        return numberOfDeletes;
    }

    
    /**
     * @return One-line summary report, useful for display when full report
     * may be unwieldy. 
     */
    public String oneLineReport() {
    	StringBuffer rep = new StringBuffer();
    	rep.append(allClassQueuesMap.size()+" queues: ");
    	rep.append(readyClassQueues.size()+" ready, ");
    	rep.append(snoozeQueues.size()+" snoozed, ");
    	// rep.append(inactiveClassQueues.size()+" inactive");    	
    	return rep.toString();
    }
    
    /**
     * This method compiles a human readable report on the status of the frontier
     * at the time of the call.
     *
     * @return A report on the current status of the frontier.
     */
    public synchronized String report()
    {
        long now = System.currentTimeMillis();
        StringBuffer rep = new StringBuffer();

        rep.append("Frontier report - "
                   + ArchiveUtils.TIMESTAMP12.format(new Date()) + "\n");
        rep.append(" Job being crawled: "
                   + controller.getOrder().getCrawlOrderName() + "\n");
        rep.append("\n -----===== STATS =====-----\n");
        rep.append(" Discovered:    " + discoveredUriCount() + "\n");
        rep.append(" Queued:        " + queuedCount + "\n");
        rep.append(" Finished:      " + finishedUriCount() + "\n");
        rep.append("  Successfully: " + successCount + "\n");
        rep.append("  Failed:       " + failedCount + "\n");
        rep.append("  Disregarded:  " + disregardedCount + "\n");
        rep.append("\n -----===== QUEUES =====-----\n");
        rep.append(" Already included size:     " + alreadyIncluded.count()+"\n");
//        rep.append(" Pending queue length:      " + pendingQueue.length()+ "\n");
        rep.append("\n All class queues map size: " + allClassQueuesMap.size() + "\n");
        if(allClassQueuesMap.size()!=0)
        {
            Iterator q = allClassQueuesMap.entrySet().iterator();
            while(q.hasNext())
            {
                NewWorkQueue kq = (NewWorkQueue)q.next();
                appendKeyedQueue(rep,kq,now);
            }
        }
        rep.append("\n Ready class queues size:   " + readyClassQueues.size() + "\n");
        for(int i=0 ; i < readyClassQueues.size() ; i++)
        {
            NewWorkQueue kq = (NewWorkQueue)readyClassQueues.get(i);
            appendKeyedQueue(rep,kq,now);
        }

        rep.append("\n Snooze queues size:        " + snoozeQueues.size() + "\n");
        if(snoozeQueues.size()!=0)
        {
            Object[] q = ((TreeSet)snoozeQueues).toArray();
            for(int i=0 ; i < q.length ; i++)
            {
                if(q[i] instanceof NewWorkQueue)
                {
                    NewWorkQueue kq = (NewWorkQueue)q[i];
                    appendKeyedQueue(rep,kq,now);
                }
            }
        }
        //rep.append("\n Inactive queues size:        " + inactiveClassQueues.size() + "\n");


        return rep.toString();
    }


    private void appendKeyedQueue(StringBuffer rep, NewWorkQueue kq, long now) {
        rep.append("    NewWorkQueue  " + kq.getClassKey() + "\n");
        rep.append("     Length:        " + kq.length() + "\n");
//        rep.append("     Is ready:  " + kq.shouldWake() + "\n");
        if(kq instanceof NewWorkQueue){
//        	rep.append("     Status:        " +
//            ((NewWorkQueue)kq).getState().toString() + "\n");
//        }
//        if(kq.getState()==NewWorkQueue.SNOOZED) {
//            rep.append("     Wakes in:      " + ArchiveUtils.formatMillisecondsToConventional(kq.getWakeTime()-now)+"\n");
//        }
//        if(kq.getInProcessItems().size()>0) {
//            Iterator iter = kq.getInProcessItems().iterator();
//            while (iter.hasNext()) {
//                rep.append("     InProcess:     " + iter.next() + "\n");
//            }
        }
        rep.append("     Last enqueued: " + kq.getLastQueued()+"\n");
        rep.append("     Last dequeued: " + kq.getLastDequeued()+"\n");

    }

    /**
     * Force logging, etc. of operator- deleted CrawlURIs
     * @see org.archive.crawler.framework.URIFrontier#deleted(org.archive.crawler.datamodel.CrawlURI)
     */
    public synchronized void deleted(CrawlURI curi) {
        //treat as disregarded
        controller.fireCrawledURIDisregardEvent(curi);
        log(curi);
        disregardedCount++;
        curi.stripToMinimal();
        curi.processingCleanup();
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.framework.URIFrontier#considerIncluded(org.archive.crawler.datamodel.UURI)
     */
    public void considerIncluded(UURI u) {
        this.alreadyIncluded.note(u);
    }
}
