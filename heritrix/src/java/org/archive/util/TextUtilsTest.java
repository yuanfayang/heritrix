/* TextUtilsTest.java
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

import java.util.regex.Matcher;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * JUnit test suite for TextUtils
 * 
 * @author gojomo
 * @version $ Id$
 */
public class TextUtilsTest extends TestCase {
    /**
     * Create a new TextUtilsTest object
     * 
     * @param testName
     *            the name of the test
     */
    public TextUtilsTest(final String testName) {
        super(testName);
    }

    /**
     * run all the tests for TextUtilsTest
     * 
     * @param argv
     *            the command line arguments
     */
    public static void main(String argv[]) {
        junit.textui.TestRunner.run(suite());
    }

    /**
     * return the suite of tests for MemQueueTest
     * 
     * @return the suite of test
     */
    public static Test suite() {
        return new TestSuite(TextUtilsTest.class);
    }

    public void testMatcherRecycling() {
        String pattern = "f.*";
        Matcher m1 = TextUtils.getMatcher(pattern,"foo");
        assertTrue("matcher against 'foo' problem", m1.matches());
        Matcher m2 = TextUtils.getMatcher(pattern,"");
        assertFalse("matcher against '' problem", m2.matches());
        Matcher m3 = TextUtils.getMatcher(pattern,"fuggedaboutit");
        assertTrue("matcher against 'fuggedaboutit' problem",m3.matches());
    }
    
    public void testGetFirstWord() {
        final String firstWord = "one";
        String tmpStr = TextUtils.getFirstWord(firstWord + " two three");
        assertTrue("Failed to get first word 1 " + tmpStr,
            tmpStr.equals(firstWord));
        tmpStr = TextUtils.getFirstWord(firstWord);
        assertTrue("Failed to get first word 2 " + tmpStr,
            tmpStr.equals(firstWord));       
    }
}

