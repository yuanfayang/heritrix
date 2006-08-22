package org.archive.crawler.byexample.datastructure.invertedindex;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.archive.crawler.byexample.constants.OutputConstants;
import org.archive.crawler.byexample.utils.FileUtils;

/**
 * InvertedIndex interface In-Memory implementation.
 * All index data is stored in main memory 
 * as a Map during the crawl
 * 
 * @author Michael Bendersky
 *
 */
public class InMemoryIndex implements InvertedIndex {
        
    private Map<String,IndexRow> indexRowsHash; 
            
    public InMemoryIndex(){        
       indexRowsHash=new ConcurrentHashMap<String,IndexRow>();
    }
    
    public void addNewRow(String rowKey){
        indexRowsHash.put(rowKey,new IndexRow());
    }
    
    public void addRow(String rowKey, IndexRow row) {
        indexRowsHash.put(rowKey,row);
    }
    
    public void addEntry(String rowKey, IndexEntry rowEntry) {
        getRow(rowKey).addRowEntry(rowEntry);
    }
    
    public void increaseRowLatestEntryValue(String rowKey) {
        getRow(rowKey).increaseRowLatestEntryValue();
    }
    
    public String getRowLatestEntryId(String rowKey) {
        return getRow(rowKey).getLatestEntryId();
    }        
    
    public void normalizeRow(String rowKey) {
        getRow(rowKey).normalizeRow();
    }
        
    public IndexRow getRow(String rowKey){
        return indexRowsHash.get(rowKey);
    }
    
    public void removeRow(String rowKey){
        indexRowsHash.remove(rowKey);
    }     
    
   public void sortRow(String rowKey, Comparator<IndexEntry> comparator) {
       getRow(rowKey).sortRow(comparator);
    }
   
    public int getSize(){
        return indexRowsHash.size();
    }
       
    public boolean containsKey(String rowKey){
        return indexRowsHash.containsKey(rowKey);
    }
    
    public Iterator<String> getIndexKeysIterator(){
       return indexRowsHash.keySet().iterator();
    }

    public void closeIndex(String jobId, String filePath){        
        BufferedWriter out=FileUtils.createFileForJob(jobId,filePath,OutputConstants.TERMS_INDEX_FILENAME,true);        
        StringBuffer dump=new StringBuffer();
        String currKey;
        
        for (Iterator<String> iter = getIndexKeysIterator(); iter.hasNext();) {
            currKey=iter.next();
            dump.append(currKey);
            dump.append(OutputConstants.KEY_SEPARATOR);
            dump.append(getRow(currKey).toString());
            dump.append("\n");
        }
        FileUtils.dumpBufferToFile(out,dump);    
        FileUtils.closeFile(out);
    }

    public void openIndex(String filePath){     
        try {
            BufferedReader in=FileUtils.readBufferFromFile(filePath);
            String iter = in.readLine();
            String[] parts;
            
            //File is empty
            if (in==null) 
                return;
                
                while (!(iter==null)){         
                    parts=iter.split(OutputConstants.KEY_SEPARATOR);
                    //Add row for the keyword
                    addNewRow(parts[0]);
                    getRow(parts[0]).addEntriesFromString(parts[1],parts[2]);
                    iter=in.readLine();
                }        
                in.close();
        } catch (IOException e) {
            throw new RuntimeException("Failed to open index",e);
        }
    }
    
    public String toString(){
        return indexRowsHash.toString();
    }
} //END OF CLASS
