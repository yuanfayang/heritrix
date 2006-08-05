package org.archive.crawler.byexample.constants;

import java.io.FileInputStream;
import java.util.Properties;

/**
 * This class contains all parameters used by clustering and classification
 * algorithms To change these parameters values, user should edit the properties
 * file found under BY_EXAMPLE_HOME/CONF folder
 * 
 * @author Michael Bendersky
 * 
 */
public class ByExampleProperties {

    // Inverted Index type constants

    /**
     * Denotes the type of inverted index in use
     * 
     * @see org.archive.crawler.byexample.datastructure.invertedindex.InvertedIndex
     * @see rg.archive.crawler.byexample.constants.OutputConstants
     */
    public static String INVERTED_INDEX_TYPE;

    // Apriori algorithm constants

    /**
     * Only terms that appear in more than <i>MIN_GLOBAL_SUPPORT</i> per cent
     * of the documents in the collection will be counted as 1-frequent itemsets
     */
    public static int MIN_GLOBAL_SUPPORT;

    /**
     * <i>MAX_DEPTH</i> determines the depth of clustering hierarchy. Itemsets
     * in the deepest hierarchy level can contain at most <i>MAX_DEPTH</i>
     * items.
     */
    public static int MAX_DEPTH;

    /**
     * Only <i>MAX_1_FREQUENT_TERMS></i> with top global support will be
     * considereed in building the clustering hierarchy. Note: setting this
     * value to too high value, may cause significant decrease in algorithm
     * performance, as total number of possible k-frequent itemsets will
     * increase exponentially.
     */
    public static int MAX_1_FREQUENT_TERMS;

    // Clustering algorithm constants

    /**
     * Terms that appear in more than <i>MIN_CLUSTER_SUPPORT</i> per cent of
     * the documents in the cluster will be considered as terms associated with
     * the cluster
     */
    public static int MIN_CLUSTER_SUPPORT;

    /**
     * Clusters that contain less than <i>MIN_SIZE_TO_PRUNE</i> per cent of the
     * documents in the collection will be merged with larger most similar
     * cluster and pruned.
     */
    public static int MIN_SIZE_TO_PRUNE;

    /**
     * Label of special cluster that contains all the documents that couldn't be
     * assigned to any cluster by clustering/classification algorithm
     */
    public static String UNCLASSIFIED_LABEL;

    // Classification algorithm constants

    /**
     * During classification stage each document will be assigned to at most
     * <i>TOP_CLASSIFICATIONS</i> clusters
     */
    public static int TOP_CLASSIFICATIONS;

    /**
     * After classification is completed, most/least relevant pages lists will
     * be created, each containing <i>TOP_RELEVANT</i> pages
     */
    public static int TOP_RELEVANT;

    /**
     * Reads algorithm parameters from properties file
     * 
     * @param path
     *            properties file path
     * @throws Exception
     */
    public static void readPropeties(String path) throws Exception {
        // Read properties file.
        Properties properties = new Properties();
        properties.load(new FileInputStream(path));

        INVERTED_INDEX_TYPE = properties.getProperty("inverted_index_type");

        MIN_GLOBAL_SUPPORT = Integer.parseInt(properties
                .getProperty("min_global_support"));
        MAX_DEPTH = Integer.parseInt(properties.getProperty("max_depth"));
        MAX_1_FREQUENT_TERMS = Integer.parseInt(properties
                .getProperty("max_1_frequent_terms"));

        MIN_CLUSTER_SUPPORT = Integer.parseInt(properties
                .getProperty("min_cluster_support"));
        MIN_SIZE_TO_PRUNE = Integer.parseInt(properties
                .getProperty("min_size_to_prune"));
        UNCLASSIFIED_LABEL = properties.getProperty("unclassified_label");

        TOP_CLASSIFICATIONS = Integer.parseInt(properties
                .getProperty("top_classifications"));
        TOP_RELEVANT = Integer.parseInt(properties.getProperty("top_relevant"));
    }

}
