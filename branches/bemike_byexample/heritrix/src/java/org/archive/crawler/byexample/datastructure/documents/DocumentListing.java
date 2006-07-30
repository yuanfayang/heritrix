package org.archive.crawler.byexample.datastructure.documents;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

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
    private static Logger logger =
        Logger.getLogger(DocumentListing.class.getName());
    
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
            try {
                dumpListingToFile();
                docList.clear();
            } catch (Exception e) {
                logger.info("Could not dump documents list from memory to file...");
            }
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
     * @throws Exception
     */
    public void dumpListingToFile() throws Exception{
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
     * @throws Exception
     */
    public void readListingFromFile(String filePath) throws Exception{
        BufferedReader in=FileUtils.readBufferFromFile(filePath);
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
