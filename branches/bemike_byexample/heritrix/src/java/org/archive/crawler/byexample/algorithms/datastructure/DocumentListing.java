package org.archive.crawler.byexample.algorithms.datastructure;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import org.archive.crawler.byexample.algorithms.clustering.ClusteringRunner;
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
    
    static int MAX_ENTRIES_IN_MEMORY=1000;
    
    List<DocumentEntry> docList;
    BufferedWriter dumpFile=null;
    private static Logger logger =
        Logger.getLogger(DocumentListing.class.getName());
    
    public DocumentListing(){
        docList=new ArrayList<DocumentEntry>(); 
    }
    
    public DocumentListing(BufferedWriter bw){
        docList=new ArrayList<DocumentEntry>();
        dumpFile=bw;
    }
    
    public void addToListing(long id, String url, boolean isAutoIn){
        
        if (docList.size()>MAX_ENTRIES_IN_MEMORY && dumpFile!=null){
            try {
                dumpListingToFile();
                docList.clear();
            } catch (Exception e) {
                logger.info("Could not dump documents list from memory to file...");
            }
        }
          
        docList.add(new DocumentEntry(id,url,isAutoIn));
    }
    
    public Iterator<DocumentEntry> getListingIterator(){
        return docList.iterator();
    }
    
    public void dumpListingToFile() throws Exception{
        //No dump file defined - do nothing
        if (dumpFile==null)
            return;
        
        StringBuffer dump=new StringBuffer();
        for (DocumentEntry currKey : docList) {            
            dump.append(currKey+"\n");
        }
        FileHandler.dumpBufferToFile(dumpFile,dump);  
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
