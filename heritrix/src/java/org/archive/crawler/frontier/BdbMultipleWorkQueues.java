/* BdbMultipleWorkQueues
 * 
 * Created on Dec 24, 2004
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
package org.archive.crawler.frontier;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.framework.FrontierMarker;
import org.archive.util.ArchiveUtils;

import st.ata.util.FPGenerator;

import com.sleepycat.bind.serial.SerialBinding;
import com.sleepycat.bind.serial.StoredClassCatalog;
import com.sleepycat.je.Cursor;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.DatabaseNotFoundException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.OperationStatus;


/**
 * A BerkeleyDB-database-backed structure for holding ordered
 * groupings of CrawlURIs. Reading the groupings from specific
 * per-grouping (per-classKey/per-Host) starting points allows
 * this to act as a collection of independent queues. 
 * 
 * TODO: cleanup/close handles, refactor, improve naming
 * @author gojomo
 */
public class BdbMultipleWorkQueues {
    /** database holding all pending URIs, grouped in virtual queues */
    protected Database pendingUrisDB = null;
    /**  Supporting bdb serialization of CrawlURIs*/
    protected SerialBinding crawlUriBinding;

    /**
     * Create the multi queue in the given environment. 
     * 
     * @param env bdb environment to use
     * @param classCatalog Class catalog to use.
     * @throws DatabaseException
     */
    public BdbMultipleWorkQueues(Environment env,
        StoredClassCatalog classCatalog)
    throws DatabaseException {
        // Open the database. Create it if it does not already exist. 
        DatabaseConfig dbConfig = new DatabaseConfig();
        dbConfig.setAllowCreate(true);
        try {
            // new preferred truncateDatabase() method cannot be used
            // on an open (and thus certain to exist) database like 
            // the deprecated method it replaces -- so use before
            // opening the database and be ready if it doesn't exist
            env.truncateDatabase(null, "pending", false);
        } catch (DatabaseNotFoundException e) {
            // ignored
        }
        pendingUrisDB = env.openDatabase(null, "pending", dbConfig);
        crawlUriBinding = new SerialBinding(classCatalog, CrawlURI.class);
    }

    /**
     * Delete all CrawlURIs matching the given expression.
     * 
     * @param match
     * @param queue
     * @param headKey
     * @return count of deleted items
     * @throws DatabaseException
     * @throws DatabaseException
     */
    public long deleteMatchingFromQueue(String match, String queue,
            DatabaseEntry headKey) throws DatabaseException {
        long deletedCount = 0;
        Pattern pattern = Pattern.compile(match);
        DatabaseEntry key = headKey;
        DatabaseEntry value = new DatabaseEntry();

        Cursor cursor = null;
        try {
            cursor = pendingUrisDB.openCursor(null, null);
            OperationStatus result = cursor.getSearchKeyRange(headKey,
                    value, null);

            while (result == OperationStatus.SUCCESS) {
                CrawlURI curi = (CrawlURI) crawlUriBinding
                        .entryToObject(value);
                if (!curi.getClassKey().equals(queue)) {
                    // rolled into next queue; finished with this queue
                    break;
                }
                if (pattern.matcher(curi.getURIString()).matches()) {
                    cursor.delete();
                    deletedCount++;
                }
                result = cursor.getNext(key, value, null);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return deletedCount;
    }
    
    /**
     * @param m marker
     * @param maxMatches
     * @return list of matches starting from marker position
     * @throws DatabaseException
     */
    public List getFrom(FrontierMarker m, int maxMatches) throws DatabaseException {
        int matches = 0;
        int tries = 0;
        ArrayList results = new ArrayList(maxMatches);
        BdbFrontierMarker marker = (BdbFrontierMarker) m;
        
        DatabaseEntry key = marker.getStartKey();
        DatabaseEntry value = new DatabaseEntry();
        
        Cursor cursor = null;
        OperationStatus result = null;
        try {
            cursor = pendingUrisDB.openCursor(null,null);
            result = cursor.getSearchKey(key, value, null);
            
            while(matches<maxMatches && result == OperationStatus.SUCCESS) {
                CrawlURI curi = (CrawlURI) crawlUriBinding.entryToObject(value);
                if(marker.accepts(curi)) {
                    results.add(curi);
                    matches++;
                }
                tries++;
                result = cursor.getNext(key,value,null);
            }
        } finally {
            if (cursor !=null) {
                cursor.close();
            }
        }
        
        if(result != OperationStatus.SUCCESS) {
            // end of scan
            marker.setStartKey(null);
        }
        return results;
    }
    
    /**
     * Get a marker for beginning a scan over all contents
     * 
     * @param regexpr
     * @return a marker pointing to the first item
     */
    public FrontierMarker getInitialMarker(String regexpr) {
        try {
            return new BdbFrontierMarker(getFirstKey(), regexpr);
        } catch (DatabaseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null; 
        }
    }
    
    /**
     * @return the key to the first item in the database
     * @throws DatabaseException
     */
    protected DatabaseEntry getFirstKey() throws DatabaseException {
        DatabaseEntry key = new DatabaseEntry();
        DatabaseEntry value = new DatabaseEntry();
        Cursor cursor = pendingUrisDB.openCursor(null,null);
        cursor.getNext(key,value,null);
        cursor.close();
        return key;
    }
    
    /**
     * Get the next nearest item after the given key. Relies on 
     * external discipline to avoid asking for something from an
     * origin where there are no associated items -- otherwise
     * could get first item of next 'queue' by mistake. 
     * 
     * TODO: hold within a queue's range
     * 
     * @param headKey
     * @return CrawlURI.
     * @throws DatabaseException
     */
    public CrawlURI get(DatabaseEntry headKey) throws DatabaseException {
        DatabaseEntry result = new DatabaseEntry();
        Cursor cursor = pendingUrisDB.openCursor(null,null);
        cursor.getSearchKeyRange(headKey, result, null);
        cursor.close();
        CrawlURI retVal = (CrawlURI) crawlUriBinding.entryToObject(result);
        retVal.setHolderKey(headKey);
        return retVal;
    }
    
    
    
    /**
     * Put the given CrawlURI in at the appropriate place. 
     * 
     * @param curi
     * @throws DatabaseException
     */
    public void put(CrawlURI curi) throws DatabaseException {
        DatabaseEntry insertKey = (DatabaseEntry) curi.getHolderKey();
        if (insertKey == null) {
            insertKey = calculateInsertKey(curi);
            curi.setHolderKey(insertKey);
        }
        DatabaseEntry value = new DatabaseEntry();
        crawlUriBinding.objectToEntry(curi, value);
        pendingUrisDB.put(null, insertKey, value);
    }
    
    /**
     * Calculate the insertKey that places a CrawlURI in the
     * desired spot. First 60 bits are always host (classKey)
     * based -- ensuring grouping by host. Next 4 bits are
     * priority -- allowing 'immediate' and 'soon' items to 
     * sort above regular. Next 8 bits are 'cost'. Last 64 bits 
     * are ordinal serial number, ensuring earlier-discovered 
     * URIs sort before later. 
     * 
     * @param curi
     * @return a DatabaseEntry key for the CrawlURI
     */
    private DatabaseEntry calculateInsertKey(CrawlURI curi) {
        byte[] keyData = new byte[16];
        long fpPlus = FPGenerator.std64.fp(curi.getClassKey()) & 0xFFFFFFFFFFFFFFF0L;
        fpPlus = fpPlus | curi.getSchedulingDirective(); 
        ArchiveUtils.longIntoByteArray(fpPlus, keyData, 0);
        long ordinalPlus = curi.getOrdinal() & 0x00FFFFFFFFFFFFFFL;
        ordinalPlus = (((long)curi.getHolderCost()) << 56) | ordinalPlus;
        ArchiveUtils.longIntoByteArray(ordinalPlus, keyData, 8);
        return new DatabaseEntry(keyData);
    }
    
    /**
     * Delete the given CrawlURI from persistent store. Requires
     * the key under which it was stored be available. 
     * 
     * @param item
     * @throws DatabaseException
     */
    public void delete(CrawlURI item) throws DatabaseException {
        pendingUrisDB.delete(null, (DatabaseEntry) item.getHolderKey());
    }
    
    /**
     * clean up 
     *
     */
    public void close() {
        try {
            pendingUrisDB.close();
        } catch (DatabaseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    /**
     * Marker for remembering a position within the BdbMultipleWorkQueues.
     * 
     * @author gojomo
     */
    public class BdbFrontierMarker implements FrontierMarker {
        DatabaseEntry startKey;
        Pattern pattern; 
        int nextItemNumber;
        
        /**
         * Create a marker pointed at the given start location.
         * 
         * @param startKey
         * @param regexpr
         */
        public BdbFrontierMarker(DatabaseEntry startKey, String regexpr) {
            this.startKey = startKey;
            pattern = Pattern.compile(regexpr);
            nextItemNumber = 1;
        }
        
        /**
         * @param curi
         * @return whether the marker accepts the given CrawlURI
         */
        public boolean accepts(CrawlURI curi) {
            boolean retVal = pattern.matcher(curi.getURIString()).matches();
            if(retVal==true) {
                nextItemNumber++;
            }
            return retVal;
        }
        
        /**
         * @param key position for marker
         */
        public void setStartKey(DatabaseEntry key) {
            startKey = key;
        }
        
        /**
         * @return startKey
         */
        public DatabaseEntry getStartKey() {
            return startKey;
        }
        
        /* (non-Javadoc)
         * @see org.archive.crawler.framework.FrontierMarker#getMatchExpression()
         */
        public String getMatchExpression() {
            return pattern.pattern();
        }
        
        /* (non-Javadoc)
         * @see org.archive.crawler.framework.FrontierMarker#getNextItemNumber()
         */
        public long getNextItemNumber() {
            return nextItemNumber;
        }
        
        /* (non-Javadoc)
         * @see org.archive.crawler.framework.FrontierMarker#hasNext()
         */
        public boolean hasNext() {
            // as long as any startKey is stated, consider as having next
            return startKey != null;
        }
    }
}