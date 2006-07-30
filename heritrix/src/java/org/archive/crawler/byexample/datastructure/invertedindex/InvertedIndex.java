package org.archive.crawler.byexample.datastructure.invertedindex;

import java.util.Comparator;
import java.util.Iterator;

public interface InvertedIndex {
    
    public abstract void openIndex(String filePath) throws Exception;
    
    public abstract void closeIndex(String jobId, String filePath) throws Exception;
        
    public abstract void addRow(String rowKey);
    
    public abstract void addEntry(String rowKey, IndexEntry rowEntry);
    
    public abstract void increaseRowLatestEntryValue(String rowKey);
    
    public abstract String getRowLatestEntryId(String rowKey);
    
    public abstract void normalizeRow(String rowKey);
    
    public abstract void sortRow(String rowKey, Comparator<IndexEntry> comparator);
    
    public abstract IndexRow getRow(String rowKey);

    public abstract void removeRow(String rowKey);

    public abstract int getSize();

    public abstract boolean containsKey(String rowKey);

    public abstract Iterator<String> getIndexKeysIterator();
    
    public abstract String toString();

}