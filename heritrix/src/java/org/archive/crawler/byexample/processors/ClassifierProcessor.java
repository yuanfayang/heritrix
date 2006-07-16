package org.archive.crawler.byexample.processors;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Date;
import java.util.logging.Logger;

import org.archive.crawler.byexample.algorithms.classification.Classifier;
import org.archive.crawler.byexample.algorithms.datastructure.documents.ClassificationDocumentsListing;
import org.archive.crawler.byexample.algorithms.datastructure.info.ClusteringInfo;
import org.archive.crawler.byexample.algorithms.datastructure.support.ClusterScore;
import org.archive.crawler.byexample.algorithms.datastructure.support.ClusterSupportIndex;
import org.archive.crawler.byexample.algorithms.preprocessing.PorterStemmer;
import org.archive.crawler.byexample.algorithms.preprocessing.StopWordsHandler;
import org.archive.crawler.byexample.constants.OutputConstants;
import org.archive.crawler.byexample.utils.FileUtils;
import org.archive.crawler.byexample.utils.ParseUtils;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.framework.Processor;
import org.archive.crawler.settings.SimpleType;
import org.archive.crawler.settings.Type;
import org.archive.util.ArchiveUtils;
import org.archive.util.HttpRecorder;
import org.htmlparser.util.ParserException;


public class ClassifierProcessor extends Processor {
    
    //Specific processor attributes
    
    public static final String PROCESSOR_NAME="Classifier Processor";
    
    public static final String PROCESSOR_FULL_NAME=ClassifierProcessor.class.getName();
    
    public static final String PROCESSOR_DESCRIPTION="Classifies the crawled pages according to supplied clustering hierarchy";
    
    public static final String BASE_ON_KEY="based-on";
    
    public static final String EMPTY_BASE_ON="none";
    
    // Clustering info    
    private ClusteringInfo clusteringInfo=null;

    //Cluster Support index
    private ClusterSupportIndex csi=null; 
    
    //Stop words removal and stemming handlers
    StopWordsHandler stopWordsHandler=null;    
    PorterStemmer stemmer=null;
        
    // Number of documents processed by this processor
    long numOfProcessedDocs=0;
    
    // Logger
    static Logger logger =
        Logger.getLogger(PROCESSOR_FULL_NAME);
    
    // Clustering job that classification will be based on
    private String basedOnJob=null;
    
    // Classification job ID
    private String jobID=null;
    
    // Classifier instance
    private Classifier myClassifier=null;
    
    //Classification Documents listing
    private ClassificationDocumentsListing myClassificationDocListing=null;
    private BufferedWriter docListDumpFile=null;
    
    
    public ClassifierProcessor(String name){
        super (name,PROCESSOR_NAME);
        Type e= addElementToDefinition(new SimpleType(BASE_ON_KEY,
                "Defines the by-example-id of the clustering hierarchy " +
                "on which the classification will be based",EMPTY_BASE_ON));
        e.setExpertSetting(false);
        e.setOverrideable(false);
    }
    
    protected void initialTasks(){ 
        
        try {
            basedOnJob=(String)getAttribute(BASE_ON_KEY);
        } catch (Exception e) {
            logger.severe("Failed to find attribute: "+ BASE_ON_KEY);
            return;
        }        
        
        if (basedOnJob.equals(EMPTY_BASE_ON)){
            logger.severe("Clustering job to base on the classification is not defined");
            return;
        }            
        
        // Load clustering info XML
        try {
            clusteringInfo=new ClusteringInfo();
            clusteringInfo.fromXML(OutputConstants.getJobPath(basedOnJob),OutputConstants.CLUSTERING_XML_FILENAME);
        } catch (Exception e) {
            logger.severe("Failed to build clustering info based on job "+basedOnJob+": "+e.getMessage());
            return;
        }
        
        // Load cluster support index
        try {
            csi= new ClusterSupportIndex();
            csi.readIndexFromFile(clusteringInfo.getClusterTermSupportFN());
        } catch (Exception e1) {
            logger.severe("Failed to load clustering support index");
            return;
        }
        
        // Create classifier instance
        myClassifier=new Classifier(csi);
                
        //Create Job ID
        jobID=OutputConstants.KEY_SEPARATOR+ArchiveUtils.TIMESTAMP17.format(new Date());
        
        //Create output files
        try {
            docListDumpFile=FileUtils.createFileForJob(basedOnJob,OutputConstants.CLASSIFICATION_FILES_HOME,
                                                    OutputConstants.CLASSIFICATION_DOCUMENT_LISTING+jobID,true);
        } catch (Exception e1) {
            logger.severe("Failed to create classified docs file: "+e1.getMessage());
        }
        
        //Create documents listing
        try {
            myClassificationDocListing=new ClassificationDocumentsListing(docListDumpFile);
        } catch (Exception e1) {
           logger.severe("Couldn't create classification listing file: "+e1.getMessage());
        }
        
        //Create stop words removal and stemming handlers
        try {
            stopWordsHandler=new StopWordsHandler();
        } catch (Exception e) {
            logger.severe("Failed to create stop words set: "+e.getStackTrace());
            return;
        }
        stemmer=new PorterStemmer();
        
    }
    
    protected void innerProcess(CrawlURI uriToProcess){
        
        // Get the URI from the CrawlURI
        String currURL = uriToProcess.toString();
        
        ClusterScore[] classifications=null;

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
            classifications=myClassifier.classify(cs,stopWordsHandler,stemmer);
        } catch (ParserException e) {
            logger.severe("Failed to parse page"+e.getMessage());
            return;
        }
        
        //Add document classifications to listing
        myClassificationDocListing.addClassification(currURL,classifications,"good");
        
        numOfProcessedDocs++;
    }
    
    
    public String report() {
        StringBuffer ret = new StringBuffer();
        ret.append("Processor: "+PROCESSOR_FULL_NAME+"\n");
        ret.append("  Function:     "+PROCESSOR_DESCRIPTION+"\n");
        ret.append("  Documents classified:  "+numOfProcessedDocs+"\n");
        return ret.toString();
    }
   

    protected void finalTasks(){
        try {
            myClassificationDocListing.dumpListingToFile();
            FileUtils.closeFile(docListDumpFile);
        } catch (Exception e) {
            logger.severe("Couldn't close dump files: "+e.getMessage());
        }
    }
    

    
} //END OF CLASS
