package org.archive.crawler.byexample.processors;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Date;
import java.util.logging.Logger;

import org.apache.tools.ant.types.selectors.modifiedselector.Algorithm;
import org.archive.crawler.byexample.algorithms.preprocessing.StopWordsHandler;
import org.archive.crawler.byexample.algorithms.preprocessing.TermIndexManipulator;
import org.archive.crawler.byexample.constants.ByExampleProperties;
import org.archive.crawler.byexample.constants.OutputConstants;
import org.archive.crawler.byexample.datastructure.documents.DocumentListing;
import org.archive.crawler.byexample.datastructure.info.PreprocessInfo;
import org.archive.crawler.byexample.utils.FileUtils;
import org.archive.crawler.byexample.utils.ParseUtils;
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
        
    
    // File where document listing will be dumped
    private BufferedWriter documentDumpFile=null;
        
    //Stop Words set
    private StopWordsHandler stopWordsHandler=null;
    
    // Terms-documents index handler
    private TermIndexManipulator termsIndexHandler;
    
    // Documents listing
    private DocumentListing docList=null;
    
    // Number of documents processed by this processor
    long numOfProcessedDocs=0;
    
    //Number of documents that have passed the filters
    long numOfAcceptedDocs=0;
    
    //Number of documents that were rejected by the filters;
    long numOfRejectedDocs=0;
    
    static Logger logger =
        Logger.getLogger(PROCESSOR_FULL_NAME);
    
    public TermsIndexingProcessor(String name){
        super (name,PROCESSOR_NAME);
    }
    
    /**
     * Processor initial tasks
     */
    protected void initialTasks(){        
        
        //Reset the counters
        numOfProcessedDocs=0;
        numOfAcceptedDocs=0;
        numOfRejectedDocs=0;
        
        jobID=OutputConstants.JOB_NAME_PREFIX+ArchiveUtils.TIMESTAMP17.format(new Date());
        filesPath=OutputConstants.getPreprocessPath(jobID);
        
        //Load algorithm parameters
        try {
            ByExampleProperties.readPropeties(OutputConstants.CONFIG_HOME+OutputConstants.PROPERTIES_FILENAME);
        } catch (Exception e) {
            logger.severe("Failed to load properties file: "+e.getMessage());
            return;
        }
        
        //Create documents dump file
        try {
            documentDumpFile= FileUtils.createFileForJob(jobID,OutputConstants.PREPROCESS_FILES_HOME,OutputConstants.DOCUMENT_LISTING_FILENAME,true);
            docList=new DocumentListing(documentDumpFile);
        } catch (Exception e1) {
            logger.severe("Failed to create file at path: "+filesPath);
            return;
        }
        
        //Create Inverted Index        
        try {
            termsIndexHandler=new TermIndexManipulator(ByExampleProperties.INVERTED_INDEX_TYPE,filesPath);
        } catch (Exception e) {
            logger.severe("Failed to create inverted index manipulator: "+e.getMessage());
        }
                
        //Load Stop Words list
        try {
           stopWordsHandler=new StopWordsHandler();
        } catch (Exception e1) {
            logger.severe("Failed to create stop words set: "+e1.getStackTrace());
            return;
        }
        
    }
    
    /**
     * Processes the given URI and adds the process results to terms index and processed document listing.
     * URI will be processed only if it meets the process conditions such as mime-type, html response type etc.
     * 
     * @param uriToProcess URI to process
     * @param isAccepted indicator whether the URI passed the Processor filters 
     * @return TRUE - URI was processed, FALSE - else
     */
    protected boolean processURI(CrawlURI uriToProcess, boolean isAccepted){
        // Get the URI from the CrawlURI
        String currURL = uriToProcess.toString();
        

        // Handles only HTTP at the moment
        if (!uriToProcess.isHttpTransaction())
            return false; //ignore dns fetches
                
        // Ignore non-text mimetypes
        String mimetype = uriToProcess.getContentType();   
        if (mimetype == null || mimetype.indexOf("text/html")==-1) 
            return false;
        
        // Ignore robots.txt
        if ( currURL.endsWith("robots.txt") || currURL.endsWith("robots.txt)") )
            return false;
        
        //Parse only 2XX success responses
        if (!uriToProcess.is2XXSuccess())
            return false;  
        
        // Get the Web Page Content
        CharSequence cs = null;
        HttpRecorder recorder = uriToProcess.getHttpRecorder();
        try {
            cs = recorder.getReplayCharSequence();
        } catch (IOException e) {
            return false;
        }
        
        //Parse it to terms and add to the terms inverted index
        try {
            termsIndexHandler.addDocumentToIndex(ParseUtils.tokenizer(cs),numOfProcessedDocs,stopWordsHandler);
        } catch (ParserException e) {
            logger.severe("Failed to parse document: "+currURL);
        }         
       
        docList.addToListing(numOfProcessedDocs,uriToProcess.toString(),isAccepted);
        
        //Increase the number of processed docs
        numOfProcessedDocs++;
        
        return true;
    }
    
    protected void innerProcess(CrawlURI uriToProcess){
        if (processURI(uriToProcess,true))
            numOfAcceptedDocs++;
    }
    
    protected void innerRejectProcess(CrawlURI uriToProcess){
        if (processURI(uriToProcess,false))
            numOfRejectedDocs++;
    }
    
    public void createPreprocessXmlFile(){
        try {
            PreprocessInfo info=new PreprocessInfo
                                (filesPath+OutputConstants.TERMS_INDEX_FILENAME,
                                 filesPath+OutputConstants.DOCUMENT_LISTING_FILENAME,
                                 numOfProcessedDocs,termsIndexHandler.getIndex().getSize());
            info.toXML(OutputConstants.getJobPath(jobID),OutputConstants.PREPROCESS_XML_FILENAME);
        } catch (Exception e) {
            logger.severe("Unable to create preprocess xml file: "+e.getMessage());
        }
    }

    
    public String report() {
        StringBuffer ret = new StringBuffer();
        ret.append("Processor: "+PROCESSOR_FULL_NAME+"\n");
        ret.append("  Function:     "+PROCESSOR_DESCRIPTION+"\n");
        ret.append("  Documents processed:  "+numOfProcessedDocs+"\n");        
        ret.append("    Marked as relevant:  "+numOfAcceptedDocs+"\n");
        ret.append("    Marked as irrelevant:  "+numOfRejectedDocs+"\n");
        ret.append("  Terms count in processed documents:  "+termsIndexHandler.getIndex().getSize()+"\n\n");
        return ret.toString();
    }
   

    protected void finalTasks(){
        try {
            termsIndexHandler.getIndex().closeIndex(jobID,OutputConstants.PREPROCESS_FILES_HOME);                 
            docList.dumpListingToFile();
            FileUtils.closeFile(documentDumpFile);            
        } catch (Exception e) {
            logger.severe("Problems with file dump: "+e.getMessage());
        }        
        
        createPreprocessXmlFile();
    }
    
} //END OF CLASS
