package org.archive.crawler.byexample.algorithms.clustering;

import java.io.BufferedWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.archive.crawler.byexample.algorithms.clustering.apriori.AprioriItemSetComputation;
import org.archive.crawler.byexample.algorithms.clustering.fihc.ClusterGenerator;
import org.archive.crawler.byexample.constants.ByExampleProperties;
import org.archive.crawler.byexample.constants.OutputConstants;
import org.archive.crawler.byexample.datastructure.documents.DocumentListing;
import org.archive.crawler.byexample.datastructure.info.ClusteringInfo;
import org.archive.crawler.byexample.datastructure.info.PreprocessInfo;
import org.archive.crawler.byexample.datastructure.invertedindex.BdbIndex;
import org.archive.crawler.byexample.datastructure.invertedindex.InMemoryIndex;
import org.archive.crawler.byexample.datastructure.invertedindex.InvertedIndex;
import org.archive.crawler.byexample.datastructure.itemset.FrequentItemSets;
import org.archive.crawler.byexample.utils.FileUtils;
import org.archive.crawler.byexample.utils.TimerUtils;

/**
 * Main class for running clustering algorithm.
 * 
 * As an input this class receives pre-processing results for a job. As an
 * output it provides clustering of the crawled documents. Clustering output is
 * created under
 * <p>
 * $HERITRIX_HOME/BYEXAMPLE_HOME/JOBS_HOME/JOB_ID folder.
 * <p>
 * <p>
 * Clustering is based on FIHC algorithm. More details about algorithm outline
 * can be found <a href="http://www.cs.sfu.ca/~ddm/pub/FWE03_FIHC.pdf">here</a>
 * <p>
 * See following classes for algorithm implementation:
 * 
 * @see org.archive.crawler.byexample.algorithms.clustering.fihc.ClusterGenerator
 * @see org.archive.crawler.byexample.algorithms.clustering.apriori.AprioriItemSetComputation
 * 
 * @author Michael Bendersky
 * 
 */
public class ClusteringRunner {

    private static Logger logger = Logger.getLogger(ClusteringRunner.class
            .getName());

    /**
     * Main function that runs the clustering. As an argument (args[0]) it
     * should receive the JOB_ID to run clustering on
     * 
     * @param args
     */
    public static void main(String[] args) throws Exception {

        long docCount;
        BufferedWriter docsDumpFile = null;
        BufferedWriter supportDumpFile = null;
        TimerUtils myTimer = new TimerUtils();
        PreprocessInfo preProcessInfo = null;
        ClusteringInfo clusteringInfo = null;

        if (args.length == 0) {
            logger.severe("Missing argument: JOB_ID");
            return;
        }

        String jobID = args[0];

        try {
            ByExampleProperties.readPropeties(OutputConstants.CONFIG_HOME
                    + OutputConstants.PROPERTIES_FILENAME);
        } catch (Exception e) {
            logger.log(Level.SEVERE,"Failed to load properties file",e);
            return;
        }

        try {
            // Create docs dump file
            docsDumpFile = FileUtils.createFileForJob(jobID,
                    OutputConstants.CLUSTERING_FILES_HOME,
                    OutputConstants.DOCUMENTS_CLUSTERING_LISTING_FILENAME,
                    false);
            // Create term supports dump file
            supportDumpFile = FileUtils.createFileForJob(jobID,
                    OutputConstants.CLUSTERING_FILES_HOME,
                    OutputConstants.TERMS_SUPPORT_LISTING_FILENAME, false);
        } catch (Exception e) {
            logger.log(Level.SEVERE,"Could not create clustering output file",e);
            return;
        }

        try {
            preProcessInfo = new PreprocessInfo();
            preProcessInfo.fromXML(OutputConstants.getJobPath(jobID),
                    OutputConstants.PREPROCESS_XML_FILENAME);
        } catch (Exception e) {
            logger.log(Level.SEVERE,"Could not load preprocess.xml file",e);
            return;
        }

        InvertedIndex index;
        if (ByExampleProperties.INVERTED_INDEX_TYPE
                .equals(OutputConstants.IN_MEMORY_INDEX)) {
            index = new InMemoryIndex();
            index.openIndex(preProcessInfo.getTermsFN());
        } else if (ByExampleProperties.INVERTED_INDEX_TYPE
                .equals(OutputConstants.BDB_INDEX))
            index = new BdbIndex(OutputConstants.getPreprocessPath(jobID));
        else
            throw new RuntimeException("Invalid index type");

        DocumentListing docListing = new DocumentListing();

        try {
            docListing.readListingFromFile(preProcessInfo.getDocsFN());
        } catch (Exception e) {
            logger.log(Level.SEVERE,"Failed to load input pre-process files",e);
            return;
        }

        docCount = preProcessInfo.getDocsNo();

        // Create freq. ItemSets
        myTimer.startTimer();
        AprioriItemSetComputation apriori = new AprioriItemSetComputation(
                docCount, index);
        myTimer.reportActionTimer("BUILDING FREQUENT TERMS INVERTED INDEX");

        myTimer.startTimer();
        FrequentItemSets fis = apriori.findKFrequentItemSets();
        myTimer.reportActionTimer("CREATING " + fis.getSize()
                + " FREQUENT ITEM SETS");

        // Create clustering info xml
        clusteringInfo = new ClusteringInfo(OutputConstants
                .getClusteringPath(jobID)
                + OutputConstants.DOCUMENTS_CLUSTERING_LISTING_FILENAME,
                OutputConstants.getClusteringPath(jobID)
                        + OutputConstants.TERMS_SUPPORT_LISTING_FILENAME);
        // Create Clustering Generator
        ClusterGenerator clusterGen = new ClusterGenerator(docCount, apriori
                .getFrequentItemSetsIndex(), apriori.getFrequentItemSupport(),
                docListing, OutputConstants.getClusteringPath(jobID));

        
            try {
                clusterGen.doClustering(fis, OutputConstants.JOBS_HOME + jobID,
                        OutputConstants.CLUSTERING_XML_FILENAME, clusteringInfo);
                clusterGen.getClusterDocuments().dumpIndexToFile(docsDumpFile);
                clusterGen.getClusterSupport().dumpIndexToFile(supportDumpFile);
                FileUtils.closeFile(docsDumpFile);
                FileUtils.closeFile(supportDumpFile);
            } catch (Exception e) {
                logger.log(Level.SEVERE,"Failed to write output clustering file",e);
            }
            
    }

} // END OF CLASS
