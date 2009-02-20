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

import java.io.File;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.math.RandomUtils;

/**
 * @author stack
 * @version $Date$, $Revision$
 */
public class CachedBdbMapTest extends TmpDirTestCase {
    File envDir; 
    CachedBdbMap cache = null;
    
    @SuppressWarnings("unchecked")
    protected void setUp() throws Exception {
        super.setUp();
        this.envDir = new File(getTmpDir(),"CachedBdbMapTest");
        this.envDir.mkdirs();
        
    }
    
    protected void tearDown() throws Exception {
        if(cache!=null) {
            cache.close();
            cache = null;
        }
        FileUtils.deleteDir(this.envDir);
        super.tearDown();
    }
    
    @SuppressWarnings("unchecked")
    public void testReadConsistencyUnderLoad() throws Exception {
        final CachedBdbMap<Integer,AtomicInteger> cbdbmap = 
            new CachedBdbMap(
                    this.envDir, 
                    this.getClass().getName(), 
                    Integer.class, 
                    AtomicInteger.class);
        this.cache = cbdbmap;
        final AtomicInteger level = new AtomicInteger(0);
        final int keyCount = 128 * 1024; // 128K  keys
        final int maxLevel = 64; 
        // initial fill
        for(int i=0; i < keyCount; i++) {
            cbdbmap.put(i, new AtomicInteger(level.get()));
        }
        // backward checking that all values always at level or higher
        new Thread() {
            public void run() {
                untilmax: while(true) {
                    for(int j=keyCount-1; j >= 0; j--) {
                        int targetValue = level.get(); 
                        if(targetValue>=maxLevel) {
                            break untilmax;
                        }
                        assertTrue("stale value revseq key "+j,cbdbmap.get(j).get()>=targetValue);
                        Thread.yield();
                    }
                }
            }
        }.start();
        // random checking that all values always at level or higher
        new Thread() {
            public void run() {
                untilmax: while(true) {
                    int j = RandomUtils.nextInt(keyCount);
                    int targetValue = level.get(); 
                    if(targetValue>=maxLevel) {
                        break untilmax;
                    }
                    assertTrue("stale value random key "+j,
                            cbdbmap.get(j).get()>=targetValue);
                    Thread.yield();
                }
            }
        }.start();
        // increment all keys
        for(; level.get() < maxLevel; level.incrementAndGet()) {
            for(int k = 0; k < keyCount; k++) {
                int foundValue = cbdbmap.get(k).getAndIncrement();
                assertEquals("stale value preinc key "+k, level.get(), foundValue);
            }
            if(level.get() % 10 == 0) {
                System.out.println("level to "+level.get());
            }
            Thread.yield(); 
        }
        // SUCCESS
    }
    
    @SuppressWarnings("unchecked")
    public void testBackingDbGetsUpdated() throws Exception {
        CachedBdbMap<String,HashMap<String,String>> cbdbmap = 
            new CachedBdbMap(
                    this.envDir, 
                    this.getClass().getName(), 
                    String.class, 
                    HashMap.class);
        this.cache = cbdbmap;
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
            cbdbmap.put(key + Integer.toString(i), new HashMap<String,String>());
        }
        // Now add value to hash map.
        for (int i = 0; i < upperbound; i++) {
            HashMap<String,String> m = cbdbmap.get(key + Integer.toString(i));
            m.put(key, value);
        }
        cbdbmap.sync();
        for (int i = 0; i < upperbound; i++) {
            HashMap<String,String> m = cbdbmap.get(key + Integer.toString(i));
            String v = m.get(key);
            if (v == null || !v.equals(value)) {
                Logger.getLogger(CachedBdbMap.class.getName()).
                    warning("Wrong value " + i);
            }
        }
        cbdbmap.close();
    }
    
    public static void main(String [] args) {
        junit.textui.TestRunner.run(CachedBdbMapTest.class);
    }
}
