/* RuntimeLimitEnforcer
 * 
 * Created on July 7, 2006
 *
 * Copyright (C) 2005 Internet Archive.
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
package org.archive.crawler.prefetch;


import java.util.logging.Level;
import java.util.logging.Logger;

import org.archive.crawler.datamodel.CrawlURI;

import static org.archive.modules.fetcher.FetchStatusCodes.*;

import org.archive.crawler.framework.CrawlControllerImpl;
import org.archive.crawler.framework.CrawlStatus;
import org.archive.crawler.framework.StatisticsTracker;
import org.archive.modules.ProcessResult;
import org.archive.modules.Processor;
import org.archive.modules.ProcessorURI;
import org.archive.state.Key;
import org.archive.state.KeyManager;


/**
 * A processor to enforce runtime limits on crawls.
 * <p>
 * This processor extends and improves on the 'max-time' capability of Heritrix.
 * Essentially, the 'Terminate job' option functions the same way as 'max-time'. 
 * The processor however also enables pausing when the runtime is exceeded and  
 * the blocking of all URIs. 
 * <p>
 * <ol>
 * <li>Pause job - Pauses the crawl. A change (increase) to the 
 *     runtime duration will make it pausible to resume the crawl. 
 *     Attempts to resume the crawl without modifying the run time 
 *     will cause it to be immediately paused again.</li>
 * <li>Terminate job - Terminates the job. Equivalent
 *     to using the max-time setting on the CrawlController.</li>
 * <li>Block URIs - Blocks each URI with an -5002
 *     (blocked by custom processor) fetch status code. This will
 *     cause all the URIs queued to wind up in the crawl.log.</li>
 * <ol>
 * <p>
 * The processor allows variable runtime based on host (or other  
 * override/refinement criteria) however using such overrides only makes sense  
 * when using 'Block URIs' as pause and terminate will have global impact once
 * encountered anywhere. 
 * 
 * @author Kristinn Sigur&eth;sson
 */
public class RuntimeLimitEnforcer extends Processor {

    private static final long serialVersionUID = 3L;
    
    protected static Logger logger = Logger.getLogger(
            RuntimeLimitEnforcer.class.getName());

    
    final public static Key<CrawlControllerImpl> CONTROLLER = 
        Key.makeAuto(CrawlControllerImpl.class);
    
    final public static Key<StatisticsTracker> STATISTICS_TRACKER =
        Key.makeAuto(StatisticsTracker.class);

    /**
     * The action that the processor takes once the runtime has elapsed.
     */
    public static enum Operation { 

        /**
         * Pauses the crawl. A change (increase) to the runtime duration will
         * make it pausible to resume the crawl. Attempts to resume the crawl
         * without modifying the run time will cause it to be immediately paused
         * again.
         */
        PAUSE, 
        
        /**
         * Terminates the job. Equivalent to using the max-time setting on the
         * CrawlController.
         */
        TERMINATE, 
        
        /**
         * Blocks each URI with an -5002 (blocked by custom processor) fetch
         * status code. This will cause all the URIs queued to wind up in the
         * crawl.log.
         */
        BLOCK_URIS 
    };
    
    /**
     * The amount of time, in seconds, that the crawl will be allowed to run
     * before this processor performs it's 'end operation.'
     */
    final public static Key<Long> RUNTIME_SECONDS = Key.make(86400L);


    /**
     * The action that the processor takes once the runtime has elapsed.
     * <p>
     * Operation: Pause job - Pauses the crawl. A change (increase) to the
     * runtime duration will make it pausible to resume the crawl. Attempts to
     * resume the crawl without modifying the run time will cause it to be
     * immediately paused again.
     * <p>
     * Operation: Terminate job - Terminates the job. Equivalent to using the
     * max-time setting on the CrawlController.
     * <p>
     * Operation: Block URIs - Blocks each URI with an -5002 (blocked by custom
     * processor) fetch status code. This will cause all the URIs queued to wind
     * up in the crawl.log.
     */
    final public static Key<Operation> END_OPERATION = Key.make(Operation.PAUSE);

    
    public RuntimeLimitEnforcer() {
        super();
    }

    
    @Override
    protected boolean shouldProcess(ProcessorURI puri) {
        return puri instanceof CrawlURI;
    }

    
    @Override 
    protected void innerProcess(ProcessorURI curi) {
        throw new AssertionError();
    }
    
    @Override
    protected ProcessResult innerProcessResult(ProcessorURI curi)
    throws InterruptedException {
        CrawlControllerImpl controller = curi.get(this, CONTROLLER);
        StatisticsTracker stats = curi.get(this, STATISTICS_TRACKER);
        long allowedRuntime = getRuntime(curi);
        long currentRuntime = stats.crawlDuration();
        if(currentRuntime > allowedRuntime){
            Operation op = curi.get(this, END_OPERATION);
            if(op != null){
                if (op.equals(Operation.PAUSE)) {
                    controller.requestCrawlPause();
                } else if (op.equals(Operation.TERMINATE)){
                    controller.requestCrawlStop(CrawlStatus.FINISHED_TIME_LIMIT);
                } else if (op.equals(Operation.BLOCK_URIS)) {
                    curi.setFetchStatus(S_BLOCKED_BY_RUNTIME_LIMIT);
                    curi.getAnnotations().add("Runtime exceeded " + allowedRuntime + 
                            "ms");
                    return ProcessResult.FINISH;
                }
            } else {
                logger.log(Level.SEVERE,"Null value for end-operation " + 
                        " when processing " + curi.toString());
            }
        }
        return ProcessResult.PROCEED;
    }
    
    /**
     * Returns the amount of time to allow the crawl to run before this 
     * processor interrupts.
     * @return the amount of time in milliseconds.
     */
    protected long getRuntime(ProcessorURI curi){
        return curi.get(this, RUNTIME_SECONDS) * 1000L;
    }
    
    // good to keep at end of source: must run after all per-Key 
    // initialization values are set.
    static {
        KeyManager.addKeys(RuntimeLimitEnforcer.class);
    }
}
