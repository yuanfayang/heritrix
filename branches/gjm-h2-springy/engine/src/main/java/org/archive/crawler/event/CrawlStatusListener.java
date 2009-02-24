/*
 *  This file is part of the Heritrix web crawler (crawler.archive.org).
 *
 *  Licensed to the Internet Archive (IA) by one or more individual 
 *  contributors. 
 *
 *  The IA licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.archive.crawler.event;

import java.io.File;

import org.archive.crawler.framework.CrawlControllerImpl;
import org.archive.state.StateProvider;
import org.springframework.context.ApplicationListener;


/**
 * Listen for CrawlStatus events.
 * 
 * Classes that implement this interface can register themselves with
 * a CrawlController to receive notifications about the events that
 * affect a crawl job's current status.
 *
 * @author Kristinn Sigurdsson
 *
 * @see org.archive.crawler.framework.CrawlControllerImpl#addCrawlStatusListener(CrawlStatusListener)
 */

public interface CrawlStatusListener extends ApplicationListener {
    /**
     * Called on crawl start.
     * @param message Start message.
     */
    public void crawlStarted(String message);
    
    /**
     * Called when a CrawlController is ending a crawl (for any reason)
     *
     * @param sExitMessage Type of exit. Should be one of the STATUS constants
     * in defined in CrawlJob.
     *
     * @see org.archive.crawler.admin.CrawlJob
     */
    public void crawlEnding(String sExitMessage);

    /**
     * Called when a CrawlController has ended a crawl and is about to exit.
     *
     * @param sExitMessage Type of exit. Should be one of the STATUS constants
     * in defined in CrawlJob.
     *
     * @see org.archive.crawler.admin.CrawlJob
     */
    public void crawlEnded(String sExitMessage);

    /**
     * Called when a CrawlController is going to be paused.
     *
     * @param statusMessage Should be
     * {@link org.archive.crawler.admin.CrawlJob#STATUS_WAITING_FOR_PAUSE
     * STATUS_WAITING_FOR_PAUSE}. Passed for convenience
     */
    public void crawlPausing(String statusMessage);

    /**
     * Called when a CrawlController is actually paused (all threads are idle).
     *
     * @param statusMessage Should be
     * {@link org.archive.crawler.admin.CrawlJob#STATUS_PAUSED}. Passed for
     * convenience
     */
    public void crawlPaused(String statusMessage);

    /**
     * Called when a CrawlController is resuming a crawl that had been paused.
     *
     * @param statusMessage Should be
     * {@link org.archive.crawler.admin.CrawlJob#STATUS_RUNNING}. Passed for
     * convenience
     */
    public void crawlResuming(String statusMessage);
    
    /**
     * Called by {@link CrawlControllerImpl} when checkpointing.
     * @param checkpointDir Checkpoint dir.  Write checkpoint state here.
     * @throws Exception A fatal exception.  Any exceptions
     * that are let out of this checkpoint are assumed fatal
     * and terminate further checkpoint processing.
     */
    public void crawlCheckpoint(StateProvider provider,
            File checkpointDir) throws Exception;

}
