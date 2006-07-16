package org.archive.crawler.byexample.algorithms.tfidf;

import java.util.Iterator;

import org.archive.crawler.byexample.algorithms.datastructure.invertedindex.InvertedIndex;
import org.archive.crawler.byexample.algorithms.datastructure.invertedindex.InvertedIndex.IdComparator;
import org.archive.crawler.byexample.algorithms.datastructure.invertedindex.InvertedIndex.IndexEntry;
import org.archive.crawler.byexample.algorithms.datastructure.invertedindex.InvertedIndex.IndexRow;



public class DocumentIndexManipulator {
    
    private InvertedIndex myIndex;
    private InvertedIndex termsIndex;
    private long myDocCount;
    
    public DocumentIndexManipulator(InvertedIndex ti, long docCount){
        myIndex=new InvertedIndex();
        termsIndex=ti;
        myDocCount=docCount;
    }
    
    public void createSortedByIdTFIDFIndex(){
        createTermTFIDFIndex(myDocCount);
        sortByDocId();
    }
    
    public void createTermTFIDFIndex(long docCount){
        String currTerm;
        IndexRow currRow;
        Iterator<IndexEntry> currRowIterator;
        IndexEntry currEntry;
        for (Iterator<String> iter=termsIndex.getIndexKeysIterator();iter.hasNext();) {
            currTerm=iter.next();
            currRow=termsIndex.getRow(currTerm);
            for (currRowIterator=currRow.getRowIterator();currRowIterator.hasNext();){
                currEntry=currRowIterator.next();
                addTermTFIDFToIndex(currTerm,
                        TfIdfComputation.computeTFIDF(currEntry.getEntryValue(),currRow.getRowSize(),docCount),
                        currEntry.getEntryId());                        
            }                                              
        }
    }
    
    public void sortByDocId(){
        IndexRow currRow;   
        IdComparator idComparator=myIndex.new IdComparator();     
        for (Iterator<String> iter=myIndex.getIndexKeysIterator();iter.hasNext();){
            currRow=myIndex.getRow(iter.next());
            //Normalize TFIDF values
            currRow.normalizeRow();
            //Sort lexicographically
            currRow.sortRow(idComparator);
        }
    }
    
    
    public void addTermTFIDFToIndex(String termEntryID, double termEntryValue, String docID){
        IndexRow currRow=myIndex.getRow(docID);
        //If this key doesn't have an assigned row, create it
        if (currRow==null){
            myIndex.addRow(docID);
            currRow=myIndex.getRow(docID);
        }
        // Add term TFIDF count to the document row 
        currRow.addRowEntry(termEntryID,termEntryValue);
    }
   
    /**
     * This search should only be used on a sorted row
     * @param comparator
     * @param value
     * @return
     */
    public int binarySearch(String key, String valueToSearch){
        IndexRow currRow=myIndex.getRow(key);
        int low = 0;
        int high = currRow.getRowSize() - 1;
        while (low <= high) {
            int mid = (low + high) / 2;                
            int c = valueToSearch.compareTo(currRow.getIndex(mid).getEntryId());                
            if (c < 0)
                    high = mid - 1;
            else if (c > 0)
                    low = mid + 1;
            else
                    return mid;
        }
        return -1;
    }  
    
    /**
     * This function should only be used on a sorted row since it employs binary search
     * @param key
     * @param valueToSearch
     * @return
     */
    public boolean valueExistsAtKey(String key, String valueToSearch){
        // No key found
        if (!myIndex.containsKey(key))
            return false;
        //Key found, use binary search to find the value
        if (binarySearch(key,valueToSearch)>=0)
            return true;
        return false;
    }
    
    /**
     * This function should only be used on a sorted row since it employs binary search
     * Returns 0 if term doesn't appear in the document
     * @param term
     * @param docId
     * @return
     */
    public double getTermTfidfInDoc(String term, String docId){
        // No key found
        if (!myIndex.containsKey(docId))
            return 0;
        //Key found, use binary search to find the term tfidf
        int index=binarySearch(docId,term);
        if (index==-1)  
            return 0;
        return myIndex.getRow(docId).getIndex(index).getEntryValue();
    }
    
    
    public InvertedIndex getIndex(){
        return myIndex;
    }

        
    
} //END OF CLASS
