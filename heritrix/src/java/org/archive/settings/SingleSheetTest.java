/* Copyright (C) 2007 Internet Archive.
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
 * SingleSheetTest.java
 * Created on January 18, 2007
 *
 * $Header$
 */
package org.archive.settings;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.archive.state.ExampleConcreteProcessor;
import static org.archive.state.ExampleConcreteProcessor.LEFT;
import static org.archive.state.ExampleConcreteProcessor.RIGHT;

import junit.framework.TestCase;

public class SingleSheetTest extends TestCase {

    
    public AtomicReference<ExampleConcreteProcessor> ecp = 
        new AtomicReference<ExampleConcreteProcessor>();


    public void testGarbageCollection() throws Exception {
        Map<String,Object> map = new HashMap<String,Object>();
        MemorySheetManager mgr = new MemorySheetManager(map);
        
        // Create a module, configure it.
        ecp.set(new ExampleConcreteProcessor());
        int id = System.identityHashCode(ecp);
        SingleSheet def = mgr.getDefault();
        SingleSheet over = mgr.addSingleSheet("over");
        def.set(ecp.get(), LEFT, 46);
        def.set(ecp.get(), RIGHT, 23);
        
        over.set(ecp.get(), LEFT, -46);
        over.set(ecp.get(), RIGHT, -23);
        
        // Check that configuration remains after a garbage collection event.
        System.gc();
        assertEquals(46, def.get(ecp.get(), LEFT).intValue());
        assertEquals(23, def.get(ecp.get(), RIGHT).intValue());
        assertEquals(-46, over.get(ecp.get(), LEFT).intValue());
        assertEquals(-23, over.get(ecp.get(), RIGHT).intValue());

        // Clear the only reference to the module.
        ecp.set(null);
        
        // Check that the configuration has vaniashed after gc.
        System.gc();
        assertFalse(def.hasSettings(id));
        assertFalse(over.hasSettings(id));
    }
    
    
}
