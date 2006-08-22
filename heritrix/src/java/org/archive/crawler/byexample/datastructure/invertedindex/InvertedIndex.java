package org.archive.crawler.byexample.datastructure.invertedindex;

import java.util.Comparator;
import java.util.Iterator;

/**
 * Interface of Inverted Index datastructure. InvertedIndex maps key string to an index row  
 * 
 * @see org.archive.crawler.byexample.datastructure.invertedindex.IndexRow
 * @author Michael Bendersky
 *
 */
public interface InvertedIndex {
    
    /**
     * Opens new index instance. Index data will be read from file at given filepath 
     * 
     * @param filePath Path from which index data will be read
     */
    public abstract void openIndex(String filePath);
    
    /**
     * Closes index and dumps it's content to file at given filepath
     *  
     * @param jobId Job Id assigned to the index
     * @param filePath filepath to which index data will be written
     */
    public abstract void closeIndex(String jobId, String filePath);

    /**
     * Adds new empty row with given key
     * 
     * @param rowKey Key string
     */
    public abstract void addNewRow(String rowKey);
    
    /**
     * Maps given row to given key in the index 
     *  
     * @param rowKey Key string
     * @param row Row which will be mapped to the key
     */
    public abstract void addRow(String rowKey, IndexRow row);
    
    /**
     * Adds given entry to the row at given key in the index
     * 
     * @param rowKey Key string
     * @param rowEntry entry to add
     */
    public abstract void addEntry(String rowKey, IndexEntry rowEntry);
    
    /**
     * Increases the value of the last index row entry at given key  
     * 
     * @param rowKey Key string
     */
    public abstract void increaseRowLatestEntryValue(String rowKey);
    
    /**
     * Returns the value of the last index row entry at given key
     * 
     * @param rowKey Key string 
     * @return Last index row entry value as String
     */
    public abstract String getRowLatestEntryId(String rowKey);
    
    /**
     * Normalizes all index row values. 
     * For algorithm correctness all row values should be normalized to be in (0..1] range.   
     *  
     * @param rowKey Key mapped to the normalized row in the index
     */
    public abstract void normalizeRow(String rowKey);
    
    /**
     * Sort row according to the given comparator
     * 
     * @param rowKey Key mapped to the sorted row
     * @param comparator Comparator, according to which row will be sorted 
     */
    public abstract void sortRow(String rowKey, Comparator<IndexEntry> comparator);
    
    /**
     * Returns row mapped to the given key
     * 
     * @param rowKey Key string
     * @return IndexRow mapped to the rowKey
     */
    public abstract IndexRow getRow(String rowKey);
    
    /**
     * Removes row mapped to the given key
     * 
     * @param rowKey Key string
     */
    public abstract void removeRow(String rowKey);

    /**
     * @return number rows in the index
     */
    public abstract int getSize();

    /**
     * @param rowKey Key string
     * @return TRUE - if key is mapped to any row in the index, FALSE - otherwise
     */
    public abstract boolean containsKey(String rowKey);
    
    /**
     * @return Iterator over index keys
     */
    public abstract Iterator<String> getIndexKeysIterator();
    
    /**
     * Converts index to string (Resulting String can be quite lengthy for large indexes)
     * @return Index string representation
     */
    public abstract String toString();

}