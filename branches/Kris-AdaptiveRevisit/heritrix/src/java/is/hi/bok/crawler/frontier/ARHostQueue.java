/* ARHostQueue.java
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

import java.io.IOException;

import org.archive.crawler.datamodel.CrawlURI;

import com.sleepycat.bind.EntryBinding;
import com.sleepycat.bind.serial.SerialBinding;
import com.sleepycat.bind.serial.StoredClassCatalog;
import com.sleepycat.bind.tuple.StringBinding;
import com.sleepycat.bind.tuple.TupleBinding;
import com.sleepycat.je.Cursor;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import com.sleepycat.je.SecondaryConfig;
import com.sleepycat.je.SecondaryDatabase;
import com.sleepycat.je.SecondaryKeyCreator;

/**
 * A priority based queue of CrawlURIs. Each queue should represent
 * one host (although this is not enforced in this class). Items are ordered
 * by the time of next processing and also indexed by the URI.
 * <p>
 * The HQ does no calculations on the 'time of next processing.' It always 
 * relies on values already set on the CrawlURI.
 * <p>
 * Note: Class is not 'thread safe.' In multi threaded environment the caller
 * must ensure that two threads do not make overlapping calls.
 * <p>
 * Any BDB DatabaseException will be converted to an IOException by public 
 * methods. This includes preserving the original stacktrace, in favor of the
 * one created for the IOException, so that the true source of the exception
 * is not lost. 
 *
 * @author Kristinn Sigurdsson
 */
public class ARHostQueue {
    // Constants declerations
    /** HQ contains no queued CrawlURIs elements. This state only occurs after 
     *  queue creation before the first add. After the first item is added the 
     *  state can never become empty again.  */
    public static final int HQSTATE_EMPTY = 0;
    /** HQ has a CrawlURI ready for processing */
    public static final int HQSTATE_READY = 1;
    /** HQ has maximum number of CrawlURI currently being processed. This number
     *  is either equal to the 'valence' (maximum number of simultanious 
     *  connections to a host) or (if smaller) the total number of CrawlURIs 
     *  in the HQ. */
    public static final int HQSTATE_BUSY = 2; 
    /** HQ is in a suspended state until it can be woken back up */
    public static final int HQSTATE_SNOOZED = 3;
    
    // Internal class variables
    /** Name of the host that this ARHostQueue represents */
    final String hostName;
    /** Last known state of HQ -- ALL methods should use getState() to read
     *  this value, never read it directly. */
    int state;
    /** Time (in milliseconds) when the HQ will next be ready to issue a URI
     *  for processing. 
     */
    long nextReadyTime;
    /** Time (in milliseconds) when each URI 'slot' becomes available again.<p>
     *  Any positive value larger then the current time signifies a taken slot 
     *  where the URI has completed processing but the politness wait has not
     *  ended. <p>
     *  A zero or positive value smaller then the current time in milliseconds
     *  signifies an empty slot.<p> 
     *  Any negative value signifies a slot for a URI that is being processed.
     */
    long[] wakeUpTime;
    /** Number of simultanious connections permitted to this host. I.e. this 
     *  many URIs can be issued before state of HQ becomes busy until one of 
     *  the is returned via the update method. */
    int valence;
    /**
     * Size of queue. That is, the number of CrawlURIs that have been added to
     * it, including any that are currently being processed.
     */
    long size;
    
    // Berkeley DB - All class member variables related to BDB JE
    // Databases
    /** Database containing the URI priority queue, indexed by a 64 bit 
     *  fingerprint of the URI. */
    protected Database primaryUriDB;
    /** Secondary index into {@link #primaryUriDB the primary DB}, URIs indexed
     *  by the time when they can next be processed again. */
    protected SecondaryDatabase secondaryUriDB;
    /** A database containing those URIs that are currently being processed. */
    protected Database processingUriDB;
    /** DB for the BDB serialization */
    protected Database classCatalogDB;
    // Serialization support
    /** For BDB serialization of objects */
    protected StoredClassCatalog classCatalog;
    /** A binding for the serialization of the primary key (URI string) */
    protected EntryBinding primaryKeyBinding;
    /** A binding for the serialization of the secondary index (time of next
     *  processing)*/
    protected EntryBinding secondaryKeyBinding;
    /** A binding for the CrawlURIARWrapper object */
    protected EntryBinding crawlURIBinding;
    // Cursors into databases
    /** A cursor into the secondaryUriDB. The first item here is the head of 
     *  this HQ's priority queue. The cursors position at any given time is
     *  undefined. Methods using it must seek the proper position themselves.*/
    protected Cursor secondaryCursor;

    
    
    /**
     * Constructor
     * 
     * @param hostName Name of the host this queue represents. This name must
     *                 be unique for all HQs in the same Environment.
     * @param env Berkeley DB Environment. All BDB databases created will use 
     *            it.
     * @param valence The total number of simultanous URIs that the HQ can issue
     *                for processing. Once this many URIs have been issued for
     *                processing, the HQ will go into {@link #HQSTATE_BUSY busy}
     *                state until at least one of the URI is 
     *                {@link #update(CrawlURI, boolean, long) updated}.
     *                Value should be larger then zero. Zero and negative values
     *                will be treated same as 1.
     * 
     * @throws IOException if an error occurs opening/creating the 
     *         database
     */
    public ARHostQueue(String hostName, Environment env, int valence) 
            throws IOException{
        try{
            if(valence < 1){
                this.valence = 1;
            } else {
                this.valence = valence;
            }
            wakeUpTime = new long[valence];
            for(int i = 0 ; i < valence ; i++){
                wakeUpTime[i]=0; // 0 means open slot.
            }
            
            this.hostName = hostName;
            
            state = HQSTATE_EMPTY; //HQ is initially empty.
            nextReadyTime = Long.MAX_VALUE; //Empty and busy HQ get this value.
            
            // Set up the primary URI database, it is indexed by URI names 
            DatabaseConfig dbConfig = new DatabaseConfig();
            dbConfig.setTransactional(false); 
            dbConfig.setAllowCreate(true);
            primaryUriDB = env.openDatabase(null, 
                                            hostName,
                                            dbConfig);
    
            // Set up a DB for storing class definitions. This allows for more
            // compact storage of Java serialized classes stored in the DB.
            DatabaseConfig catalogConfig = new DatabaseConfig();
            catalogConfig.setTransactional(false); 
            catalogConfig.setAllowCreate(true);
            classCatalogDB = env.openDatabase(null, 
                                              "catalogDb",
                                               catalogConfig);
            classCatalog = new StoredClassCatalog(classCatalogDB);
            
            // Set up a DB for storing URIs being processed
            DatabaseConfig dbConfig2 = new DatabaseConfig();
            dbConfig2.setTransactional(false); 
            dbConfig2.setAllowCreate(true);
            processingUriDB = env.openDatabase(null, 
                    hostName+"/processing",
                    dbConfig2);
            
            // Create a primitive binding for the primary key (URI string) 
            primaryKeyBinding = TupleBinding.getPrimitiveBinding(String.class);
            // Create a serial binding for the CrawlURIARWrapper object 
            crawlURIBinding = new SerialBinding(classCatalog, 
                                               CrawlURI.class);
            // Create a primitive binding for the secondary index (time of fetch)
            secondaryKeyBinding = TupleBinding.getPrimitiveBinding(Long.class);
    
            // Open a secondary database to allow accessing the primary
            // database by the secondary key value.
            SecondaryConfig secConfig = new SecondaryConfig();
            secConfig.setAllowCreate(true);
            secConfig.setSortedDuplicates(true);
            secConfig.setKeyCreator(
                    new MyKeyCreator(secondaryKeyBinding,crawlURIBinding));
            secondaryUriDB = env.openSecondaryDatabase(null, 
                                                       hostName+"/timeOfProcessing",
                                                       primaryUriDB,
                                                       secConfig);
            
            // Open a cursor into the secondary database;
            secondaryCursor = secondaryUriDB.openCursor(null,null);
            
            // Check if we are opening an existing DB...
            size = countCrawlURIs();
            if (size > 0) {
                // If size > 0 then we just opened an existing DB.
                // Set nextReadyTime;
                nextReadyTime = peek().getAList().getLong(
                        AdaptiveRevisitFrontier.A_TIME_OF_NEXT_PROCESSING);
                // Move any items in processingUriDB into the primariUriDB, ensure
                // that they wind up on top!
                flushProcessingURIs();
                state = HQSTATE_READY;
            } 
        } catch (DatabaseException e) {
            // Blanket catch all DBExceptions and convert to IOExceptions.
            IOException e2 = new IOException(e.getMessage());
            e2.setStackTrace(e.getStackTrace());
            throw e2; 
        }
    }
    
    /**
     * Returns the HQ's name
     * @return the HQ's name
     */
    public String getHostName() {
        return hostName;
    }
    
    /**
     * Add a CrawlURI to this host queue.
     * <p>
     * Calls can optionally chose to have the time of next processing value 
     * override existing values for the URI if the existing values are 'later'
     * then the new ones. 
     * 
     * @param curi The CrawlURI to add.
     * @param overrideSetTimeOnDups If true then the time of next processing for
     *                           the supplied URI will override the any
     *                           existing time for it already stored in the HQ.
     *                           If false, then no changes will be made to any
     *                           existing values of the URI. Note: Will never
     *                           override with a later time.
     * @throws IOException When an error occurs accessing the database
     */
    public void add(CrawlURI curi, boolean overrideSetTimeOnDups) 
            throws IOException{
        try{
            if(inProcessing(curi.getURIString())){
                // If it is currently being processed, then it is already been added
                // and we sure as heck can't fetch it any sooner!
                return;
            }
            
            OperationStatus opStatus = strictAdd(curi,false);
            
            long curiProcessingTime = curi.getAList().getLong(
                    AdaptiveRevisitFrontier.A_TIME_OF_NEXT_PROCESSING);
    
            if (opStatus == OperationStatus.KEYEXIST){ 
                if(overrideSetTimeOnDups==false){
                    // We are done. The URI already existed and we are not going to
                    // override
                    return;
                } else {
                    // Override an existing URI
                    // We need to extract the old CrawlURI (it contains vital info
                    // on past crawls), update its time of next processing and
                    // put it back into the queue (overwriting it's old value)
                    CrawlURI curiExisting = getCrawlURI(curi.getURIString());
                    long oldCuriProcessingTime = curiExisting.getAList().getLong(
                            AdaptiveRevisitFrontier.A_TIME_OF_NEXT_PROCESSING);
                    if( curiProcessingTime >= oldCuriProcessingTime ){
                        // The new processing time is later or equal to the 
                        // existing processing time. Since we never want to delay
                        // the fetching of a URI we can do nothing and exit method.
                        return;
                    }
                    curiExisting.getAList().putLong(
                            AdaptiveRevisitFrontier.A_TIME_OF_NEXT_PROCESSING,
                            curiProcessingTime);
                    opStatus = strictAdd(curiExisting,true); //Override
                    
                }
            } else if(opStatus == OperationStatus.SUCCESS) {
                // Just inserted a brand new CrawlURI into the queue.
                size++;
            }
    
            // Finally, check if insert (fresh add or override) into DB was 
            // successful and if so check if we need to update nextReadyTime.
            if(opStatus == OperationStatus.SUCCESS){
                if (curiProcessingTime < nextReadyTime){
                    // Update nextReadyTime to reflect new value.
                    nextReadyTime = curiProcessingTime;
                }
                if(state == HQSTATE_EMPTY){
                    // Definately no longer empty.
                    state = HQSTATE_READY;
                }
            } else {
                // Something went wrong. Throw an exception.
                throw new DatabaseException("Error on add into database for " +
                        "CrawlURI " + curi.getURIString() + ". " + 
                        opStatus.toString());
            }
        } catch (DatabaseException e) {
            // Blanket catch all DBExceptions and convert to IOExceptions.
            IOException e2 = new IOException(e.getMessage());
            e2.setStackTrace(e.getStackTrace()); //preserve original stacktrace
            throw e2; 
        }
    }
    
    /**
     * An internal method for adding URIs to the queue. 
     * 
     * @param curi The CrawlURI to add
     * @param overrideDuplicates If true then any existing CrawlURI in the DB
     *                           will be overwritten. If false insert into the
     *                           queue is only performed if the key doesn't 
     *                           already exist.
     * @return The OperationStatus object returned by the put method.
     * 
     * @throws DatabaseException
     */
    protected OperationStatus strictAdd(CrawlURI curi, 
                                        boolean overrideDuplicates)
            throws DatabaseException{
        DatabaseEntry keyEntry = new DatabaseEntry();
        DatabaseEntry dataEntry = new DatabaseEntry();
        
        StringBinding.stringToEntry(curi.getURIString(),keyEntry);
        crawlURIBinding.objectToEntry(curi,dataEntry);
        
        OperationStatus opStatus = null;
        if(overrideDuplicates){
            opStatus = primaryUriDB.put(null,keyEntry,dataEntry);
        } else {
            opStatus = primaryUriDB.putNoOverwrite(null,keyEntry,dataEntry);
        }
        
        return opStatus;
    }
    
    /**
     * Flush any CrawlURIs in the processingUriDB into the primaryUriDB. URIs
     * flushed will have their 'time of next fetch' maintained and the 
     * nextReadyTime will be updated if needed.
     * <p>
     * No change is made to the list of available slots. 
     * 
     * @throws DatabaseException if one occurs while flushing
     */
    protected void flushProcessingURIs() throws DatabaseException {
        Cursor processingCursor = processingUriDB.openCursor(null,null);
        DatabaseEntry keyEntry = new DatabaseEntry();
        DatabaseEntry dataEntry = new DatabaseEntry();
        
        while(true){
            OperationStatus opStatus = processingCursor.getFirst(
                    keyEntry, dataEntry, LockMode.DEFAULT);
            
            if(opStatus == OperationStatus.SUCCESS){
                // Got one!
                CrawlURI curi = 
                    (CrawlURI) crawlURIBinding.entryToObject(dataEntry);
                // Delete it from processingUriDB
                deleteInProcessing(curi.getURIString());
                // Add to processingUriDB;
                strictAdd(curi,false); // Ignore any duplicates. Go with the
                                       // ones already in the queue.
                // Update nextReadyTime if needed.
                long curiNextReadyTime = curi.getAList().getLong(
                        AdaptiveRevisitFrontier.A_TIME_OF_NEXT_PROCESSING);
                if(curiNextReadyTime<nextReadyTime){
                    nextReadyTime = curiNextReadyTime;
                }
            } else {
                // No more entries in processingUriDB
                processingCursor.close();
                return;
            }
        } 
    }
    
    /**
     * Count all entries in both primaryUriDB and processingUriDB.
     * <p>
     * This method is needed since BDB does not provide a simple way of counting
     * entries.
     * <p>
     * Note: This is an expensive operation, requires a loop through the entire
     * queue!
     * @return the number of distinct CrawlURIs in the HQ.
     * @throws DatabaseException
     */
    protected long countCrawlURIs() throws DatabaseException{
        long count = 0;
        
        DatabaseEntry keyEntry = new DatabaseEntry();
        DatabaseEntry dataEntry = new DatabaseEntry();        
        
        // Count URIs in the queue
        Cursor primaryCursor = primaryUriDB.openCursor(null,null);
        OperationStatus opStatus = primaryCursor.getFirst(keyEntry,
                                                            dataEntry,
                                                            LockMode.DEFAULT);
        while(opStatus == OperationStatus.SUCCESS){
            count++;
            opStatus = primaryCursor.getNext(keyEntry,
                                             dataEntry,
                                             LockMode.DEFAULT);
        }
        
        primaryCursor.close();

        // Now count URIs in the processingUriDB
        Cursor processingCursor = processingUriDB.openCursor(null,null);
        opStatus = processingCursor.getFirst(keyEntry,
                                             dataEntry,
                                             LockMode.DEFAULT);
        while(opStatus == OperationStatus.SUCCESS){
            count++;
            opStatus = processingCursor.getNext(keyEntry,
                                                dataEntry,
                                                LockMode.DEFAULT);
        }
        
        processingCursor.close();
        return count;
    }
    
    /**
     * Returns true if this HQ has a CrawlURI matching the uri string currently
     * being processed. False otherwise.
     * 
     * @param uri Uri to check
     * @return true if this HQ has a CrawlURI matching the uri string currently
     * being processed. False otherwise.
     * 
     * @throws DatabaseException
     */
    protected boolean inProcessing(String uri) throws DatabaseException{
        DatabaseEntry keyEntry = new DatabaseEntry();
        DatabaseEntry dataEntry = new DatabaseEntry();
                
        StringBinding.stringToEntry(uri,keyEntry);
        
        OperationStatus opStatus = processingUriDB.get(null,
                                                       keyEntry,
                                                       dataEntry,
                                                       LockMode.DEFAULT);
        
        if (opStatus == OperationStatus.SUCCESS){
            return true;
        }
        
        return false; //Not found
    }
    
    /**
     * Removes a URI from the list of URIs belonging to this HQ and are 
     * currently being processed.
     * <p>
     * Returns true if successful, false if the URI was not found.
     * 
     * @param uri The URI string of the CrawlURI to delete.
     * 
     * @throws DatabaseException
     * @throws IllegalStateException if the URI was not on the list
     */
    protected void deleteInProcessing(String uri) throws DatabaseException{
        DatabaseEntry keyEntry = new DatabaseEntry();
        
        StringBinding.stringToEntry(uri,keyEntry);
        
        OperationStatus opStatus = 
            processingUriDB.delete(null,keyEntry);
        
        if(opStatus != OperationStatus.SUCCESS){
            if(opStatus == OperationStatus.NOTFOUND){
                throw new IllegalStateException("Trying to deleta a " +
                        "non-existant URI from the list of URIs being " +
                        "processed. HQ: " + hostName + ", CrawlURI: " + 
                        uri);
            } else {
                throw new DatabaseException("Error occured deleting URI: " +
                        uri + " from HQ " + hostName + " list " +
                        "of URIs currently being processed. " + 
                        opStatus.toString());
            } 
        }
    }

    /**
     * Adds a CrawlURI to the list of CrawlURIs belonging to this HQ and are
     * being processed at the moment.
     * 
     * @param curi The CrawlURI to add to the list
     * @throws DatabaseException
     * @throws IllegalStateException if the CrawlURI is already in the list of
     *                               URIs being processed. 
     */
    protected void addInProcessing(CrawlURI curi) 
            throws DatabaseException, IllegalStateException{
        DatabaseEntry keyEntry = new DatabaseEntry();
        DatabaseEntry dataEntry = new DatabaseEntry();
        
        StringBinding.stringToEntry(curi.getURIString(),keyEntry);
        crawlURIBinding.objectToEntry(curi,dataEntry);
        
        OperationStatus opStatus = 
            processingUriDB.putNoOverwrite(null,keyEntry,dataEntry);
        
        if(opStatus != OperationStatus.SUCCESS){
            if(opStatus == OperationStatus.KEYEXIST){
                throw new IllegalStateException("Can not insert duplicate " +
                        "URI into list of URIs being processed. " +
                        "HQ: " + hostName + ", CrawlURI: " + 
                        curi.getURIString());
            } else {
                throw new DatabaseException("Error occured adding CrawlURI: " +
                        curi.getURIString() + " to HQ " + hostName + " list " +
                        "of URIs currently being processed. " + 
                        opStatus.toString());
            } 
        }
    }
    
    /**
     * Returns the CrawlURI associated with the specified URI (string) or null
     * if no such CrawlURI is queued in this HQ. If CrawlURI is being processed
     * it is not considered to be <i>queued</i> and this method will return null
     * for any such URIs.
     * 
     * @param uri A string representing the URI 
     * @return the CrawlURI associated with the specified URI (string) or null
     * if no such CrawlURI is queued in this HQ.
     * 
     * @throws DatabaseException if a errors occurs reading the database
     */
    protected CrawlURI getCrawlURI(String uri) throws DatabaseException{
        DatabaseEntry keyEntry = new DatabaseEntry();
        DatabaseEntry dataEntry = new DatabaseEntry();
        
        primaryKeyBinding.objectToEntry(uri,keyEntry);
        primaryUriDB.get(null,keyEntry,dataEntry,LockMode.DEFAULT);
        
        CrawlURI curi = (CrawlURI)crawlURIBinding.entryToObject(dataEntry);
        
        return curi;
    }
    
    /**
     * Update CrawlURI that has completed processing.
     * 
     * @param curi The CrawlURI. This must be a CrawlURI issued by this HQ's 
     *             {@link #next() next()} method.
     * @param needWait If true then the URI was processed successfully, 
     *                 requiring a period of suspended action on that host. If
     *                 valence is > 1 then seperate times are maintained for 
     *                 each slot.
     * @param wakeupTime If new state is 
     *                   {@link ARHostQueue#HQSTATE_SNOOZED snoozed}
     *                   then this parameter should contain the time (in 
     *                   milliseconds) when it will be safe to wake the HQ up
     *                   again. Otherwise this parameter will be ignored.
     * 
     * @throws IllegalStateException if the CrawlURI
     *         does not match a CrawlURI issued for crawling by this HQ's
     *         {@link ARHostQueue#next() next()}.
     * @throws IOException if an error occurs accessing the database
     */
    public void update(CrawlURI curi, boolean needWait, long wakeupTime) 
            throws IllegalStateException, IOException{
        try{
            // First add it to the regular queue. 
            OperationStatus opStatus = strictAdd(curi,false); 
            
            if(opStatus != OperationStatus.SUCCESS){
                if(opStatus == OperationStatus.KEYEXIST){
                    throw new IllegalStateException("Trying to update a CrawlURI" +
                            " failed because it was in the queue" +
                            " of URIs waiting for processing. URIs currently" +
                            " being processsed can never be in that queue." +
                            " HQ: " + hostName + ", CrawlURI: " + 
                            curi.getURIString());
                }
            }
            
            // Then remove from list of in processing URIs
            deleteInProcessing(curi.getURIString());
    
            // Check if we need to update nextReadyTime
            long curiTimeOfNextProcessing = curi.getAList().getLong(
                    AdaptiveRevisitFrontier.A_TIME_OF_NEXT_PROCESSING);
            if(nextReadyTime > curiTimeOfNextProcessing){
                nextReadyTime = curiTimeOfNextProcessing;
            }
            
            // Update the wakeUpTime slot.
            if(needWait==false){
                // Ok, no wait then. Set wake up time to 0.
                wakeupTime = 0;
            }
            updateWakeUpTimeSlot(wakeupTime);
        } catch (DatabaseException e) {
            // Blanket catch all DBExceptions and convert to IOExceptions.
            IOException e2 = new IOException(e.getMessage());
            e2.setStackTrace(e.getStackTrace()); //preserve original stacktrace
            throw e2; 
        }
    }

    /**
     * Returns the 'top' URI in the ARHostQueue. 
     * <p>
     * HQ state will be set to {@link ARHostQueue#HQSTATE_BUSY busy} if this 
     * method returns normally.
 
     * 
     * @return a CrawlURI ready for processing
     * 
     * @throws IllegalStateException if the HostQueues current state is not
     *         ready {@link ARHostQueue#HQSTATE_READY ready}
     * @throws IOException if an error occurs reading from the database
     */
    public CrawlURI next() throws IllegalStateException, IOException{
        try{
            // Ok, lets issue a URI, first check state and reserve slot.
            if(getState()!=HQSTATE_READY || useWakeUpTimeSlot()==false){
                throw new IllegalStateException("Can not issue next URI when " +
                        "HQ " + hostName + " state is " + getStateByName());
            }
    
            DatabaseEntry keyEntry = new DatabaseEntry();
            
            // Get the top URI
            CrawlURI curi = peek();
            
            // Add it to processingUriDB
            addInProcessing(curi);
            
            // Delete it from the primaryUriDB
            StringBinding.stringToEntry(curi.getURIString(),keyEntry);
            OperationStatus opStatus = primaryUriDB.delete(null,keyEntry);
            
            if(opStatus != OperationStatus.SUCCESS){
                throw new DatabaseException("Error occured removing URI: " +
                        curi.getURIString() + " from HQ " + hostName + 
                        " priority queue for processing. " + opStatus.toString());
            }
            
            // Finally update nextReadyTime with new top.
            nextReadyTime = peek().getAList().getLong(
                    AdaptiveRevisitFrontier.A_TIME_OF_NEXT_PROCESSING);
            return curi;
        } catch (DatabaseException e) {
            // Blanket catch all DBExceptions and convert to IOExceptions.
            IOException e2 = new IOException(e.getMessage());
            e2.setStackTrace(e.getStackTrace()); //preserve original stacktrace
            throw e2; 
        }
    }
    
    /**
     * Returns the URI with the earliest time of next processing. I.e. the URI
     * at the head of this host based priority queue.
     * <p>
     * Note: This method will return the head CrawlURI regardless of wether it 
     * is safe to start processing it or not. CrawlURI will remain in the queue.
     * The returned CrawlURI should only be used for queue inspection, it can 
     * <i>not</i> be updated and returned to the queue. To get URIs ready for
     * processing use {@link #next() next()}.
     * 
     * @return the URI with the earliest time of next processing
     * 
     * @throws IllegalStateException if the HQ is empty
     * @throws IOException if an error occurs reading from the database
     */
    public CrawlURI peek() throws IllegalStateException, IOException{
        try{
            
            DatabaseEntry keyEntry = new DatabaseEntry();
            DatabaseEntry dataEntry = new DatabaseEntry();
            
            CrawlURI curi = null;
            
            OperationStatus opStatus = 
                secondaryCursor.getFirst(keyEntry, dataEntry, LockMode.DEFAULT);
            
            if( opStatus == OperationStatus.SUCCESS){
                Long key = (Long) secondaryKeyBinding.entryToObject(keyEntry);
                curi = (CrawlURI)crawlURIBinding.entryToObject(dataEntry);
            } else {
                if( opStatus == OperationStatus.NOTFOUND ){
                    throw new IllegalStateException("Can not perform peek() on " +
                            "empty queue");
                } else {
                    throw new DatabaseException("Error occured in " +
                            "ARHostQueue.peek()." + opStatus.toString());
                }
            }
            return curi;
        } catch (DatabaseException e) {
            // Blanket catch all DBExceptions and convert to IOExceptions.
            IOException e2 = new IOException(e.getMessage());
            e2.setStackTrace(e.getStackTrace()); //preserve original stacktrace
            throw e2; 
        }
    }
    
    
    
    /**
     * Returns the current state of the HQ.
     * 
     * @return the current state of the HQ.
     * 
     * @see ARHostQueue#HQSTATE_BUSY
     * @see ARHostQueue#HQSTATE_EMPTY
     * @see ARHostQueue#HQSTATE_READY
     * @see ARHostQueue#HQSTATE_SNOOZED
     */
    public int getState(){
        if(state != HQSTATE_EMPTY){
            // Need to confirm state
            if(isBusy()){
                state = HQSTATE_BUSY;
            } else {             
                long currentTime = System.currentTimeMillis();
                long wakeTime = getEarliestWakeUpTimeSlot();
                
                if(wakeTime > currentTime || nextReadyTime > currentTime){
                    state = HQSTATE_SNOOZED;
                } else {
                    state = HQSTATE_READY;
                }
            }
        }
        return state;
    }
    
    /**
     * Returns the time when the HQ will next be ready to issue a URI. 
     * <p>
     * If the queue is in a {@link #HQSTATE_SNOOZED snoozed} state then this 
     * time will be in the future and reflects either the time when the HQ will
     * again be able to issue URIs for processing because politness constraints
     * have ended, or when a URI next becomes available for visit, whichever is
     * larger.
     * <p>
     * If the queue is in a {@link #HQSTATE_READY ready} state this time will
     * be in the past and reflect the earliest time when the HQ had a URI ready
     * for processing, taking time spent snoozed for politness concerns into
     * account. 
     * <p>
     * If the HQ is in any other state then the return value of this method is
     * equal to Long.MAX_VALUE.
     * <p>
     * This value may change each time a URI is added, issued or updated.
     * 
     * @return the time when the HQ will next be ready to issue a URI
     */
    public long getNextReadyTime(){
        if(getState()==HQSTATE_BUSY || getState()==HQSTATE_EMPTY){
            // Have no idea when HQ next be able issue a URI
            return Long.MAX_VALUE;
        }
        long wakeTime = getEarliestWakeUpTimeSlot();
        return nextReadyTime > wakeTime ? nextReadyTime : wakeTime;
    }
    


    /**
     * Same as {@link #getState() getState()} except this method returns a 
     * human readable name for the state instead of its constant integer value.
     * <p>
     * Should only be used for reports, error messages and other strings 
     * intended for human eyes.
     * 
     * @return the human readable name of the current state
     */
    public String getStateByName() {
        switch(getState()){
            case HQSTATE_BUSY : return "busy";
            case HQSTATE_EMPTY : return "empty";
            case HQSTATE_READY : return "ready";
            case HQSTATE_SNOOZED : return "snoozed";
        }
        // This should be impossible unless new states are added without 
        // updating this method.
        return "undefined"; 
    }
    
    /**
     * Returns the size of the HQ. That is, the number of URIs queued, 
     * including any that are currently being processed.
     * 
     * @return the size of the HQ.
     */   
    public long getSize() throws DatabaseException {
        return size;
    }
    
    /**
     * Compares the this HQ to the supplied one.
     * 
     * @param comp the HQ to compare to.
     * @return the value 0 if the argument HQ's time of next ready
     *         is equal to this HQ's; a value less than 0 if this 
     *         value is less than the argument HQ's; and a value 
     *         greater than 0 if this value is greater.
     */
    public int compareTo(ARHostQueue comp){
        long thisTime = getNextReadyTime();
        long compTime = comp.getNextReadyTime();
        if(thisTime>compTime){
            return 1;
        } else if(thisTime<compTime){
            return -1;
        } else {
            // Equal time. Use hostnames
            return hostName.compareTo(comp.getHostName());
        }
    }

    /**
     * Cleanup all open Berkeley Database objects.
     * <p>
     * Does <I>not</I> close the Environment.
     * 
     * @throws IOException if an error occurs closing a database object
     */
    public void close() throws IOException{
        try{
            secondaryCursor.close();
            secondaryUriDB.close();
            processingUriDB.close();
            classCatalogDB.close();
            primaryUriDB.close();
        } catch (DatabaseException e) {
            // Blanket catch all DBExceptions and convert to IOExceptions.
            IOException e2 = new IOException(e.getMessage());
            e2.setStackTrace(e.getStackTrace()); //preserve original stacktrace
            throw e2; 
        }
    }
    
    
    /**
     * If true then the HQ has no available slot for issuing URIs. 
     * <p>
     * I.e. number of in processing URIs = valence.
     * 
     * @return true if number of in processing URIs = valence 
     */
    private boolean isBusy(){
        for(int i=0 ; i < valence ; i++){
            if(wakeUpTime[i]>-1){
                return false;
            }
        }
        return true;
    }
    
    /**
     * Overwrites a used (-1) value in wakeUpTime[] with the supplied value.
     * @param newVal
     */
    private void updateWakeUpTimeSlot(long newVal){
        for(int i=0 ; i < valence ; i++){
            if(wakeUpTime[i]==-1){
                wakeUpTime[i]=newVal;
            }
        }
    }
    
    /**
     * A new URI is being issued. Set the wakeup time on an unused slot to -1.
     * 
     * @return true if a slot was successfully reserved. False otherwise.
     */
    private boolean useWakeUpTimeSlot(){
        for(int i=0 ; i < valence ; i++){
            if(wakeUpTime[i]>-1 && wakeUpTime[i]<=System.currentTimeMillis()){
                wakeUpTime[i]=-1;
                return true;
            }
        }
        return false;
    }
    
    /**
     * Returns the earliest time when a wake up slot will become available. If
     * one is already available then this time will be in the past.
     * <p>
     * If all slots are taken with URIs currently being processed (i.e. HQ state
     * is {@link #HQSTATE_BUSY busy} then this will return Long.MAX_VALUE;
     * @return the earliest time when a wake up slot will become available
     */
    private long getEarliestWakeUpTimeSlot(){
        long earliest = Long.MAX_VALUE;
        for(int i=0 ; i < valence ; i++){
            if(wakeUpTime[i]>-1 && wakeUpTime[i]<earliest){
                earliest = wakeUpTime[i];
            }
        }
        return earliest;
    }
    
    /**
     * Creates the secondary (time of next processing) key for the secondary
     * index. 
     */
    private static class MyKeyCreator implements SecondaryKeyCreator {

        private EntryBinding secKeyBinding;
        private EntryBinding dataBinding;

        MyKeyCreator(EntryBinding secKeyBinding, EntryBinding dataBinding) {

            this.secKeyBinding = secKeyBinding;
            this.dataBinding = dataBinding;
        }

        public boolean createSecondaryKey(SecondaryDatabase secondaryDb,
                                          DatabaseEntry keyEntry,
                                          DatabaseEntry dataEntry,
                                          DatabaseEntry resultEntry)
            throws DatabaseException {

            // Extract the time of next processing and encode it.
            CrawlURI curi = 
                (CrawlURI) dataBinding.entryToObject(dataEntry);
            long key = curi.getAList().getLong(
                    AdaptiveRevisitFrontier.A_TIME_OF_NEXT_PROCESSING);
            secKeyBinding.objectToEntry(new Long(key), resultEntry);
            return true;
        }
    }
}
