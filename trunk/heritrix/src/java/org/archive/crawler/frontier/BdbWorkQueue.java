/* BdbWorkQueue
 * 
 * Created on Dec 24, 2004
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
package org.archive.crawler.frontier;

import java.io.Serializable;

import org.archive.crawler.datamodel.CrawlURI;
import org.archive.util.ArchiveUtils;

import st.ata.util.FPGenerator;

import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;


/**
 * One independent queue of items with the same 'classKey' (eg host). 
 * 
 * @author gojomo
 */
public class BdbWorkQueue
implements Comparable, Serializable {
    private String classKey;
    
    /** Total number of stored items */
    private long count = 0;
    
    /** The next item to be returned */ 
    private CrawlURI peekItem = null;

    /** Key coordinate to begin seeks, to find queue head */
    private byte[] origin; 

    // Useful for reporting
    /** Last URI enqueued */
    private String lastQueued; 
    /** Last URI peeked */
    private String lastPeeked;

    /** Whether queue is already in lifecycle stage */
    boolean isHeld = false; 
    
    /** Time to wake, if snoozed */
    private long wakeTime = 0;
    
    /** Running 'budget' indicating whether queue should stay active */
    private int sessionBalance = 0;
    
    /** Cost of the last item to be charged against queue */
    private int lastCost = 0;
    
    /** Total number of items charged against queue; with totalExpenditure
     * can be used to calculate 'average cost'. */
    private long costCount = 0;
    
    /** Running tally of total expenditures on this queue */
    private long totalExpenditure = 0;
    
    /** Total to spend on this queue over its lifetime */
    private long totalBudget = 0;

    /**
     * Create a virtual queue inside the given BdbMultipleWorkQueues 
     * 
     * @param classKey
     */
    public BdbWorkQueue(String classKey) {
        this.classKey = classKey;
        origin = new byte[16];
        long fp = FPGenerator.std64.fp(classKey) & 0xFFFFFFFFFFFFFFF0l;
        ArchiveUtils.longIntoByteArray(fp, origin, 0);
    }

    /**
     * Set the session 'activity budget balance' to the given value
     * 
     * @param balance to use
     */
    public void setSessionBalance(int balance) {
        this.sessionBalance = balance;
    }

    /**
     * Return current session 'activity budget balance' 
     * 
     * @return session balance
     */
    public int getSessionBalance() {
        return this.sessionBalance;
    }

    /**
     * Set the total expenditure level allowable before queue is 
     * considered inherently 'over-budget'. 
     * 
     * @param budget
     */
    public void setTotalBudget(long budget) {
        this.totalBudget = budget;
    }

    /**
     * Check whether queue has temporarily or permanently exceeded
     * its budget. 
     * 
     * @return true if queue is over its set budget(s)
     */
    public boolean isOverBudget() {
        // check whether running balance is depleted 
        // or totalExpenditure exceeds totalBudget
        return this.sessionBalance <= 0
                || (this.totalBudget >= 0 
                        && this.totalExpenditure > this.totalBudget);
    }
    
    /**
     * Return the tally of all expenditures on this queue
     * 
     * @return total amount expended on this queue
     */
    public long getTotalExpenditure() {
        return totalExpenditure;
    }

    /**
     * Increase the internal running budget to be used before 
     * deactivating the queue
     * 
     * @param amount amount to increment
     * @return updated budget value
     */
    public int incrementSessionBalance(int amount) {
        this.sessionBalance = this.sessionBalance + amount;
        return this.sessionBalance;
    }

    /**
     * Decrease the internal running budget by the given amount. 
     * @param amount tp decrement
     * @return updated budget value
     */
    public int expend(int amount) {
        this.sessionBalance = this.sessionBalance - amount;
        this.totalExpenditure = this.totalExpenditure + amount;
        this.lastCost = amount;
        this.costCount++;
        return this.sessionBalance;
    }

    /**
     * Delete URIs matching the given pattern from this queue. 
     * @param queues WorkQueues manager.
     * @param match
     * @return count of deleted URIs
     */
    public long deleteMatching(BdbMultipleWorkQueues queues, String match) {
        try {
            long deleteCount =
                queues.deleteMatchingFromQueue(match, classKey,
                    new DatabaseEntry(origin));
            this.count -= deleteCount;
            return deleteCount;
        } catch (DatabaseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    /**
     * @return a string describing this queue
     */
    public String report() {
        return "Queue " + classKey + "\n" + 
               "  "+ count + " items" + "\n" +
               "    last enqueued: " + lastQueued + "\n" +
               "      last peeked: " + lastPeeked + "\n" +
               ((wakeTime == 0) ? "" :
               "         wakes in: " + 
                       (wakeTime-System.currentTimeMillis()) + "ms\n") +
               "   total expended: " + totalExpenditure + 
                   " (total budget: " + totalBudget + ")\n" +
               "   active balance: " + sessionBalance + "\n" +
               "   last(avg) cost: " + lastCost + 
                   "("+ArchiveUtils.doubleToString(
                           ((double)totalExpenditure/costCount),1)+")\n";
    }

    /**
     * @param l
     */
    public void setWakeTime(long l) {
        wakeTime = l;
    }

    /**
     * @return wakeTime
     */
    public long getWakeTime() {
        return wakeTime;
    }
    
    /**
     * @return classKey
     */
    public String getClassKey() {
        return classKey;
    }
    
    /**
     * Clear isHeld to false
     */
    public void clearHeld() {
        isHeld = false;
    }

    /**
     * Whether the queue is already in a lifecycle stage --
     * such as ready, in-progress, snoozed -- and thus should
     * not be redundantly inserted to readyClassQueues
     * 
     * @return isHeld
     */
    public boolean isHeld() {
        return isHeld;
    }

    /**
     * Set isHeld to true
     */
    public void setHeld() {
        isHeld = true; 
    }

    /**
     * Remove the peekItem from the queue. 
     * @param queues Work queues manager.
     * 
     */
    public synchronized void dequeue(BdbMultipleWorkQueues queues) {
        try {
            queues.delete(peekItem);
        } catch (DatabaseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        unpeek();
        count--;
    }

    /**
     * Forgive the peek, allowing a subsequent peek to 
     * return a different item. 
     * 
     */
    public void unpeek() {
        peekItem = null;
    }

    /**
     * Return the topmost queue item -- and remember it,
     * such that even later higher-priority inserts don't
     * change it. 
     * 
     * TODO: evaluate if this is really necessary
     * @param queues Work queues manager
     * 
     * @return topmost queue item
     */
    public CrawlURI peek(BdbMultipleWorkQueues queues) {
        if (peekItem == null && count > 0) {
            try {
                peekItem = queues.get(new DatabaseEntry(origin));
                lastPeeked = peekItem.getURIString();
            } catch (DatabaseException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
        return peekItem;
    }

    /**
     * Add the given CrawlURI, noting its addition in running count. (It
     * should not already be present.)
     * 
     * @param queues Work queues manager.
     * @param curi
     */
    public synchronized void enqueue(BdbMultipleWorkQueues queues, CrawlURI curi) {
        insert(queues, curi);
        count++;
    }

    /**
     * Insert the given curi, whether it is already present or not. 
     * 
     * @param queues
     * @param curi
     */
    protected void insert(BdbMultipleWorkQueues queues, CrawlURI curi) {
        try {
            queues.put(curi);
            lastQueued = curi.getURIString();
        } catch (DatabaseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    /**
     * Update the given CrawlURI, which should already be present. (This
     * is not checked.) Equivalent to an enqueue without affecting the count.
     * 
     * @param queues Work queues manager.
     * @param curi
     */
    public void update(BdbMultipleWorkQueues queues, CrawlURI curi) {
        insert(queues, curi);
    }
    
    /* (non-Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(Object obj) {
        if(this==obj) {
            return 0; // for exact identity only
        }
        BdbWorkQueue other = (BdbWorkQueue)obj;
        if (getWakeTime() > other.getWakeTime()) {
            return 1;
        }
        if (getWakeTime() < other.getWakeTime()) {
            return -1;
        }
        // at this point, the ordering is arbitrary, but still
        // must be consistent/stable over time
        return this.classKey.compareTo(other.getClassKey());
    }
    /**
     * @return Returns the count.
     */
    public long getCount() {
        return this.count;
    }
}