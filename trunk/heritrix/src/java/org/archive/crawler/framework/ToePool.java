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
import java.util.Date;
import java.util.Iterator;

import org.archive.crawler.event.CrawlStatusAdapter;
import org.archive.util.ArchiveUtils;

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
public class ToePool extends CrawlStatusAdapter {
    /** run worker thread slightly lower than usual */
    public static int DEFAULT_TOE_PRIORITY = Thread.NORM_PRIORITY - 1;

    protected CrawlController controller;
    protected ArrayList toes;
    protected ArrayList killedToes = null;
    protected int effectiveSize = 0;

    protected boolean paused;

    /**
     * Constructor. Creates a pool of ToeThreads. Threads start in a paused
     * state.
     *
     * @param c A reference to the CrawlController for the current crawl.
     * @param count The number of ToeThreads to start with
     */
    public ToePool(CrawlController c, int count) {
        paused = true; // Begin in a paused state
        controller = c;
        controller.addCrawlStatusListener(this);
        toes = new ArrayList(count);
        // TODO make number of threads self-optimizing
        setSize(count);
    }

//  No longer used. Threads 'pull' work now. 
//    public synchronized ToeThread available() {
//        while(true) {
//            for(int i=0; i < effectiveSize ; i++){
//                if(((ToeThread)toes.get(i)).isAvailable()) {
//                    return (ToeThread) toes.get(i);
//                }
//            }
//            // nothing available
//            try {
//                wait(200);
//            } catch (InterruptedException e) {
//                DevUtils.logger.log(Level.SEVERE,"available()"+DevUtils.extraInfo(),e);
//            }
//        }
//    }
//
//    /**
//     * @param thread
//     */
//    public synchronized void noteAvailable(ToeThread thread) {
//        notify();
//    }

    /**
     * @return The number of ToeThreads that are not available
     */
    public int getActiveToeCount() {
        int count = 0;
        // will be an approximation
        for(int i=0; i < toes.size();i++){
            ToeThread tt = (ToeThread)toes.get(i);
            if(tt!=null && !tt.isIdleOrDead()) {
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
        return toes.size();
    }

    /**
     * The crawl controller uses this method to notify the pool that the crawl 
     * has ended.
     * All toe threads will be ordered to stop after current.
     * 
     * @param statusMessage Supplied status message is of no interest.
     * 
     * @see org.archive.crawler.event.CrawlStatusListener#crawlEnding(String)
     */
    public void crawlEnding(String statusMessage) {
        Iterator it = toes.iterator();
        while(it.hasNext())
        {
            ToeThread t = (ToeThread)it.next();
            t.stopAfterCurrent();
        }
    }

    /** 
     * @see org.archive.crawler.event.CrawlStatusListener#crawlEnded(java.lang.String)
     */
    public void crawlEnded(String statusMessage)
    {
        // Destory referances to facilitate GC.
        toes.removeAll(toes); //Empty it
        // toes = null; // gjm: superfluous
        if(killedToes!=null){
            killedToes.removeAll(killedToes);
        }
        // TODO Can anything more be done to ensure that the killed threads die?
        // killedToes = null; // gjm: superfluous
        controller = null;
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
                controller.getOrder().getCrawlOrderName() + "\n");
        rep.append(" Number of toe threads in pool: " + 
            getToeCount() + " (" + getActiveToeCount() + " active)\n");

        for (int i = 0; i < toes.size(); i++) {
            ToeThread tt = (ToeThread)toes.get(i);
            if(tt!=null) {
                rep.append("   ToeThread #" + tt.getSerialNumber() + "\n");
                rep.append(tt.report());
            }
        }
        
        if (killedToes != null){
            rep.append("\n --- Killed threads --- \n\n");
            for (int i = 0; i < killedToes.size(); i++) {
                rep.append("   Killed ToeThread #" + i + "\n");
                rep.append(((ToeThread)killedToes.get(i)).report());
            }
        }
            
        return rep.toString();
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
				// start paused if controller is paused.
                newThread.setShouldPause(paused); 
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

    /**
     * Broadcasts a new value for <tt>shouldPause</tt> to all ToeThreads.
     * If this value is true then all threads should enter a paused state as
     * soon as possible and stay there. If false then the treads should 
     * resume (continue) their work. The ToePool will also remember the 
     * current value and issue it to any new ToeThreads that may be created.
     * @param b New value for <tt>shouldPause</tt>
     */
    protected void setShouldPause(boolean b) {
        Iterator iter = toes.iterator();
        paused = b;
        while(iter.hasNext()) {
            ((ToeThread)iter.next()).setShouldPause(paused);
        }
    }

    /** 
     * @see org.archive.crawler.event.CrawlStatusListener#crawlPausing(java.lang.String)
     */
    public void crawlPausing(String statusMessage) {
        setShouldPause(true);
    }
    
    /** 
     * @see org.archive.crawler.event.CrawlStatusListener#crawlResuming(java.lang.String)
     */
    public void crawlResuming(String statusMessage) {
        setShouldPause(false);
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
        // Thread number should always be equal to it's placement in toes.
        ToeThread toe = (ToeThread)toes.get(threadNumber);
        toe.kill(threadNumber);
        toes.remove(threadNumber);
        if(killedToes==null){
            killedToes = new ArrayList(1);
        }
        killedToes.add(toe);
        if(replace){
            // create a new toe thread to take it's place.
            // Replace toe
            ToeThread newThread = new ToeThread(controller,this,threadNumber);
            newThread.setPriority(DEFAULT_TOE_PRIORITY);
            // start paused if controller is paused.
            newThread.setShouldPause(paused); 
            toes.add(threadNumber,newThread);
            newThread.start();
        } 
    }
}
