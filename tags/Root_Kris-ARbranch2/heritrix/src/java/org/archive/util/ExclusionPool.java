/*
 * Created on Aug 12, 2004
 *
 */
package org.archive.util;

import java.util.HashMap;

import EDU.oswego.cs.dl.util.concurrent.FIFOSemaphore;
import EDU.oswego.cs.dl.util.concurrent.Sync;

/**
 * @author gojomo
 *
 */
public class ExclusionPool  {

    HashMap syncs = new HashMap(); // Object -> Sync

    public void acquire(Object key) throws InterruptedException {
        CountFIFOSemaphore sync;
        synchronized(this) {;
            sync = (CountFIFOSemaphore) syncs.get(key);
            if(sync==null) {
                sync = new CountFIFOSemaphore(1);
                syncs.put(key,sync);
            }
        }
        // TODO this is broke, race issue
        sync.acquire();
    }



    /* (non-Javadoc)
     * @see EDU.oswego.cs.dl.util.concurrent.Sync#release()
     */
    public void release(Object key) {
        CountFIFOSemaphore sync;
        synchronized(this) {
            sync = (CountFIFOSemaphore) syncs.get(key);
            if(sync==null) {
                throw new UnsupportedOperationException("no sync for "+key);
            }
            sync.release();
            if (sync.isEmpty()) {
                syncs.remove(key);
            }
        }
    }

    /**
     * FIFOSemaphore that tracks how many (if any) items are waiting.
     * 
     * @author gojomo
     */
    public class CountFIFOSemaphore extends FIFOSemaphore implements Sync {
        volatile int count = 0;
        
        /* (non-Javadoc)
         * @see EDU.oswego.cs.dl.util.concurrent.Sync#acquire()
         */
        public void acquire() throws InterruptedException {
            count++;
            super.acquire();
        }
        /**
         * @return
         */
        public boolean isEmpty() {
            return count == 0;
        }
        
        /* (non-Javadoc)
         * @see EDU.oswego.cs.dl.util.concurrent.Sync#release()
         */
        public void release() {
            super.release();
            count--;
        }
        /**
         * @param permits
         */
        public CountFIFOSemaphore(long permits) {
            super(permits);
        }
    }
}
