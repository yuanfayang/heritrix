package org.archive.crawler.byexample.datastructure.documents;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.archive.crawler.byexample.constants.OutputConstants;
import org.archive.crawler.byexample.utils.FileUtils;

/**
 * Datastructure class implementing listing of crawled documents.
 * Each listing record is an object of type DocumentEntry
 * 
 * @see org.archive.crawler.byexample.datastructure.documents.DocumentEntry
 * @author Michael Bendersky
 *
 */
public class DocumentListing {
    
    List<DocumentEntry> docList;
    BufferedWriter dumpFile=null;
    
    /**
     * Default constructor
     *
     */
    public DocumentListing(){
        docList=new ArrayList<DocumentEntry>(); 
    }
    
    /**
     * Constructor with specified output file
     * @param bw BufferedWriter for the output file
     */
    public DocumentListing(BufferedWriter bw){
        docList=new ArrayList<DocumentEntry>();
        dumpFile=bw;
    }
    
    /**
     * Return entry at specified position
     * @param i position to return
     * @return DocumentEntry at position i
     */
    public DocumentEntry getEntryAtPos(int i){
        return docList.get(i);
    }
    
    /**
     * Add new DocumentEntry to the list, according to specified parameters
     * @param id
     * @param url
     * @param isAutoIn
     */
    public void addToListing(long id, String url, boolean isAutoIn){        
        if (docList.size()>OutputConstants.MAX_ENTRIES_IN_MEMORY && dumpFile!=null){
            dumpListingToFile();
            docList.clear();
        }          
        docList.add(new DocumentEntry(id,url,isAutoIn));
    }
    
    /**
     * Returns iterator over list rows
     */
    public Iterator<DocumentEntry> getListingIterator(){
        return docList.iterator();
    }
    
    /**
     * Write listing to designated output file
     */
    public void dumpListingToFile(){
        //No dump file defined - do nothing
        if (dumpFile==null)
            return;
        
        StringBuffer dump=new StringBuffer();
        for (DocumentEntry currKey : docList) {            
            dump.append(currKey+"\n");
        }
        FileUtils.dumpBufferToFile(dumpFile,dump);  
    }
    
    /**
     * Read listing from file at specified file path
     * @param filePath String representing the input file path
     */
    public void readListingFromFile(String filePath){
        BufferedReader in=FileUtils.readBufferFromFile(filePath);
        try {
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
        } catch (NumberFormatException e) {
            throw new RuntimeException("Could not read file at path "+filePath,e);
        } catch (IOException e) {
            throw new RuntimeException("Could not read file at path "+filePath,e);
        } 
    }
    
    /**
     * Get ratio of relevant documents in the listing
     */
    public double getAutoInRatio(){
        double ratio=0;
        for (DocumentEntry de : docList) {
            if (de.isRelevant)
                ratio++;
        }
        return ratio/docList.size();
    }
    
} //END_OF_CLASS
