package org.archive.crawler.byexample.algorithms.datastructure.support;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.archive.crawler.byexample.algorithms.datastructure.itemset.ItemSet;

public class DocumentSupportIndex {
   private Map<String,TermSupportListing> myClusteringHash;
    
    public DocumentSupportIndex(){
        myClusteringHash=new ConcurrentHashMap<String,TermSupportListing>();
    }
        
    public void addIndexKey(String key){
        myClusteringHash.put(key,new TermSupportListing());
    }
    
    public void initializeKey(String key, List<TermSupport> terms){
        addIndexKey(key);
        myClusteringHash.get(key).insertInitialTerms(terms);
    }
    
    public void addIndexValue(String key, TermSupport value){
        if (!myClusteringHash.containsKey(key))
            addIndexKey(key);        
        myClusteringHash.get(key).addValueToRow(value);
    }
    
    public Iterator<String> getIndexKeysIterator(){
        return myClusteringHash.keySet().iterator();
    }
    
    public String toString(){
        return myClusteringHash.toString();
    }
    
    public TermSupportListing getRow(String key){
        return myClusteringHash.get(key);
    }
    
    public void removeRow(ItemSet key){
        myClusteringHash.remove(key);
    }
}
