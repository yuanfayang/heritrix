/* DiskBackedQueueTest
 *
 * $Id$
 *
 * Created Tue Jan 20 14:17:59 PST 2004
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.archive.crawler.checkpoint.ObjectPlusFilesInputStream;
import org.archive.crawler.checkpoint.ObjectPlusFilesOutputStream;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * JUnit test suite for DiskBackedQueue
 *
 * @author <a href="mailto:me@jamesc.net">James Casey</a>
 * @version $Id$
 */
public class DiskBackedQueueTest extends QueueTestBase {
    /**
     * Create a new DiskBackedQueueTest object
     *
     * @param testName the name of the test
     */
    public DiskBackedQueueTest(final String testName) {
        super(testName);
    }

    /**
     * run all the tests for DiskBackedQueueTest
     *
     * @param argv the command line arguments
     */
    public static void main(String argv[]) {
        junit.textui.TestRunner.run(suite());
    }

    /**
     * return the suite of tests for DiskBackedQueueTest
     *
     * @return the suite of test
     */
    public static Test suite() {
        return new TestSuite(DiskBackedQueueTest.class);
    }


    protected Queue makeQueue() {
        try {
            return new DiskBackedQueue(getTmpDir(), "foo", false, 5);
        } catch (final IOException e) {
            fail("Caught IO Exception on creation of queue : " + e.getMessage());
            // never gets here
            return null;
        }
    }

    // TODO - implement test methods in DiskBackedQueueTest

    /**
     * Test which will overflow into disk backing,
     * trigger flip of write and read files.
     */
    public synchronized void testIntoDiskBacking() {
        DiskBackedQueue queue = (DiskBackedQueue)this.queue;
        
        queue.enqueue("foo1");
        queue.enqueue("foo2");
        queue.enqueue("foo3");
        queue.enqueue("foo4");
        queue.enqueue("foo5");
        // these next six go to disk backing (first file)
        queue.enqueue("foo6");
        queue.enqueue("foo7");
        queue.enqueue("foo8");
        queue.enqueue("foo9");
        queue.enqueue("foo10");
        queue.enqueue("foo11");
        // now dequeue
        assertEquals("foo1 dequeued","foo1",queue.dequeue());
        assertEquals("foo2 dequeued","foo2",queue.dequeue());
        assertEquals("foo3 dequeued","foo3",queue.dequeue());
        assertEquals("foo4 dequeued","foo4",queue.dequeue());
        assertEquals("foo5 dequeued","foo5",queue.dequeue());
        // this next forces read of 5 into memory from disk, file flip
        assertEquals("foo6 dequeued","foo6",queue.dequeue());
       
        // these next 4 write to new file
        queue.enqueue("foo12");
        queue.enqueue("foo13");
        queue.enqueue("foo14");
        queue.enqueue("foo15");

        // dequeue rest
        assertEquals("foo7 dequeued","foo7",queue.dequeue());
        assertEquals("foo8 dequeued","foo8",queue.dequeue());
        assertEquals("foo9 dequeued","foo9",queue.dequeue());
        assertEquals("foo10 dequeued","foo10",queue.dequeue());
        assertEquals("foo11 dequeued","foo11",queue.dequeue());
        assertEquals("foo12 dequeued","foo12",queue.dequeue());
        assertEquals("foo13 dequeued","foo13",queue.dequeue());
        assertEquals("foo14 dequeued","foo14",queue.dequeue());
        // Only one left. (less then or equal to a quarter of availible in
        // memory cache. Files are deleted.
        assertFalse("files released",queue.tailQ.isInitialized());
        queue.enqueue("foo16");
        queue.enqueue("foo17");
        queue.enqueue("foo18");
        queue.enqueue("foo19");
        // Next two will be written to file again.
        queue.enqueue("foo20");
        queue.enqueue("foo21");
        assertEquals("foo15 dequeued","foo15",queue.dequeue());
        assertEquals("foo16 dequeued","foo16",queue.dequeue());
        assertEquals("foo17 dequeued","foo17",queue.dequeue());
        assertEquals("foo18 dequeued","foo18",queue.dequeue());
        assertEquals("foo19 dequeued","foo19",queue.dequeue());
        assertEquals("foo20 dequeued","foo20",queue.dequeue());
        assertEquals("foo21 dequeued","foo21",queue.dequeue());
        assertTrue("queue is empty", queue.isEmpty());    
    }
    
    /**
     * Test that object + files properly serializes and deserialized
     * from ObjectPlusFiles streams.
     * 
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public void testSerialization() throws IOException, ClassNotFoundException {
        queue.enqueue("foo1");
        queue.enqueue("foo2");
        queue.enqueue("foo3");
        queue.enqueue("foo4");
        queue.enqueue("foo5");
        // these next six go to disk backing (first file)
        queue.enqueue("foo6");
        queue.enqueue("foo7");
        queue.enqueue("foo8");
        queue.enqueue("foo9");
        queue.enqueue("foo10");
        queue.enqueue("foo11");
        // now dequeue
        assertEquals("foo1 dequeued","foo1",queue.dequeue());
        assertEquals("foo2 dequeued","foo2",queue.dequeue());
        assertEquals("foo3 dequeued","foo3",queue.dequeue());
        assertEquals("foo4 dequeued","foo4",queue.dequeue());
        assertEquals("foo5 dequeued","foo5",queue.dequeue());
        // this next forces read of 5 into memory from disk, file flip
        assertEquals("foo6 dequeued","foo6",queue.dequeue());
        
        // next 4 enqueues write to new file
        queue.enqueue("foo12");
        
        // TODO serialize/deserialize
        File tmpDir = getTmpDir();
        File serFile = new File(tmpDir,"foo.ser");
        File storeDir = new File(tmpDir,"serialization");
        ObjectPlusFilesOutputStream out = null;
		try {
			out = new ObjectPlusFilesOutputStream(
			        new FileOutputStream(serFile),storeDir);
			out.writeObject(queue);
		} finally {
			out.close();
        }
        
        ((DiskBackedQueue)queue).release();
        queue = null;
        
        ObjectPlusFilesInputStream in = null;
        try {
            in = new ObjectPlusFilesInputStream(
                        new FileInputStream(serFile),
                        storeDir);
            queue = (Queue) in.readObject();
        } finally {
            in.close();
        }
 
        // continue where we left off
        queue.enqueue("foo13");
        queue.enqueue("foo14");
        queue.enqueue("foo15");

        // dequeue rest
        assertEquals("foo1 dequeued","foo7",queue.dequeue());
        assertEquals("foo1 dequeued","foo8",queue.dequeue());
        assertEquals("foo1 dequeued","foo9",queue.dequeue());
        assertEquals("foo1 dequeued","foo10",queue.dequeue());
        assertEquals("foo1 dequeued","foo11",queue.dequeue());
        assertEquals("foo1 dequeued","foo12",queue.dequeue());
        assertEquals("foo1 dequeued","foo13",queue.dequeue());
        assertEquals("foo1 dequeued","foo14",queue.dequeue());
        assertEquals("foo1 dequeued","foo15",queue.dequeue());
        assertTrue("queue is empty", queue.isEmpty());    
    }
}

