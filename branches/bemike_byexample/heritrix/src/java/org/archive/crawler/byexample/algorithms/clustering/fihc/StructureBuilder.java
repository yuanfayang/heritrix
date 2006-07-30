package org.archive.crawler.byexample.algorithms.clustering.fihc;


import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.archive.crawler.byexample.algorithms.preprocessing.TermIndexManipulator;
import org.archive.crawler.byexample.algorithms.tfidf.DocumentIndexManipulator;
import org.archive.crawler.byexample.constants.ByExampleProperties;
import org.archive.crawler.byexample.datastructure.documents.ClusterDocumentsIndex;
import org.archive.crawler.byexample.datastructure.documents.DocumentListing;
import org.archive.crawler.byexample.datastructure.documents.IdListing;
import org.archive.crawler.byexample.datastructure.documents.DocumentEntry;
import org.archive.crawler.byexample.datastructure.invertedindex.IndexEntry;
import org.archive.crawler.byexample.datastructure.invertedindex.IndexRow;
import org.archive.crawler.byexample.datastructure.invertedindex.InvertedIndex;
import org.archive.crawler.byexample.datastructure.itemset.FrequentItemSets;
import org.archive.crawler.byexample.datastructure.itemset.ItemSet;
import org.archive.crawler.byexample.datastructure.support.ClusterScore;
import org.archive.crawler.byexample.datastructure.support.ClusterSupportIndex;
import org.archive.crawler.byexample.datastructure.support.DocumentSupportIndex;
import org.archive.crawler.byexample.datastructure.support.TermSupport;
import org.archive.crawler.byexample.utils.TimerUtils;

/**
 * Class that constructs the clustering structure
 * 
 * @author Michael Bendersky
 *
 */
public class StructureBuilder {

    private TermIndexManipulator myTermsIndex;
    private DocumentIndexManipulator myTFIDFIndex;   
    private ClusterDocumentsIndex myDocumentClusteringIndex;
    private ClusterSupportIndex myClusterSupportIndex;
    private DocumentSupportIndex myDocumentSupportIndex;
    private List<TermSupport> myGlobalSupportIndex;
    private DocumentListing myDocListing;    
    
    /**
     * Default Constructor
     */
    public StructureBuilder(long docCount, InvertedIndex termsIndex, 
            List<TermSupport> termSupport, DocumentListing allDocs, String indexFilePath) 
    throws Exception{
        myTermsIndex=new TermIndexManipulator(termsIndex);
        myTFIDFIndex=new DocumentIndexManipulator(ByExampleProperties.INVERTED_INDEX_TYPE,indexFilePath, termsIndex, docCount);
        myTFIDFIndex.createSortedByIdTFIDFIndex();
        myDocumentClusteringIndex=new ClusterDocumentsIndex();
        myClusterSupportIndex=new ClusterSupportIndex();
        myGlobalSupportIndex=termSupport;
        myDocListing=allDocs;
        myDocumentSupportIndex=new DocumentSupportIndex();
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
                    myDocumentClusteringIndex.getRow(clusterScoresHash.get(currDocID).getClusterLabel()).removeValue(currDocID);                    
                    clusterScoresHash.put(currDocID,new ClusterScore(currIS,currScore));
                }
                //
                else
                    myDocumentClusteringIndex.getRow(currIS).removeValue(currDocID);
            }                
        }        
    }
    
    public void calculateClusterSupport(){
        ItemSet currIS=null;
        IdListing currDocs=null;
        int currDocsNo=0;
        myClusterSupportIndex.clearIndex();
        for (Iterator<ItemSet> iter = myDocumentClusteringIndex.getIndexKeysIterator(); iter.hasNext();) {
            currIS=iter.next();            
            currDocs=myDocumentClusteringIndex.getRow(currIS);     
            currDocsNo=currDocs.getListSize();       
            //No documents in the cluster. Do not calculate support
            if (currDocsNo==0)
                continue;
            //else calculate support for each 1-frequent item
            myClusterSupportIndex.initializeKey(currIS,myGlobalSupportIndex);
            for (int i = 0; i <myGlobalSupportIndex.size(); i++) {
                for (int j = 0; j < currDocsNo; j++) {
                    myClusterSupportIndex.getRow(currIS).getEntryAtIndex(i).setSupport(
                    (myDocumentSupportIndex.getRow(currDocs.getValueAtPos(j)).getEntryAtIndex(i).getSupport()+
                    myClusterSupportIndex.getRow(currIS).getEntryAtIndex(i).getSupport()));
                }
                myClusterSupportIndex.getRow(currIS).getEntryAtIndex(i).setSupport(
                        myClusterSupportIndex.getRow(currIS).getEntryAtIndex(i).getSupport()/currDocsNo);
            }            
        }
    }
    
    public void calculateDocumentSupport(){
        String currDocId=null;
        IndexRow currIR=null;
        TermSupport currSE=null;
        for (Iterator<DocumentEntry> iter = myDocListing.getListingIterator(); iter.hasNext();) {
            currDocId=String.valueOf(iter.next().getId());
            myDocumentSupportIndex.initializeKey(currDocId,myGlobalSupportIndex);
            for (int i=0; i<myGlobalSupportIndex.size(); i++){
                currSE=myDocumentSupportIndex.getRow(currDocId).getEntryAtIndex(i);
                currIR=myTermsIndex. getIndex().getRow(myGlobalSupportIndex.get(i).getTerm());
                currSE.setSupport(getTermSupport(currDocId,currIR));                
            }            
        }
    }
    
    public double getTermSupport(String s, IndexRow ir){
        IndexEntry ie=null;
        for (int i = 0; i < ir.getRowSize(); i++) {
            ie=ir.getIndex(i);
            if (ie.getEntryId().equals(s))
                return 1;
        }
        return 0;
    }

    /**
     * Creates initial clustering based on given FrequentItemSets.
     * At this point, each document can belong to several clusters.
     */
    public void createInitialDocumentClusters(FrequentItemSets fis){
        String docId;
        
        ItemSet currItemSet;
        boolean isClassified=false;
        ItemSet unClassified=new ItemSet(ByExampleProperties.UNCLASSIFIED_LABEL);
        
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
     * @see org.archive.crawler.byexample.datastructure.documents.ClusterDocumentsIndex
     */
    public ClusterDocumentsIndex getClusterDocuments(){
        return myDocumentClusteringIndex;
    }
    
    /**
     * @return current support index
     * 
     * @see org.archive.crawler.byexample.datastructure.support.ClusterSupportIndex
     * 
     */
    public ClusterSupportIndex getClusterSupport(){
        return myClusterSupportIndex;
    }
    
    /**
     * @return current TFIDF index
     * 
     * @see org.archive.crawler.byexample.datastructure.invertedindex.InvertedIndex
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
        TimerUtils myTH=new TimerUtils();
        
        myTH.startTimer();
        createInitialDocumentClusters(fis);
        myTH.reportActionTimer("CREATING INITIAL CLUSTERS");
        
        myTH.startTimer();
        calculateDocumentSupport();
        myTH.reportActionTimer("CALCULATING DOCUMENTS SUPPORT SCORES");
        
        myTH.startTimer();        
        calculateClusterSupport();
        myTH.reportActionTimer("CALCULATING INITIAL CLUSTERS SUPPORT SCORES");
        
        myTH.startTimer();
        disjoinClusters();
        myTH.reportActionTimer("DISJOINING CLUSTERS");
    }
    
} //END OF CLASS
