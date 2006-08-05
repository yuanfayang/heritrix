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
import org.archive.crawler.settings.Type;
import org.archive.util.ArchiveUtils;
import org.archive.util.HttpRecorder;
import org.htmlparser.util.ParserException;

public class ClassifierProcessor extends Processor {

    // Specific processor attributes

    public static final String PROCESSOR_NAME = "Classifier Processor";

    public static final String PROCESSOR_FULL_NAME = ClassifierProcessor.class
            .getName();

    public static final String PROCESSOR_DESCRIPTION = "Classifies the crawled pages according to supplied clustering hierarchy";

    public static final String BASE_ON_KEY = "based-on";

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
        Type e = addElementToDefinition(new SimpleType(BASE_ON_KEY,
                "Defines the by-example-id of the clustering hierarchy "
                        + "on which the classification will be based",
                EMPTY_BASE_ON));
        e.setExpertSetting(false);
        e.setOverrideable(false);
    }

    protected void initialTasks() {

        try {
            basedOnJob = (String) getAttribute(BASE_ON_KEY);
        } catch (Exception e) {
            logger.severe("Failed to find attribute: " + BASE_ON_KEY);
            return;
        }

        if (basedOnJob.equals(EMPTY_BASE_ON)) {
            logger
                    .severe("Clustering job to base on the classification is not defined");
            return;
        }

        // Load clustering info XML
        try {
            clusteringInfo = new ClusteringInfo();
            clusteringInfo.fromXML(OutputConstants.getJobPath(basedOnJob),
                    OutputConstants.CLUSTERING_XML_FILENAME);
        } catch (Exception e) {
            logger.severe("Failed to build clustering info based on job "
                    + basedOnJob + ": " + e.getMessage());
            return;
        }

        // Load cluster support index
        try {
            csi = new ClusterSupportIndex();
            csi.readIndexFromFile(clusteringInfo.getClusterTermSupportFN());
        } catch (Exception e1) {
            logger.severe("Failed to load clustering support index");
            return;
        }

        // Create classifier instance
        myClassifier = new Classifier(csi, clusteringInfo);

        // Load algorithm parameters
        try {
            ByExampleProperties.readPropeties(OutputConstants.CONFIG_HOME
                    + OutputConstants.PROPERTIES_FILENAME);
        } catch (Exception e) {
            logger.severe("Failed to load properties file: " + e.getMessage());
            return;
        }

        // Create Job ID
        jobID = OutputConstants.JOB_NAME_PREFIX
                + ArchiveUtils.TIMESTAMP17.format(new Date());

        // Create output files
        try {            
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
        } catch (Exception e1) {
            logger.severe("Failed to create classified docs file: "
                    + e1.getMessage());
        }

        // Create documents listings
        try {
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
        } catch (Exception e1) {
            logger.severe("Couldn't create classification listing file: "
                    + e1.getMessage());
        }

        // Create stop words removal and stemming handlers
        try {
            stopWordsHandler = new StopWordsHandler();
        } catch (Exception e) {
            logger.severe("Failed to create stop words set: "
                    + e.getStackTrace());
            return;
        }
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
        try {
            currUrlClassifications = myClassifier.classify(currURL, cs,
                    stopWordsHandler, stemmer);
        } catch (ParserException e) {
            logger.severe("Failed to parse page" + e.getMessage());
            return;
        }

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

    public void createClassificaionXmlFile() {
        String filesPath = OutputConstants.getClassificationPath(jobID);
        try {
            ClassificationInfo info = new ClassificationInfo(OutputConstants
                    .getJobPath(basedOnJob), 
                    filesPath + OutputConstants.CLASSIFICATION_DOCUMENT_LISTING,
                    filesPath + OutputConstants.UNCLASSIFIED_DOCUMENT_LISTING,
                    filesPath + OutputConstants.MOST_RELEVANT_LISTING,
                    filesPath + OutputConstants.LEAST_RELEVANT_LISTING,
                    numOfProcessedDocs);
            info.toXML(OutputConstants.getJobPath(jobID),
                    OutputConstants.CLASSIFICATION_XML_FILENAME);
        } catch (Exception e) {
            logger.severe("Unable to create preprocess xml file: "
                    + e.getMessage());
        }
    }

    public String report() {
        StringBuffer ret = new StringBuffer();
        ret.append("Processor: " + PROCESSOR_FULL_NAME + "\n");
        ret.append("  Function:     " + PROCESSOR_DESCRIPTION + "\n");
        ret.append("  Documents classified:  " + numOfProcessedDocs + "\n");
        return ret.toString();
    }

    protected void finalTasks() {
        try {
            myClassificationDocListing.dumpListingToFile();
            FileUtils.closeFile(listDumpFile);
            myUnclassifiedDocListing.dumpListingToFile();
            FileUtils.closeFile(unclassifiedDumpFile);
            myMostRelevantSet.dumpListingToFile();
            FileUtils.closeFile(mostRelevantDumpFile);
            myLeastRelevantSet.dumpListingToFile();
            FileUtils.closeFile(leastRelevantDumpFile);
        } catch (Exception e) {
            logger.severe("Couldn't close dump files: " + e.getMessage());
        }

        createClassificaionXmlFile();
    }

} // END OF CLASS
