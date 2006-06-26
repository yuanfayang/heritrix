package org.archive.crawler.byexample.algorithms.clustering;

import java.io.BufferedWriter;
import java.io.File;

import org.archive.crawler.byexample.algorithms.clustering.apriori.AprioriItemSetComputation;
import org.archive.crawler.byexample.algorithms.clustering.fihc.ClusterGenerator;
import org.archive.crawler.byexample.algorithms.datastructure.FrequentItemSets;
import org.archive.crawler.byexample.algorithms.datastructure.InvertedIndex;
import org.archive.crawler.byexample.utils.FileHandler;

public class ClusteringTester {

    /**
     * @param args
     */
    public static void main(String[] args) {
        
        long docCount=224;
        BufferedWriter dumpFile=null;
        
        //Create the path for writing the processor files
        File path=new File("d:\\temp");
        if (!path.exists())
            path.mkdir();
        
        try {
            //Create dump file
            dumpFile =FileHandler.createFileAtPath(path,"cluster.txt");
        } catch (Exception e1) {
            //logger.severe("Could not create file at path: "+path.getAbsolutePath());
        }
        
        InvertedIndex index=new InvertedIndex();
        try {
            index.readIndexFromFile("D:\\temp/termsInvertedIndex-224.txt");
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // Create freq. ItemSets
        AprioriItemSetComputation apriori=new AprioriItemSetComputation(docCount,index);
        FrequentItemSets fis=apriori.findKFrequentItemSets();
        
        System.out.println("Total of "+fis.getSize()+" frequent item sets found:\n"+fis);
        
        //Create Clustering
        ClusterGenerator clusterGen=new ClusterGenerator(docCount,apriori.getFrequentItemSetsIndex(), apriori.getFrequentItemSupport());
                
        clusterGen.doClustering(fis);
        
        try {
            clusterGen.getClusterDocuments().dumpIndexToFile(dumpFile);
            FileHandler.closeFile(dumpFile);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
    }

}
