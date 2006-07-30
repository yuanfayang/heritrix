package org.archive.crawler.byexample.datastructure.invertedindex;

import org.archive.crawler.byexample.constants.OutputConstants;

import com.sleepycat.bind.tuple.TupleBinding;
import com.sleepycat.bind.tuple.TupleInput;
import com.sleepycat.bind.tuple.TupleOutput;

public class IndexEntry extends TupleBinding{     

    private String myEntryId;
    private double myEntryValue;
    
    public IndexEntry(String entryId, double entryValue){
        myEntryId=entryId;
        myEntryValue=entryValue;
    }
    
    public String getEntryId() {
        return myEntryId;
    }
    
    public void setEntryId(String id) {
        this.myEntryId = id;
    }
    
    public double getEntryValue() {
        return myEntryValue;
    }
    
    public void setEntryValue(double ev) {
        myEntryValue=ev;
    }
    
    public void scaleEntryValue(double scale) {
        myEntryValue/=scale;
    }
    
    public IndexEntry increaseEntryValue() {
        myEntryValue++;
        return this;
    }
    
    public String toString(){
        return myEntryId+OutputConstants.ENTRY_SEPARATOR+myEntryValue;
    }      
      
    
    public void objectToEntry(Object object, TupleOutput dbEntry) {
        
        IndexEntry indexEntry=(IndexEntry) object;
        
        dbEntry.writeString(indexEntry.myEntryId);
        dbEntry.writeDouble(indexEntry.myEntryValue);        
    }
    
    public IndexEntry entryToObject(TupleInput dbEntry) {        
        return new IndexEntry(dbEntry.readString(),dbEntry.readDouble());
    }
}
