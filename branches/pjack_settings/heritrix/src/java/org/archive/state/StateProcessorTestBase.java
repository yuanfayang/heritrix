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
 * StateProcessorTestBase.java
 * Created on October 5, 2006
 *
 * $Header$
 */
package org.archive.state;


import java.lang.reflect.Field;
import java.util.Locale;
import java.util.Map;

import junit.framework.TestCase;


/**
 * Base class for unit testing StateProcessor implementations.
 * 
 * @author pjack
 */
public abstract class StateProcessorTestBase extends TestCase {


    protected Class processorClass;


    /**
     * Sets up the test.  Subclasses should use this method to assign a class
     * value to {@link processorClass}.
     */
    public abstract void setUp();


    /**
     * Tests that the processor developer remembered to invoke 
     * {@link KeyManager#addKeys(Class)}.
     */
    public void testKeyManagerRegistration() {
        Map<String,Key<Object>> keys = KeyManager.getKeys(processorClass);
        Field[] fields = processorClass.getFields();
        int expectedKeyCount = 0;
        for (Field f: fields) {
            if (f.getType() == Key.class) {
                expectedKeyCount++;
            }
        }
        if ((expectedKeyCount > 0) && keys.isEmpty()) {
            fail("Developer forgot KeyManager.addKeys.");
        }
        
        assertEquals(expectedKeyCount, keys.size());
    }

    
    /**
     * Tests that all keys defined by the processor class have names and
     * descriptions in English.
     */
    public void testDefaultLocale() {
        Map<String,Key<Object>> keys = KeyManager.getKeys(processorClass);
        for (Key k: keys.values()) {
            String n = k.getFieldName();
            TestCase.assertNotNull(n, k.getName(Locale.ENGLISH));
            TestCase.assertNotNull(n, k.getDescription(Locale.ENGLISH));
        }
    }
}
