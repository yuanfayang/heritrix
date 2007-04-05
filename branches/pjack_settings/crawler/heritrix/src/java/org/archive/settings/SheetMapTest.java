/* 
 * Copyright (C) 2007 Internet Archive.
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
 * SheetMapTest.java
 *
 * Created on Feb 9, 2007
 *
 * $Id:$
 */
package org.archive.settings;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import junit.framework.TestCase;

public class SheetMapTest extends TestCase {

    
    final private static int TEST_DATA_SIZE = 100;

    
    private Integer[] testKeys;
    private Integer[] testKeyClones;
    private Date[] testValues;
    
    
    public void setUp() {
        Random random = new Random();
        testKeys = new Integer[TEST_DATA_SIZE];
        testKeyClones = new Integer[TEST_DATA_SIZE];
        testValues = new Date[TEST_DATA_SIZE];
        for (int i = 0; i < TEST_DATA_SIZE; i++) {
            int x = random.nextInt();
            testKeys[i] = new Integer(x);
            testKeyClones[i] = new Integer(x);
            testValues[i] = new Date();
        }
    }
    
    
    public void testGetPutRemove() {
        SheetMap<Integer,Date> sheetMap = new SheetMap<Integer,Date>();
        
        // Empty map should never produce results from get()
        for (Integer key: testKeys) {
            assertTrue(sheetMap.get(key) == null);
        }

        // Put, then get, then remove
        // Make sure map stays empty at end of each pass
        for (int i = 0; i < testKeys.length; i++) {
            Integer key = testKeys[i];
            Date value = testValues[i];
            assertEquals(0, sheetMap.size);

            assertTrue(sheetMap.putIfAbsent(key, value) == null);
            assertEquals(1, sheetMap.size);
            
            assertTrue(sheetMap.putIfAbsent(key, value) == value);
            assertEquals(1, sheetMap.size);
            
            assertTrue(sheetMap.get(key) == value);
            assertEquals(1, sheetMap.size);

            assertTrue(sheetMap.remove(key) == value);
            assertEquals(0, sheetMap.size);

            assertTrue(sheetMap.get(key) == null);
            assertEquals(0, sheetMap.size);
            
            assertTrue(sheetMap.remove(key) == null);
            assertEquals(0, sheetMap.size);
        }

        // Put then get each key, without remove
        // Ensure that map is growing over time to accomodate more elements
        for (int i = 0; i < testKeys.length; i++) {
            Integer key = testKeys[i];
            Date value = testValues[i];
            assertTrue(sheetMap.putIfAbsent(key, value) == null);
            assertTrue(sheetMap.get(key) == value);
            assertEquals(i + 1, sheetMap.size);
            assertTrue(sheetMap.putIfAbsent(key, new Date()) == value);
            assertEquals(i + 1, sheetMap.size);
            assertTrue(sheetMap.get(key) == value);
            assertTrue(i < sheetMap.threshold);
            assertEquals(sheetMap.buckets.length() * 75 / 100, 
                    sheetMap.threshold);
            for (int j = 0; j <= i; j++) {
                Date n = new Date();
                Date o = sheetMap.putIfAbsent(testKeys[j], n);
                assertTrue(o == testValues[j]);
            }
        }

        // Remove each key, make sure map contains no elements
        for (int i = 0; i < testKeys.length; i++) {
            Integer key = testKeys[i];
            Date value = testValues[i];
            assertTrue(sheetMap.remove(key) == value);
            assertEquals(testKeys.length - i - 1, sheetMap.size);
            assertTrue(sheetMap.remove(key) == null);
            assertEquals(testKeys.length - i - 1, sheetMap.size);
        }
    }

    
    
    public void testWeakness() {
        SheetMap<Integer,Date> sheetMap = makeSheetMap();
        
        for (int i = 0; i < testKeys.length; i++) {
            testKeys[i] = null;
            System.gc();
            sheetMap.get(testKeyClones[i]);
            assertEquals(testKeys.length - i - 1, sheetMap.size);
        }

    }
    
    
    private SheetMap<Integer,Date> makeSheetMap() {
        SheetMap<Integer,Date> sheetMap = new SheetMap<Integer,Date>();
        for (int i = 0; i < testKeys.length; i++) {
            Integer key = testKeys[i];
            Date value = testValues[i];
            assertTrue(sheetMap.putIfAbsent(key, value) == null);
        }
        return sheetMap;
    }
    
 
    /**
     * Tests that the map works under many threads.  100 threads are created.
     * The threads share some state defined in BlastThread.  All BlastThreads
     * access a single static SheetMap instance.  The BlastThread.present list
     * defines key/value pairs we expect to find in the SheetMap.  The
     * BlastThread.removed list defines key/value we expect <i>not</i> to 
     * find in the SheetMap.
     * 
     * <p>89 of the threads are "get" threads, which perform a get() lookup
     * on the SheetMap.
     * 
     * <p>5 of the threads are "put" threads, which perform a remove() from
     * the BlastThread.removed list and then puts that key/value pair into
     * the SheetMap and also into the present list.
     * 
     * <p>5 of the threads are "remove" threads, which perform a remove() from
     * the present list, adds the key/value pair to the removed list, and 
     * removes the key from the SheetMap.
     * 
     * <p>The last thread is a "garbage collection" thread, which just
     * removes something from the present list without adding it anywhere else.
     * That should be the only strong reference to the key, so that key 
     * should not appear in the SheetMap.
     * 
     * <p>To make a long story short, the Get threads are constantly performing
     * read operations while the put and remove threads compete to modify the
     * map.  These threads last for ten seconds, after which all threads are
     * shut down.  After all the threads shut down, we inspect the present list,
     * the removed list, and the SheetMap to ensure that the SheetMap contains
     * all the expected elements, and does not contain any of the unexpected
     * elements.
     * 
     * @throws Exception
     */
    public void testManyThreads() throws Exception {
        BlastThread[] threads = new BlastThread[100];
        for (int i = 0; i < 89; i++) {
            threads[i] = new BlastThread(BlastThread.GET);
        }
        threads[89] = new BlastThread(BlastThread.GC);
        for (int i = 90; i < 95; i++) {
            threads[i] = new BlastThread(BlastThread.PUT);
        }
        for (int i = 95; i < 100; i++) {
            threads[i] = new BlastThread(BlastThread.REMOVE);
        }
        for (int i = 0; i < threads.length; i++) {
            threads[i].start();
        }
        
        Thread.sleep(10000);
        BlastThread.go = false;
        
        boolean exception = false;
        for (int i = 0; i < threads.length; i++) {
            threads[i].join();
            exception = exception | threads[i].exception;
        }
        
        assertFalse(exception);
        
        for (Pair p: BlastThread.present) {
            assertTrue(BlastThread.sheetMap.get(p.key) == p.value);
        }
        for (Pair p: BlastThread.removed) {
            assertTrue(BlastThread.sheetMap.get(p.key) == null);
        }
        
        long total = 0;
        int count = 0;
        for (int i = 0; i < 89; i++) {
            total += threads[i].duration;
            count += threads[i].runs;
        }
        long getAvg = total * 1000L / count;
        System.out.println("Average get() duration in nanoseconds: " + getAvg);

        total = 0;
        count = 0;
        for (int i = 90; i < 95; i++) {
            total += threads[i].duration;
            count += threads[i].runs;
        }
        long putAvg = total * 1000L / count;
        System.out.println("Average put() duration in nanoseconds: " + putAvg);

        total = 0;
        count = 0;
        for (int i = 95; i < 100; i++) {
            total += threads[i].duration;
            count += threads[i].runs;
        }
        long remAvg = total * 1000L / count;
        System.out.println("Average rem() duration in nanoseconds: " + remAvg);

        assertTrue(getAvg < putAvg);
        assertTrue(getAvg < remAvg);
    }

    
    public void testSerialization() throws Exception {
        // Serialize a sheet map.
        SheetMap<Integer,Date> first = makeSheetMap();
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ObjectOutputStream oout = new ObjectOutputStream(bout);
        oout.writeObject(first);
        byte[] firstSer = bout.toByteArray();
        
        // Deserialize it.
        ByteArrayInputStream binp = new ByteArrayInputStream(firstSer);
        ObjectInputStream oinp = new ObjectInputStream(binp);
        SheetMap second = (SheetMap)oinp.readObject();
        
        Set firstPairs = first.makeNodeSet();
        Set secondPairs = second.makeNodeSet();
        assertTrue(firstPairs.equals(secondPairs));
    }
    
    
}


class Pair {
    public Object key = new Object();
    public Object value = new Object();
    
    public Pair() {
        this.key = new Object();
        this.value = new Object();
    }
    
    
    public Pair(Object key, Object value) {
        this.key = key;
        this.value = value;
    }
}


class BlastThread extends Thread {

    
    final public static int PUT = 1;
    final public static int REMOVE = 2;
    final public static int GET = 3;
    final public static int GC = 4;

    static List<Pair> removed = new CopyOnWriteArrayList<Pair>();
    static List<Pair> present = new CopyOnWriteArrayList<Pair>();
    volatile static boolean go = true;
    static SheetMap<Object,Object> sheetMap = new SheetMap<Object,Object>();
    
    final public int mode;
    public boolean exception;
    
    
    public long duration;
    public int runs;
    
    
    static {
        for (int i = 0; i < 1000; i++) {
            removed.add(new Pair());
            Pair n = new Pair();
            present.add(n);
            sheetMap.putIfAbsent(n.key, n.value);
        }
    }
    
    public BlastThread(int mode) {
        this.mode = mode;
    }
    
    
    public void run() {
        long start = System.currentTimeMillis();
        int runs = 0;
        try {
            while (go) {
                runs++;
                switch (mode) {
                    case PUT:
                        put();
                        break;
                    case GET:
                        get();
                        break;
                    case REMOVE:
                        remove();
                        break;
                    case GC:
                        gc();
                        break;
                    default:
                        throw new Error();
                }
            }
        } catch (RuntimeException e) {
            exception = true;
            e.printStackTrace(System.out);
            System.out.println();
        }
        duration = System.currentTimeMillis() - start;
        this.runs = runs;
    }

    
    private void put() {
        try {
            Pair p = removed.remove(0);
            sheetMap.putIfAbsent(p.key, p.value);
            present.add(p);
        } catch (IndexOutOfBoundsException e) {
            
        }
    }
    
    
    private void get() {
        Pair p;
        try {
            p = present.get(present.size() - 1);
        } catch (IndexOutOfBoundsException e) {
            return;
        }
        
        sheetMap.get(p.key);
    }
    
    
    private void remove() {
        try {
            Pair p = present.remove(0);
            sheetMap.remove(p.key);
            removed.add(p);
        } catch (IndexOutOfBoundsException e) {
            
        }
    }
    
    
    private void gc() {
        try {
            present.remove(0);
            Thread.sleep(200);
        } catch (IndexOutOfBoundsException e) {
            
        } catch (InterruptedException e) {
            return;
        }
        
    }
}