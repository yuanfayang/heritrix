/* DiskStackTest
 *
 * Created May 21, 2004
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
import java.io.IOException;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * JUnit test suite for DiskStack
 * based on James Casey's queue tests
 *
 * @author gojomo
 * @version $Id$
 */
public class DiskStackTest extends TmpDirTestCase {
    private static final String FILE_PREFIX = "foo";
    protected Stack stack;

    /**
     * Create a new DiskStackTest object
     *
     * @param testName the name of the test
     */
    public DiskStackTest(final String testName) {
        super(testName);
    }

    /**
     * run all the tests for DiskStackTest
     *
     * @param argv the command line arguments
     */
    public static void main(String argv[]) {
        junit.textui.TestRunner.run(suite());
    }

    /**
     * return the suite of tests for DiskStackTest
     *
     * @return the suite of test
     */
    public static Test suite() {
        return new TestSuite(DiskStackTest.class);
    }

    public void setUp() throws Exception {
        super.setUp();
        stack = makeStack();
    }

    public void tearDown() {
        if(stack != null) {
            stack.release();
        }
    }
    /** make a new DiskStack, using <code>/tmp</code> and <code>foo</code>
     * as the <code>scratchDir</code> and <code>prefix</code> respectively.
     * @return the Queue
     */
    protected Stack makeStack() {
        try {
            File storeFile = new File(getTmpDir(), FILE_PREFIX);
            storeFile.delete();
            return new DiskStack(storeFile);
        } catch (IOException e) {
            fail("IOException: " + e.getMessage());
            // never gets here
            return null;
        }
    }

    /*
     * DiskStack specific tests
     */

    /** test the creation of a stack using a non-creatable file */
    public void testCtorBadFile() {
         try {
            File storeFile = new File("/foo/boo/doo/");
            DiskStack stack = new DiskStack(storeFile);
            stack.height(); // suppress never-accessed warning
        } catch(IOException e) {
            return;
        }
        fail("no exception on bad file");
    }

    /** test that stack puts things on, and they stay there :) */
    public void testStack() {
        assertEquals("no items in new stack", 0, stack.height());
        assertTrue("stack is empty", stack.isEmpty());
        stack.push("foo");
        assertEquals("now one item in stack", 1, stack.height());
        assertFalse("stack not empty", stack.isEmpty());
    }

    /** test that push/pop works */
    public void testDequeue() {
        assertEquals("no items in new stack", 0, stack.height());
        assertTrue("stack is empty", stack.isEmpty());
        stack.push("foo");
        stack.push("bar");
        stack.push("baz");
        assertEquals("now three items in queue", 3, stack.height());
        assertEquals("foo popped", "baz", stack.pop());
        assertEquals("bar popped", "bar", stack.pop());
        assertEquals("baz popped", "foo", stack.pop());

        assertEquals("no items in new stack", 0, stack.height());
        assertTrue("stack is empty", stack.isEmpty());

    }

}


