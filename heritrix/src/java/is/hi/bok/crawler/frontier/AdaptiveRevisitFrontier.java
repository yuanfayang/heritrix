/* AdaptiveRevisitFrontier.java
*
* Created on Sep 13, 2004
*
* Copyright (C) 2004 Kristinn Sigurðsson.
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
*/
package is.hi.bok.crawler.frontier;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.archive.crawler.datamodel.CandidateURI;
import org.archive.crawler.datamodel.CoreAttributeConstants;
import org.archive.crawler.datamodel.CrawlServer;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.FetchStatusCodes;
import org.archive.crawler.datamodel.UURI;
import org.archive.crawler.framework.CrawlController;
import org.archive.crawler.framework.Frontier;
import org.archive.crawler.framework.FrontierMarker;
import org.archive.crawler.framework.exceptions.EndedException;
import org.archive.crawler.framework.exceptions.FatalConfigurationException;
import org.archive.crawler.framework.exceptions.InvalidFrontierMarkerException;
import org.archive.crawler.frontier.FrontierJournal;
import org.archive.crawler.settings.ModuleType;


/**
 * A Frontier that will repeatedly visit all encountered URIs. 
 * <p>
 * Wait time between visits is configurable and varies based on observed 
 * changes of documents.
 * <p>
 * The Frontier borrows many things from HostQueuesFrontier, but implements 
 * an entirely different strategy in issuing URIs and consequently in keeping a
 * record of discovered URIs.
 *
 * @author Kristinn Sigurdsson
 */
public class AdaptiveRevisitFrontier extends ModuleType 
        implements Frontier, FetchStatusCodes, CoreAttributeConstants {

    // Constants for storing information in a CrawlURI's AList
    public static final String A_TIME_OF_NEXT_PROCESSING = 
        "time-of-next-processing";

    protected CrawlController controller;
    
    protected ARHostQueueList hostQueues;
    
    /**
     * @param name
     */
    public AdaptiveRevisitFrontier(String name) {
        this(name,"ARFrontier. A Frontier that will repeatedly visit all " +
                "encountered URIs. \nWait time between visits is configurable" +
                " and varies based on observed changes of documents."); 
        
       
    }

    /**
     * @param name
     * @param description
     */
    public AdaptiveRevisitFrontier(String name, String description) {
        super(Frontier.ATTR_NAME,description);
        
    }

    
    /* (non-Javadoc)
     * @see org.archive.crawler.framework.Frontier#initialize(org.archive.crawler.framework.CrawlController)
     */
    public synchronized void initialize(CrawlController c)
            throws FatalConfigurationException, IOException {
        controller = c;
        
        hostQueues = new ARHostQueueList(c.getStateDisk());
        
        loadSeeds();
    }

    /**
     * Loads the seeds
     * <p>
     * This method is called by initialize() and kickUpdate()
     */
    private void loadSeeds() throws IOException {
        // Get the seeds to refresh and then get an iterator inside a
        // synchronization block.  The seeds list may get updated during our
        // iteration. This will throw a concurrentmodificationexception unless
        // we synchronize.
        //
        List seeds = this.controller.getScope().getSeedlist();
        synchronized(seeds) {
            for (Iterator i = seeds.iterator(); i.hasNext();) {
                UURI u = (UURI)i.next();
                CandidateURI caUri = new CandidateURI(u);
                caUri.setSeed();
                caUri.setSchedulingDirective(CandidateURI.MEDIUM);
                innerSchedule(caUri);
            }
        }
    }

    /**
     * 
     * @param caUri The URI to schedule.
     */
    private void innerSchedule(CandidateURI caUri) throws IOException {
        // TODO: Should method throw IOException?
        CrawlURI curi;
        if(caUri instanceof CrawlURI) {
            curi = (CrawlURI) caUri;
        } else {
            curi = CrawlURI.from(caUri,System.currentTimeMillis());
        }
        curi.setServer(getServer(curi));
        curi.setClassKey(curi.getServer().getHostname());

        hostQueues.getHQ(curi.getClassKey()).add(curi,false); //TODO: Handle embedds
        
    }
    
    /**
     * @param curi
     * @return the CrawlServer to be associated with this CrawlURI
     */
    protected CrawlServer getServer(CrawlURI curi) {
        return this.controller.getServerCache().getServerFor(curi);
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.framework.Frontier#next()
     */
    public synchronized CrawlURI next() 
            throws InterruptedException, EndedException {
        if(hostQueues.getSize()==0){
            // This crawl is over
            // TODO: Handle this
        }
        
        ARHostQueue hq = hostQueues.getTopHQ();
        
        while(hq.getState() != ARHostQueue.HQSTATE_READY){
            // Ok, so we don't have a ready queue, wait until the top one
            // will become available.
            wait(hq.getNextReadyTime());
            hq = hostQueues.getTopHQ(); //A busy hq may have become 'unbusy'
        }             
        
        try {
            return hq.next();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return null;
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.framework.Frontier#isEmpty()
     */
    public boolean isEmpty() {
        // TODO: There may be 'batched' URIs waiting!
        return hostQueues.getSize() == 0;
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.framework.Frontier#schedule(org.archive.crawler.datamodel.CandidateURI)
     */
    public void schedule(CandidateURI caURI) {
        // TODO Auto-generated method stub
        // Use thread based temp location.
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.framework.Frontier#finished(org.archive.crawler.datamodel.CrawlURI)
     */
    public synchronized void finished(CrawlURI cURI) {
        // TODO Auto-generated method stub
        // flush URIs waiting to be scheduled
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.framework.Frontier#discoveredUriCount()
     */
    public long discoveredUriCount() {
        // TODO Auto-generated method stub
        return 0;
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.framework.Frontier#queuedUriCount()
     */
    public long queuedUriCount() {
        // TODO Auto-generated method stub
        return 0;
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.framework.Frontier#finishedUriCount()
     */
    public long finishedUriCount() {
        // TODO Auto-generated method stub
        return 0;
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.framework.Frontier#succeededFetchCount()
     */
    public long succeededFetchCount() {
        // TODO Auto-generated method stub
        return 0;
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.framework.Frontier#failedFetchCount()
     */
    public long failedFetchCount() {
        // TODO Auto-generated method stub
        return 0;
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.framework.Frontier#disregardedUriCount()
     */
    public long disregardedUriCount() {
        // TODO Auto-generated method stub
        return 0;
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.framework.Frontier#totalBytesWritten()
     */
    public long totalBytesWritten() {
        // TODO Auto-generated method stub
        return 0;
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.framework.Frontier#oneLineReport()
     */
    public String oneLineReport() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.framework.Frontier#report()
     */
    public synchronized String report() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.framework.Frontier#importRecoverLog(java.lang.String)
     */
    public void importRecoverLog(String pathToLog) throws IOException {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see org.archive.crawler.framework.Frontier#getInitialMarker(java.lang.String, boolean)
     */
    public synchronized FrontierMarker getInitialMarker(String regexpr, boolean inCacheOnly) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.framework.Frontier#getURIsList(org.archive.crawler.framework.FrontierMarker, int, boolean)
     */
    public synchronized ArrayList getURIsList(FrontierMarker marker, int numberOfMatches,
            boolean verbose) throws InvalidFrontierMarkerException {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.framework.Frontier#deleteURIs(java.lang.String)
     */
    public synchronized long deleteURIs(String match) {
        // TODO Auto-generated method stub
        return 0;
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.framework.Frontier#deleted(org.archive.crawler.datamodel.CrawlURI)
     */
    public synchronized void deleted(CrawlURI curi) {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see org.archive.crawler.framework.Frontier#considerIncluded(org.archive.crawler.datamodel.UURI)
     */
    public void considerIncluded(UURI u) {
        // TODO Make into CrawlURI and schedule

    }

    /* (non-Javadoc)
     * @see org.archive.crawler.framework.Frontier#kickUpdate()
     */
    public synchronized void kickUpdate() {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see org.archive.crawler.framework.Frontier#pause()
     */
    public synchronized void pause() {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see org.archive.crawler.framework.Frontier#unpause()
     */
    public synchronized void unpause() {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see org.archive.crawler.framework.Frontier#terminate()
     */
    public synchronized void terminate() {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see org.archive.crawler.framework.Frontier#getFrontierJournal()
     */
    public FrontierJournal getFrontierJournal() {
        // TODO Auto-generated method stub
        return null;
    }

}
