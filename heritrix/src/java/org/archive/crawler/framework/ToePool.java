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

import java.util.Date;
import java.util.Iterator;
import java.util.TreeSet;

import org.archive.util.ArchiveUtils;
import org.archive.util.Histotable;

/**
 * A collection of ToeThreads. The class manages the ToeThreads currently
 * running. Including increasing and decreasing their number, keeping track
 * of their state and it can be used to kill hung threads.
 *
 * @author Gordon Mohr
 * @author Kristinn Sigurdsson
 *
 * @see org.archive.crawler.framework.ToeThread
 */
public class ToePool extends ThreadGroup {
    /** run worker thread slightly lower than usual */
    public static int DEFAULT_TOE_PRIORITY = Thread.NORM_PRIORITY - 1;
    
    protected CrawlController controller;
    protected int nextSerialNumber = 1;
    protected int targetSize = 0; 

    /**
     * Constructor. Creates a pool of ToeThreads. 
     *
     * @param c A reference to the CrawlController for the current crawl.
     */
    public ToePool(CrawlController c) {
        super("ToeThreads");
        this.controller = c;
    }

    /**
     * @return The number of ToeThreads that are not available (Approximation).
     */
    public int getActiveToeCount() {
        Thread[] toes = getToes();
        int count = 0;
        for (int i = 0; i<toes.length; i++) {
            if((toes[i] instanceof ToeThread) &&
                    ((ToeThread)toes[i]).isActive()) {
                count++;
            }
        }
        return count; 
    }

    /**
     * @return The number of ToeThreads. This may include killed ToeThreads
     *         that were not replaced.
     */
    public int getToeCount() {
        Thread[] toes = getToes();
        int count = 0;
        for (int i = 0; i<toes.length; i++) {
            if((toes[i] instanceof ToeThread)) {
                count++;
            }
        }
        return count; 
    }
    
    /**
     * @return One-line summary report, useful for display before drilling
     * into full report.
     */
    public String oneLineReport() {
    	StringBuffer rep = new StringBuffer();
    	Histotable ht = new Histotable();
        Thread[] toes = getToes();
        for (int i = 0; i < toes.length; i++) {

            if(!(toes[i] instanceof ToeThread)) {
                continue;
            }
            ToeThread tt = (ToeThread)toes[i];
            if(tt!=null) {
                ht.tally(tt.getStep());
            }
        }
        TreeSet sorted = ht.getSorted();
        rep.append(getToeCount()+" threads: ");        
        rep.append(Histotable.entryString(sorted.first()));
        if(sorted.size()>1) {
        	Iterator iter = sorted.iterator();
        	iter.next();
            rep.append("; "+Histotable.entryString(iter.next()));
        }
        if(sorted.size()>2) {
        	rep.append("; etc...");
        }
    	return rep.toString();
    }
    
    private Thread[] getToes() {
        Thread[] toes = new Thread[activeCount()+10];
        this.enumerate(toes);
        return toes;
    }

    /**
     * Get ToeThreads internal status report. Presented in human readable form.
     *
     * @return ToeThreads internal status report.
     */
    public String report()
    {
        StringBuffer rep = new StringBuffer(32);
        rep.append("Toe threads report - " +
                ArchiveUtils.TIMESTAMP12.format(new Date()) + "\n");
        rep.append(" Job being crawled: " +
                this.controller.getOrder().getCrawlOrderName() + "\n");
        rep.append(" Number of toe threads in pool: " +
            getToeCount() + " (" + getActiveToeCount() + " active)\n");

        Thread[] toes = this.getToes();
        synchronized (toes) {
            for (int i = 0; i < toes.length ; i++) {
                if(!(toes[i] instanceof ToeThread)) {
                    continue;
                }
                ToeThread tt = (ToeThread)toes[i];
                if(tt!=null) {
                    rep.append("   ToeThread #" + tt.getSerialNumber() + "\n");
                    rep.append(tt.report());
                }
            }
        }
        return rep.toString();
    }

    /**
     * Change the number of ToeThreads.
     *
     * @param newsize The new number of ToeThreads.
     */
    public void setSize(int newsize)
    {
        targetSize = newsize;
        int toSpawn = newsize - getToeCount(); 
        for(int i = 1; i <= toSpawn; i++) {
            startNewThread();
        }
    }

    /**
     * Kills specified thread. Killed thread can be optionally replaced with a
     * new thread.
     *
     * <p><b>WARNING:</b> This operation should be used with great care. It may
     * destabilize the crawler.
     *
     * @param threadNumber Thread to kill
     * @param replace If true then a new thread will be created to take the
     *           killed threads place. Otherwise the total number of threads
     *           will decrease by one.
     */
    public void killThread(int threadNumber, boolean replace){

        Thread[] toes = getToes();
        for (int i = 0; i< toes.length; i++) {
            if(! (toes[i] instanceof ToeThread)) {
                continue;
            }
            ToeThread toe = (ToeThread) toes[i];
            if(toe.getSerialNumber()==threadNumber) {
                toe.kill();
            }
        }

        if(replace){
            // Create a new toe thread to take its place. Replace toe
            startNewThread();
        }
    }

    private synchronized void startNewThread() {
        ToeThread newThread = new ToeThread(this, nextSerialNumber++);
        newThread.setPriority(DEFAULT_TOE_PRIORITY);
        newThread.start();
    }

    /**
     * @return Instance of CrawlController.
     */
    public CrawlController getController() {
        return controller;
    }
}
