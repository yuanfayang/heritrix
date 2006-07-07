package org.archive.crawler.byexample.constants;

import java.io.File;

public class OutputConstants {
    
    // Print-out Constants
    
    public static final String KEY_SEPARATOR="~";
    public static final String ENTRY_SEPARATOR=":";
    public static final String PATH_SEPARATOR=File.separator;
    
    // Folder structure constants
    public static final String BYEXAMPLE_HOME="byexample"+PATH_SEPARATOR;
    public static final String CONFIG_HOME=BYEXAMPLE_HOME+"conf"+PATH_SEPARATOR;
    public static final String JOBS_HOME=BYEXAMPLE_HOME+"jobs"+PATH_SEPARATOR;
    public static final String PREPROCESS_FILES_HOME="preprocess"+PATH_SEPARATOR;    
    public static final String CLUSTERING_FILES_HOME="clustering"+PATH_SEPARATOR;
    public static final String CLASSIFICATION_FILES_HOME="classification"+PATH_SEPARATOR;
    
    // Output file-names constants
    public static final String JOB_NAME_PREFIX="byexamplejob-";
    public static final String STOP_WORDS_FILENAME="stopwords.txt";
    public static final String PROPERTIES_FILENAME="byexample.properties";
    public static final String TERMS_INDEX_FILENAME="termIndex.txt";
    public static final String DOCUMENT_LISTING_FILENAME="documentListing.txt";
    public static final String DOCUMENTS_CLUSTERING_LISTING_FILENAME="clusteringDocs.txt";
    public static final String PREPROCESS_XML_FILENAME="preprocess-results.xml";
    public static final String CLUSTERING_XML_FILENAME="clustering-results.xml";
    
    
    /**
     * Returns path under JOBS_HOME for a job of given id
     * @param id
     * @return job path
     */
    public static String getJobPath(String id){
        return JOBS_HOME+id+PATH_SEPARATOR;
    }
    
    public static String getPreprocessPath(String id){
        return getJobPath(id)+PREPROCESS_FILES_HOME;
    }
    
    public static String getClusteringPath(String id){
        return getJobPath(id)+CLUSTERING_FILES_HOME;
    }

    public static String getClassificationPath(String id){
        return getJobPath(id)+CLASSIFICATION_FILES_HOME;
    }
}
