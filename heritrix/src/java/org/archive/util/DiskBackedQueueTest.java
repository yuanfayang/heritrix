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

import java.io.IOException;

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
            return new DiskBackedQueue(getTmpDir(), "foo", 10);
        } catch (final IOException e) {
            fail("Caught IO Exception on creation of queue : " + e.getMessage());
            // never gets here
            return null;
        }
    }

    // TODO - implement test methods in DiskBackedQueueTest
}

