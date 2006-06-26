package org.archive.crawler.byexample.algorithms.datastructure;

import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.archive.crawler.byexample.utils.FileHandler;

public class ClusteringDocumentIndex {
    
    public class DocumentRow{
        private ArrayList<String> idList;
        
        public DocumentRow(){
            idList=new ArrayList<String>();
        }
        
        public void addValueToRow(String value){
            idList.add(value);
        }
        
        public void removeValueFromRow(String value){
            idList.remove(value);
        }
        
        public String[] toArray(){
            String[] s=new String[idList.size()];
            return idList.toArray(s);
        }
        
        public String toString(){
            return idList.toString();
        }
    }
    
    public static final String KEY_SEPARATOR="~";
    
    private Map<ItemSet,DocumentRow> myClusteringHash;
    
    public ClusteringDocumentIndex(){
        myClusteringHash=new ConcurrentHashMap<ItemSet,DocumentRow>();
    }
    
    public void addIndexKey(ItemSet key){
        myClusteringHash.put(key,new DocumentRow());
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
    
    public DocumentRow getRow(ItemSet key){
        return myClusteringHash.get(key);
    }
    
    public void dumpIndexToFile(BufferedWriter bw)throws Exception{
        StringBuffer dump=new StringBuffer();
        ItemSet currKey;
        for (Iterator<ItemSet> iter = getIndexKeysIterator(); iter.hasNext();) {
            currKey=iter.next();
            dump.append(currKey);
            dump.append(KEY_SEPARATOR);
            dump.append(getRow(currKey).toString());
            dump.append("\n");
        }
        FileHandler.dumpBufferToFile(bw,dump);        
    }    
}
