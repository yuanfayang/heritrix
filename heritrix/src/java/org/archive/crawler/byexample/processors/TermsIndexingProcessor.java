/**
 * 
 */
package org.archive.crawler.byexample.processors;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import org.archive.crawler.byexample.algorithms.preprocessing.StopWordsHandler;
import org.archive.crawler.byexample.algorithms.preprocessing.TermIndexManipulator;
import org.archive.crawler.byexample.utils.FileHandler;
import org.archive.crawler.byexample.utils.ParseHandler;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.framework.Processor;
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
    
    public static final String PROCESSOR_DESCRIPTION=" This processor is used to create an inverted index of terms in the crawled pages";
    
    public static final String DEFAULT_PATH="clustering";
    
   
    // File where all crawled documents will be dumped as collections of terms
    private BufferedWriter dumpFile=null;
    
    
    //Stop Words set
    private StopWordsHandler stopWordsHandler=null;
    
    // HashTables containing key terms and their IDF rank
    private TermIndexManipulator termsIndexHandler=new TermIndexManipulator(); 
    
    long numOfProcessedDocs=0;
    
    static Logger logger =
        Logger.getLogger(PROCESSOR_FULL_NAME);
    
    public TermsIndexingProcessor(String name){
        super (name,PROCESSOR_NAME);
    }
    
    protected void initialTasks(){
        
        //Create the path for writing the processor files
        File path=new File(getController().getDisk(), DEFAULT_PATH);
        if (!path.exists())
            path.mkdir();
        
        try {
            //Create dump file
            dumpFile =FileHandler.createFileAtPath(path,"termsInvertedIndex.txt");
        } catch (Exception e1) {
            logger.severe("Could not create file at path: "+path.getAbsolutePath());
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
      
        try {
            termsIndexHandler.addDocumentToIndex(ParseHandler.tokenizer(cs),numOfProcessedDocs,stopWordsHandler);
        } catch (ParserException e) {
            logger.severe("Failed to parse document: "+currURL);
        }   
        
        //Increase the number of processed docs
        numOfProcessedDocs++;
        
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
            termsIndexHandler.getIndex().dumpIndexToFile(dumpFile);
            FileHandler.closeFile(dumpFile);           
        } catch (Exception e) {
            logger.severe("Problems with file dump: "+e.getStackTrace());
        }        
    }
    
} //END OF CLASS
