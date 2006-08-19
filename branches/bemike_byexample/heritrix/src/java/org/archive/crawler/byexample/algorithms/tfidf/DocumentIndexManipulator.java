package org.archive.crawler.byexample.algorithms.tfidf;

import java.util.Iterator;

import org.archive.crawler.byexample.constants.OutputConstants;
import org.archive.crawler.byexample.datastructure.invertedindex.BdbIndex;
import org.archive.crawler.byexample.datastructure.invertedindex.InMemoryIndex;
import org.archive.crawler.byexample.datastructure.invertedindex.IdComparator;
import org.archive.crawler.byexample.datastructure.invertedindex.IndexEntry;
import org.archive.crawler.byexample.datastructure.invertedindex.IndexRow;
import org.archive.crawler.byexample.datastructure.invertedindex.InvertedIndex;


/**
 * This class is a manipulation wrapper around InvertedIndex.
 * Manipulations implemented here are specific to TFIDF Index.
 * TFIDF Index contains mappings of type: 
 * <p>
 * [DOC_ID] - [LIST OF DOCUMENT TERM TFIDF SCORES]
 * 
 * @see  org.archive.crawler.byexample.datastructure.invertedindex.InvertedIndex
 * 
 * @author Michael Bendersky
 *
 */
public class DocumentIndexManipulator {
    
    private InvertedIndex myIndex;
    private InvertedIndex termsIndex;
    private long myDocCount;
    
    /**
     * Constructor that creates new Inverted index
     * and builds manipulator for it
     *
     */
    public DocumentIndexManipulator(String indexType, 
            String indexFilePath, InvertedIndex ti, long docCount){
                
        if (indexType.equals(OutputConstants.IN_MEMORY_INDEX))
                myIndex=new InMemoryIndex();
        else if (indexType.equals(OutputConstants.BDB_INDEX))
            try {
                myIndex=new BdbIndex(indexFilePath);
            } catch (Exception e) {
                throw new RuntimeException("Failed to create "+OutputConstants.BDB_INDEX,e);
            }
        else 
            throw new RuntimeException("Invalid index type");
        
        termsIndex=ti;
        myDocCount=docCount;
    }
    
    /**
     * Creates tfidf index based on term inverted index and 
     * sorts index rows in terms lexicographical order 
     *
     */
    public void createSortedByIdTFIDFIndex(){
        createTermTFIDFIndex(myDocCount);
        sortTfidfRows();
    }
    
    private void createTermTFIDFIndex(long docCount){
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
    
    private void sortTfidfRows(){
        IdComparator idComparator=new IdComparator();     
        String currKey;
        for (Iterator<String> iter=myIndex.getIndexKeysIterator();iter.hasNext();){
            currKey=iter.next();
            //Normalize TFIDF values
            myIndex.normalizeRow(currKey);
            //Sort lexicographically
            myIndex.sortRow(currKey,idComparator);            
        }
    }
    
    
    /**
     * Add term TFIDF count to the document row in the index 
     * @param termEntryID 
     * @param termEntryValue
     * @param docID
     */
    public void addTermTFIDFToIndex(String termEntryID, double termEntryValue, String docID){
        IndexRow currRow=myIndex.getRow(docID);
        //If this key doesn't have an assigned row, create it
        if (currRow==null){
            myIndex.addNewRow(docID);
            currRow=myIndex.getRow(docID);
        }
        // Add term TFIDF count to the document row 
        myIndex.addEntry(docID,new IndexEntry(termEntryID,termEntryValue));
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
    
    
    /**     
     * @return InvertedIndex inverted index that is wrapped by this manipulator
     */
    public InvertedIndex getIndex(){
        return myIndex;
    }

        
    
} //END OF CLASS
