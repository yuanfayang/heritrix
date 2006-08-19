package org.archive.crawler.byexample.datastructure.support;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.archive.crawler.byexample.constants.OutputConstants;
import org.archive.crawler.byexample.datastructure.itemset.ItemSet;
import org.archive.crawler.byexample.utils.FileUtils;

public class ClusterSupportIndex{

    private Map<ItemSet,TermSupportListing> myClusteringHash;
    
    public ClusterSupportIndex(){
        myClusteringHash=new ConcurrentHashMap<ItemSet,TermSupportListing>();
    }
    
    public void addIndexKey(ItemSet key){
        myClusteringHash.put(key,new TermSupportListing());
    }
    
    public void initializeKey(ItemSet key, List<TermSupport> terms){
        addIndexKey(key);
        myClusteringHash.get(key).insertInitialTerms(terms);
    }
    
    public void addIndexValue(ItemSet key, TermSupport value){
        if (!myClusteringHash.containsKey(key))
            addIndexKey(key);        
        myClusteringHash.get(key).addValueToRow(value);
    }
    
    public Iterator<ItemSet> getIndexKeysIterator(){
        return myClusteringHash.keySet().iterator();
    }
    
    public String toString(){
        return myClusteringHash.toString();
    }
    
    public TermSupportListing getRow(ItemSet key){
        return myClusteringHash.get(key);
    }
    
    public void removeRow(ItemSet key){
        if (myClusteringHash.containsKey(key))
            myClusteringHash.remove(key);
    }
    
    public void truncateAllRows(int percentage){
        ItemSet currIS=null;
        for (Iterator<ItemSet> iter = getIndexKeysIterator(); iter.hasNext();) {
            currIS=iter.next();
            getRow(currIS).truncateByPercentage(percentage);            
        }
    }
   
    public void clearIndex(){
        myClusteringHash.clear();
    }
    
    
    public void dumpIndexToFile(BufferedWriter bw){
        StringBuffer dump=new StringBuffer();
        ItemSet currKey;
        for (Iterator<ItemSet> iter = getIndexKeysIterator(); iter.hasNext();) {
            currKey=iter.next();
            dump.append(currKey);
            dump.append(OutputConstants.KEY_SEPARATOR);
            dump.append(getRow(currKey).toString());
            dump.append("\n");
        }
        FileUtils.dumpBufferToFile(bw,dump);        
    }    
    
    public void readIndexFromFile(String filePath){
        BufferedReader in=FileUtils.readBufferFromFile(filePath);
        try {
            String iter = in.readLine();
            String[] parts;
            ItemSet currSet=null;
            
            //File is empty
            if (in==null) 
                return;
            
            while (!(iter==null)){         
                parts=iter.split(OutputConstants.KEY_SEPARATOR);
                currSet=ItemSet.createfromString(parts[0]);
                //Add row for the keyword
                addIndexKey(currSet);
                addEntriesFromString(currSet,parts[1]);
                iter=in.readLine();
            }        
            in.close();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file at path "+filePath,e);
        }
    }
    
    public void addEntriesFromString(ItemSet key, String valuesString){
        String[] arrayValues;
        String[] entryValues;
        //Remove "[]"
        valuesString=valuesString.substring(1,valuesString.length()-1);
        //Split to entries            
        if (valuesString.length()>0) {
            arrayValues=valuesString.split(", ");
            for (int i = 0; i < arrayValues.length; i++) {
                entryValues=arrayValues[i].split(OutputConstants.ENTRY_SEPARATOR);
                this.addIndexValue(key,new TermSupport(entryValues[0],Double.parseDouble(entryValues[1])));                
            }
        }
    }
}
