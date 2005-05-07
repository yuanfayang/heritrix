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

import java.io.IOException;
import java.io.Serializable;

import org.archive.crawler.datamodel.CrawlURI;
import org.archive.util.ArchiveUtils;

import st.ata.util.FPGenerator;

import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;


/**
 * One independent queue of items with the same 'classKey' (eg host). 
 * @author gojomo
 */
public class BdbWorkQueue extends WorkQueue
implements Comparable, Serializable {
    // be robust against trivial implementation changes
    private static final long serialVersionUID = ArchiveUtils
        .classnameBasedUID(BdbWorkQueue.class, 1);

    private byte[] origin;

    /**
     * Create a virtual queue inside the given BdbMultipleWorkQueues 
     * 
     * @param classKey
     */
    public BdbWorkQueue(String classKey) {
        super(classKey);
        origin = new byte[16];
        long fp = FPGenerator.std64.fp(classKey) & 0xFFFFFFFFFFFFFFF0l;
        ArchiveUtils.longIntoByteArray(fp, origin, 0);
    }

    protected long deleteMatchingFromQueue(final WorkQueueFrontier frontier,
            final String match) throws IOException {
        try {
            final BdbMultipleWorkQueues queues = ((BdbFrontier) frontier)
                .getWorkQueues();
            return queues.deleteMatchingFromQueue(match, classKey,
                new DatabaseEntry(origin));
        } catch (DatabaseException e) {
            throw (IOException) new IOException(e.getMessage()).initCause(e);
        }
    }

    protected void deleteItem(final WorkQueueFrontier frontier,
            final CrawlURI peekItem) throws IOException {
        try {
            final BdbMultipleWorkQueues queues = ((BdbFrontier) frontier)
                .getWorkQueues();
             queues.delete(peekItem);
        } catch (DatabaseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            throw (IOException) new IOException(e.getMessage()).initCause(e);
        }
    }

    protected CrawlURI peekItem(final WorkQueueFrontier frontier)
    throws IOException {
        try {
            final BdbMultipleWorkQueues queues = ((BdbFrontier) frontier)
                .getWorkQueues();
            return queues.get(new DatabaseEntry(origin));
        } catch (DatabaseException e) {
            throw (IOException)new IOException(e.getMessage()).initCause(e);
        }
    }

    protected void insertItem(final WorkQueueFrontier frontier,
            final CrawlURI curi) throws IOException {
        try {
            final BdbMultipleWorkQueues queues = ((BdbFrontier) frontier)
                .getWorkQueues();
            queues.put(curi);
        } catch (DatabaseException e) {
            throw (IOException)new IOException(e.getMessage()).initCause(e);
        }
    }
}
