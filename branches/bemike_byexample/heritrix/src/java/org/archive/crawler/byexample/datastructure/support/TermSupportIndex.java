package org.archive.crawler.byexample.datastructure.support;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.archive.crawler.byexample.datastructure.itemset.ItemSet;

public class TermSupportIndex {
               
        private Map<String,ClusterScoreListing> myTermScoresHash;
        
        public TermSupportIndex(){
            myTermScoresHash=new ConcurrentHashMap<String,ClusterScoreListing>();
        }
            
        public void addIndexKey(String key){
            myTermScoresHash.put(key,new ClusterScoreListing());
        }
                
        public void fromCSI(ClusterSupportIndex csi){
            ItemSet currIS;
            String currTerm;
            TermSupportListing rowIter;
            for (Iterator<ItemSet> iter = csi.getIndexKeysIterator(); iter.hasNext();) {
                currIS=iter.next();
                rowIter=csi.getRow(currIS);
                for (int i = 0; i < rowIter.getSize(); i++) {
                    currTerm=rowIter.getEntryAtIndex(i).getTerm();
                    // Add term to hash and add cluster score to terms
                    addIndexValue(currTerm,new ClusterScore(currIS,rowIter.getEntryAtIndex(i).getSupport()));
                }                
            }
        }
        
        public void addIndexValue(String key, ClusterScore value){
            if (!myTermScoresHash.containsKey(key))
                addIndexKey(key);        
            myTermScoresHash.get(key).addValueToRow(value);
        }
        
        public Iterator<String> getIndexKeysIterator(){
            return myTermScoresHash.keySet().iterator();
        }
        
        public String toString(){
            return myTermScoresHash.toString();
        }
        
        public ClusterScoreListing getRow(String key){
            if (myTermScoresHash.containsKey(key))
                return myTermScoresHash.get(key);
            else
                return null;
        }
        
        public void removeRow(ItemSet key){
            myTermScoresHash.remove(key);
        }

} //END OF CLASS
