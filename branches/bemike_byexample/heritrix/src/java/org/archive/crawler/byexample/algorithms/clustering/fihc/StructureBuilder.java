package org.archive.crawler.byexample.algorithms.clustering.fihc;


import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.archive.crawler.byexample.algorithms.datastructure.ClusterScore;
import org.archive.crawler.byexample.algorithms.datastructure.ClusteringDocumentIndex;
import org.archive.crawler.byexample.algorithms.datastructure.ClusteringSupportIndex;
import org.archive.crawler.byexample.algorithms.datastructure.DocumentListing;
import org.archive.crawler.byexample.algorithms.datastructure.FrequentItemSets;
import org.archive.crawler.byexample.algorithms.datastructure.InvertedIndex;
import org.archive.crawler.byexample.algorithms.datastructure.ItemSet;
import org.archive.crawler.byexample.algorithms.datastructure.TermSupport;
import org.archive.crawler.byexample.algorithms.datastructure.DocumentListing.DocumentEntry;
import org.archive.crawler.byexample.algorithms.preprocessing.TermIndexManipulator;
import org.archive.crawler.byexample.algorithms.tfidf.DocumentIndexManipulator;
import org.archive.crawler.byexample.constants.AlgorithmConstants;
import org.archive.crawler.byexample.utils.TimerHandler;

/**
 * Class that constructs the clustering structure
 * 
 * @author Michael Bendersky
 *
 */
public class StructureBuilder {

    private TermIndexManipulator myTermsIndex;
    private DocumentIndexManipulator myTFIDFIndex;   
    private ClusteringDocumentIndex myDocumentClusteringIndex;
    private ClusteringSupportIndex myClusterSupportIndex;
    private List<TermSupport> myGlobalSupportIndex;
    private DocumentListing myDocListing;    
    
    /**
     * Default Constructor
     */
    public StructureBuilder(long docCount, InvertedIndex termsIndex, List<TermSupport> termSupport, DocumentListing allDocs){
        myTermsIndex=new TermIndexManipulator(termsIndex);
        myTFIDFIndex=new DocumentIndexManipulator(termsIndex, docCount);
        myTFIDFIndex.createSortedByIdTFIDFIndex();
        myDocumentClusteringIndex=new ClusteringDocumentIndex();
        myClusterSupportIndex=new ClusteringSupportIndex();
        myGlobalSupportIndex=termSupport;
        myDocListing=allDocs;
    }
    
    /**
     * Disjoin clusters - after this method is run each document belongs to exactly one cluster
     *
     */
    public void disjoinClusters(){
        Map<String,ClusterScore> clusterScoresHash=new ConcurrentHashMap<String, ClusterScore>();
        ItemSet currIS;
        String[] currRow;
        double currScore;
        String currDocID;
        
        for (Iterator<ItemSet> iter = myDocumentClusteringIndex.getIndexKeysIterator(); iter.hasNext();) {
            currIS=iter.next();
            currRow=myDocumentClusteringIndex.getRow(currIS).toArray();
            for (int i = 0; i < currRow.length; i++){                
                currDocID=currRow[i];
                currScore=ScoreComputation.calculateClusterDocumentScore(currIS,currDocID, 
                                                                         myClusterSupportIndex, 
                                                                         myTFIDFIndex, myGlobalSupportIndex);
                //Initialize best cluster and score if this is the first time document is encountered
                if (clusterScoresHash.get(currDocID)==null){
                    clusterScoresHash.put(currDocID,new ClusterScore(currIS,currScore));
                    continue;
                }
                //Update best cluster and score and remove document from previously best cluster
                if (currScore>=clusterScoresHash.get(currDocID).getClusterScore()){
                    myDocumentClusteringIndex.getRow(clusterScoresHash.get(currDocID).getClusterLabel()).removeValueFromRow(currDocID);                    
                    clusterScoresHash.put(currDocID,new ClusterScore(currIS,currScore));
                }
                //
                else
                    myDocumentClusteringIndex.getRow(currIS).removeValueFromRow(currDocID);
            }                
        }        
    }
    
    /**
     * Creates initial clustering support based on given FrequentItemSets.
     */
    public void createInitialClusterSupport(FrequentItemSets fis){
        ItemSet currIS;
        String[] currRow;
        String currDoc;
        String termId;
        TermSupport se=null;
        for (Iterator<ItemSet> iter = myDocumentClusteringIndex.getIndexKeysIterator(); iter.hasNext();) {
            currIS=iter.next();            
            currRow=myDocumentClusteringIndex.getRow(currIS).toArray();
            for (int i=0; i<myGlobalSupportIndex.size(); i++){
                termId = myGlobalSupportIndex.get(i).getTerm();
                myClusterSupportIndex.addIndexValue(currIS,new TermSupport(termId,0));
                for (int j = 0; j < currRow.length; j++) {
                    currDoc=currRow[j];                               
                    se=myClusterSupportIndex.getRow(currIS).getEntry(termId);
                    if (myTermsIndex.valueExistsAtKey(termId,currDoc))                      
                      se.increaseSupport();
                }
                se.setSupport(se.getSupport()/currRow.length);
            }            
        }
    }
    
    /**
     * Creates initial clustering based on given FrequentItemSets.
     * At this point, each document can belong to several clusters.
     */
    public void createInitialDocumentClusters(FrequentItemSets fis){
        String docId;

        ItemSet currItemSet;
        boolean isClassified=false;
        ItemSet unClassified=new ItemSet(AlgorithmConstants.UNCLASSIFIED_LABEL);
        
        for (Iterator<DocumentEntry> docIter = myDocListing.getListingIterator(); docIter.hasNext();) {
            isClassified=false;
            docId=String.valueOf(docIter.next().getId());
            for (Iterator<ItemSet> itemIter = fis.getSetsIterator(); itemIter.hasNext();) {
                currItemSet=itemIter.next();
                if (containsItemSet(currItemSet,docId)){
                    myDocumentClusteringIndex.addIndexValue(currItemSet,docId);
                    isClassified=true;
                }
            }
            //Gather all the documents that hasn't been classified under special UNCLASSIFIED cluster
            if (!isClassified)
                myDocumentClusteringIndex.addIndexValue(unClassified,docId);
        }           
    }
    
    /**
     * Returns true if document contains all items in given ItemSet.
     * Otherwise, returns false
     */
    public boolean containsItemSet(ItemSet is, String docId){
        String[] items=is.getItems();
        for (String currItem : items) {
            // Binary search can be employed, since terms list is sorted lexicograhically
            if (myTFIDFIndex.valueExistsAtKey(docId,currItem))
                continue;
            return false;
        }        
        return true;
    }
    
    /**
     * @return current document clustering index
     * 
     * @see org.archive.crawler.byexample.algorithms.datastructure.ClusteringDocumentIndex
     */
    public ClusteringDocumentIndex getClusterDocuments(){
        return myDocumentClusteringIndex;
    }
    
    /**
     * @return current support index
     * 
     * @see org.archive.crawler.byexample.algorithms.datastructure.ClusteringSupportIndex
     * 
     */
    public ClusteringSupportIndex getClusterSupport(){
        return myClusterSupportIndex;
    }
    
    /**
     * @return current TFIDF index
     * 
     * @see org.archive.crawler.byexample.algorithms.datastructure.InvertedIndex
     */
    public InvertedIndex getTFIDFIndex(){
        return myTFIDFIndex.getIndex();
    }
    
    /**
     * Builds clustering structure based on given FrequentItemSets and reports invocation times for each step
     * This is the only method that should be invoked by outside classes
     * @param fis given FrequentItemSets
     */
    public void buildStructure(FrequentItemSets fis){
        TimerHandler myTH=new TimerHandler();
        
        myTH.startTimer();
        createInitialDocumentClusters(fis);
        myTH.reportActionTimer("CREATING INITIAL CLUSTERS");
        
        myTH.startTimer();
        createInitialClusterSupport(fis);
        myTH.reportActionTimer("CALCULATING CLUSTERS SUPPORT");
        
        myTH.startTimer();
        disjoinClusters();
        myTH.reportActionTimer("DISJOINING CLUSTERS");
    }
    
} //END OF CLASS
