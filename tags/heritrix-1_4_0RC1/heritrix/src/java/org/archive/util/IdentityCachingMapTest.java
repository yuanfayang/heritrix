/* IdentityCachingMapTest
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

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * JUnit test suite for IdentityCachingMapTest
 *
 * @author gojomo
 * @version $Id$
 */
public class IdentityCachingMapTest extends TestCase {
    /**
     * Create a new IdentityCachingMapTest object
     *
     * @param testName the name of the test
     */
    public IdentityCachingMapTest(final String testName) {
        super(testName);
    }

    /**
     * run all the tests for IdentityCachingMapTest
     *
     * @param argv the command line arguments
     */
    public static void main(String argv[]) {
        junit.textui.TestRunner.run(suite());
    }

    /**
     * return the suite of tests for IdentityCachingMapTest
     *
     * @return the suite of test
     */
    public static Test suite() {
        return new TestSuite(IdentityCachingMapTest.class);
    }

    /**  basic tests */
    public void testBasic() {
        Map fakeMap = new ManufacturingMap();
        IdentityCachingMap map = new IdentityCachingMap(fakeMap);
        Object value1 = map.get(new Integer(1));
        Object value2 = map.get(new Integer(2));
        Object value3 = map.get(new Integer(3));
        Object value1b = map.get(new Integer(1));
        Object value1c = fakeMap.get(new Integer(1));
        assertTrue("identity not preserved", value1 == value1b);
        assertTrue("underlying map provides identity", value1 != value1c);
        value2 = null;
        assertTrue("cache not holding instance", map.cacheContainsKey(new Integer(2)));
        TestUtils.forceScarceMemory();
        assertFalse("cache not cleared appropriately", map.cacheContainsKey(new Integer(2)));
        Object value3b = map.get(new Integer(3));
        assertTrue("identity not preserved", value3 == value3b);
    }

    /**
     * Works as a Map, but actually creates String return values from Integer 
     * keys. Useful for testing, because the new-instance-per-get is much 
     * like BDB JE's persistent collections.  
     * 
     * @author gojomo
     */
    public class ManufacturingMap implements Map {

        /* (non-Javadoc)
         * @see java.util.Map#size()
         */
        public int size() {
            throw new UnsupportedOperationException();
        }

        /* (non-Javadoc)
         * @see java.util.Map#isEmpty()
         */
        public boolean isEmpty() {
            return false;
        }

        /* (non-Javadoc)
         * @see java.util.Map#containsKey(java.lang.Object)
         */
        public boolean containsKey(Object key) {
            return key instanceof Integer;
        }

        /* (non-Javadoc)
         * @see java.util.Map#containsValue(java.lang.Object)
         */
        public boolean containsValue(Object value) {
            return value instanceof String && ((String)value).matches("\\d+");
        }

        /* (non-Javadoc)
         * @see java.util.Map#get(java.lang.Object)
         */
        public Object get(Object key) {
            if(!containsKey(key)) {
                return null;
            }
            return new String(((Integer)key).toString());
        }

        /* (non-Javadoc)
         * @see java.util.Map#put(java.lang.Object, java.lang.Object)
         */
        public Object put(Object arg0, Object arg1) {
            // TODO: ???
            return get(arg0);
        }

        /* (non-Javadoc)
         * @see java.util.Map#remove(java.lang.Object)
         */
        public Object remove(Object key) {
            // TODO ???
            return get(key);
        }

        /* (non-Javadoc)
         * @see java.util.Map#putAll(java.util.Map)
         */
        public void putAll(Map arg0) {
            throw new UnsupportedOperationException();
        }

        /* (non-Javadoc)
         * @see java.util.Map#clear()
         */
        public void clear() {
            throw new UnsupportedOperationException();
        }

        /* (non-Javadoc)
         * @see java.util.Map#keySet()
         */
        public Set keySet() {
            throw new UnsupportedOperationException();
        }

        /* (non-Javadoc)
         * @see java.util.Map#values()
         */
        public Collection values() {
            throw new UnsupportedOperationException();
        }

        /* (non-Javadoc)
         * @see java.util.Map#entrySet()
         */
        public Set entrySet() {
            throw new UnsupportedOperationException();
        }
    }
}

