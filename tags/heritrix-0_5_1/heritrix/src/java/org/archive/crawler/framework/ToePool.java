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
 * ToePool.java
 * Created on Oct 1, 2003
 *
 * $Header$
 */
package org.archive.crawler.framework;

import java.util.ArrayList;
import java.util.logging.Level;

import org.archive.crawler.event.CrawlStatusAdapter;
import org.archive.util.DevUtils;

/**
 * A collection of ToeThreads.
 *
 * @author gojomo
 *
 */
public class ToePool extends CrawlStatusAdapter {
    public static int DEFAULT_TOE_PRIORITY = Thread.NORM_PRIORITY - 1;

    protected CrawlController controller;
    protected ArrayList toes;
    protected int effectiveSize = 0;

    /**
     * Constructor
     *
     * @param c A reference to the CrawlController for the current crawl.
     * @param count The number of ToeThreads to start with
     */
    public ToePool(CrawlController c, int count) {
        controller = c;
        controller.addCrawlStatusListener(this);
        toes = new ArrayList(count);
        // TODO make number of threads self-optimizing
        setSize(count);
    }

    public synchronized ToeThread available() {
        while(true) {
            for(int i=0; i < effectiveSize ; i++){
                if(((ToeThread)toes.get(i)).isAvailable()) {
                    return (ToeThread) toes.get(i);
                }
            }
            // nothing available
            try {
                wait(200);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                DevUtils.logger.log(Level.SEVERE,"available()"+DevUtils.extraInfo(),e);
            }
        }
    }

    /**
     * @param thread
     */
    public synchronized void noteAvailable(ToeThread thread) {
        notify();
    }

    /**
     * @return The number of ToeThreads that are not available
     */
    public int getActiveToeCount() {
        int count = 0;
        // will be an approximation
        for(int i=0; i < toes.size();i++){
            if(!((ToeThread)toes.get(i)).isAvailable()) {
                count++;
            }
        }
        return count;
    }

    /**
     * @return The number of ToeThreads
     */
    public int getToeCount() {
        return toes.size();
    }

    /**
     * The crawl controller uses this method to notify the pool that the crawl has ended.
     * All toe threads will be ordered to stop after current.
     * All references in this object will be set to null to facilitate GC.
     * Once the CrawlController has called this method, this object should be considered
     * as having been destroyed.
     * @param statusMessage
     */
    public void crawlEnding(String statusMessage) {
        while(toes.size()>0)
        {
            ToeThread t = (ToeThread)toes.get(0);
            t.stopAfterCurrent();
            toes.remove(0);
        }
        controller = null;
    }

    public void crawlEnded(String statusMessage)
    {
        toes = null;
    }

    /**
     * Gets a ToeThreads internal status report.
     *
     * @param toe the number of the ToeThread to query.
     * @return ToeThreads internal status report.
     */
    public String getReport(int toe)
    {
        return ((ToeThread)toes.get(toe)).report();
    }
    /**
     * Change the number of availible ToeThreads.
     * @param newsize The new number of availible ToeThreads.
     */
    public void setSize(int newsize)
    {
        if(newsize > getToeCount())
        {
            // Adding more ToeThreads.
            for(int i = getToeCount(); i<newsize; i++) {
                ToeThread newThread = new ToeThread(controller,this,i);
                newThread.setPriority(DEFAULT_TOE_PRIORITY);
                toes.add(newThread);
                newThread.start();
            }
            effectiveSize = newsize;
        }
        else if(newsize < getToeCount())
        {
            effectiveSize = newsize;
            // Removing some ToeThreads.
            while(getToeCount()>newsize)
            {
                ToeThread t = (ToeThread)toes.get(newsize);
                // Tell it to exit gracefully
                t.stopAfterCurrent();
                // and then remove it from the pool
                toes.remove(newsize);
            }
        }
    }
}
