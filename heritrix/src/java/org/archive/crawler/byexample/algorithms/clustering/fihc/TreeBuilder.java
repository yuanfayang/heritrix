package org.archive.crawler.byexample.algorithms.clustering.fihc;


import java.util.Iterator;
import java.util.List;

import org.archive.crawler.byexample.algorithms.datastructure.ClusteringDocumentIndex;
import org.archive.crawler.byexample.algorithms.datastructure.ClusteringInfo;
import org.archive.crawler.byexample.algorithms.datastructure.ClusteringSupportIndex;
import org.archive.crawler.byexample.algorithms.datastructure.FrequentItemSets;
import org.archive.crawler.byexample.algorithms.datastructure.ItemSet;
import org.archive.crawler.byexample.algorithms.datastructure.TermSupport;
import org.archive.crawler.byexample.algorithms.tfidf.DocumentIndexManipulator;
import org.archive.crawler.byexample.constants.AlgorithmConstants;
import org.archive.crawler.byexample.utils.TimerHandler;

/**
 * Receives as input clustering structure created by StructureBuilder and prunes and merges redundant clusters
 * 
 * @author Michael Bendersky
 *
 */
public class TreeBuilder {

    private ClusteringDocumentIndex myDocumentClusteringIndex;
    private ClusteringSupportIndex myClusterSupportIndex;
    private DocumentIndexManipulator myTFIDFIndex;
    private List<TermSupport> myGlobalSupportIndex;
    private long myDocCount;
    
    /**
     * Default constructor
     */
    public TreeBuilder(long docCount, ClusteringDocumentIndex cdi, DocumentIndexManipulator mti, List<TermSupport> gsi, 
                        ClusteringSupportIndex csi){
        myDocumentClusteringIndex=cdi;
        myTFIDFIndex=mti;
        myGlobalSupportIndex=gsi;
        myClusterSupportIndex=csi;
        myDocCount=docCount;
    }

    /**
     * Builds tree levels according to k (1st level - 1-frequent item sets, 2nd level - 2-frequent item sets, etc...)
     */
    public FrequentItemSets[] buildLevels(){
        ItemSet currIS=null;
        FrequentItemSets[] itemSetLevels=new FrequentItemSets[AlgorithmConstants.MAX_DEPTH];
        
        for (int i = 0; i < itemSetLevels.length; i++) {
            itemSetLevels[i]=new FrequentItemSets();
        }
        
        for (Iterator<ItemSet> iter = myDocumentClusteringIndex.getIndexKeysIterator(); iter.hasNext();) {
            currIS=iter.next();
            itemSetLevels[currIS.getSize()-1].insertToSet(currIS);            
        }
        return itemSetLevels;
    }
    
    /**
     * Merges most similar clusters between different levels, if clusters size is less than MIN_SIZE_TO_PRUNE
     * 
     * @see org.archive.crawler.byexample.algorithms.datastructure.AlgorithmConstants
     * @param fis Tree levels structure
     */
    public void mergeLevels(FrequentItemSets[] fis){
        
        if (fis.length<2)
            return;
        
        ItemSet currParent=null;
        ItemSet currChild=null;
        ItemSet tempParent=null;
        int childClusterSize=0;
        double currScore=0;
        double tempScore=0;
        long sizeToPrune=Math.round(myDocCount*((double)AlgorithmConstants.MIN_SIZE_TO_PRUNE/100));
        for (Iterator<ItemSet> iter1=fis[fis.length-1].getSetsIterator(); iter1.hasNext();){
            currChild=iter1.next();
            childClusterSize=myDocumentClusteringIndex.getRow(currChild).getRowSize();
            //Merge all non-empty children of small size with their parents
            if (childClusterSize>0 && childClusterSize<sizeToPrune){
                for (Iterator<ItemSet> iter2=fis[fis.length-2].getSetsIterator(); iter2.hasNext();){
                    currParent=iter2.next();
                    //No parent is still chosen
                    if (tempParent==null)
                        tempParent=currParent;
                    if (currChild.contains(currParent)){
                        currScore=ScoreComputation.interClusterSimilarity(currParent,currChild,
                                    myDocumentClusteringIndex,myClusterSupportIndex,
                                    myTFIDFIndex,myGlobalSupportIndex);
                        if(currScore>=tempScore){
                            tempScore=currScore;
                            tempParent=currParent;
                        }
                    }
                }
                try {
                    myDocumentClusteringIndex.getRow(tempParent).addListToRow(
                                                                 myDocumentClusteringIndex.getRow(currChild));
                    myDocumentClusteringIndex.getRow(currChild).removeAll();
                } catch (RuntimeException e) {
                    e.printStackTrace();
                }
                
            }
        }
        
       FrequentItemSets[] newFIS=new FrequentItemSets[fis.length-1];
       System.arraycopy(fis,0,newFIS,0,fis.length-1);
       mergeLevels(newFIS);
    }
    
    /**
     * Removes empty clusters
     */
    public void pruneLevels(){
        myDocumentClusteringIndex.removeEmptyRows();
    }
    
    /**
     * Writes clustering structure into XML
     * @param path file path 
     * @param filename file name
     * @throws Exception
     */
    public void createClusteringInfoOutput(String path, String filename) throws Exception{
        ItemSet currIS=null;
        ClusteringInfo info=new ClusteringInfo();
        
        for (Iterator<ItemSet> iter = myDocumentClusteringIndex.getIndexKeysIterator(); iter.hasNext();) {
            currIS=iter.next();
            //TO DO: Determine relevance
            info.addCluster(currIS,myDocumentClusteringIndex.getRow(currIS).getRowSize(),true);            
        }
        
        info.toXML(path, filename);
    }

    /**
     * Builds clustering tree, prunes and merges levels.
     * Outputs resulting tree as an XML file and reports invocation times for each step.
     * This is the only method that should be invoked by outside classes 
     * @param path file path 
     * @param filename file name
     * @throws Exception
     */
    public void buildTree(String path, String filename) throws Exception{
        TimerHandler myTH=new TimerHandler();
        
        myTH.startTimer();
        mergeLevels(buildLevels());
        myTH.reportActionTimer("MERGING SMALL CLUSTERS");
        
        myTH.startTimer();
        pruneLevels();    
        myTH.reportActionTimer("PRUNING EMPTY CLUSTERS");
        
        myTH.startTimer();
        createClusteringInfoOutput(path, filename);
        myTH.reportActionTimer("CREATING CLUSTERING OUTPUT XML FILE");
    }
    
} //END OF CLASS
