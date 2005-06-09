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

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Date;
import java.util.Iterator;
import java.util.TreeSet;

import org.archive.util.ArchiveUtils;
import org.archive.util.Histotable;
import org.archive.util.Reporter;

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
public class ToePool extends ThreadGroup implements Reporter {
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
        StringWriter sw = new StringWriter();
        reportTo(sw);
        return sw.toString();
    }

    /**
     * Change the number of ToeThreads.
     *
     * @param newsize The new number of ToeThreads.
     */
    public void setSize(int newsize)
    {
        targetSize = newsize;
        int difference = newsize - getToeCount(); 
        if (difference > 0) {
            // must create threads
            for(int i = 1; i <= difference; i++) {
                startNewThread();
            }
        } else {
            // must retire extra threads
            int retainedToes = targetSize; 
            Thread[] toes = this.getToes();
            for (int i = 0; i < toes.length ; i++) {
                if(!(toes[i] instanceof ToeThread)) {
                    continue;
                }
                retainedToes--;
                if (retainedToes>=0) {
                    continue; // this toe is spared
                }
                // otherwise:
                ToeThread tt = (ToeThread)toes[i];
                tt.retire();
            }
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

    /* (non-Javadoc)
     * @see org.archive.util.Reporter#reportTo(java.io.Writer)
     */
    public void reportTo(Writer writer) {
        try {
            writer.write("Toe threads report - " +
                    ArchiveUtils.TIMESTAMP12.format(new Date()) + "\n");
            writer.write(" Job being crawled: " +
                    this.controller.getOrder().getCrawlOrderName() + "\n");
            writer.write(" Number of toe threads in pool: " +
                getToeCount() + " (" + getActiveToeCount() + " active)\n");

            Thread[] toes = this.getToes();
            synchronized (toes) {
                for (int i = 0; i < toes.length ; i++) {
                    if(!(toes[i] instanceof ToeThread)) {
                        continue;
                    }
                    ToeThread tt = (ToeThread)toes[i];
                    if(tt!=null) {
                        writer.write("   ToeThread #" + tt.getSerialNumber() + "\n");
                        tt.reportTo(writer);
                    }
                }
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    /* (non-Javadoc)
     * @see org.archive.util.Reporter#reportTo(java.io.Writer)
     */
    public void compactReportTo(Writer writer) {
        try {
            writer.write(
                getToeCount() + " threads (" + getActiveToeCount() + " active)\n");

            Thread[] toes = this.getToes();
            synchronized (toes) {
                for (int i = 0; i < toes.length ; i++) {
                    if(!(toes[i] instanceof ToeThread)) {
                        continue;
                    }
                    ToeThread tt = (ToeThread)toes[i];
                    if(tt!=null) {
                        writer.write(tt.oneLineReport());
                    }
                }
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
