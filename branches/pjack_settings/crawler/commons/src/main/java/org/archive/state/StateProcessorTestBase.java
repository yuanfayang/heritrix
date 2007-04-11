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


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import junit.framework.TestCase;


/**
 * Base class for unit testing StateProcessor implementations.
 * 
 * @author pjack
 */
public abstract class StateProcessorTestBase extends TestCase {



    
    public StateProcessorTestBase() {
        KeyMetadataMaker.makeDefaultLocale(new File("src/main/java"), 
                getModuleClass());
    }

    
    protected abstract Class getModuleClass();
    
    
    protected abstract Object makeModule() throws Exception ;


    /**
     * Tests that the processor developer remembered to invoke 
     * {@link KeyManager#addKeys(Class)}.
     */
    public void testKeyManagerRegistration() {
        Map<String,Key<Object>> keys = KeyManager.getKeys(getModuleClass());
        Field[] fields = getModuleClass().getFields();
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
        Map<String,Key<Object>> keys = KeyManager.getKeys(getModuleClass());
        List<String> problems = new ArrayList<String>();
        for (Key k: keys.values()) {
            String name = k.getName(Locale.ENGLISH);
            String desc = k.getDescription(Locale.ENGLISH);
            if (name == null || desc == null) {
                problems.add(k.getFieldName());
            }
        }
        TestCase.assertTrue(problems.toString(), problems.isEmpty());
    }

    
    public void testSerialization() throws Exception {
        Object first = makeModule();
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ObjectOutputStream output = new ObjectOutputStream(bout);
        output.writeObject(first);
        
        ByteArrayInputStream binp = new ByteArrayInputStream(bout.toByteArray());
        ObjectInputStream input = new ObjectInputStream(binp);
        Object second = input.readObject();
        
        ByteArrayOutputStream bout2 = new ByteArrayOutputStream();
        output = new ObjectOutputStream(bout2);
        output.writeObject(second);
        
        verifySerialization(first, 
                bout.toByteArray(), second, bout2.toByteArray());

    }
    
    
    protected void verifySerialization(Object first, byte[] firstBytes, 
            Object second, byte[] secondBytes) throws Exception {
        assertTrue(Arrays.equals(firstBytes, secondBytes));
    }
}
