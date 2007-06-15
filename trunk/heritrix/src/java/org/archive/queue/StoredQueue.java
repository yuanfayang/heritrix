/* StoredQueue.java
 *
 * $Id: BloomFilter32bitSplit.java 5197 2007-06-06 01:31:46Z gojomo $
 *
 * Created on Jun 14, 2007
 *
 * Copyright (C) 2007 Internet Archive
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
package org.archive.queue;

import java.io.Serializable;
import java.util.AbstractQueue;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicLong;

import com.sleepycat.bind.EntryBinding;
import com.sleepycat.bind.serial.SerialBinding;
import com.sleepycat.bind.serial.StoredClassCatalog;
import com.sleepycat.bind.tuple.TupleBinding;
import com.sleepycat.collections.StoredSortedMap;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;

/**
 * Queue backed by a JE Collections StoredSortedMap. 
 * 
 * @author gojomo
 *
 * @param <E>
 */
public class StoredQueue<E extends Serializable> extends AbstractQueue<E>  {
    StoredSortedMap queueMap; // Long -> E
    AtomicLong tailIndex = new AtomicLong(0); // next spot for insert
    AtomicLong headIndex = new AtomicLong(0); // next spot for read
 
    /**
     * Create a StoredQueue backed by the given Database. 
     * 
     * The Class of values to be queued may be provided; there is only a 
     * benefit when a primitive type is specified. A StoredClassCatalog
     * must be provided if a primitive type is not supplied. 
     * 
     * @param db
     * @param clsOrNull 
     * @param classCatalog
     */
    public StoredQueue(Database db, Class clsOrNull, StoredClassCatalog classCatalog) {
        EntryBinding valueBinding = TupleBinding.getPrimitiveBinding(clsOrNull);
        if(valueBinding == null) {
            valueBinding = new SerialBinding(classCatalog, clsOrNull);
        }
        queueMap = new StoredSortedMap(
                db,
                TupleBinding.getPrimitiveBinding(Long.class),
                valueBinding,
                true);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Iterator<E> iterator() {
        return queueMap.values().iterator();
    }

    @Override
    public int size() {
        return queueMap.size();
    }

    public boolean offer(E o) {
        synchronized (tailIndex) {
            queueMap.put(tailIndex.getAndIncrement(), o);
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    public E peek() {
        synchronized (headIndex) {
            return (E) queueMap.get(headIndex.get());
        }
    }

    @SuppressWarnings("unchecked")
    public E poll() {
        synchronized (headIndex) {
            return (E) queueMap.remove(headIndex.getAndIncrement());
        }
    }

    /**
     * A suitable DatabaseConfig for the Database backing a StoredQueue. 
     * (However, it is not necessary to use these config options.)
     * 
     * @return DatabaseConfig suitable for queue
     */
    protected static DatabaseConfig databaseConfig() {
        DatabaseConfig dbConfig = new DatabaseConfig();
        dbConfig.setTransactional(false);
        dbConfig.setAllowCreate(true);
        dbConfig.setDeferredWrite(true);
        return dbConfig;
    }
}