/* ARHostQueueList.java
*
* Created on Sep 13, 2004
*
* Copyright (C) 2004 Kristinn Sigurðsson.
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
package is.hi.bok.crawler.frontier;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeSet;

import org.archive.crawler.datamodel.CrawlServer;

import com.sleepycat.bind.EntryBinding;
import com.sleepycat.bind.tuple.IntegerBinding;
import com.sleepycat.bind.tuple.StringBinding;
import com.sleepycat.je.Cursor;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;

/**
 * Maintains an ordered list of ARHostQueues used by a Frontier.
 * <p> 
 * The list is ordered by the 
 * {@link ARHostQueue#getNextReadyTime() ARHostQueue.getNextReadyTime()}, 
 * smallest value at the top of the list and then on in descending order.
 * <p>
 * The list will maintain a list of hostnames in a seperate DB. On creation a
 * list will try to open the DB at a specified location. If it already exists
 * the list will create HQs for all the hostnames in the list, discarding 
 * those that turn out to be empty.
 * <p>
 * Any BDB DatabaseException will be converted to an IOException by public 
 * methods. This includes preserving the original stacktrace, in favor of the
 * one created for the IOException, so that the true source of the exception
 * is not lost. 
 *
 * @author Kristinn Sigurdsson
 */
public class ARHostQueueList {

    // TODO: Handle HQs becoming empty.
    
    /** The Environment for the BerkleyDB databases in the HQs */
    protected Environment env;
    /** Contains host names for all HQs. Name is key, valence is value */
    protected Database hostNamesDB;
    protected EntryBinding keyBinding;
    protected EntryBinding valueBinding;
    
    /** A hash table of all the HQs, keyed by hostName */
    protected HashMap hostQueues; 
    /** Contains the hostQueues sorted by their time of next fetch */
    protected TreeSet sortedHostQueues;
    
    public ARHostQueueList(File locationDirectory) throws IOException{
        try{
            // First, set up the BerkleyDB environment.
            EnvironmentConfig envConfig = new EnvironmentConfig();
            envConfig.setTransactional(true); 
            envConfig.setAllowCreate(true);    
            env = new Environment(locationDirectory,envConfig);
            
            keyBinding = new StringBinding();
            valueBinding = new IntegerBinding();
            
            // Then initialize other data
            hostQueues = new HashMap();
            sortedHostQueues = new TreeSet();
            
            // Open the hostNamesDB
            DatabaseConfig dbConfig = new DatabaseConfig();
            dbConfig.setTransactional(false); 
            dbConfig.setAllowCreate(true);
            hostNamesDB = env.openDatabase(null, 
                                           "/hostNames", 
                                           dbConfig);
            
            // Read any existing hostNames and create relevant HQs
            Cursor cursor = hostNamesDB.openCursor(null,null);
            DatabaseEntry keyEntry = new DatabaseEntry();
            DatabaseEntry dataEntry = new DatabaseEntry();
            
            OperationStatus opStatus = cursor.getFirst(
                    keyEntry, dataEntry, LockMode.DEFAULT);
            
            while(opStatus == OperationStatus.SUCCESS){
                // Got one!
                String hostName = (String) keyBinding.entryToObject(keyEntry);
                int valence = 
                    ((Integer)valueBinding.entryToObject(dataEntry)).intValue();
                opStatus = cursor.getNext(keyEntry,dataEntry,LockMode.DEFAULT);
                // Create HQ
                ARHostQueue tmp = createHQ(hostName,valence);
                // TODO: If the hq is empty, then it can be discarded immediately
            }
            cursor.close();
        } catch (DatabaseException e) {
            IOException e2 = new IOException(e.getMessage());
            e2.setStackTrace(e.getStackTrace());
            throw e2; 
        }
    }
        
    /**
     * Get an ARHostQueue for the specified host.
     * <p>
     * If one does not already exist, null is returned 
     * 
     * @param hostName The host's name 
     * @return an ARHostQueue for the specified host
     */
    public ARHostQueue getHQ(String hostName){
        ARHostQueue hq = 
            (ARHostQueue)hostQueues.get(hostName);
        return hq;
    }
    
    /**
     * Creates a new ARHostQueue.
     * <p>
     * If a HQ already existed for the specified hostName, the existing HQ
     * is returned as it is. It's existing valence will <i>not</i> be updated
     * to reflect a different valence.
     * 
     * @param hostName
     * @param valence number of simultaneous connections allowed to this host
     * @return the newly created HQ
     * @throws IOException
     */
    public ARHostQueue createHQ(String hostName, int valence)
            throws IOException{
        ARHostQueue hq = (ARHostQueue)hostQueues.get(hostName);
        if(hq == null){
            // Ok, the HQ does not already exist. (Had to make sure) 
            // Create it, save it and return it.
            hq = new ARHostQueue(hostName,env,valence);
            hq.setOwner(this);
            
            try{
                DatabaseEntry keyEntry = new DatabaseEntry();
                DatabaseEntry dataEntry = new DatabaseEntry();
                keyBinding.objectToEntry(hostName,keyEntry);
                valueBinding.objectToEntry(new Integer(valence),dataEntry);
                hostNamesDB.put(null,keyEntry,dataEntry);
                hostQueues.put(hostName,hq);
                sortedHostQueues.add(new ARHostQueueReference(hq));
            } catch (DatabaseException e) {
                IOException e2 = new IOException(e.getMessage());
                e2.setStackTrace(e.getStackTrace());
                throw e2; 
            }
        }
        return hq;
    }
    
    public ARHostQueue getTopHQ(){
        ARHostQueueReference ref = 
            (ARHostQueueReference)sortedHostQueues.first(); 
        return (ARHostQueue)hostQueues.get(ref.hostName);
    }
    
    public int getSize(){
        return hostQueues.size(); 
    }
    
    /**
     * This method reorders the host queues. Method is only called by the
     * ARHostQueues that it 'owns' when their reported time of next ready is 
     * being updated.
     * @param hq The calling HQ
     * @param oldvalue The old value of the HQs time of next ready which has
     *                 now been overwritten. 
     */
    protected void reorder(ARHostQueue hq, long oldvalue){
System.err.println("HQlist.reorder(" + hq.getHostName() + ")");
        // FIXME: This isn't working. Write Unit test maybe.
        // Create a 'comparably equal to the old value' ref.
        ARHostQueueReference ref = 
            new ARHostQueueReference(hq.getHostName(),oldvalue);
        // Remove it from the sorted list
        System.err.println(sortedHostQueues.remove(ref));
        // Update the time on the ref.
        ref.nextReadyTime = hq.getNextReadyTime();
        // Readd to the list
        sortedHostQueues.add(ref);
    }
    
    /**
     * The total number of URIs queued in all the HQs belonging to this list.
     * 
     * @return total number of URIs queued in all the HQs belonging to this list.
     */
    public long getUriCount(){
        Iterator it = hostQueues.keySet().iterator();
        long count = 0;
        while(it.hasNext()){
            ARHostQueueReference hqref = (ARHostQueueReference)it.next();
            ARHostQueue tmp = (ARHostQueue)hostQueues.get(hqref.hostName);
            count += tmp.getSize();
        }
        return count;
    }
    
    public String report(){
        StringBuffer ret = new StringBuffer(256);
        Iterator it = sortedHostQueues.iterator();
        while(it.hasNext()){
            ARHostQueueReference hqref = (ARHostQueueReference)it.next();
            ARHostQueue tmp = (ARHostQueue)hostQueues.get(hqref.hostName);
          
            ret.append(tmp.report());
            ret.append("\n\n");
        }
        return ret.toString();
    }
    
    public String oneLineReport(){
        StringBuffer ret = new StringBuffer(256);
        Iterator it = sortedHostQueues.iterator();
        while(it.hasNext()){
            ARHostQueueReference hqref = (ARHostQueueReference)it.next();
            ret.append(hqref.hostName + " - " + (hqref.nextReadyTime - System.currentTimeMillis()));
            ret.append(", ");
        }
        return ret.toString();
    }
    
    /**
     * Rather then store the actual ARHostQueue object in the sorted order,
     * we store an instance of this 'reference' class. While this means 
     * additional dereferencing when it comes time to get an elemented out of
     * the sorted list, it does ensure that the values by which we sort them
     * have not changed.
     * <p>
     * Without this class, by the time the call came to reorder an item, its
     * values (on which the sort order is based) would already have changed,
     * making it impossible to retrieve it from the sorted list.
     *
     * @author Kristinn Sigurdsson
     */
    private class ARHostQueueReference implements Comparable{
        public String hostName;
        public long nextReadyTime;
        
        public ARHostQueueReference(String hostName, long nextReadyTime){
            this.hostName = hostName;
            this.nextReadyTime = nextReadyTime;
        }
        
        public ARHostQueueReference(ARHostQueue hq){
            hostName = hq.getHostName();
            nextReadyTime = hq.getNextReadyTime();
        }
        
        /**
         * Compares the this ARHQRef to the supplied one.
         * 
         * @param obj the HQ to compare to. If this object is not an instance
         *             of ARHostQueueReference then the method will throw a 
         *             ClassCastException.
         * @return the value 0 if the argument HQRef's time of next ready
         *         is equal to this HQRef's; a value less than 0 if this 
         *         value is less than the argument HQRef's; and a value 
         *         greater than 0 if this value is greater.
         */
        public int compareTo(Object obj){
            ARHostQueueReference comp = (ARHostQueueReference)obj;

            long compTime = comp.nextReadyTime;
            if(nextReadyTime>compTime){
                return 1;
            } else if(nextReadyTime<compTime){
                return -1;
            } else {
                // Equal time. Use hostnames
                return hostName.compareTo(comp.hostName);
            }
        }
    }
}
