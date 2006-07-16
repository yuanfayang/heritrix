package org.archive.crawler.byexample.algorithms.datastructure.documents;

import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.archive.crawler.byexample.algorithms.datastructure.itemset.FrequentItemSets;
import org.archive.crawler.byexample.algorithms.datastructure.itemset.ItemSet;
import org.archive.crawler.byexample.constants.OutputConstants;
import org.archive.crawler.byexample.utils.FileUtils;

public class ClusterDocumentsIndex {
    
   
          
    private Map<ItemSet,IdListing> myClusteringHash;
    
    public ClusterDocumentsIndex(){
        myClusteringHash=new ConcurrentHashMap<ItemSet,IdListing>();
    }
    
    public void addIndexKey(ItemSet key){
        myClusteringHash.put(key,new IdListing());
    }
    
    public void addIndexValue(ItemSet key, String value){
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
    
    public IdListing getRow(ItemSet key){
        return myClusteringHash.get(key);
    }
    
    public FrequentItemSets removeEmptyRows(){
        FrequentItemSets removedIS=new FrequentItemSets();
        ItemSet currIS;        
        for (Iterator<ItemSet> iter = getIndexKeysIterator(); iter.hasNext();) {
            currIS=iter.next();
            if (getRow(currIS).getRowSize()==0){
                myClusteringHash.remove(currIS);
                removedIS.insertToSet(currIS);
            }
        }
        return removedIS;
    }
    
    
    public void dumpIndexToFile(BufferedWriter bw)throws Exception{
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
}
