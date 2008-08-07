/* Copyright (C) 2006 Internet Archive.
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
 *
 * KeyManagerTest.java
 * Created on October 5, 2006
 *
 * $Header$
 */
package org.archive.state;


import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import junit.framework.TestCase;


/**
 * Unit test for KeyManager.
 * 
 * @author pjack
 */
public class KeyManagerTest extends TestCase {
    
    
    /**
     * Tests registering of keys.  First tests that, for a class that has 
     * been loaded but not initialized, the querying of that class's keys
     * causes that class to be initialized first.  Next tests an already
     * loaded class.  This also implicitly tests the addKeys method as well.
     */
    public void testGetKeys() {
        // Test uninitialize classes
        Map<String,Key<Object>> m = KeyManager.getKeys(ExampleConcreteProcessor.class);
        
        Set<String> expected = new HashSet<String>();
        expected.add("left");
        expected.add("right");
        expected.add("catch-division-by-zero");
        expected.add("division-by-zero-result");
        assertEquals(expected, m.keySet());
         
        m = KeyManager.getKeys(ExampleAbstractProcessor.class);
        expected.remove("catch-division-by-zero");
        expected.remove("division-by-zero-result");
        assertEquals(expected, m.keySet());
        
        // Classes were initialized by above code
        Set<Key> expectedKeys = new HashSet<Key>();
        expectedKeys.add(ExampleAbstractProcessor.LEFT);
        expectedKeys.add(ExampleAbstractProcessor.RIGHT);

        m = KeyManager.getKeys(ExampleAbstractProcessor.class);
        assertEquals(expectedKeys, new HashSet<Key>(m.values()));
        
        expectedKeys.add(ExampleConcreteProcessor.CATCH_DIVISION_BY_ZERO);
        expectedKeys.add(ExampleConcreteProcessor.DIVISION_BY_ZERO_RESULT);
        m = KeyManager.getKeys(ExampleConcreteProcessor.class);
        assertEquals(expectedKeys, new HashSet<Key>(m.values()));
        
        try {
            m = KeyManager.getKeys(ExampleInvalidProcessor1.class);
            fail();
        } catch (ExceptionInInitializerError e) {
            // Expected; make sure cause was DuplicateKey
            assertTrue(e.getCause() instanceof DuplicateKeyException);
        }

        try {
            m = KeyManager.getKeys(ExampleInvalidProcessor2.class);
            fail();
        } catch (ExceptionInInitializerError e) {
            // Expected; make sure cause was InvalidKey
            assertTrue(e.getCause() instanceof InvalidKeyException);
        }
        
        try {
            m = KeyManager.getKeys(ExampleInvalidProcessor3.class);
            fail();
        } catch (ExceptionInInitializerError e) {
            // Expected; make sure cause was InvalidKey
            assertTrue(e.getCause() instanceof InvalidKeyException);
        }
        
        try {
            m = KeyManager.getKeys(ExampleInvalidProcessor4.class);
            fail();
        } catch (ExceptionInInitializerError e) {
            // Expected; make sure cause was InvalidKey
            assertTrue(e.getCause() instanceof InvalidKeyException);
        }        
    }
    
    
    /**
     * Tests that the ExampleConcreteProcessor works as expected.  Didn't
     * want to give it its own test class, as it's really test data in my
     * mind.  
     */
    public void testExampleConcreteProcessor() {
        Random random = new Random();
        ExampleStateProvider state = new ExampleStateProvider();
        ExampleConcreteProcessor proc = new ExampleConcreteProcessor();
        for (int i = 0; i < 1000; i++) {
            int left = random.nextInt();
            int right = 0;
            while (right == 0) {
                right = random.nextInt();
            }
            int expected = left / right;
            state.set(proc, ExampleAbstractProcessor.LEFT, left);
            state.set(proc, ExampleAbstractProcessor.RIGHT, right);
            assertEquals(expected, proc.process(state).intValue());
        }
    }

    
    /**
     * Tests that ExampleInvalidProcessor5's keys raise IllegalStateException
     * 
     */
    public void testUnregisteredKey() {
        try {
            String s = ExampleInvalidProcessor5.EXAMPLE.getFieldName();
            fail("ExampleInvalidProcessor5.EXAMPLE is an unregistered key.");
        } catch (UnregisteredKeyException e) {
            System.err.println(e.getMessage());
            // Expected; KeyManager.addKeys was never called for this class.
        }
    }
}
