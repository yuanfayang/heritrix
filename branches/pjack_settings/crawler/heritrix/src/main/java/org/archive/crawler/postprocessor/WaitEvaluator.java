/* WaitEvaluator
 * 
 * $Id$
 * 
 * Created on 26.11.2004
 *
 * Copyright (C) 2004 Internet Archive.
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
package org.archive.crawler.postprocessor;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.frontier.AdaptiveRevisitAttributeConstants;
import org.archive.processors.Processor;
import org.archive.processors.ProcessorURI;
import org.archive.state.Key;
import org.archive.state.KeyManager;


/**
 * A processor that determines when a URI should be revisited next. Does
 * <b>not</b> account for DNS and robots.txt expiration. That should be 
 * handled seperately by the Frontiers.
 *
 * @author Kristinn Sigurdsson
 */
public class WaitEvaluator extends Processor
implements AdaptiveRevisitAttributeConstants {
    
    private static final long serialVersionUID = 3L;

    static Logger logger = Logger.getLogger(WaitEvaluator.class.getName());


    /**
     * The initial wait time between revisits. Will then be updated according to
     * crawler experiance. I.e. shorter wait, visit more often, if document has
     * changed between visits, and vica versa.
     */
    final public static Key<Long> INITIAL_WAIT_INTERVAL_SECONDS =
        Key.make(86400L);

    
    /**
     * The maximum settable wait time between revisits. Once a URIs wait time
     * reaches this value, it will not grow further, regardless of subsequent
     * visits that discover no changes. Note that this does not ensure that the
     * URI does not wait any longer, since the crawler might be 'behind,'
     * forcing a URI to wait until other URIs, scheduled for earlier are
     * completed.
     */
    final public static Key<Long> MAX_WAIT_INTERVAL_SECONDS =
        Key.make(2419200L); // 4 weeks


    /**
     * The minum settable wait time between revisits. Once a URIs wait time
     * reaches this value, it will not be shortened further, regardlesss of
     * subsequent visits that discover changes.
     */
    final public static Key<Long> MIN_WAIT_INTERVAL_SECONDS =
        Key.make(3600L); // 1 hour

    
    /**
     * The factor by which a URIs wait time is increased when a revisit reveals
     * an unchanged document. A value of 1 will leave it unchanged, a value of 2
     * will double it etc.
     */
    final public static Key<Double> UNCHANGED_FACTOR = Key.make(1.5);


    /**
     * The factor by which a URIs wait time is decreased when a revisit reveals
     * a changed document. A value of 1 will leave it unchanged, a value of two
     * will half it etc.
     */
    final public static Key<Double> CHANGED_FACTOR = Key.make(1.5);


    /**
     * Fixed wait time for 'unknown' change status. I.e. wait time for URIs
     * whose content change detection is not available.
     */    
    final public static Key<Long> DEFAULT_WAIT_INTERVAL_SECONDS =
        Key.make(259200L); // 3 days


    /**
     * Indicates if the amount of time the URI was overdue should be added to
     * the wait time before the new wait time is calculated.
     */
    final public static Key<Boolean> USE_OVERDUE_TIME = Key.make(false);


    /**
     * Constructor.
     */
    public WaitEvaluator() {
        CrawlURI.getPersistentDataKeys().add(A_WAIT_INTERVAL);
    }

    
    @Override
    protected boolean shouldProcess(ProcessorURI puri) {
        return puri instanceof CrawlURI;
    }
    
    
    @Override
    protected void innerProcess(ProcessorURI puri) throws InterruptedException {
        CrawlURI curi = (CrawlURI)puri;
        if (!isSuccess(curi)) {
            // If the URI was not crawled successfully, we can not reevaluate
            // the wait interval.
            return;
        }

        if (curi.isWaitReevaluated()) {
            // This CrawlURIs wait interval has already been reevaluted during
            // this processing round.
            return;
        }
            
        long min = curi.get(this, MIN_WAIT_INTERVAL_SECONDS) * 1000L;
        long max = curi.get(this, MAX_WAIT_INTERVAL_SECONDS) * 1000L;        
        long waitInterval;
        if (curi.getContentState() == CrawlURI.ContentState.UNKNOWN) {
            waitInterval = curi.get(this, DEFAULT_WAIT_INTERVAL_SECONDS) * 1000L;
        } else {
            /* Calculate curi's time of next processing */ 
            waitInterval = INITIAL_WAIT_INTERVAL_SECONDS.getDefaultValue() 
            * 1000L;

            // Retrieve wait interval
            if(curi.containsDataKey(A_WAIT_INTERVAL)) {
                waitInterval = (Long)curi.getData().get(A_WAIT_INTERVAL); 

                // Should override time be taken into account?
                boolean useOverrideTime = curi.get(this, USE_OVERDUE_TIME); 
                if(useOverrideTime){
                    waitInterval += curi.getFetchOverdueTime();
                }

                // Revise the wait interval
                if (curi.getContentState() == CrawlURI.ContentState.CHANGED) {
                    // Had changed. Decrease wait interval time.
                    double factor = curi.get(this, CHANGED_FACTOR);
                    waitInterval = (long)(waitInterval / factor);
                } else if (curi.getContentState() == CrawlURI.ContentState.UNCHANGED) {
                    // Had not changed. Increase wait interval time
                    double factor = curi.get(this, UNCHANGED_FACTOR);
                    waitInterval = (long)(waitInterval*factor);
                }
            } else {
                // If wait element not found, use initial wait interval 
                waitInterval = curi.get(this, INITIAL_WAIT_INTERVAL_SECONDS) 
                    * 1000L;
            }
        }
        
        if(waitInterval < min){
            waitInterval = min;
        } else if(waitInterval > max){
            waitInterval = max;
        }
        
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("URI " + curi.toString() + ", change: "
                    + curi.getContentState() + " new wait interval: "
                    + waitInterval);
        }
        // Update wait interval
        curi.setWaitInterval(waitInterval);
        curi.setWaitReevaluated(true);
    }
    
    // good to keep at end of source: must run after all per-Key 
    // initialization values are set.
    static {
        KeyManager.addKeys(WaitEvaluator.class);
    }
}
