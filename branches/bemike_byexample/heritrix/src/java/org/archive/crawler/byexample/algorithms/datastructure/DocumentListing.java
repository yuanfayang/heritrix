package org.archive.crawler.byexample.algorithms.datastructure;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.archive.crawler.byexample.constants.OutputConstants;
import org.archive.crawler.byexample.utils.FileHandler;

public class DocumentListing {
    
    public class DocumentEntry{
        long id;
        String url;
        boolean isAutoIn;
        
        public DocumentEntry(long id, String url, boolean isAutoIn){
            this.id=id;
            this.url=url;
            this.isAutoIn=isAutoIn;
        }
        
        public String toString(){
            return id+OutputConstants.ENTRY_SEPARATOR+url+OutputConstants.ENTRY_SEPARATOR+isAutoIn;
        }
        
        public long getId(){
            return this.id;
        }
    }
    
    List<DocumentEntry> docList;
    
    public DocumentListing(){
        docList=new ArrayList<DocumentEntry>(); 
    }
    
    public void addToListing(long id, String url, boolean isAutoIn){
        docList.add(new DocumentEntry(id,url,isAutoIn));
    }
    
    public Iterator<DocumentEntry> getListingIterator(){
        return docList.iterator();
    }
    
    public void dumpListingToFile(BufferedWriter bw) throws Exception{
        StringBuffer dump=new StringBuffer();
        for (DocumentEntry currKey : docList) {            
            dump.append(currKey+"\n");
        }
        FileHandler.dumpBufferToFile(bw,dump);   
    }
    
    public void readListingFromFile(String filePath) throws Exception{
        BufferedReader in=FileHandler.readBufferFromFile(filePath);
        String iter = in.readLine();
        String[] parts;
        
        //File is empty
        if (in==null) 
            return;
        
        while (!(iter==null)){         
            parts=iter.split(OutputConstants.ENTRY_SEPARATOR);
            //Add row for the keyword
            addToListing(Long.parseLong(parts[0]),parts[1], Boolean.parseBoolean(parts[2]));
            iter=in.readLine();
        }        
        in.close(); 
    }
    
}
