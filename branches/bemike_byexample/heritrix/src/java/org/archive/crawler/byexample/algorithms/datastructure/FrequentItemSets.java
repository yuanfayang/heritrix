package org.archive.crawler.byexample.algorithms.datastructure;

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
    
    public String toString(){
        if (myFrequentItemSets.size()==0)
            return "[]";
        return myFrequentItemSets.toString();
    }

}
