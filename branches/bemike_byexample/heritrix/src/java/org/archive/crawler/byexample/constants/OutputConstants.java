package org.archive.crawler.byexample.constants;

import java.io.File;

/**
 * This class combines all output filenames and output constant values
 * 
 * @author Michael Bendersky
 *
 */
public class OutputConstants {
        
    // Inverted index constants
    
    /**
     * Default String encoding
     */
   public static final String DEFAULT_ENCODING="UTF-8";
   /**
    * Constant value denoting use of InMemoryIndex type 
    */
   public static final String IN_MEMORY_INDEX="IN_MEMORY_INDEX";
   /**
    * Constant value denoting use of BdbIndex type
    */
   public static final String BDB_INDEX="BDB_INDEX";
   
    // Listing constants
   
   /**
    * Maximum number of records to hold in memory before dumping to file in a listings 
    */
    public static final int MAX_ENTRIES_IN_MEMORY=1000;
    
    // Print-out Constants    

    public static final String KEY_SEPARATOR="~~";
    public static final String ENTRY_SEPARATOR="::";
    public static final String LIST_SEPARATOR=";";
    public static final String PATH_SEPARATOR=File.separator;
    
    // Folder structure constants
    /**
     * BYEXAMPLE home directory
     */
    public static final String BYEXAMPLE_HOME="byexample"+PATH_SEPARATOR;
    /**
     * Configuration files directory
     */
    public static final String CONFIG_HOME=BYEXAMPLE_HOME+"conf"+PATH_SEPARATOR;
    /**
     * Jobs home directory
     */
    public static final String JOBS_HOME=BYEXAMPLE_HOME+"jobs"+PATH_SEPARATOR;
    /**
     * Preprocess files directory for a job
     */
    public static final String PREPROCESS_FILES_HOME="preprocess"+PATH_SEPARATOR;
    /**
     * Clustering files directory for a job
     */
    public static final String CLUSTERING_FILES_HOME="clustering"+PATH_SEPARATOR;
    /**
     * Classification files directory for a job
     */
    public static final String CLASSIFICATION_FILES_HOME="classification"+PATH_SEPARATOR;
    
    // Output file-names constants
    public static final String JOB_NAME_PREFIX="byexamplejob-";
    public static final String STOP_WORDS_FILENAME="stopwords.txt";
    public static final String PROPERTIES_FILENAME="byexample.properties";
    public static final String TERMS_INDEX_FILENAME="termIndex.txt";
    public static final String DOCUMENT_LISTING_FILENAME="documentListing.txt";
    public static final String DOCUMENTS_CLUSTERING_LISTING_FILENAME="clusteringDocs.txt";
    public static final String TERMS_SUPPORT_LISTING_FILENAME="clusteringTermSupport.txt";  
    public static final String CLASSIFICATION_DOCUMENT_LISTING="classifiedDocs.txt";  
    public static final String UNCLASSIFIED_DOCUMENT_LISTING="unclassifiedDocs.txt";
    public static final String MOST_RELEVANT_LISTING="mostRelevantList.txt";
    public static final String LEAST_RELEVANT_LISTING="leastRelevantList.txt";    
    public static final String PREPROCESS_XML_FILENAME="preprocess-results.xml";
    public static final String CLUSTERING_XML_FILENAME="clustering-results.xml";
    public static final String CLASSIFICATION_XML_FILENAME="classification-results.xml";
    
    /**
     * Returns path under JOBS_HOME for a job of given id
     * @param id
     * @return job path
     */
    public static String getJobPath(String id){
        return JOBS_HOME+id+PATH_SEPARATOR;
    }
    
    /**
     * Returns path under JOBS_HOME for preprocess files of a job of given id
     * @param id
     * @return preprocess files path
     */
    public static String getPreprocessPath(String id){
        return getJobPath(id)+PREPROCESS_FILES_HOME;
    }
    
    /**
     * Returns path under JOBS_HOME for clustering files of a job of given id
     * @param id
     * @return clustering files path
     */   
    public static String getClusteringPath(String id){
        return getJobPath(id)+CLUSTERING_FILES_HOME;
    }

    /**
     * Returns path under JOBS_HOME for classification files of a job of given id
     * @param id
     * @return classification files path
     */   
    public static String getClassificationPath(String id){
        return getJobPath(id)+CLASSIFICATION_FILES_HOME;
    }
}
