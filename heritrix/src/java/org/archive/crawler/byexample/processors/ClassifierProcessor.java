package org.archive.crawler.byexample.processors;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Date;
import java.util.logging.Logger;

import org.archive.crawler.byexample.algorithms.classification.Classifier;
import org.archive.crawler.byexample.algorithms.preprocessing.PorterStemmer;
import org.archive.crawler.byexample.algorithms.preprocessing.StopWordsHandler;
import org.archive.crawler.byexample.constants.ByExampleProperties;
import org.archive.crawler.byexample.constants.OutputConstants;
import org.archive.crawler.byexample.datastructure.documents.BottomUpComparator;
import org.archive.crawler.byexample.datastructure.documents.DocumentClassificationEntry;
import org.archive.crawler.byexample.datastructure.documents.DocumentClassificationListing;
import org.archive.crawler.byexample.datastructure.documents.TopClassificationSet;
import org.archive.crawler.byexample.datastructure.documents.TopDownComparator;
import org.archive.crawler.byexample.datastructure.info.ClassificationInfo;
import org.archive.crawler.byexample.datastructure.info.ClusteringInfo;
import org.archive.crawler.byexample.datastructure.support.ClusterSupportIndex;
import org.archive.crawler.byexample.utils.FileUtils;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.framework.Processor;
import org.archive.crawler.settings.SimpleType;
import org.archive.util.ArchiveUtils;
import org.archive.util.HttpRecorder;

/**
 * Processor in charge of making classifications based on previous clustering  
 * 
 * @author Michael Bendersky
 *
 */
public class ClassifierProcessor extends Processor {

    // Specific processor attributes

    public static final String PROCESSOR_NAME = "Classifier Processor";

    public static final String PROCESSOR_FULL_NAME = ClassifierProcessor.class
            .getName();

    public static final String PROCESSOR_DESCRIPTION = "Classifies the crawled pages according to supplied clustering hierarchy";
    
    public static final String JOB_ID_ATTR = "crawl-by-example Job ID"; 

    public static final String BASE_ON_ATTR = "based-on";

    public static final String EMPTY_BASE_ON = "none";

    // Clustering info
    private ClusteringInfo clusteringInfo = null;

    // Cluster Support index
    private ClusterSupportIndex csi = null;

    // Stop words removal and stemming handlers
    StopWordsHandler stopWordsHandler = null;

    PorterStemmer stemmer = null;

    // Number of documents processed by this processor
    long numOfProcessedDocs = 0;

    // Logger
    static Logger logger = Logger.getLogger(PROCESSOR_FULL_NAME);

    // Clustering job that classification will be based on
    private String basedOnJob = null;

    // Classification job ID
    private String defaultJobID=null;
    private String jobID = null;

    // Classifier instance
    private Classifier myClassifier = null;

    // Classification Documents listing
    private DocumentClassificationListing myClassificationDocListing = null;

    private BufferedWriter listDumpFile = null;

    // Unclassified documents listing
    private DocumentClassificationListing myUnclassifiedDocListing = null;

    private BufferedWriter unclassifiedDumpFile = null;

    // Most relevant documents set
    private TopClassificationSet myMostRelevantSet = null;

    private BufferedWriter mostRelevantDumpFile = null;

    // Least relevant documents set
    private TopClassificationSet myLeastRelevantSet = null;

    private BufferedWriter leastRelevantDumpFile = null;

    public ClassifierProcessor(String name) {
        super(name, PROCESSOR_NAME);
        // Add based-on job attribute
        addElementToDefinition(new SimpleType(BASE_ON_ATTR,
                "Defines the crawl-by-example id of the clustering hierarchy "
                        + "on which the classification will be based",
                EMPTY_BASE_ON));
        //Create default job id
        defaultJobID=OutputConstants.JOB_NAME_PREFIX+ArchiveUtils.TIMESTAMP17.format(new Date());
        //Add Job ID attribute
        addElementToDefinition(new SimpleType(JOB_ID_ATTR,
                "Defines the crawl-by-example job ID", defaultJobID));
        
    }

    protected void initialTasks() {
        
        // Load "Based-on" attribute
        try {
            basedOnJob = (String) getAttribute(BASE_ON_ATTR);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load attribute "+BASE_ON_ATTR,e);
        }

        if (basedOnJob.equals(EMPTY_BASE_ON)) {
            logger
                    .severe("Clustering job to base on the classification is not defined");
            return;
        }
        
        // Load "Job-Id" attribute
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

        // Load clustering info XML
        clusteringInfo = new ClusteringInfo();
        clusteringInfo.fromXML(OutputConstants.getJobPath(basedOnJob),
                    OutputConstants.CLUSTERING_XML_FILENAME);

        // Load cluster support index
        csi = new ClusterSupportIndex();
        csi.readIndexFromFile(clusteringInfo.getClusterTermSupportFN());

        // Create classifier instance
        myClassifier = new Classifier(csi, clusteringInfo);

        // Load algorithm parameters
        ByExampleProperties.readPropeties(OutputConstants.getPropertiesFilePath());

        // Create output files         
        listDumpFile = FileUtils.createFileForJob(jobID,
                OutputConstants.CLASSIFICATION_FILES_HOME,
                OutputConstants.CLASSIFICATION_DOCUMENT_LISTING, true);
        unclassifiedDumpFile=FileUtils.createFileForJob(jobID,
                OutputConstants.CLASSIFICATION_FILES_HOME,
                OutputConstants.UNCLASSIFIED_DOCUMENT_LISTING, true);
        mostRelevantDumpFile = FileUtils.createFileForJob(jobID,
                OutputConstants.CLASSIFICATION_FILES_HOME,
                OutputConstants.MOST_RELEVANT_LISTING, true);
        leastRelevantDumpFile = FileUtils.createFileForJob(jobID,
                OutputConstants.CLASSIFICATION_FILES_HOME,
                OutputConstants.LEAST_RELEVANT_LISTING, true);


        // Create documents listings
        myClassificationDocListing = new DocumentClassificationListing(
                listDumpFile);
        myUnclassifiedDocListing = new DocumentClassificationListing(
                unclassifiedDumpFile);
        myMostRelevantSet = new TopClassificationSet(
                new TopDownComparator(), ByExampleProperties.TOP_RELEVANT,
                mostRelevantDumpFile);
        myLeastRelevantSet = new TopClassificationSet(
                new BottomUpComparator(), ByExampleProperties.TOP_RELEVANT,
                leastRelevantDumpFile);

        // Create stop words removal and stemming handlers
        stopWordsHandler = new StopWordsHandler();
        stemmer = new PorterStemmer();

    }

    protected void innerProcess(CrawlURI uriToProcess) {

        // Get the URI from the CrawlURI
        String currURL = uriToProcess.toString();
        // Document top classification scores
        DocumentClassificationEntry currUrlClassifications = null;

        // Handles only HTTP at the moment
        if (!uriToProcess.isHttpTransaction())
            return; // ignore dns fetches

        // Ignore non-text mimetypes
        String mimetype = uriToProcess.getContentType();
        if (mimetype == null || mimetype.indexOf("text/html") == -1)
            return;

        // Ignore robots.txt
        if (currURL.endsWith("robots.txt") || currURL.endsWith("robots.txt)"))
            return;

        // Parse only 2XX success responses
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

        // Get document classification and scoping
        currUrlClassifications = myClassifier.classify(currURL, cs,
                    stopWordsHandler, stemmer);

        // Add document classifications to listing
        if (currUrlClassifications.getClassificationRelevanceScore()==0){
            myUnclassifiedDocListing.addClassification(currUrlClassifications);
        }
        else{
            myClassificationDocListing.addClassification(currUrlClassifications);
        }
        myMostRelevantSet.addEntry(currUrlClassifications);
        myLeastRelevantSet.addEntry(currUrlClassifications);

        numOfProcessedDocs++;
    }

    /**
     * Creates Classification InfoXML file
     * 
     * @see org.archive.crawler.byexample.datastructure.info.ClassificationInfo
     *
     */
    public void createClassificaionXmlFile() {        
        ClassificationInfo info = new ClassificationInfo(
                OutputConstants.getJobPath(basedOnJob),
                OutputConstants.getClassificationDocumentListingFilePath(jobID),
                OutputConstants.getUnclassifiedDocumentListingFilePath(jobID),
                OutputConstants.getMostRelevantListingFilePath(jobID),
                OutputConstants.getLeastRelevantListingFilePath(jobID),
                numOfProcessedDocs);
        info.toXML(OutputConstants.getJobPath(jobID),
                OutputConstants.CLASSIFICATION_XML_FILENAME);
    }

    public String report() {
        StringBuffer ret = new StringBuffer();
        ret.append("Processor: " + PROCESSOR_FULL_NAME + "\n");
        ret.append("  Function:     " + PROCESSOR_DESCRIPTION + "\n");
        ret.append("  Documents classified:  " + numOfProcessedDocs + "\n");
        return ret.toString();
    }

    protected void finalTasks() {
        //Close used files
        myClassificationDocListing.dumpListingToFile();
        FileUtils.closeFile(listDumpFile);
        myUnclassifiedDocListing.dumpListingToFile();
        FileUtils.closeFile(unclassifiedDumpFile);
        myMostRelevantSet.dumpListingToFile();
        FileUtils.closeFile(mostRelevantDumpFile);
        myLeastRelevantSet.dumpListingToFile();
        FileUtils.closeFile(leastRelevantDumpFile);
        //Create info file
        createClassificaionXmlFile();
    }

} // END OF CLASS
