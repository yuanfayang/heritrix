package org.archive.crawler.byexample.algorithms.datastructure;

import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.archive.crawler.byexample.constants.OutputConstants;
import org.archive.crawler.byexample.utils.FileHandler;

public class ClusteringSupportIndex{
   
    public class SupportRow{
        private ArrayList<TermSupport> termList;
        
        public SupportRow(){
            termList=new ArrayList<TermSupport>();
        }
        
        public void addValueToRow(TermSupport value){
            termList.add(value);
        }
        
        public String toString(){
            return termList.toString();
        }
        
        public Iterator<TermSupport> getTermsIterator(){
            return termList.iterator();
        }
        
        public TermSupport getEntry(String term){
            for (TermSupport se : termList) {
                if (se.getTerm().equals(term))
                    return se;                    
            }
            return null;
        }
    }
    
    
    private Map<ItemSet,SupportRow> myClusteringHash;
    
    public ClusteringSupportIndex(){
        myClusteringHash=new ConcurrentHashMap<ItemSet,SupportRow>();
    }
    
    public void addIndexKey(ItemSet key){
        myClusteringHash.put(key,new SupportRow());
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
    
    public SupportRow getRow(ItemSet key){
        return myClusteringHash.get(key);
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
        FileHandler.dumpBufferToFile(bw,dump);        
    }    

}
