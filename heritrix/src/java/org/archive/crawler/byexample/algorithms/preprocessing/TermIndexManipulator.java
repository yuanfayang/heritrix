package org.archive.crawler.byexample.algorithms.preprocessing;


import org.archive.crawler.byexample.constants.OutputConstants;
import org.archive.crawler.byexample.datastructure.invertedindex.BdbIndex;
import org.archive.crawler.byexample.datastructure.invertedindex.InMemoryIndex;
import org.archive.crawler.byexample.datastructure.invertedindex.IndexEntry;
import org.archive.crawler.byexample.datastructure.invertedindex.IndexRow;
import org.archive.crawler.byexample.datastructure.invertedindex.InvertedIndex;

/**
 * This class is a manipulation wrapper around InvertedIndex
 * Manipulations implemented here are specific to Terms Inverted Index
 * 
 * @see  org.archive.crawler.byexample.datastructure.invertedindex.InvertedIndex
 * 
 * @author Michael Bendersky
 *
 */
public class TermIndexManipulator {
    
    private InvertedIndex myIndex;
    
    /**
     * Constructor that creates new Inverted index
     * and builds manipulator for it
     *
     */
    public TermIndexManipulator(String indexType, String indexFilePath) throws Exception{
        
        if (indexType.equals(OutputConstants.IN_MEMORY_INDEX))
                myIndex=new InMemoryIndex();
        else if (indexType.equals(OutputConstants.BDB_INDEX))
                myIndex=new BdbIndex(indexFilePath);
        
        else throw new Exception("Invalid inverted index type");
    }
    
    /**
     * Constructor that build manipulator on existing Inverted Index
     * @param index existing Inverted Index
     */
    public TermIndexManipulator(InvertedIndex index){
        myIndex=index;
    }
       
    /**
     * Adds terms count from the document to the inverted index
     * @param terms
     * @param docOrdinal
     * @param index
     * @param stopWordsHandler
     * @return
     */
    public void addDocumentToIndex(String[] terms, long docOrdinal,StopWordsHandler stopWordsHandler){
        String iter=new String();        
        PorterStemmer stemmer=new PorterStemmer();
    
        if (terms!=null) {
          for (int i=1; i<terms.length; i++) {
              iter=terms[i];
              
              //Ignore words with 2 or less characters and pre-defined stop words
              if (iter.length()<3 || stopWordsHandler.isStopWord(iter))
                     continue;
              
              //Add the term/document pair into the terms Inverted Index              
              addRowEntry(stemmer.stem(iter),String.valueOf(docOrdinal));                                      
          }
        }
        return;
    }
    
    // Add entry to list
    private void addRowEntry(String rowKey, String entryID){
        IndexRow currRow=myIndex.getRow(rowKey);
        // If this key doesn't have an assigned row, create it
        if (currRow==null){
            myIndex.addRow(rowKey);
            currRow=myIndex.getRow(rowKey);
        }
        //Add unique entry mapping to the rowKey 
        //Mapping to key is added only once per entry 
        if (!myIndex.getRowLatestEntryId(rowKey).equals(entryID))
            myIndex.addEntry(rowKey, new IndexEntry(entryID,1));                    
        //Entry already exists for a key. Increase entry occurences count
        else
            myIndex.increaseRowLatestEntryValue(rowKey);
            //indexRowsHash.put(rowKey,currRow.increaseRowLatestEntryValue(entryID));
    }

    /**
     * Returns true if valueToSearch exists at key row
     * @param key  row key 
     * @param value value to search
     * @return TRUE if value exists at key row, else otherwise
     */
    public boolean valueExistsAtKey(String key, String valueToSearch){
        IndexRow currRow=myIndex.getRow(key);
        for (int i = 0; i < currRow.getRowSize(); i++) {
            if (currRow.getIndex(i).getEntryId().equals(valueToSearch))
                return true;
        }
        return false;
    }
    
    /**
     * Get the InvertedIndex of the manipulato
     */
    public InvertedIndex getIndex() {
        return myIndex;
    }
    
    
}
