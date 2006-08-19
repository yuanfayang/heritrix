package org.archive.crawler.byexample.algorithms.clustering.apriori;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.archive.crawler.byexample.constants.ByExampleProperties;
import org.archive.crawler.byexample.datastructure.invertedindex.InMemoryIndex;
import org.archive.crawler.byexample.datastructure.invertedindex.IndexEntry;
import org.archive.crawler.byexample.datastructure.invertedindex.IndexRow;
import org.archive.crawler.byexample.datastructure.invertedindex.InvertedIndex;
import org.archive.crawler.byexample.datastructure.itemset.FrequentItemSets;
import org.archive.crawler.byexample.datastructure.itemset.ItemSet;
import org.archive.crawler.byexample.datastructure.support.TermSupport;

/**
 * Creates k-frequent Item Sets for the crawled documents according to APRIORI algorithm
 * k-frequent item set is a collection of k terms which appears in at least MIN_SUPPORT  
 * per cent of the documents. 
 * @see org.archive.crawler.byexample.constants.ByExampleProperties
 * 
 * @author Michael Bendersky
 *
 */
public class AprioriItemSetComputation {
    
    private FrequentItemSets myFrequentItemSets;
    private InvertedIndex myTermsIndex;
    private long myDocCount;
    private List<TermSupport> frequentItemSupport;

    /**
     * Default constructor.
     * Builds 1-frequent item sets according to terms index.
     * @param docCount number of documents in the collection
     * @param index Inverted Index of terms
     */
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
    
    /**
     * Calculates support 
     * 
     * @param docsIN number of docs containing item set
     * @return (docsIN/allDocs)*100
     */
    public int calculateSupport(int docsIN){
        return (int)((double)docsIN/myDocCount*100);        
    }
    
    /**
     * Calculates support for a given Item Set
     * 
     * @param kSet k-items set
     * @return kSet support
     */
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
    
    
    /**
     * Indicates whether k-item set has minimum support in the collection
     * @param kSet k-items set
     * @return TRUE - item set is supported, FALSE - else
     */
    public boolean isMinSupported(ItemSet kSet){
        if (findMinSupport(kSet)>ByExampleProperties.MIN_GLOBAL_SUPPORT)
            return true;
        return false;            
    }
    
    /**
     * Merges two given IndexRows into one.
     * This method is used for support calculation
     * @param r1 IndexRow
     * @param r2 IndexRow
     * @return merge of r1 and r2
     */
    public IndexRow mergeRows(IndexRow r1, IndexRow r2){
        
        //If one of the rows is null return the second one as merge between rows
        if (r1==null)
            return r2;
        
        if (r2==null)
            return r1;
        
        Iterator<IndexEntry>iter1=r1.getRowIterator();
        Iterator<IndexEntry>iter2=r2.getRowIterator();
        IndexRow mergedRow=new IndexRow();
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
            mergedRow.addRowEntry(new IndexEntry(d1,1));
            d1=iter1.next().getEntryId();
            d2=iter2.next().getEntryId();
        }
        return mergedRow;
    }
    
    /**
     * Recursive method, which generates k-frequent item sets.
     * Stops when: either no more k-frequent item sets to generate or MAX_DEPTH is reached
     * 
     * @see org.archive.crawler.byexample.constants.ByExampleProperties
     * 
     * @param prevSets previous k-frequent items
     * @param counter recursion depth
     */
    public void generateAll(FrequentItemSets prevSets, int counter){

        if (prevSets.getSize()==0 || counter==ByExampleProperties.MAX_DEPTH) 
            return;
        
        generateAll(selfJoin(prevSets),++counter);
    }    
    
    /**
     * This method is used to generate k-frequent item sets by generateAll method
     * 
     * @param kSets
     * @return FrequentItemSets
     */
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
    
    /**
     * Merges given string into Item Set
     * @param is1 ItemSet to expand
     * @param toAdd string to merge
     * @return expanded ItemSet
     */
    public ItemSet mergeRule(ItemSet is1, String toAdd){
        ItemSet mergeK=new ItemSet(is1);
        mergeK.insertToSet(toAdd);
        return mergeK;
    }
    
    /**
     * Returns non-matching item if ItemSets differ in only one item. 
     * Otherwise returns null
     * @param is1 ItemSet
     * @param is2 ItemSet
     * @return non-matching item
     */
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
    
    /**
     * Removes candidates for (k+1)-frequent item sets, 
     * if they differ in more than 1 item from items in k-frequent item sets
     * @param candidates candidate sets to add to k-frequent item sets
     * @param kSets k-frequent item sets
     * @return FrequentItemSets
     */
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
    
    /**
     * Creates 1-frequent item sets and sorts them by support.
     * If number of 1-frequent item sets exceeds MAX_1_FREQUENT_TERMS, only top MAX_1_FREQUENT_TERMS will be returned
     * 
     * @see org.archive.crawler.byexample.constants.ByExampleProperties
     */
    public void find1FrequentItemSets(){        
        String term;        
        ItemSet candidateSet; 
        double candidateSupport;        
        
        //Count 1-Frequent items
        for (Iterator<String> iter = myTermsIndex.getIndexKeysIterator(); iter.hasNext();) {
            term=iter.next();
            candidateSet=new ItemSet(term);
            candidateSupport=findMinSupport(candidateSet);
            if (candidateSupport>ByExampleProperties.MIN_GLOBAL_SUPPORT){
                frequentItemSupport.add(new TermSupport(term,candidateSupport));
            }
        }

        //If there are many 1-frequent items, include only top MAX_1_FREQUENT_TERMS items in 1-frequent items set
        if (frequentItemSupport.size()>ByExampleProperties.MAX_1_FREQUENT_TERMS){       
            Collections.sort(frequentItemSupport);
            frequentItemSupport=
                frequentItemSupport.subList(frequentItemSupport.size()-1-ByExampleProperties.MAX_1_FREQUENT_TERMS,
                                            frequentItemSupport.size()-1);
        }
        
        for (TermSupport ts : frequentItemSupport) {
            candidateSet=new ItemSet(ts.getTerm());
            myFrequentItemSets.insertToSet(candidateSet);
        }        
    }
    
    /**
     * Removes from inverted index all the terms that are not 1-frequent
     */
    public void buildFrequentItemSetsIndex(){
        String term;
        InvertedIndex frequentTermsIndex=new InMemoryIndex();
        for (Iterator<String> iter  = myTermsIndex.getIndexKeysIterator(); iter.hasNext();) {
            term =iter.next();
            if (myFrequentItemSets.contains(new ItemSet(term)))
                frequentTermsIndex.addRow(term,myTermsIndex.getRow(term));
        }
        myTermsIndex=frequentTermsIndex;
    }
    
    /**    
     * @return terms InvertedIndex
     */
    public InvertedIndex getFrequentItemSetsIndex(){
        return myTermsIndex;
    }
    
    /**
     * @return TermSupport list
     */
    public List<TermSupport> getFrequentItemSupport(){
        return frequentItemSupport;
    }
    
    /**
     * Generates and returns k-frequent item sets
     * This is the only method that should be invoked by outside classes
     */
    public FrequentItemSets findKFrequentItemSets(){
        generateAll(myFrequentItemSets, 1);                      
        return myFrequentItemSets;        
    }
    
} //END OF CLASS
