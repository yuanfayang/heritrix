package org.archive.crawler.byexample.algorithms.clustering.apriori;

import java.util.ArrayList;
import java.util.Iterator;

import org.archive.crawler.byexample.algorithms.datastructure.FrequentItemSets;
import org.archive.crawler.byexample.algorithms.datastructure.InvertedIndex;
import org.archive.crawler.byexample.algorithms.datastructure.ItemSet;
import org.archive.crawler.byexample.algorithms.datastructure.TermSupport;
import org.archive.crawler.byexample.algorithms.datastructure.InvertedIndex.IndexEntry;
import org.archive.crawler.byexample.algorithms.datastructure.InvertedIndex.IndexRow;

public class AprioriItemSetComputation {
    
    private FrequentItemSets myFrequentItemSets;
    private InvertedIndex myTermsIndex;
    private long myDocCount;
    private ArrayList<TermSupport> frequentItemSupport;
    
    public static int MIN_SUPPORT=50;
    public static int MAX_DEPTH=3;
    
    public AprioriItemSetComputation(long docCount, InvertedIndex index){
        myFrequentItemSets=new FrequentItemSets();
        myDocCount=docCount;
        myTermsIndex=index;
        frequentItemSupport=new ArrayList<TermSupport>();
        
        //Find all 1 frequent terms
        find1FrequentItemSets(); 
        //Truncate terms index to include only 1-frequent terms
        buildFrequentItemSetsIndex();
    }
    
    public int calculateSupport(int docsIN){
        return (int)((double)docsIN/myDocCount*100);
        
    }
    
    public int findMinSupport(ItemSet kSet){
        ItemSet temp=new ItemSet(kSet);
        
        String[] items=temp.getItems();
        
        if (temp.getSize()==1){
            return calculateSupport(myTermsIndex.getRow(items[0]).getRowSize());
        }
        
        IndexRow merger=null;
        int i=0;
        
        while(i<items.length){
            merger=mergeRows(merger,myTermsIndex.getRow(items[i]));
            //No matching documents were found
            if (merger.getRowSize()==0)
                return 0;
            temp.removeFromSet(items[i]);
            i++;
        }      
        return calculateSupport(merger.getRowSize());
    }
    
    public boolean isMinSupported(ItemSet kSet){
        if (findMinSupport(kSet)>MIN_SUPPORT)
            return true;
        return false;            
    }
    
    public IndexRow mergeRows(IndexRow r1, IndexRow r2){
        
        //If one of the rows is null return the second one as merge between rows
        if (r1==null)
            return r2;
        
        if (r2==null)
            return r1;
        
        Iterator<IndexEntry>iter1=r1.getRowIterator();
        Iterator<IndexEntry>iter2=r2.getRowIterator();
        IndexRow mergedRow=myTermsIndex.new IndexRow();
        String d1,d2;    
        d1=iter1.next().getEntryId();        
        d2=iter2.next().getEntryId();        
        while (iter1.hasNext() && iter2.hasNext()){            
            if(d1.compareTo(d2)>0){
                d2=iter2.next().getEntryId();
                continue;
            }
            if (d1.compareTo(d2)<0){
                d1=iter1.next().getEntryId();
                continue;
            }            
            mergedRow.addRowEntry(d1,1);
            d1=iter1.next().getEntryId();
            d2=iter2.next().getEntryId();
        }
        return mergedRow;
    }
    
    public void generateAll(FrequentItemSets prevSets, int counter){
        
        if (prevSets.getSize()==0 || counter==MAX_DEPTH) 
            return;
        
        generateAll(selfJoin(prevSets),++counter);
    }    
      
    public FrequentItemSets selfJoin(FrequentItemSets kSets){
        ItemSet temp1, temp2;
        String nonMatch;
        FrequentItemSets candidates=new FrequentItemSets();
        for (Iterator<ItemSet> iter1 = kSets.getSetsIterator(); iter1.hasNext();) {
            temp1=new ItemSet(iter1.next());
            for (Iterator<ItemSet> iter2 = kSets.getSetsIterator(); iter2.hasNext();) {
                temp2=new ItemSet(iter2.next());
                if (!temp1.equals(temp2)){
                    nonMatch=expandRule(temp1,temp2);
                    if (nonMatch!=null){                        
                        candidates.insertToSet(mergeRule(temp1,nonMatch));
                    }                        
                }
            }    
        }
        candidates=pruneRule(candidates, kSets);        
        //Test if candidates have MIN_SUPPORT
        ItemSet candidate;
        FrequentItemSets candidatesToRemove=new FrequentItemSets();
        for (Iterator<ItemSet> iter=candidates.getSetsIterator();iter.hasNext();){
            candidate=iter.next();
            if (isMinSupported(candidate))
                myFrequentItemSets.insertToSet(candidate);
            else
                candidatesToRemove.insertToSet(candidate);
        }
        //Remove the candidates with no MIN_SUPPORT
        candidates.removeFromSet(candidatesToRemove);            
        return candidates;
    }
    
    public ItemSet mergeRule(ItemSet is1, String toAdd){
        ItemSet mergeK=new ItemSet(is1);
        mergeK.insertToSet(toAdd);
        return mergeK;
    }
    
    //Return the non-matching item
    public String expandRule(ItemSet is1, ItemSet is2){
        String[] s2=is2.getItems();
        int diffs=0;
        String item=null;
        for (int j = 0; j < s2.length; j++) {        
            if (!is1.contains(s2[j])){
                diffs++;
                item=s2[j];
            }
        }
        //Return non-matching item if ItemSets differ in only one item
        //Otherwise return -1
        if (diffs!=1)
            return null;
        return item;
    }
    
    public FrequentItemSets pruneRule(FrequentItemSets candidates, FrequentItemSets kSets){
        ItemSet temp1, temp2;
        String[] tempItems;
        int i;
        for (Iterator<ItemSet> iter1 = kSets.getSetsIterator(); iter1.hasNext();) {
            temp1=iter1.next();
            tempItems=temp1.getItems();
            for (i=0; i<tempItems.length; i++){
                temp2=new ItemSet(temp1);
                temp2.removeFromSet(tempItems[i]);
                if (!kSets.contains(temp2)){
                    candidates.removeFromSet(temp1);
                    break;
                }                
            }
        } 
        return candidates;
    }
    
    public void find1FrequentItemSets(){        
        String term;        
        ItemSet candidateSet; 
        double candidateSupport;
        //Add 1-Frequent ItemSets to the frequent sets
        for (Iterator<String> iter = myTermsIndex.getIndexKeysIterator(); iter.hasNext();) {
            term=iter.next();
            candidateSet=new ItemSet(term);
            candidateSupport=findMinSupport(candidateSet);
            if (candidateSupport>MIN_SUPPORT){
                myFrequentItemSets.insertToSet(candidateSet);
                frequentItemSupport.add(new TermSupport(term,candidateSupport));
            }
        }
        
    }
    
    /**
     * Removes from inverted index all the terms that are not 1-frequent
     *
     */
    public void buildFrequentItemSetsIndex(){
        String term;
        for (Iterator<String> iter  = myTermsIndex.getIndexKeysIterator(); iter.hasNext();) {
            term =iter.next();
            if (!myFrequentItemSets.contains(new ItemSet(term))){
                myTermsIndex.removeRow(term);
            }
        }
    }
    
    public InvertedIndex getFrequentItemSetsIndex(){
        return myTermsIndex;
    }
    
    public ArrayList<TermSupport> getFrequentItemSupport(){
        return frequentItemSupport;
    }
    
    public FrequentItemSets findKFrequentItemSets(){
        generateAll(myFrequentItemSets, 1);                      
        return myFrequentItemSets;        
    }
}
