/*
 * Created on May 29, 2004
 *
 */
package org.archive.util;

import EDU.oswego.cs.dl.util.concurrent.Sync;

/**
 * Synchronization aid which may be locked or unlocked;
 * if locked, all acquires() block; if unlocked, all 
 * succeed. (Patterned after the util.concurrent 
 * Semaphore.)
 * 
 * @author gojomo
 */
public class GateSync implements Sync {
    boolean locked = false;

    /**
     * Make all subsequent acquire()s block
     * (until opened).
     */
    public synchronized void lock() {
        locked = true;
    }
    
    /**
     * Allow all subsequent acquire()s to succeed
     * (until closed). 
     */
    public synchronized void unlock() {
        locked = false;
        notifyAll();
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see EDU.oswego.cs.dl.util.concurrent.Sync#acquire()
     */
    public void acquire() throws InterruptedException {
        if (Thread.interrupted()) { throw new InterruptedException(); }
        synchronized (this) {
            try {
                while (locked) {
                    wait();
                }
            } catch (InterruptedException ex) {
                notifyAll();
                throw ex;
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see EDU.oswego.cs.dl.util.concurrent.Sync#attempt(long)
     */
    public boolean attempt(long msecs) throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }

        synchronized (this) {
            if (locked==false) {
                return true;
            } else if (msecs <= 0) {
                return false;
            } else {
                try {
                    long startTime = System.currentTimeMillis();
                    long waitTime = msecs;

                    for (;;) {
                        wait(waitTime);
                        if (locked==false) {
                            return true;
                        } else {
                            waitTime = msecs
                                    - (System.currentTimeMillis() - startTime);
                            if (waitTime <= 0) {
                                return false;
                            }
                        }
                    }
                } catch (InterruptedException ex) {
                    notifyAll();
                    throw ex;
                }
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see EDU.oswego.cs.dl.util.concurrent.Sync#release()
     */
    public synchronized void release() {
        // do nothing
    }

}