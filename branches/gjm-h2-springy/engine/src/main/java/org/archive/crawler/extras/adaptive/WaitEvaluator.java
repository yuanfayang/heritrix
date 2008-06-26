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
package org.archive.crawler.extras.adaptive;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.archive.crawler.datamodel.CrawlURI;
import org.archive.modules.PostProcessor;
import org.archive.modules.Processor;
import org.archive.modules.ProcessorURI;


/**
 * A processor that determines when a URI should be revisited next. Does
 * <b>not</b> account for DNS and robots.txt expiration. That should be 
 * handled seperately by the Frontiers.
 *
 * @author Kristinn Sigurdsson
 */
public class WaitEvaluator extends Processor
implements AdaptiveRevisitAttributeConstants, PostProcessor {
    
    private static final long serialVersionUID = 3L;

    static Logger logger = Logger.getLogger(WaitEvaluator.class.getName());


    /**
     * The initial wait time between revisits. Will then be updated according to
     * crawler experiance. I.e. shorter wait, visit more often, if document has
     * changed between visits, and vica versa.
     */
    {
        setInitialWaitIntervalSeconds(24*60*60L); // 1 day
    }
    public long getInitialWaitIntervalSeconds() {
        return (Long) kp.get("initialWaitIntervalSeconds");
    }
    public void setInitialWaitIntervalSeconds(long secs) {
        kp.put("initialWaitIntervalSeconds",secs);
    }
    
    /**
     * The maximum settable wait time between revisits. Once a URIs wait time
     * reaches this value, it will not grow further, regardless of subsequent
     * visits that discover no changes. Note that this does not ensure that the
     * URI does not wait any longer, since the crawler might be 'behind,'
     * forcing a URI to wait until other URIs, scheduled for earlier are
     * completed.
     */
    {
        setMaxWaitIntervalSeconds(4*7*24*60*60L); // 4 weeks
    }
    public long getMaxWaitIntervalSeconds() {
        return (Long) kp.get("maxWaitIntervalSeconds");
    }
    public void setMaxWaitIntervalSeconds(long secs) {
        kp.put("maxWaitIntervalSeconds",secs);
    }

    /**
     * The minum settable wait time between revisits. Once a URIs wait time
     * reaches this value, it will not be shortened further, regardlesss of
     * subsequent visits that discover changes.
     */
    {
        setMinWaitIntervalSeconds(60*60L); // 1 hour
    }
    public long getMinWaitIntervalSeconds() {
        return (Long) kp.get("minWaitIntervalSeconds");
    }
    public void setMinWaitIntervalSeconds(long secs) {
        kp.put("minWaitIntervalSeconds",secs);
    }
    
    /**
     * The factor by which a URIs wait time is increased when a revisit reveals
     * an unchanged document. A value of 1 will leave it unchanged, a value of 2
     * will double it etc.
     */
    {
        setUnchangedFactor(1.5);
    }
    public double getUnchangedFactor() {
        return (Long) kp.get("unchangedFactor");
    }
    public void setUnchangedFactor(double factor) {
        kp.put("unchangedFactor",factor);
    }


    /**
     * The factor by which a URIs wait time is decreased when a revisit reveals
     * a changed document. A value of 1 will leave it unchanged, a value of two
     * will half it etc.
     */
    {
        setChangedFactor(1.5);
    }
    public double getChangedFactor() {
        return (Long) kp.get("changedFactor");
    }
    public void setChangedFactor(double factor) {
        kp.put("changedFactor",factor);
    }

    /**
     * Fixed wait time for 'unknown' change status. I.e. wait time for URIs
     * whose content change detection is not available.
     */    
    {
        setDefaultWaitIntervalSeconds(3*24*60*60L); // 3 days
    }
    public long getDefaultWaitIntervalSeconds() {
        return (Long) kp.get("defaultWaitIntervalSeconds");
    }
    public void setDefaultWaitIntervalSeconds(long secs) {
        kp.put("defaultWaitIntervalSeconds",secs);
    }
    
    /**
     * Indicates if the amount of time the URI was overdue should be added to
     * the wait time before the new wait time is calculated.
     */
    {
        setUseOverdueTime(false);
    }
    public boolean getUseOverdueTime() {
        return (Boolean) kp.get("useOverdueTime");
    }
    public void setUseOverdueTime(boolean digest) {
        kp.put("useOverdueTime",digest);
    }

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
            
        long min = getMinWaitIntervalSeconds() * 1000L;
        long max = getMaxWaitIntervalSeconds() * 1000L;        
        long waitInterval;
        if (curi.getContentState() == CrawlURI.ContentState.UNKNOWN) {
            waitInterval = getDefaultWaitIntervalSeconds() * 1000L;
        } else {
            /* Calculate curi's time of next processing */ 
            waitInterval = getInitialWaitIntervalSeconds() * 1000L;

            // Retrieve wait interval
            if(curi.containsDataKey(A_WAIT_INTERVAL)) {
                waitInterval = (Long)curi.getData().get(A_WAIT_INTERVAL); 

                // Should override time be taken into account?
                boolean useOverrideTime = getUseOverdueTime(); 
                if(useOverrideTime){
                    waitInterval += curi.getFetchOverdueTime();
                }

                // Revise the wait interval
                if (curi.getContentState() == CrawlURI.ContentState.CHANGED) {
                    // Had changed. Decrease wait interval time.
                    double factor = getChangedFactor();
                    waitInterval = (long)(waitInterval / factor);
                } else if (curi.getContentState() == CrawlURI.ContentState.UNCHANGED) {
                    // Had not changed. Increase wait interval time
                    double factor = getUnchangedFactor();
                    waitInterval = (long)(waitInterval*factor);
                }
            } else {
                // If wait element not found, use initial wait interval 
                waitInterval = getInitialWaitIntervalSeconds() * 1000L;
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
}
