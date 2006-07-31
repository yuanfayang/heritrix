package org.archive.crawler.byexample.datastructure.invertedindex;

import java.io.BufferedWriter;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import org.archive.crawler.byexample.constants.OutputConstants;
import org.archive.crawler.byexample.utils.FileUtils;
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
 * InvertedIndex interface implementation using Berkeley DB
 * 
 * @author Michael Bendersky
 *
 */
public class BdbIndex implements InvertedIndex {
    
    private Environment myEnv;
    private Database indexDb;
    private int numOfRecords;
    private Map<String, CachedIndexEntry> indexCache; 
    private static Logger logger =
        Logger.getLogger(BdbIndex.class.getName());
    
    public BdbIndex(String envHome) throws Exception{
        openIndex(envHome);
        indexCache=new ConcurrentHashMap<String,CachedIndexEntry>();
        loadIndexCache();
        numOfRecords=indexCache.size();        
    }
    
    public void openIndex(String envHome)throws Exception {
        boolean readOnly=false;
        //     Instantiate an environment and database configuration object
        EnvironmentConfig myEnvConfig = new EnvironmentConfig();
        DatabaseConfig myDbConfig = new DatabaseConfig();
        //     Configure the environment and databases for the read-only
        //     state as identified by the readOnly parameter on this
        //     method call.
        myEnvConfig.setReadOnly(readOnly);
        myDbConfig.setReadOnly(readOnly);
        //     If the environment is opened for write, then we want to be
        //     able to create the environment and databases if
        //     they do not exist.
        myEnvConfig.setAllowCreate(!readOnly);
        myDbConfig.setAllowCreate(!readOnly);
        //     Instantiate the Environment. This opens it and also possibly
        //     creates it.
        myEnv = new Environment(new File(envHome), myEnvConfig);
        //      Create and open index DB 
        indexDb = myEnv.openDatabase(null,OutputConstants.TERMS_INDEX_FILENAME,myDbConfig);
    }
    
    public void moveCacheToDb(String rowKey){
        DatabaseEntry entryToAdd=new DatabaseEntry();
        DatabaseEntry dbKey=null;
        try {
            dbKey = new DatabaseEntry(rowKey.getBytes(OutputConstants.DEFAULT_ENCODING));
        } catch (UnsupportedEncodingException e) {
            logger.severe("Invalid encoding: "+e.getMessage());
        }
        DatabaseEntry dbEntry = new DatabaseEntry();        
        //byte[] byteArray=null; 
        
        IndexEntry ie=indexCache.get(rowKey).getCachedEntry();
        //ie.objectToEntry(ie,entryToAdd);
        
        try {            
            if (indexDb.get(null, dbKey, dbEntry, LockMode.DEFAULT) == OperationStatus.SUCCESS){
                //Write cache to database
                IndexRow ir=new IndexRow();
                ir=(IndexRow)ir.entryToObject(dbEntry);
                ir.addRowEntry(ie);
                ir.setTotalValue(indexCache.get(rowKey).getCachedRowValue());
                ir.objectToEntry(ir,entryToAdd);
                indexDb.put(null,dbKey,entryToAdd);
                //byteArray= new byte[dbEntry.getSize()+entryToAdd.getSize()];
                //System.arraycopy(dbEntry.getData(),0,byteArray,0,dbEntry.getSize());
                //System.arraycopy(entryToAdd.getData(),0,byteArray,dbEntry.getSize(),entryToAdd.getSize());
                //dbEntry.setData(byteArray);
                //indexDb.put(null,dbKey,dbEntry);
            }
        } catch (DatabaseException e) {
            logger.severe("Problems accessing db: "+e.getMessage());
        }                     
    }
    
    public void addEntry(String rowKey, IndexEntry rowEntry){
        CachedIndexEntry cie=indexCache.get(rowKey);        
        //Cached entry exists
        if (cie.getCachedEntry()!=null){
            //Added Entry Id differs from cached Entry Id
            //Write cached IndexEntry to database and add new IndexEntry to cache
            if (cie.getCachedEntry().getEntryId()!=rowEntry.getEntryId()){                
                //Write cache to database
                moveCacheToDb(rowKey);
                //Write new entry to cache. Increase total row value by 1
                indexCache.put(rowKey,new CachedIndexEntry(rowEntry,cie.getCachedRowValue()+1));
            }              
        }
        //No cache. Write the new IndexEntry to cache
        else
            indexCache.put(rowKey,new CachedIndexEntry(rowEntry,1));
    }

    public void increaseRowLatestEntryValue(String rowKey) {
        indexCache.get(rowKey).getCachedEntry().increaseEntryValue();
        indexCache.get(rowKey).increaseCachedRowValue();
    }
    
    public String getRowLatestEntryId(String rowKey) {
        IndexEntry latestEntry=indexCache.get(rowKey).getCachedEntry();
        if (latestEntry==null)
            return String.valueOf(-1);
        else
            return latestEntry.getEntryId();
    }
    
    public void sortRow(String rowKey, Comparator<IndexEntry> comparator) {
        IndexRow rowToSort=getRow(rowKey);
        DatabaseEntry dbKey=null;
        try {
            dbKey = new DatabaseEntry(rowKey.getBytes(OutputConstants.DEFAULT_ENCODING));
        } catch (UnsupportedEncodingException e) {
            logger.severe("Invalid encoding: "+e.getMessage());
        }
        DatabaseEntry de=new DatabaseEntry();
        
        rowToSort.sortRow(comparator);
        
        rowToSort.objectToEntry(rowToSort,de);
        try {
            indexDb.put(null,dbKey,de);
        } catch (DatabaseException e) {
            logger.severe("Problems accessing db: "+e.getMessage());
        }
    }
    
    public void normalizeRow(String rowKey) {
        IndexRow rowToNorm=getRow(rowKey);
        int normValue=indexCache.get(rowKey).getCachedRowValue();
        for (Iterator<IndexEntry> iter = rowToNorm.getRowIterator(); iter.hasNext();) {              
            iter.next().scaleEntryValue(normValue);
        }
    }
    
    public void loadIndexCache() throws Exception{
        Cursor cursor=indexDb.openCursor(null,null);
        DatabaseEntry foundKey = new DatabaseEntry();
        DatabaseEntry foundData = new DatabaseEntry();
        IndexRow ir=new IndexRow();
        String keyString;
        while (cursor.getNext(foundKey, foundData, LockMode.DEFAULT) == OperationStatus.SUCCESS){
            keyString = new String(foundKey.getData(), OutputConstants.DEFAULT_ENCODING);
            ir=(IndexRow)ir.entryToObject(foundData);
            indexCache.put(keyString,new CachedIndexEntry(null,ir.getTotalValue()));                        
        }     
         cursor.close();
    }
    
    public void closeIndex(String jobId, String envHome) throws Exception{        
        BufferedWriter out=FileUtils.createFileForJob(jobId,envHome,OutputConstants.TERMS_INDEX_FILENAME,true);        
        StringBuffer dump=new StringBuffer();

        //Write all cache to db
        String currKey;        
        for (Iterator<String> iter = indexCache.keySet().iterator(); iter.hasNext();) {
            currKey=iter.next();
            moveCacheToDb(currKey);            
            //Write term index to file            
            dump.append(currKey);
            dump.append(OutputConstants.KEY_SEPARATOR);
            dump.append(getRow(currKey).toString());
            dump.append("\n");            
        }
        if (myEnv != null) {
            indexDb.close();        
            myEnv.close();        
        }
        FileUtils.dumpBufferToFile(out,dump);    
        FileUtils.closeFile(out);
    }
   
    public Database getIndexDb() {
        return indexDb;
    }

    public Environment getMyEnv() {
        return myEnv;
    }

    public void addNewRow (String rowKey){
        try {
            //Create database key
            DatabaseEntry dbKey = new DatabaseEntry(rowKey.getBytes(OutputConstants.DEFAULT_ENCODING));
            //Create database entry
            DatabaseEntry dbEntry=new DatabaseEntry();
            IndexRow rowToPut=new IndexRow();        
            rowToPut.objectToEntry(rowToPut,dbEntry);
            //Write to database
            indexDb.put(null,dbKey,dbEntry);
            //Increase number of database records
            numOfRecords++;
            //Update key set
            indexCache.put(rowKey,new CachedIndexEntry());
        } catch (Exception e) {
            logger.severe("Problems in adding index row: "+e.getMessage());
        }
    }
    
    public void addRow(String rowKey, IndexRow row) {
        try {
            //Create database key
            DatabaseEntry dbKey = new DatabaseEntry(rowKey.getBytes(OutputConstants.DEFAULT_ENCODING));
            //Create database entry
            DatabaseEntry dbEntry=new DatabaseEntry();       
            row.objectToEntry(row,dbEntry);
            //Write to database
            indexDb.put(null,dbKey,dbEntry);
            //Increase number of database records
            numOfRecords++;
            //Update key set
            indexCache.put(rowKey,new CachedIndexEntry(row.getIndex(row.getRowSize()),row.getTotalValue()));
        } catch (Exception e) {
            logger.severe("Problems in adding index row: "+e.getMessage());
        }
    }
    
    public IndexRow getRow(String rowKey) {
        //Get row for key
        try {
            DatabaseEntry dbKey = new DatabaseEntry(rowKey.getBytes(OutputConstants.DEFAULT_ENCODING));
            DatabaseEntry dbEntry = new DatabaseEntry();
            IndexRow rowToReturn=new IndexRow();
            
            if (indexDb.get(null, dbKey, dbEntry, LockMode.DEFAULT) == OperationStatus.SUCCESS){
                return (IndexRow)rowToReturn.entryToObject(dbEntry);
            }
        } catch (Exception e) {
            logger.severe("Problems in getting index row: "+e.getMessage());
        }
        // No row found
        return null;
    }

    public void removeRow(String rowKey) {        
        try {
            DatabaseEntry dbKey = new DatabaseEntry(rowKey.getBytes(OutputConstants.DEFAULT_ENCODING));
            indexDb.delete(null,dbKey);
            //Update key set
            indexCache.remove(rowKey);
        } catch (Exception e){
            logger.severe("Problems in deleting index row: "+e.getMessage());
        }        
    }

    public int getSize(){
        return numOfRecords;
    }

    public boolean containsKey(String rowKey) {
        return indexCache.containsKey(rowKey);
    }

    public Iterator<String> getIndexKeysIterator() {
        return indexCache.keySet().iterator();
    }
        
}
