/* CachedBdbMapTest
 * 
 * Created on Apr 11, 2005
 *
 * Copyright (C) 2005 Internet Archive.
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

import java.util.HashMap;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author stack
 * @version $Date$, $Revision$
 */
public class CachedBdbMapTest extends TmpDirTestCase {
    private CachedBdbMap cache;
    
    protected void setUp() throws Exception {
        super.setUp();
        this.cache = new CachedBdbMap(getTmpDir(),
            this.getClass().getName(), String.class, HashMap.class);
    }
    
    protected void tearDown() throws Exception {
        this.cache.close();
        super.tearDown();
    }
    
    public void testBackingDbGetsUpdated() {
        // Enable all logging. Up the level on the handlers and then
        // on the big map itself.
        Handler [] handlers = Logger.getLogger("").getHandlers();
        for (int index = 0; index < handlers.length; index++) {
            handlers[index].setLevel(Level.FINEST);
        }
        Logger.getLogger(CachedBdbMap.class.getName()).
            setLevel(Level.FINEST);
        // Set up values.
        final String value = "value";
        final String key = "key";
        final int upperbound = 3;
        // First put in empty hashmap.
        for (int i = 0; i < upperbound; i++) {
            this.cache.put(key + Integer.toString(i), new HashMap());
        }
        // Now add value to hash map.
        for (int i = 0; i < upperbound; i++) {
            HashMap m = (HashMap)this.cache.get(key + Integer.toString(i));
            m.put(key, value);
        }
        this.cache.sync();
        for (int i = 0; i < upperbound; i++) {
            HashMap m = (HashMap)this.cache.get(key + Integer.toString(i));
            String v = (String)m.get(key);
            if (v == null || !v.equals(value)) {
                Logger.getLogger(CachedBdbMap.class.getName()).
                    warning("Wrong value " + i);
            }
        }
    }
    
    public static void main(String [] args) {
        junit.textui.TestRunner.run(CachedBdbMapTest.class);
    }
}
