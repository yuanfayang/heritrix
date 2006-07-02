package org.archive.crawler.byexample.constants;

public class OutputConstants {
    
    // Print-out Constants
    
    public static final String KEY_SEPARATOR="~";
    public static final String ENTRY_SEPARATOR=":";
    
    // Folder structure constants
    public static final String BYEXAMPLE_HOME="byexample/";
    public static final String CONFIG_HOME=BYEXAMPLE_HOME+"conf/";
    public static final String JOBS_HOME=BYEXAMPLE_HOME+"jobs/";
    public static final String PREPROCESS_FILES_HOME="/preprocess/";    
    public static final String CLUSTERING_FILES_HOME="/clustering/";
    public static final String CLASSIFICATION_FILES_HOME="/classification/";
    
    // Output file-names constants
    public static final String JOB_NAME_PREFIX="byexamplejob-";
    public static final String STOP_WORDS_FILENAME="stopwords.txt";
    public static final String PROPERTIES_FILENAME="byexample.properties";
    public static final String TERMS_INDEX_FILENAME="termIndex.txt";
    public static final String DOCUMENT_LISTING_FILENAME="documentListing.txt";
    public static final String DOCUMENTS_CLUSTERING_LISTING_FILENAME="clusteringDocs.txt";
    public static final String PREPROCESS_XML_FILENAME="preprocess-results.xml";
    public static final String CLUSTERING_XML_FILENAME="clustering-results.xml";

}
