/*
 * SharedExclusiveLock
 *
 * $Id$
 *
 * Created on Jan 15, 2004
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
package org.archive.util;

/**
 * Also sometimes called a ReaderWriterLock, allows either many
 * threads to have shared access ("readers"), or a single thread
 * to have exclusive access. 
 * 
 * This implementation gives precedence to the exclusive requests.
 * 
 * 
 * @author gojomo
 *
 */
public class SharedExclusiveLock {
    private int sharedTotal = 0; // total of threads holding the shared lock
    private boolean exclusiveLock = false;
    private int exclusiveWaitingTotal = 0;
    // total of threads holding OR WAITING FOR the exclusive lock

    public synchronized void acquireShared() {
        while (exclusiveLock || (exclusiveWaitingTotal > 0)) {
            try {
                wait();
            } catch (InterruptedException e) {
                // TODO Convert to runtime exception?
            }
        }
        sharedTotal++;
    }

    public synchronized void releaseShared() {
        sharedTotal--;
        if (sharedTotal == 0) {
            notifyAll();
        }
    }

    public synchronized void acquireExclusive() {
        exclusiveWaitingTotal++;
        while (exclusiveLock || (sharedTotal > 0)) {
            try {
                wait();
            } catch (InterruptedException e) {
                // TODO Convert to runtime exception?
            }
        }
        exclusiveWaitingTotal--;
        exclusiveLock = true;
    }

    public synchronized void releaseExclusive() {
        exclusiveLock = false;
        notifyAll();
    }
}
