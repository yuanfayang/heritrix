package org.archive.crawler.byexample.algorithms.clustering;

import java.io.BufferedWriter;
import java.util.logging.Logger;

import org.archive.crawler.byexample.algorithms.clustering.apriori.AprioriItemSetComputation;
import org.archive.crawler.byexample.algorithms.clustering.fihc.ClusterGenerator;
import org.archive.crawler.byexample.algorithms.datastructure.DocumentListing;
import org.archive.crawler.byexample.algorithms.datastructure.FrequentItemSets;
import org.archive.crawler.byexample.algorithms.datastructure.InvertedIndex;
import org.archive.crawler.byexample.algorithms.datastructure.PreprocessInfo;
import org.archive.crawler.byexample.constants.AlgorithmConstants;
import org.archive.crawler.byexample.constants.OutputConstants;
import org.archive.crawler.byexample.utils.FileHandler;
import org.archive.crawler.byexample.utils.TimerHandler;

/**
 * Main class for running clustering algorithm.
 * 
 * As an input this class receives pre-processing results for a job.
 * As an output it provides clustering of the crawled documents.
 * Clustering output is created under $BYEXAMPLE_HOME/JOBS_HOME/JOB_ID folder.  
 *  
 * @author Michael Bendersky
 *
 */
public class ClusteringRunner {
    
    private static Logger logger =
        Logger.getLogger(TimerHandler.class.getName());
    
    /**
     * Main function that runs the clustering.
     * As an argument (args[0]) it should receive the JOB_ID to run clustering on
     * 
     * @param args
     */
    public static void main(String[] args) {
        
        long docCount;
        BufferedWriter dumpFile=null;
        TimerHandler myTimer=new TimerHandler();
        PreprocessInfo info=null;
        
        if (args.length==0){
            logger.severe("Missing argument: JOB_ID");
            return;
        }
        
        String jobID=args[0];
        
        try {
            AlgorithmConstants.readPropeties(OutputConstants.CONFIG_HOME+OutputConstants.PROPERTIES_FILENAME);
        } catch (Exception e2) {
            logger.severe("Failed to load properties file: "+e2.getMessage());
        }
        
        try {
            //Create dump file
            dumpFile =FileHandler.createFileAtPath(jobID,OutputConstants.CLUSTERING_FILES_HOME,
                                                   OutputConstants.DOCUMENTS_CLUSTERING_LISTING_FILENAME);
        } catch (Exception e1) {
          logger.severe("Could not create clustering output file: "+e1.getMessage());
        }
        
        try {
            info=new PreprocessInfo();
            info.fromXML(OutputConstants.JOBS_HOME+jobID+"/",OutputConstants.PREPROCESS_XML_FILENAME);
        } catch (Exception e1) {
            logger.severe("Could not load preprocess xml file: "+e1.getMessage());
        }
        
        InvertedIndex index=new InvertedIndex();
        DocumentListing docListing=new DocumentListing();
        
        try {
            index.readIndexFromFile(info.getTermsFN());
            docListing.readListingFromFile(info.getDocsFN());
        } catch (Exception e) {
           logger.severe("Failed to load input pre-process files: "+e.getMessage());
        }
        
        docCount=info.getDocsNo();
        
        // Create freq. ItemSets
        AprioriItemSetComputation apriori=new AprioriItemSetComputation(docCount,index);
        
        myTimer.startTimer();
        FrequentItemSets fis=apriori.findKFrequentItemSets();
        myTimer.reportActionTimer("CREATING "+fis.getSize()+" FREQUENT ITEM SETS");
        
        //Create Clustering
        ClusterGenerator clusterGen=new ClusterGenerator(docCount,apriori.getFrequentItemSetsIndex(), 
                                    apriori.getFrequentItemSupport(),docListing);
        
        
        try {
            clusterGen.doClustering(fis,OutputConstants.JOBS_HOME+jobID,OutputConstants.CLUSTERING_XML_FILENAME);
            clusterGen.getClusterDocuments().dumpIndexToFile(dumpFile);
            FileHandler.closeFile(dumpFile);
        } catch (Exception e) {
            logger.severe("Failed to write output clustering file: "+e.getMessage());
        }
        
    }

} //END OF CLASS
