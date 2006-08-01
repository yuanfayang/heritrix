package org.archive.crawler.byexample.datastructure.itemset;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class FrequentItemSets{

    private Set<ItemSet> myFrequentItemSets;
    
    public FrequentItemSets(){
        myFrequentItemSets=new HashSet<ItemSet>();
    }
    
    public int getSize(){
        return myFrequentItemSets.size();
    }
    
    public void insertToSet(ItemSet is){
        //Don't add ItemSet more than once
        if (!contains(is))
            myFrequentItemSets.add(is);
    }
   
    
    public void insertToSet(FrequentItemSets is){
        myFrequentItemSets.addAll(is.myFrequentItemSets);
    }
    
    public void removeFromSet(ItemSet is){
        myFrequentItemSets.remove(is);
    }
    
    public void removeFromSet(FrequentItemSets is){
        myFrequentItemSets.removeAll(is.myFrequentItemSets);
    }
    
    public boolean contains(ItemSet is){
       for (ItemSet myIS : myFrequentItemSets)
           if(myIS.equals(is))
               return true;
       return false;
    }
    
    public Iterator<ItemSet> getSetsIterator(){
        return myFrequentItemSets.iterator();
    }
    
    /**
     * Splits frequent items set to levels according to k 
     * (1st level - 1-frequent item sets, 2nd level - 2-frequent item sets, etc...)
     * <p>
     * To retrieve all k-Frequent ItemSets correctly, getKFrequentIS should be used
     * @param k number of levels to build
     * @return FrequentItemSets[]
     */
    public FrequentItemSets[] splitLevelsByK(int k){
        ItemSet currIS=null;
        FrequentItemSets[] itemSetLevels=new FrequentItemSets[k];
        
        for (int i = 0; i < itemSetLevels.length; i++) {
            itemSetLevels[i]=new FrequentItemSets();
        }
        
        for (Iterator<ItemSet> iter =myFrequentItemSets.iterator(); iter.hasNext();) {
            currIS=iter.next();
            itemSetLevels[currIS.getSize()-1].insertToSet(currIS);            
        }
        return itemSetLevels;
    }
    
    /**
     * Returns all k-FrequentItemSets according to supplied k
     * @param fis frequent itemsets split by k (as returned by splitLevelsByK)
     * @param k indicates the size of itemsets to return
     * @return FrequentItemSets containing all frequent item-sets of size k
     */
    public static FrequentItemSets getKFrequentItemSets(FrequentItemSets[] fis, int k){
        return fis[k-1];
    }
    
    
    public String toString(){
        if (myFrequentItemSets.size()==0)
            return "[]";
        return myFrequentItemSets.toString();
    }

}
