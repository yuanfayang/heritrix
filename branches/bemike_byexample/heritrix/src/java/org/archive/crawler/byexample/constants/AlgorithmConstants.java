package org.archive.crawler.byexample.constants;

import java.io.FileInputStream;
import java.util.Properties;

public class AlgorithmConstants {
    
    // Apriori algorithm constants
    
    public static int MIN_SUPPORT;
    public static int MAX_DEPTH;
    public static int MAX_1_FREQUENT_TERMS;
    
    // Clustering algorithm constants 
    
    public static int MIN_CLUSTER_SUPPORT; 
    public static int MIN_SIZE_TO_PRUNE;
    public static String UNCLASSIFIED_LABEL;
    
    public static void readPropeties(String path) throws Exception{
        // Read properties file.
        Properties properties = new Properties();
        properties.load(new FileInputStream(path));
        
        MIN_SUPPORT=Integer.parseInt(properties.getProperty("min_support"));
        MAX_DEPTH=Integer.parseInt(properties.getProperty("max_depth"));
        MAX_1_FREQUENT_TERMS=Integer.parseInt(properties.getProperty("max_1_frequent_terms"));
        
        MIN_CLUSTER_SUPPORT=Integer.parseInt(properties.getProperty("min_cluster_support"));
        MIN_SIZE_TO_PRUNE=Integer.parseInt(properties.getProperty("min_size_to_prune"));
        UNCLASSIFIED_LABEL=properties.getProperty("unclassified_label");
    }     
   
   

}
