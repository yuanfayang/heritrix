package org.archive.crawler.byexample.algorithms.datastructure;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.archive.crawler.byexample.constants.OutputConstants;
import org.archive.crawler.byexample.utils.FileHandler;





public class InvertedIndex {
    
    public class IndexEntry{     
        
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
    }
    
    public class IdComparator implements Comparator<IndexEntry>{        
        public int compare(IndexEntry a, IndexEntry b) {
            if (a.getEntryId().compareTo(b.getEntryId())>0)
                return 1;
            if (a.getEntryId().compareTo(b.getEntryId())<0)
                return -1;
            return 0;                
        }
    }
    
    public class ValueComparator implements Comparator<IndexEntry>{        
        public int compare(IndexEntry a, IndexEntry b) {
            if (a.getEntryValue()>a.getEntryValue())
                return 1;
            if (a.getEntryValue()<b.getEntryValue())
                return -1;
            return 0;                
        }
    }
    
    public class IndexRow {
        
        //Contains all the index entries mapped to a key 
        private ArrayList<IndexEntry> rowList; 
        private int totalRowValue;             
        
        public IndexRow(){
            rowList=new ArrayList<IndexEntry>();
            totalRowValue=0;
        }
        
        public IndexRow getRow() {
            return this;
        }
        
        //Add first occurence of entry ID to index row
        public void addRowEntry(String entryID, double entryValue){             
            rowList.add(new IndexEntry(entryID,entryValue));
            totalRowValue+=entryValue;
        }
        
        public void increaseRowLatestEntryValue(){
            int index=rowList.size()-1;
            IndexEntry currEntry=rowList.get(index);
            currEntry.increaseEntryValue();            
            rowList.set(index,currEntry);
            totalRowValue++;           
        }
    
        public String getLatestEntryId(){
            if (rowList.size()==0)
                return String.valueOf(-1);
            else
                return rowList.get(rowList.size()-1).getEntryId();
        }        
        
        public Iterator<IndexEntry> getRowIterator(){
            return rowList.iterator();
        }
        
        public int getRowSize(){
            return rowList.size();
        }
        
        public IndexEntry getIndex(int i){
            return rowList.get(i);
        }
        
        public void sortRow(Comparator<IndexEntry> comparator){
            Collections.sort(rowList,comparator);
        }
               
        
        public void normalizeRow(){
           for (Iterator<IndexEntry> iter = getRowIterator(); iter.hasNext();) {              
               iter.next().scaleEntryValue(totalRowValue);
           }
        }
        
        public void compressRow(short compressionRate){
            int lastIndex=getRowSize()-1;
            rowList.subList(0,lastIndex-Math.round(lastIndex/compressionRate)).clear();
            rowList.trimToSize();
        }
        
        public String toString(){
            return rowList.toString()+OutputConstants.KEY_SEPARATOR+String.valueOf(totalRowValue);
        }
        
        public void addEntriesFromString(String valuesString, String totalCountString){
            String[] arrayValues;
            String[] entryValues;
            //Remove "[]" and split to entries            
            arrayValues=valuesString.substring(1,valuesString.length()-1).split(", ");
            for (int i = 0; i < arrayValues.length; i++) {
                entryValues=arrayValues[i].split(OutputConstants.ENTRY_SEPARATOR);
                this.addRowEntry(entryValues[0],Double.parseDouble(entryValues[1]));                
            }
            this.totalRowValue=Integer.parseInt(totalCountString);
        }
        
    }
        
    private Map<String,IndexRow> indexRowsHash; 
            
    public InvertedIndex(){        
       indexRowsHash=new ConcurrentHashMap<String,IndexRow>();
    }
    
    public void addRow(String rowKey){
        indexRowsHash.put(rowKey,new IndexRow());
    }
    
    public IndexRow getRow(String rowKey){
        return indexRowsHash.get(rowKey);
    }
    
    public IndexRow removeRow(String rowKey){
        return indexRowsHash.remove(rowKey);
    }     
    
    public int getSize(){
        return indexRowsHash.size();
    }
       
    public boolean containsKey(String rowKey){
        return indexRowsHash.containsKey(rowKey);
    }
    
    public Iterator<String> getIndexKeysIterator(){
       return indexRowsHash.keySet().iterator();
    }

    public void dumpIndexToFile(BufferedWriter bw)throws Exception{
        StringBuffer dump=new StringBuffer();
        String currKey;
        for (Iterator<String> iter = getIndexKeysIterator(); iter.hasNext();) {
            currKey=iter.next();
            dump.append(currKey);
            dump.append(OutputConstants.KEY_SEPARATOR);
            dump.append(getRow(currKey).toString());
            dump.append("\n");
        }
        FileHandler.dumpBufferToFile(bw,dump);        
    }

    public void readIndexFromFile(String filePath) throws Exception{
        BufferedReader in=FileHandler.readBufferFromFile(filePath);
        String iter = in.readLine();
        String[] parts;
        
        //File is empty
        if (in==null) 
            return;
        
        while (!(iter==null)){         
            parts=iter.split(OutputConstants.KEY_SEPARATOR);
            //Add row for the keyword
            addRow(parts[0]);
            getRow(parts[0]).addEntriesFromString(parts[1],parts[2]);
            iter=in.readLine();
        }        
        in.close();
    }
    
    public String toString(){
        return indexRowsHash.toString();
    }
} //END OF CLASS
