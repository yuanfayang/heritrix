/* DiskQueueTest
 *
 * Created Tue Jan 20 14:17:59 PST 2004
 *
 * $Id$
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
import java.io.FileNotFoundException;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * JUnit test suite for DiskQueue
 *
 * @author <a href="mailto:me@jamesc.net">James Casey</a>
 * @version $Id$
 */
public class DiskQueueTest extends QueueTestBase {
    private static final String FILE_PREFIX = "foo";

    /**
     * Create a new DiskQueueTest object
     *
     * @param testName the name of the test
     */
    public DiskQueueTest(final String testName) {
        super(testName);
    }

    /**
     * run all the tests for DiskQueueTest
     *
     * @param argv the command line arguments
     */
    public static void main(String argv[]) {
        junit.textui.TestRunner.run(suite());
    }

    /**
     * return the suite of tests for DiskQueueTest
     *
     * @return the suite of test
     */
    public static Test suite() {
        return new TestSuite(DiskQueueTest.class);
    }

    /** make a new DiskQueue, using <code>/tmp</code> and <code>foo</code>
     * as the <code>scratchDir</code> and <code>prefix</code> respectively.
     * @return the Queue
     */
    protected Queue makeQueue() {
        try {
            return new DiskQueue(getTmpDir(), FILE_PREFIX);
        } catch (FileNotFoundException e) {
            fail("file not found : " + e.getMessage());
            // never gets here
            return null;
        }
    }

    /*
     * DiskQueue specific tests
     */

    /** test the creation of a queue using a non-existent dir */
    public void testCtorBadDir() {
         try {
            DiskQueue queue = new DiskQueue(new File("/foo"), "bar");
        } catch(FileNotFoundException e) {
            return;
        }
    }

    /**
     * test the creation of a queue using a <code>null</code> dir
     */
    public void testCtorNullDir() {
        try {
            DiskQueue queue = new DiskQueue(null, "bar");
        } catch (FileNotFoundException e) {
            return;
        }
    }

    /**
     * test the creation of a queue using a <code>null</code> prefix
     */
    public void testCtorNullPrefix() {
        try {
            DiskQueue queue = new DiskQueue(null, "bar");
        } catch (FileNotFoundException e) {
            return;
        }
    }
}


