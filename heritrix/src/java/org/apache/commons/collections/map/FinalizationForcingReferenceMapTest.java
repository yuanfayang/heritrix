/* FinalizationForcingReferenceMap
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

package org.apache.commons.collections.map;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.archive.util.TestUtils;

/**
 * JUnit test suite for FinalizationForcingReferenceMap
 *
 * @author gojomo
 * @version $Id$
 */
public class FinalizationForcingReferenceMapTest extends TestCase {
    /**
     * Small test helper object which simply notes that it has been 
     * finalized to a NotedFlag. 
     */
    protected class NoteOnFinalize {
        NotedFlag notedFlag;
        public NoteOnFinalize(NotedFlag flag) {
            this.notedFlag = flag;
        }
        protected void finalize() throws Throwable {
            super.finalize();
            notedFlag.setNoted(true);
        }
    }
    /**
     * Small test helper object that holds a volatile flag
     * to note that something has happened (eg finalization).
     */
    protected class NotedFlag {
        volatile boolean noted = false;
        public NotedFlag(boolean b) {
            this.noted = b;
        }
        public void setNoted(boolean b) {
            this.noted = true;
        }
        public boolean isNoted() {
            return noted;
        }
    }
    
    /**
     * Create a new FinalizationForcingReferenceMapTest object
     *
     * @param testName the name of the test
     */
    public FinalizationForcingReferenceMapTest(final String testName) {
        super(testName);
    }

    /**
     * run all the tests
     *
     * @param argv the command line arguments
     */
    public static void main(String argv[]) {
        junit.textui.TestRunner.run(suite());
    }

    /**
     * return the suite of tests
     *
     * @return the suite of test
     */
    public static Test suite() {
        return new TestSuite(FinalizationForcingReferenceMapTest.class);
    }

    /**  basic test: does it hold an object, clear it when necessary
     * in low-memory conditions, and ensure that finalization has
     * occurred before even returning that  */
    public void testBasic() {
        FinalizationForcingReferenceMap map = new FinalizationForcingReferenceMap();
        NotedFlag flag = new NotedFlag(false);
        NoteOnFinalize value = new NoteOnFinalize(flag);
        map.put("test", value);
        assertTrue("map not holding test item", map.get("test") == value);
        assertFalse("finalization noted prematurely", flag.isNoted());
        value = null;
        TestUtils.forceScarceMemory();
        assertTrue("map not cleared as expected", map.get("test") == null);
        assertTrue("value not finalized yet", flag.isNoted());
    }
}

