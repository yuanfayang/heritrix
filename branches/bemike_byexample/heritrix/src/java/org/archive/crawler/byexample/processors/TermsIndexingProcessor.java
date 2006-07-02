/**
 * 
 */
package org.archive.crawler.byexample.processors;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.Logger;

import org.archive.crawler.admin.CrawlJobHandler;
import org.archive.crawler.byexample.algorithms.datastructure.DocumentListing;
import org.archive.crawler.byexample.algorithms.datastructure.PreprocessInfo;
import org.archive.crawler.byexample.algorithms.preprocessing.StopWordsHandler;
import org.archive.crawler.byexample.algorithms.preprocessing.TermIndexManipulator;
import org.archive.crawler.byexample.constants.OutputConstants;
import org.archive.crawler.byexample.utils.FileHandler;
import org.archive.crawler.byexample.utils.ParseHandler;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.framework.Processor;
import org.archive.util.ArchiveUtils;
import org.archive.util.HttpRecorder;

import org.htmlparser.util.ParserException;

/**
 * @author Michael
 *
 */
public class TermsIndexingProcessor extends Processor {
    
    
    //Specific processor attributes
    
    public static final String PROCESSOR_NAME="Terms Indexing Processor";
    
    public static final String PROCESSOR_FULL_NAME=TermsIndexingProcessor.class.getName();
    
    public static final String PROCESSOR_DESCRIPTION=" Creates an inverted index of terms in the crawled pages";
    
    private String jobID;
    
    private String filesPath;
    
   
    // File where all crawled documents will be dumped as collections of terms
    private BufferedWriter indexDumpFile=null;
    
    // File where document listing will be dumped
    private BufferedWriter documentDumpFile=null;
        
    //Stop Words set
    private StopWordsHandler stopWordsHandler=null;
    
    // Terms-documents index handler
    private TermIndexManipulator termsIndexHandler=new TermIndexManipulator();
    
    // Documents listing
    private DocumentListing docList=new DocumentListing();
    
    // Number of documents processed by this processor
    long numOfProcessedDocs=0;
    
    static Logger logger =
        Logger.getLogger(PROCESSOR_FULL_NAME);
    
    public TermsIndexingProcessor(String name){
        super (name,PROCESSOR_NAME);
    }
    
    protected void initialTasks(){        
        
        jobID=OutputConstants.JOB_NAME_PREFIX+ArchiveUtils.TIMESTAMP17.format(new Date());
        filesPath=OutputConstants.JOBS_HOME+jobID+OutputConstants.PREPROCESS_FILES_HOME;
        
        try {
            //Create dump file
            indexDumpFile =FileHandler.createFileAtPath(jobID,OutputConstants.PREPROCESS_FILES_HOME,OutputConstants.TERMS_INDEX_FILENAME);
            documentDumpFile= FileHandler.createFileAtPath(jobID,OutputConstants.PREPROCESS_FILES_HOME,OutputConstants.DOCUMENT_LISTING_FILENAME);
        } catch (Exception e1) {
            logger.severe("Could not create file at path: "+filesPath);
        }
        
        try {
            //Load Stop Words list in memory
           stopWordsHandler=new StopWordsHandler();
        } catch (Exception e1) {
            logger.severe("Failed to create stop words set: "+e1.getStackTrace());
        }
        
    }
    
    protected void innerProcess(CrawlURI uriToProcess){

        // Get the URI from the CrawlURI
        String currURL = uriToProcess.toString();
        

        // Handles only HTTP at the moment
        if (!uriToProcess.isHttpTransaction())
            return; //ignore dns fetches
                
        // Ignore non-text mimetypes
        String mimetype = uriToProcess.getContentType();   
        if (mimetype == null || mimetype.indexOf("text/html")==-1) 
            return;
        
        // Ignore robots.txt
        if ( currURL.endsWith("robots.txt") || currURL.endsWith("robots.txt)") )
            return;
        
        //Parse only 2XX success responses
        if (!uriToProcess.is2XXSuccess())
            return;         
        
        // Get the Web Page Content
        CharSequence cs = null;
        HttpRecorder recorder = uriToProcess.getHttpRecorder();
        try {
            cs = recorder.getReplayCharSequence();
        } catch (IOException e) {
            return;
        }
        
        //Parse it to terms and add to the terms inverted index
        try {
            termsIndexHandler.addDocumentToIndex(ParseHandler.tokenizer(cs),numOfProcessedDocs,stopWordsHandler);
        } catch (ParserException e) {
            logger.severe("Failed to parse document: "+currURL);
        }   
        
        //Add the document to Documents listing
        docList.addToListing(numOfProcessedDocs,uriToProcess.toString(),true);
        
        //Increase the number of processed docs
        numOfProcessedDocs++;
    }
    
    public void createPreprocessXmlFile(){
        try {
            PreprocessInfo info=new PreprocessInfo
                                (filesPath+OutputConstants.TERMS_INDEX_FILENAME,
                                 filesPath+OutputConstants.DOCUMENT_LISTING_FILENAME,
                                 numOfProcessedDocs,termsIndexHandler.getIndex().getSize());
            info.toXML(OutputConstants.JOBS_HOME+jobID,OutputConstants.PREPROCESS_XML_FILENAME);
        } catch (Exception e) {
            logger.severe("Unable to create preprocess xml file: "+e.getMessage());
        }
    }

    
    
    public String report() {
        StringBuffer ret = new StringBuffer();
        ret.append("Processor: "+PROCESSOR_FULL_NAME+"\n");
        ret.append("  Function:     "+PROCESSOR_DESCRIPTION+"\n");
        ret.append("  Documents processed:  "+numOfProcessedDocs+"\n");
        ret.append("  Terms count in processed documents:  "+termsIndexHandler.getIndex().getSize()+"\n\n");

        return ret.toString();
    }
   

    protected void finalTasks(){
        try {
            termsIndexHandler.getIndex().dumpIndexToFile(indexDumpFile);
            FileHandler.closeFile(indexDumpFile);     
            docList.dumpListingToFile(documentDumpFile);
            FileHandler.closeFile(documentDumpFile);            
        } catch (Exception e) {
            logger.severe("Problems with file dump: "+e.getMessage());
        }        
        
        createPreprocessXmlFile();
    }
    
} //END OF CLASS
