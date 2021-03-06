package org.archive.crawler.frontier;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.framework.Frontier;
import org.archive.crawler.frontier.precedence.PrecedenceProvider;
import org.archive.crawler.frontier.precedence.SimplePrecedenceProvider;
import org.archive.modules.ProcessorURI;
import org.archive.modules.fetcher.FetchStats;
import org.archive.modules.fetcher.FetchStats.Stage;
import org.archive.settings.SheetManager;
import org.archive.state.Key;
import org.archive.state.StateProvider;
import org.archive.util.ArchiveUtils;
import org.archive.util.Reporter;

/**
 * A single queue of related URIs to visit, grouped by a classKey
 * (typically "hostname:port" or similar) 
 * 
 * @author gojomo
 * @author Christian Kohlschuetter 
 */
public abstract class WorkQueue implements Frontier.FrontierGroup,
        Serializable, Reporter, Delayed {
    private static final Logger logger =
        Logger.getLogger(WorkQueue.class.getName());
    
    /** The classKey */
    protected final String classKey;

    /** whether queue is active (ready/in-process/snoozed) or on a waiting queue */
    private boolean active = true;

    /** Total number of stored items */
    private long count = 0;

    /** Total number of items ever enqueued */
    private long enqueueCount = 0;
    
    /** Whether queue is already in lifecycle stage */
    private boolean isHeld = false;

    /** Time to wake, if snoozed */
    private long wakeTime = 0;

    /** assigned precedence */
    private PrecedenceProvider precedenceProvider = new SimplePrecedenceProvider(1);
        
    /** set of by-precedence inactive-queues on which WorkQueue is waiting */
    private Set<Integer> onInactiveQueues = new HashSet<Integer>();
    
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

    /** The next item to be returned */
    protected CrawlURI peekItem = null;

    /** Last URI enqueued */
    private String lastQueued;

    /** Last URI peeked */
    private String lastPeeked;

    /** time of last dequeue (disposition of some URI) **/ 
    private long lastDequeueTime;
    
    /** count of errors encountered */
    private long errorCount = 0;
    
    /** Substats for all CrawlURIs in this group */
    protected FetchStats substats = new FetchStats();

    private boolean retired;

    transient protected StateProvider provider;
    
    public WorkQueue(final String pClassKey) {
        this.classKey = pClassKey;
    }

    /**
     * Delete URIs matching the given pattern from this queue. 
     * @param frontier
     * @param match
     * @return count of deleted URIs
     */
    public long deleteMatching(final WorkQueueFrontier frontier, String match) {
        try {
            final long deleteCount = deleteMatchingFromQueue(frontier, match);
            this.count -= deleteCount;
            return deleteCount;
        } catch (IOException e) {
            //FIXME better exception handling
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    /**
     * Add the given CrawlURI, noting its addition in running count. (It
     * should not already be present.)
     * 
     * @param frontier Work queues manager.
     * @param curi CrawlURI to insert.
     */
    protected void enqueue(final WorkQueueFrontier frontier,
        CrawlURI curi) {
        try {
            insert(frontier, curi, false);
        } catch (IOException e) {
            //FIXME better exception handling
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        count++;
        enqueueCount++;
    }

    /**
     * Return the topmost queue item -- and remember it,
     * such that even later higher-priority inserts don't
     * change it. 
     * 
     * TODO: evaluate if this is really necessary
     * @param frontier Work queues manager
     * 
     * @return topmost queue item, or null
     */
    public CrawlURI peek(final WorkQueueFrontier frontier) {
        if(peekItem == null && count > 0) {
            try {
                peekItem = peekItem(frontier);
            } catch (IOException e) {
                //FIXME better exception handling
                logger.log(Level.SEVERE,"peek failure",e);
                e.printStackTrace();
                // throw new RuntimeException(e);
            }
            if(peekItem != null) {
                lastPeeked = peekItem.toString();
            }
        }
        return peekItem;
    }

    /**
     * Remove the peekItem from the queue and adjusts the count.
     * 
     * @param frontier  Work queues manager.
     */
    protected void dequeue(final WorkQueueFrontier frontier, CrawlURI expected) {
        try {
            deleteItem(frontier, peekItem);
        } catch (IOException e) {
            //FIXME better exception handling
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        unpeek(expected);
        count--;
        lastDequeueTime = System.currentTimeMillis();
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
            || (this.totalBudget >= 0 && this.totalExpenditure >= this.totalBudget);
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
     * A URI should not have been charged against queue (eg
     * it was disregarded); return the amount expended 
     * @param amount to return
     * @return updated budget value
     */
    public int refund(int amount) {
        this.sessionBalance = this.sessionBalance + amount;
        this.totalExpenditure = this.totalExpenditure - amount;
        this.costCount--;
        return this.sessionBalance;
    }
    
    /**
     * Note an error and assess an extra penalty. 
     * @param penalty additional amount to deduct
     */
    public void noteError(int penalty) {
        this.sessionBalance = this.sessionBalance - penalty;
        this.totalExpenditure = this.totalExpenditure + penalty;
        errorCount++;
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
     * @return classKey, the 'identifier', for this queue.
     */
    public String getClassKey() {
        return this.classKey;
    }

    /**
     * Clear isHeld to false
     */
    public void clearHeld() {
        isHeld = false;
        if(logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE,
                    "queue unheld: " + getClassKey());
        }
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
        if(logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE,
                    "queue held: " + getClassKey());
        }
    }

    /**
     * Forgive the peek, allowing a subsequent peek to 
     * return a different item. 
     * 
     */
    public void unpeek(CrawlURI expected) {
        assert expected == peekItem : "unexpected peekItem";
        peekItem = null;
    }

    /* (non-Javadoc)
     * @see java.util.concurrent.Delayed#getDelay(java.util.concurrent.TimeUnit)
     */
    public long getDelay(TimeUnit unit) {
        return unit.convert(
                getWakeTime()-System.currentTimeMillis(),
                TimeUnit.MILLISECONDS);
    }

    public final int compareTo(Delayed obj) {
        if(this == obj) {
            return 0; // for exact identity only
        }
        WorkQueue other = (WorkQueue) obj;
        if(getWakeTime() > other.getWakeTime()) {
            return 1;
        }
        if(getWakeTime() < other.getWakeTime()) {
            return -1;
        }
        // at this point, the ordering is arbitrary, but still
        // must be consistent/stable over time
        return this.classKey.compareTo(other.getClassKey());
    }

    /**
     * Update the given CrawlURI, which should already be present. (This
     * is not checked.) Equivalent to an enqueue without affecting the count.
     * 
     * @param frontier Work queues manager.
     * @param curi CrawlURI to update.
     */
    public void update(final WorkQueueFrontier frontier, CrawlURI curi) {
        try {
            insert(frontier, curi, true);
        } catch (IOException e) {
            //FIXME better exception handling
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    /**
     * Count of URIs in this queue. Only precise if called within frontier's
     * manager thread. 
     * 
     * @return Returns the count.
     */
    public long getCount() {
        return this.count;
    }

    /**
     * Insert the given curi, whether it is already present or not. 
     * @param frontier WorkQueueFrontier.
     * @param curi CrawlURI to insert.
     * @throws IOException
     */
    private void insert(final WorkQueueFrontier frontier, CrawlURI curi,
            boolean overwriteIfPresent)
        throws IOException {
        insertItem(frontier, curi, overwriteIfPresent);
        lastQueued = curi.toString();
    }

    /**
     * Insert the given curi, whether it is already present or not.
     * Hook for subclasses. 
     * 
     * @param frontier WorkQueueFrontier.
     * @param curi CrawlURI to insert.
     * @throws IOException  if there was a problem while inserting the item
     */
    protected abstract void insertItem(final WorkQueueFrontier frontier,
        CrawlURI curi, boolean overwriteIfPresent) throws IOException;

    /**
     * Delete URIs matching the given pattern from this queue. 
     * @param frontier WorkQueues manager.
     * @param match  the pattern to match
     * @return count of deleted URIs
     * @throws IOException  if there was a problem while deleting
     */
    protected abstract long deleteMatchingFromQueue(
        final WorkQueueFrontier frontier, final String match)
        throws IOException;

    /**
     * Removes the given item from the queue.
     * 
     * This is only used to remove the first item in the queue,
     * so it is not necessary to implement a random-access queue.
     * 
     * @param frontier  Work queues manager.
     * @throws IOException  if there was a problem while deleting the item
     */
    protected abstract void deleteItem(final WorkQueueFrontier frontier,
        final CrawlURI item) throws IOException;

    /**
     * Returns first item from queue (does not delete)
     * 
     * @return The peeked item, or null
     * @throws IOException  if there was a problem while peeking
     */
    protected abstract CrawlURI peekItem(final WorkQueueFrontier frontier)
        throws IOException;

    /**
     * Suspends this WorkQueue. Closes all connections to resources etc.
     * 
     * @param frontier
     * @throws IOException
     */
    protected void suspend(final WorkQueueFrontier frontier) throws IOException {
    }

    /**
     * Resumes this WorkQueue. Eventually opens connections to resources etc.
     * 
     * @param frontier
     * @throws IOException
     */
    protected void resume(final WorkQueueFrontier frontier) throws IOException {
    }

    public void setActive(final WorkQueueFrontier frontier, final boolean b) {
        if(active != b) {
            active = b;
            if(logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE,
                        (active ? "queue set active: " : "queue unset active: ") + 
                        this.getClassKey());
            }
            try {
                if(active) {
                    resume(frontier);
                } else {
                    suspend(frontier);
                }
            } catch (IOException e) {
                //FIXME better exception handling
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
    }
    
    // 
    // Reporter
    //

    /* (non-Javadoc)
     * @see org.archive.util.Reporter#getReports()
     */
    public String[] getReports() {
        return new String[] {};
    }

    /* (non-Javadoc)
     * @see org.archive.util.Reporter#reportTo(java.io.Writer)
     */
    public void reportTo(PrintWriter writer) {
        reportTo(null,writer);
    }

    /* (non-Javadoc)
     * @see org.archive.util.Reporter#singleLineReportTo(java.io.Writer)
     */
    public void singleLineReportTo(PrintWriter writer) {
        // queue name
        writer.print(classKey);
        writer.print(" ");
        // precedence
        writer.print(getPrecedence());
        writer.print(" ");
        // count of items
        writer.print(Long.toString(count));
        writer.print(" ");
        // enqueue count
        writer.print(Long.toString(enqueueCount));
        writer.print(" ");
        writer.print(sessionBalance);
        writer.print(" ");
        writer.print(lastCost);
        writer.print("(");
        writer.print(ArchiveUtils.doubleToString(
                    ((double) totalExpenditure / costCount), 1));
        writer.print(")");
        writer.print(" ");
        // last dequeue time, if any, or '-'
        if (lastDequeueTime != 0) {
            writer.print(ArchiveUtils.getLog17Date(lastDequeueTime));
        } else {
            writer.print("-");
        }
        writer.print(" ");
        // wake time if snoozed, or '-'
        if (wakeTime != 0) {
            writer.print(ArchiveUtils.formatMillisecondsToConventional(wakeTime - System.currentTimeMillis()));
        } else {
            writer.print("-");
        }
        writer.print(" ");
        writer.print(Long.toString(totalExpenditure));
        writer.print("/");
        writer.print(Long.toString(totalBudget));
        writer.print(" ");
        writer.print(Long.toString(errorCount));
        writer.print(" ");
        writer.print(lastPeeked);
        writer.print(" ");
        writer.print(lastQueued);
        writer.print("\n");
    }

    /* (non-Javadoc)
     * @see org.archive.util.Reporter#singleLineLegend()
     */
    public String singleLineLegend() {
        return "queue precedence currentSize totalEnqueues sessionBalance " +
                "lastCost (averageCost) lastDequeueTime wakeTime " +
                "totalSpend/totalBudget errorCount lastPeekUri lastQueuedUri";
    }
    
    /* (non-Javadoc)
     * @see org.archive.util.Reporter#singleLineReport()
     */
    public String singleLineReport() {
        return ArchiveUtils.singleLineReport(this);
    }
    
    /**
     * @param writer
     * @throws IOException
     */
    public void reportTo(String name, PrintWriter writer) {
        // name is ignored: only one kind of report for now
        writer.print("Queue ");
        writer.print(classKey);
        writer.print(" (p");
        writer.print(getPrecedence());
        writer.print(")\n");
        writer.print("  ");
        writer.print(Long.toString(count));
        writer.print(" items");
        if (wakeTime != 0) {
            writer.print("\n   wakes in: "+ArchiveUtils.formatMillisecondsToConventional(wakeTime - System.currentTimeMillis()));
        }
        writer.print("\n    last enqueued: ");
        writer.print(lastQueued);
        writer.print("\n      last peeked: ");
        writer.print(lastPeeked);
        writer.print("\n");
        writer.print("   total expended: ");
        writer.print(Long.toString(totalExpenditure));
        writer.print(" (total budget: ");
        writer.print(Long.toString(totalBudget));
        writer.print(")\n");
        writer.print("   active balance: ");
        writer.print(sessionBalance);
        writer.print("\n   last(avg) cost: ");
        writer.print(lastCost);
        writer.print("(");
        writer.print(ArchiveUtils.doubleToString(
                    ((double) totalExpenditure / costCount), 1));
        writer.print(")\n   ");
        writer.print(getSubstats().singleLineLegend());
        writer.print("\n   ");
        writer.print(getSubstats().singleLineReport());
        writer.print("\n   ");
        writer.print(getPrecedenceProvider().singleLineLegend());
        writer.print("\n   ");
        writer.print(getPrecedenceProvider().singleLineReport());
        writer.print("\n\n");
    }
    
    public FetchStats getSubstats() {
        return substats;
    }

    /**
     * Set the retired status of this queue.
     * 
     * @param b new value for retired status
     */
    public void setRetired(boolean b) {
        this.retired = b;
    }
    
    public boolean isRetired() {
        return retired;
    }

    /**
     * @return the precedenceProvider
     */
    public PrecedenceProvider getPrecedenceProvider() {
        return precedenceProvider;
    }

    /**
     * @param precedenceProvider the precedenceProvider to set
     */
    public void setPrecedenceProvider(PrecedenceProvider precedenceProvider) {
        this.precedenceProvider = precedenceProvider;
    }
    
    /**
     * @return the precedence
     */
    public int getPrecedence() {
        return precedenceProvider.getPrecedence();
    }
    
    public void setStateProvider(SheetManager manager) {
        if(provider!=null) {
            // no need to reset
            return; 
        }
        this.provider = manager.findConfig(getClassKey());
    }

    public <T> T get(Object module, Key<T> key) {
        if (provider == null) {
            throw new AssertionError("ToeThread never set up CrawlURI's sheet.");
        }
        return provider.get(module, key);
    }

    /**
     * @return the onInactiveQueues
     */
    public Set<Integer> getOnInactiveQueues() {
        return onInactiveQueues;
    }

    /* (non-Javadoc)
     * @see org.archive.modules.fetcher.FetchStats.HasFetchStats#tally(org.archive.modules.ProcessorURI, org.archive.modules.fetcher.FetchStats.Stage)
     */
    public void tally(ProcessorURI curi, Stage stage) {
        substats.tally(curi, stage);
        precedenceProvider.tally(curi, stage);
    }

    public boolean isActive() {
        return active;
    }
}
