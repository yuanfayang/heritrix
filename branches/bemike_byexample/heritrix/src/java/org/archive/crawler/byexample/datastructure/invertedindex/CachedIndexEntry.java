package org.archive.crawler.byexample.datastructure.invertedindex;

import org.archive.crawler.byexample.constants.OutputConstants;

public class CachedIndexEntry {
    
    private IndexEntry cachedEntry;
    private int cachedRowValue;
    
    public CachedIndexEntry(){
        this.cachedEntry=null;
        this.cachedRowValue=0;
    }
    public CachedIndexEntry(IndexEntry ie, int value){
        this.cachedEntry=ie;
        this.cachedRowValue=value;
    }
    
    public IndexEntry getCachedEntry() {
        return cachedEntry;
    }
    public void setCachedEntry(IndexEntry cachedEntry) {
        this.cachedEntry = cachedEntry;
    }
    public int getCachedRowValue() {
        return cachedRowValue;
    }
    public void setCachedRowValue(int cachedRowValue) {
        this.cachedRowValue = cachedRowValue;
    }
    public void increaseCachedRowValue(){
        cachedRowValue++;
    }    
    public String toString(){
        return cachedEntry.toString()+OutputConstants.ENTRY_SEPARATOR+cachedRowValue;
    }
        
}
