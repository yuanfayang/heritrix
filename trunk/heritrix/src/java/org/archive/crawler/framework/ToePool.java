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
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

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

    protected int effectiveSize = 0;

    protected boolean paused;

    /**
     * List of toe threads.
     *
     * All iterations need to synchronize on this object if they're to avoid
     * concurrent modification exceptions.
     * See {@link java.util.Collections#synchronizedList(List)}.
     *
     * <p>TODO: Is this list needed?  Why not just put all toe threads into
     * a group and then ask the thread group for the list of extant toe threads?
     */
    protected final List toes;

    /**
     * List of killed toe threads.
     *
     * All iterations need to synchronize on this object if they're to avoid
     * concurrent modification exceptions.
     * See {@link java.util.Collections#synchronizedList(List)}.
     */
    protected List killedToes;


    /**
     * Constructor. Creates a pool of ToeThreads. Threads start in a paused
     * state.
     *
     * @param c A reference to the CrawlController for the current crawl.
     * @param count The number of ToeThreads to start with
     */
    public ToePool(CrawlController c, int count) {
        // Begin in a paused state.
        this.paused = true;
        this.controller = c;
        this.controller.addCrawlStatusListener(this);
        this.toes = Collections.synchronizedList(new ArrayList(count));
        // TODO make number of threads self-optimizing
        setSize(count);
    }

    /**
     * @return The number of ToeThreads that are not available (Approximation).
     */
    public int getActiveToeCount() {
        int count = 0;
        synchronized (this.toes) {
            for(int i = 0; i < this.toes.size(); i++) {
                ToeThread tt = (ToeThread)this.toes.get(i);
                if(tt != null && !tt.isIdleOrDead()) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * @return The number of ToeThreads. This may include killed ToeThreads
     *         that were not replaced.
     */
    public int getToeCount() {
        return this.toes.size();
    }

    /**
     * The crawl controller uses this method to notify the pool that the crawl
     * has ended.
     *
     * All toe threads will be ordered to stop after current.
     *
     * @param statusMessage Supplied status message is of no interest.
     *
     * @see org.archive.crawler.event.CrawlStatusListener#crawlEnding(String)
     */
    public void crawlEnding(String statusMessage) {
        // statusMessage is never used.
        synchronized (this.toes) {
            for(Iterator i = this.toes.iterator(); i.hasNext();) {
                ToeThread t = (ToeThread)i.next();
                t.stopAfterCurrent();
            }
        }
    }

    public void crawlEnded(String statusMessage) {
        // statusMessage is never used.

        // Destory references to facilitate GC.
        synchronized (this.toes) {
            this.toes.removeAll(this.toes);
        }

        if(this.killedToes != null) {
            synchronized (this.killedToes) {
                this.killedToes.removeAll(this.killedToes);
            }
        }

        // TODO Can anything more be done to ensure that the killed threads die?
        this.controller = null;
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

        synchronized (this.toes) {
            for (int i = 0; i < this.toes.size(); i++) {
                ToeThread tt = (ToeThread)this.toes.get(i);
                if(tt!=null) {
                    rep.append("   ToeThread #" + tt.getSerialNumber() + "\n");
                    rep.append(tt.report());
                }
            }
        }

        if (this.killedToes != null) {
            rep.append("\n --- Killed threads --- \n\n");
            synchronized (this.killedToes) {
                for (int i = 0; i < this.killedToes.size(); i++) {
                    rep.append("   Killed ToeThread #" + i + "\n");
                    rep.append(((ToeThread)this.killedToes.get(i)).report());
                }
            }
        }

        return rep.toString();
    }

    /**
     * Change the number of availible ToeThreads.
     *
     * @param newsize The new number of availible ToeThreads.
     */
    public void setSize(int newsize)
    {
        if(newsize > getToeCount())
        {
            // Adding more ToeThreads.
            for(int i = getToeCount(); i < newsize; i++) {
                startNewThread(i);
            }
            this.effectiveSize = newsize;
        }
        else if(newsize < getToeCount())
        {
            this.effectiveSize = newsize;
            // Removing some ToeThreads.
            while(getToeCount() > newsize) {
                synchronized(this.toes) {
                    ToeThread t = (ToeThread)this.toes.get(newsize);
                    // Tell it to exit gracefully
                    t.stopAfterCurrent();
                    // and then remove it from the pool
                    this.toes.remove(newsize);
                }
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
        this.paused = b;
        synchronized(this.toes) {
            for (Iterator i = this.toes.iterator(); i.hasNext();) {
                ((ToeThread)i.next()).setShouldPause(this.paused);
            }
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
        ToeThread toe = null;
        synchronized (this.toes) {
            toe = (ToeThread)this.toes.get(threadNumber);
            toe.kill(threadNumber);
            this.toes.remove(threadNumber);
        }
        if(this.killedToes == null) {
            synchronized (this) {
                if(this.killedToes == null) {
                    this.killedToes =
                        Collections.synchronizedList(new ArrayList(1));
                }
            }
        }
        synchronized (this.killedToes) {
            this.killedToes.add(toe);
        }
        if(replace){
            // Create a new toe thread to take it's place. Replace toe
            startNewThread(threadNumber);
        }
    }

    private void startNewThread(int threadNo) {
        ToeThread newThread = new ToeThread(this.controller, this, threadNo);
        newThread.setPriority(DEFAULT_TOE_PRIORITY);
        // start paused if controller is paused.
        newThread.setShouldPause(this.paused);
        synchronized (this.toes) {
            this.toes.add(threadNo, newThread);
        }
        newThread.start();
    }
}
