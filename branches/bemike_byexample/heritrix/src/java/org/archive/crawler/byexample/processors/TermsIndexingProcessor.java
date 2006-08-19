package org.archive.crawler.byexample.processors;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Date;
import java.util.logging.Logger;

import javax.management.AttributeNotFoundException;
import javax.management.MBeanException;
import javax.management.ReflectionException;

import org.archive.crawler.byexample.algorithms.preprocessing.StopWordsHandler;
import org.archive.crawler.byexample.algorithms.preprocessing.TermIndexManipulator;
import org.archive.crawler.byexample.constants.ByExampleProperties;
import org.archive.crawler.byexample.constants.OutputConstants;
import org.archive.crawler.byexample.datastructure.documents.DocumentListing;
import org.archive.crawler.byexample.datastructure.info.PreprocessInfo;
import org.archive.crawler.byexample.relevancerules.RelevanceDecidingFilter;
import org.archive.crawler.byexample.utils.FileUtils;
import org.archive.crawler.byexample.utils.ParseUtils;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.deciderules.DecideRule;
import org.archive.crawler.deciderules.DecideRuleSequence;
import org.archive.crawler.framework.Processor;
import org.archive.crawler.settings.SimpleType;
import org.archive.crawler.settings.Type;
import org.archive.util.ArchiveUtils;
import org.archive.util.HttpRecorder;

/**
 * @author Michael
 *
 */
public class TermsIndexingProcessor extends Processor {
    
    
    //Specific processor attributes
    
    public static final String PROCESSOR_NAME="Terms Indexing Processor";
    
    public static final String PROCESSOR_FULL_NAME = TermsIndexingProcessor.class.getName();
    
    public static final String PROCESSOR_DESCRIPTION = "Creates an inverted index of terms in the crawled pages";
    
    public static final String RELEVANCE_DECIDE_RULES_ATTR = "Relevance Rules";
    
    public static final String RELEVANCE_DECIDE_FILTER_ATTR = "Relevance Filter";
   
    public static final String JOB_ID_ATTR = "crawl-by-example job ID"; 
    
    private RelevanceDecidingFilter myRelevanceDecider;
    
    private String defaultJobID; 
    
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
        //Create default job id
        defaultJobID=OutputConstants.JOB_NAME_PREFIX+ArchiveUtils.TIMESTAMP17.format(new Date());
        //Add Job ID attribute
        addElementToDefinition(new SimpleType(JOB_ID_ATTR,
                "Defines the crawl-by-example job ID", defaultJobID));
        // Add relevance filter
        addElementToDefinition(
                new RelevanceDecidingFilter(RELEVANCE_DECIDE_FILTER_ATTR));
        //Add relevance decide rules sequence
        addElementToDefinition(
                new DecideRuleSequence(RELEVANCE_DECIDE_RULES_ATTR));
    }
    
    /**
     * Processor initial tasks
     */
    protected void initialTasks(){        
        
        //Reset the counters
        numOfProcessedDocs=0;
        numOfAcceptedDocs=0;
        numOfRejectedDocs=0;
        
        try {
            jobID=(String)getAttribute(JOB_ID_ATTR);
        }  catch (Exception e) {
            throw new RuntimeException("Failed to load job-id attribute",e);
        }
        // If job id is different from default, 
        //create new job with the supplied id + unique job identifier
        if (jobID.equals(defaultJobID))
            jobID=defaultJobID;
        else
            jobID=jobID.concat("-").concat(ArchiveUtils.TIMESTAMP17.format(new Date()));
        
        filesPath=OutputConstants.getPreprocessPath(jobID);
        
        //Load algorithm parameters
        ByExampleProperties.readPropeties(OutputConstants.getPropertiesFilePath());
        
        try {
            myRelevanceDecider=(RelevanceDecidingFilter)getAttribute(RELEVANCE_DECIDE_FILTER_ATTR);
            myRelevanceDecider.setDecideRules((DecideRule)getAttribute(RELEVANCE_DECIDE_RULES_ATTR));
        } catch (Exception e) {
            throw new RuntimeException("Failed to load relevance filter attributes",e);
        }
            
        //Create documents dump file
        documentDumpFile= FileUtils.createFileForJob(jobID,OutputConstants.PREPROCESS_FILES_HOME,OutputConstants.DOCUMENT_LISTING_FILENAME,true);
        docList=new DocumentListing(documentDumpFile);
    
        //Create Inverted Index        
        termsIndexHandler=new TermIndexManipulator(ByExampleProperties.INVERTED_INDEX_TYPE,filesPath);
                
        //Load Stop Words list
        stopWordsHandler=new StopWordsHandler();
        
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
        termsIndexHandler.addDocumentToIndex(ParseUtils.tokenizer(cs),numOfProcessedDocs,stopWordsHandler);

       
        docList.addToListing(numOfProcessedDocs,uriToProcess.toString(),isAccepted);
        
        //Increase the number of processed docs
        numOfProcessedDocs++;
        
        return true;
    }
    
    private void acceptedProcess(CrawlURI uriToProcess){
        if (processURI(uriToProcess,true))
            numOfAcceptedDocs++;
    }
    
    private void rejectedProcess(CrawlURI uriToProcess){
        if (processURI(uriToProcess,false))
            numOfRejectedDocs++;
    }
    
    protected void innerProcess(CrawlURI uriToProcess){        
        if (myRelevanceDecider.accepts(uriToProcess))
            acceptedProcess(uriToProcess);
        else
            rejectedProcess(uriToProcess);
 }

    public void createPreprocessXmlFile(){
        PreprocessInfo info=new PreprocessInfo
                            (OutputConstants.getTermsIndexFilePath(jobID),
                            OutputConstants.getDocumentListingFilePath(jobID),
                             numOfProcessedDocs,termsIndexHandler.getIndex().getSize());
        info.toXML(OutputConstants.getJobPath(jobID),OutputConstants.PREPROCESS_XML_FILENAME);
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
        termsIndexHandler.getIndex().closeIndex(jobID,OutputConstants.PREPROCESS_FILES_HOME);                 
        docList.dumpListingToFile();
        FileUtils.closeFile(documentDumpFile);                  
        
        createPreprocessXmlFile();
    }
    
} //END OF CLASS
