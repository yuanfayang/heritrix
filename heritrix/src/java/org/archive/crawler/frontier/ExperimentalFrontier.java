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
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.collections.Predicate;
import org.archive.crawler.datamodel.CandidateURI;
import org.archive.crawler.datamodel.CoreAttributeConstants;
import org.archive.crawler.datamodel.CrawlServer;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.FetchStatusCodes;
import org.archive.crawler.datamodel.UURI;
import org.archive.crawler.datamodel.UriUniqFilter;
import org.archive.crawler.datamodel.UriUniqFilter.HasUriReceiver;
import org.archive.crawler.framework.CrawlController;
import org.archive.crawler.framework.Frontier;
import org.archive.crawler.framework.FrontierMarker;
import org.archive.crawler.framework.exceptions.EndedException;
import org.archive.crawler.framework.exceptions.FatalConfigurationException;
import org.archive.crawler.framework.exceptions.InvalidFrontierMarkerException;
import org.archive.crawler.util.FPUriUniqFilter;
import org.archive.queue.TieredQueue;
import org.archive.util.ArchiveUtils;
import org.archive.util.MemLongFPSet;
import org.archive.util.PaddingStringBuffer;

import EDU.oswego.cs.dl.util.concurrent.ClockDaemon;
import EDU.oswego.cs.dl.util.concurrent.ConcurrentReaderHashMap;
import EDU.oswego.cs.dl.util.concurrent.LinkedQueue;

/**
 * A basic mostly breadth-first frontier, which refrains from
 * emitting more than one CrawlURI of the same 'key' (host) at
 * once, and respects minimum-delay and delay-factor specifications
 * for politeness.
 *
 * There is one generic 'pendingQueue', and then an arbitrary
 * number of other work queues each representing a certain
 * 'key' class of URIs -- effectively, a single host (by hostname).
 *
 * CURRENT STATUS: Not yet even minimally functional. Work on hold
 * pending other experiments. 
 * 
 * @author Gordon Mohr
 */
public class ExperimentalFrontier
    extends AbstractFrontier
    implements Frontier, FetchStatusCodes, CoreAttributeConstants,
        HasUriReceiver {
    // be robust against trivial implementation changes
    private static final long serialVersionUID = ArchiveUtils.classnameBasedUID(ExperimentalFrontier.class,1);

    private static final Logger logger =
        Logger.getLogger(ExperimentalFrontier.class.getName());

    // those UURIs which are already in-process (or processed), and
    // thus should not be rescheduled
    protected UriUniqFilter alreadyIncluded;

    // initial holding place for all regularly-scheduled URIs, until they
    // get moved to an active per-host queue
    TieredQueue mainQueue;
    
    //
    int maxWorkQueues = 100; // TODO: be configurable
    
    // all per-class queues
    ConcurrentReaderHashMap allClassQueuesMap = new ConcurrentReaderHashMap(); // of String (classKey) -> KeyedQueue

    // all per-class queues whose first item may be handed out 
    LinkedQueue readyClassQueues = new LinkedQueue(); // of KeyedQueues

    // daemon to wake (put in ready queue) WorkQueues at the appropriate time
    ClockDaemon daemon = new ClockDaemon();
    
    public ExperimentalFrontier(String name){
        this(name,"NewFrontier. NOT YET FUNCTIONAL. DO NOT USE.\nMaintains the internal" +
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
    public ExperimentalFrontier(String name, String description) {
        // The 'name' of all frontiers should be the same (URIFrontier.ATTR_NAME)
        // therefore we'll ignore the supplied parameter.
        super(Frontier.ATTR_NAME, description);
    }

    /**
     * Initializes the Frontier, given the supplied CrawlController.
     *
     * @see org.archive.crawler.framework.Frontier#initialize(org.archive.crawler.framework.CrawlController)
     */
    public void initialize(CrawlController c)
        throws FatalConfigurationException, IOException {
        this.controller = c;
        mainQueue = createMainQueue(c.getStateDisk(),"mainQ");
        alreadyIncluded = createAlreadyIncluded(c.getStateDisk(),
                "alreadyIncluded");
        loadSeeds();
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
        tq.initializeDiskBackedQueues(stateDisk,string,300);
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
     * @see org.archive.crawler.framework.Frontier#schedule(org.archive.crawler.datamodel.CandidateURI)
     */
    public void schedule(CandidateURI caUri) {
        if(caUri.forceFetch()) {
            alreadyIncluded.addForce(caUri);
        } else {
            alreadyIncluded.add(caUri);
        }
    }
    
    /**
     * Accept the given CandidateURI for scheduling.
     * 
     * @param huri
     */
    public void receive(UriUniqFilter.HasUri huri) {
        CandidateURI caUri = (CandidateURI) huri;
        CrawlURI curi = asCrawlUri(caUri);

        applySpecialHandling(curi);

        if (curi.needsImmediateScheduling() || curi.needsSoonScheduling()) {
            // try to put accelerated items onto prexisting queue, if present 
            ExperimentalWorkQueue wq = (ExperimentalWorkQueue) allClassQueuesMap.get(curi.getClassKey());
            if(wq!=null) {
                synchronized(wq) {
                    if(wq.isValid()) {
                        wq.enqueue(curi);
                    } else {
                        wq = null;
                    }
                }
            } 
            if( wq == null ) {
                if(curi.needsImmediateScheduling()) {
                    mainQueue.enqueue(curi,0);
                } else {
                    mainQueue.enqueue(curi,1);
                }
            }
        } else {
            // put at end
            mainQueue.enqueue(curi);
        }
        
        if(readyClassQueues.isEmpty()) {
            fillReadyQueues();
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
     * @see org.archive.crawler.framework.Frontier#next(int)
     */
    public CrawlURI next() throws InterruptedException, EndedException {
        long wait = 0;
        while(true) {
            long now = System.currentTimeMillis();

            // do common checks for pause, terminate, bandwidth-hold
            preNext(now);

            ExperimentalWorkQueue readyQ = (ExperimentalWorkQueue) readyClassQueues.poll(5000);
            if(readyQ != null) {
                synchronized(readyQ) {
                    if(readyQ.isEmpty()) {
                        readyQ.setValid(false);
                        // TODO this code is incomplete
                        allClassQueuesMap.remove(readyQ.getClassKey());
                        continue;
                    }
                    CrawlURI curi = readyQ.peek();
                    noteAboutToEmit(curi,readyQ);
                    // TODO: restore valence capability
                    return curi; 
                }
            }
       
            // ensure any piled-up scheduled URIs are considered
            synchronized(alreadyIncluded) {
                if(alreadyIncluded.pending()>0) {
                    if(alreadyIncluded.flush()>0) {
                        continue; // the while(true) with fresh URIs
                    }
                }
            }
        }
    }

    /**
     * @throws InterruptedException
     * 
     */
    private void fillReadyQueues() {
        synchronized(allClassQueuesMap) { // ensure only one thread in this method 
            while (allClassQueuesMap.size()<maxWorkQueues  && !mainQueue.isEmpty()) {
                CrawlURI curi = (CrawlURI) mainQueue.dequeue();
                ExperimentalWorkQueue wq = (ExperimentalWorkQueue) allClassQueuesMap.get(curi.getClassKey());
                if (wq==null) {
                    wq = newWorkQueueFor(curi);
                    allClassQueuesMap.put(curi.getClassKey(),wq);
                    wq.enqueue(curi);
                    try {
                        // new queue is always ready
                        readyClassQueues.put(wq);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                        System.err.println("fillReadyQueues() couldn't ready: "+wq);
                    }
                } else {
                    // old queue is never made ready by addition
                    // TODO: is this true? esp. if valence>1?
                    wq.enqueue(curi);
                }
            }
        }
    }

    /**
     * @param curi
     * @return
     */
    private ExperimentalWorkQueue newWorkQueueFor(CrawlURI curi) {
        ExperimentalWorkQueue kq = null;

        String key = curi.getClassKey();
        // the creation of disk directories makes this a potentially
        // lengthy operation we don't want to hold full-frontier lock
        // for
        try {
            kq = new ExperimentalWorkQueue(key, this.controller.getServerCache()
                    .getServerFor(curi), scratchDirFor(key), 100);
        } catch (IOException e) {
            // An IOException occured trying to make new KeyedQueue.
            curi.getAList().putObject(A_RUNTIME_EXCEPTION, e);
            Object array[] = { curi };
            this.controller.runtimeErrors.log(Level.SEVERE, curi.getUURI()
                    .toString(), array);
        }
        return kq;
    }

    /**
     * @param curi
     */
    protected void noteAboutToEmit(CrawlURI curi, ExperimentalWorkQueue q) {
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
     * @throws InterruptedException
     * @see org.archive.crawler.framework.Frontier#finished(org.archive.crawler.datamodel.CrawlURI)
     */
    public void finished(CrawlURI curi) {
        long now = System.currentTimeMillis();
        
        curi.incrementFetchAttempts();
        logLocalizedErrors(curi);
        ExperimentalWorkQueue wq = (ExperimentalWorkQueue) curi.getHolder();
        assert (wq.peek() == curi) : "unexpected peek "+wq;

        if (needsRetrying(curi)) {
            // Consider errors which can be retried, leaving uri atop queue
            long delay_sec = retryDelayFor(curi);
            wq.unpeek();
            if (delay_sec>0) {
                long delay = delay_sec*1000;
                daemon.executeAfterDelay(delay, new WakeTask(wq));
            } else {
                try {
                    readyClassQueues.put(wq);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
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
            incrementSucceededFetchCount();
            controller.fireCrawledURISuccessfulEvent(curi); //Let everyone know in case they want to do something before we strip the curi.
            controller.recover.finishedSuccess(curi);        
        } else if(isDisregarded(curi)) {
            // Check for codes that mean that while we the crawler did
            // manage to try it, it must be disregarded for some reason.
            incrementDisregardedUriCount();
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

            incrementFailedFetchCount();
            this.controller.recover.finishedFailure(curi);
        }
        
        long delay_ms = politenessDelayFor(curi);
        synchronized(readyClassQueues) { // TODO: rethink this sync
            if (delay_ms>0) {
                daemon.executeAfterDelay(delay_ms, new WakeTask(wq));
            } else {
                try {
                    readyClassQueues.put(wq);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
        
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
        // TODO: consider synchronizing on something (allClassQueuesMap?)
        return 
            mainQueue.isEmpty() &&
            alreadyIncluded.pending()==0 &&
            allClassQueuesMap.isEmpty();
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
     * @see org.archive.crawler.framework.Frontier#discoveredUriCount()
     */
    public long discoveredUriCount(){
        return alreadyIncluded.count();
    }


    
    /** (non-Javadoc)
     * @see org.archive.crawler.framework.Frontier#getInitialMarker(java.lang.String, boolean)
     */
    public FrontierMarker getInitialMarker(String regexpr, boolean inCacheOnly) {
        ArrayList keyqueueKeys = new ArrayList();
        if(allClassQueuesMap.size()!=0)
        {
            Iterator q = allClassQueuesMap.keySet().iterator();
            while(q.hasNext())
            {
                keyqueueKeys.add(q.next());
            }
        }
        return new HostQueuesFrontierMarker(regexpr,inCacheOnly,keyqueueKeys);
    }

    /** (non-Javadoc)
     *
     * @param marker
     * @param numberOfMatches
     * @param verbose
     * @return List of URIS.
     * @throws InvalidFrontierMarkerException
     */
    public ArrayList getURIsList(FrontierMarker marker, int numberOfMatches,
            boolean verbose) throws InvalidFrontierMarkerException {
        if(marker instanceof HostQueuesFrontierMarker == false){
            throw new InvalidFrontierMarkerException();
        }

        HostQueuesFrontierMarker mark = (HostQueuesFrontierMarker)marker;
        ArrayList list = new ArrayList(numberOfMatches);

        // inspect the KeyedQueues
        while( numberOfMatches > 0 && mark.getCurrentQueue() != -1){
            String queueKey = (String)mark.getKeyQueues().
                get(mark.getCurrentQueue());
            KeyedQueue keyq = (KeyedQueue)allClassQueuesMap.get(queueKey);
            if(keyq==null){
                throw new InvalidFrontierMarkerException();
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
     * @throws InvalidFrontierMarkerException
     */
    private int inspectQueue( KeyedQueue queue,
                              String queueName,
                              ArrayList list,
                              HostQueuesFrontierMarker marker,
                              boolean verbose,
                              int numberOfMatches)
                          throws InvalidFrontierMarkerException{
        if(queue.length() < marker.getAbsolutePositionInCurrentQueue()) {
            // Not good. Invalid marker.
            throw new InvalidFrontierMarkerException();
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
                ExperimentalWorkQueue kq = (ExperimentalWorkQueue)allClassQueuesMap.get(q.next());
                numberOfDeletes += kq.deleteMatchedItems(mat);

                // If our deleting has emptied the KeyedQueue then update it's
                // state.
//                kq.checkEmpty();
            }
        }
        // Delete from pendingQueue
//        numberOfDeletes += pendingQueue.deleteMatchedItems(mat);
        decrementQueuedCount(numberOfDeletes);
        return numberOfDeletes;
    }

    /**
     * @return One-line summary report, useful for display when full report
     * may be unwieldy. 
     */
    public String oneLineReport() {
    	StringBuffer rep = new StringBuffer();
    	rep.append(allClassQueuesMap.size()+" queues: ");
//    	rep.append(readyClassQueues.size()+" ready, ");
//    	rep.append(snoozeQueues.size()+" snoozed, ");
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
        rep.append(" Queued:        " + queuedUriCount() + "\n");
        rep.append(" Finished:      " + finishedUriCount() + "\n");
        rep.append("  Successfully: " + succeededFetchCount() + "\n");
        rep.append("  Failed:       " + failedFetchCount() + "\n");
        rep.append("  Disregarded:  " + disregardedUriCount() + "\n");
        rep.append("\n -----===== QUEUES =====-----\n");
        rep.append(" Already included size:     " + alreadyIncluded.count()+"\n");
//        rep.append(" Pending queue length:      " + pendingQueue.length()+ "\n");
        rep.append("\n All class queues map size: " + allClassQueuesMap.size() + "\n");
        if(allClassQueuesMap.size()!=0)
        {
            Iterator q = allClassQueuesMap.entrySet().iterator();
            while(q.hasNext())
            {
                ExperimentalWorkQueue kq = (ExperimentalWorkQueue)q.next();
                appendKeyedQueue(rep,kq,now);
            }
        }
//        rep.append("\n Ready class queues size:   " + readyClassQueues.size() + "\n");
//        for(int i=0 ; i < readyClassQueues.size() ; i++)
//        {
//            ExperimentalWorkQueue kq = (ExperimentalWorkQueue)readyClassQueues.get(i);
//            appendKeyedQueue(rep,kq,now);
//        }
//
//        rep.append("\n Snooze queues size:        " + snoozeQueues.size() + "\n");
//        if(snoozeQueues.size()!=0)
//        {
//            Object[] q = ((TreeSet)snoozeQueues).toArray();
//            for(int i=0 ; i < q.length ; i++)
//            {
//                if(q[i] instanceof ExperimentalWorkQueue)
//                {
//                    ExperimentalWorkQueue kq = (ExperimentalWorkQueue)q[i];
//                    appendKeyedQueue(rep,kq,now);
//                }
//            }
//        }
        //rep.append("\n Inactive queues size:        " + inactiveClassQueues.size() + "\n");


        return rep.toString();
    }


    private void appendKeyedQueue(StringBuffer rep, ExperimentalWorkQueue kq, long now) {
        rep.append("    ExperimentalWorkQueue  " + kq.getClassKey() + "\n");
        rep.append("     Length:        " + kq.length() + "\n");
//        rep.append("     Is ready:  " + kq.shouldWake() + "\n");
        if(kq instanceof ExperimentalWorkQueue){
//        	rep.append("     Status:        " +
//            ((ExperimentalWorkQueue)kq).getState().toString() + "\n");
//        }
//        if(kq.getState()==ExperimentalWorkQueue.SNOOZED) {
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
     * @see org.archive.crawler.framework.Frontier#deleted(org.archive.crawler.datamodel.CrawlURI)
     */
    public synchronized void deleted(CrawlURI curi) {
        //treat as disregarded
        controller.fireCrawledURIDisregardEvent(curi);
        log(curi);
        incrementDisregardedUriCount();
        curi.stripToMinimal();
        curi.processingCleanup();
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.framework.URIFrontier#considerIncluded(org.archive.crawler.datamodel.UURI)
     */
    public void considerIncluded(UURI u) {
        this.alreadyIncluded.note(u);
    }
    
    protected class WakeTask implements Runnable {
        ExperimentalWorkQueue queue;
        
        /**
         * 
         */
        public WakeTask(ExperimentalWorkQueue q) {
            super();
            queue = q;
        }
        
        /* (non-Javadoc)
         * @see java.lang.Runnable#run()
         */
        public void run() {
            try {
                readyClassQueues.put(queue);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                System.err.println("Queue not woken: "+queue);
            }
        }
        
    }
}


