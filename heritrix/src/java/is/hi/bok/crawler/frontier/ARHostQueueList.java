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

    /** The Environment for the BerkleyDB databases in the HQs */
    protected Environment env;
    /** Contains host names for all HQs. Name is key, valence is value */
    protected Database hostNamesDB;
    protected EntryBinding keyBinding;
    protected EntryBinding valueBinding;
    
    /** A hash table of all the HQs, keyed by hostName */
    protected HashMap hostQueues; 
    
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
        }
        try{
            DatabaseEntry keyEntry = new DatabaseEntry();
            DatabaseEntry dataEntry = new DatabaseEntry();
            keyBinding.objectToEntry(hostName,keyEntry);
            valueBinding.objectToEntry(new Integer(valence),dataEntry);
            hostNamesDB.put(null,keyEntry,dataEntry);
        } catch (DatabaseException e) {
            IOException e2 = new IOException(e.getMessage());
            e2.setStackTrace(e.getStackTrace());
            throw e2; 
        }
        return hq;
    }
    
    public ARHostQueue getTopHQ(){
        // No actual list is maintained. Just find the lowest value.
        Iterator it = hostQueues.keySet().iterator();
        ARHostQueue top =  
            (ARHostQueue)hostQueues.get(it.next());
        while(it.hasNext()){
            ARHostQueue tmp = 
                (ARHostQueue)hostQueues.get(it.next());
            if(tmp.compareTo(top)<0){
                top = tmp;
            }
        }
        return top;
    }
    
    public int getSize(){
        // TODO: Run through the list and remove any HQ where state is EMPTY
        return hostQueues.size(); 
    }
    
    /**
     * The total number of URIs queued in all the HQs belonging to this list.
     * @return total number of URIs queued in all the HQs belonging to this list.
     */
    public long getUriCount(){
        // TODO: Implement getUriCount();
        return 0;
    }
    
    public String report(){
        // TODO: Implement report()
        // Sort into list and iterate over it I suppose
        return "not implemented";
    }
    
    public String oneLineReport(){
        // TODO: Implmement one line report
        return "not implemented";
    }
}
