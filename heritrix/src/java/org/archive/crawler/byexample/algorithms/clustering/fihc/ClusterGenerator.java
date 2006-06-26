package org.archive.crawler.byexample.algorithms.clustering.fihc;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.archive.crawler.byexample.algorithms.datastructure.ClusteringDocumentIndex;
import org.archive.crawler.byexample.algorithms.datastructure.ClusteringSupportIndex;
import org.archive.crawler.byexample.algorithms.datastructure.FrequentItemSets;
import org.archive.crawler.byexample.algorithms.datastructure.InvertedIndex;
import org.archive.crawler.byexample.algorithms.datastructure.ItemSet;
import org.archive.crawler.byexample.algorithms.datastructure.TermSupport;
import org.archive.crawler.byexample.algorithms.datastructure.ClusteringSupportIndex.SupportRow;
import org.archive.crawler.byexample.algorithms.preprocessing.TermIndexManipulator;
import org.archive.crawler.byexample.algorithms.tfidf.DocumentIndexManipulator;


public class ClusterGenerator {
    
    private class ClusterScore{
        ItemSet clusterLabel;
        double clusterScore;
        
        public ClusterScore(ItemSet clusterLabel, double clusterScore){
            this.clusterLabel=clusterLabel;
            this.clusterScore=clusterScore;
        }      

        public ItemSet getClusterLabel() {
            return clusterLabel;
        }
        public void setClusterLabel(ItemSet clusterLabel) {
            this.clusterLabel = clusterLabel;
        }
        public double getClusterScore() {
            return clusterScore;
        }
        public void setClusterScore(double clusterScore) {
            this.clusterScore = clusterScore;
        }
        
        public String toString(){
            return clusterLabel.toString()+":"+clusterScore;
        }
        
    }
    
    private TermIndexManipulator myTermsIndex;
    private DocumentIndexManipulator myTFIDFIndex;   
    private ClusteringDocumentIndex myDocumentClusteringIndex;
    private ClusteringSupportIndex myClusterSupportIndex;
    private ArrayList<TermSupport> myGlobalSupportIndex;
        
    private static final int MIN_CLUSTER_SUPPORT=70; 
    
    public ClusterGenerator(long docCount, InvertedIndex termsIndex, ArrayList<TermSupport> termSupport){
        myTermsIndex=new TermIndexManipulator(termsIndex);
        myTFIDFIndex=new DocumentIndexManipulator(termsIndex, docCount);
        myTFIDFIndex.createSortedByIdTFIDFIndex();
        myDocumentClusteringIndex=new ClusteringDocumentIndex();
        myClusterSupportIndex=new ClusteringSupportIndex();
        myGlobalSupportIndex=termSupport;
    }
    
    public void disjointClusters(){
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
                currScore=calculateClusterDocumentScore(currIS,currDocID);
                //Initialize best cluster and score if this is the first time document is encountered
                if (clusterScoresHash.get(currDocID)==null){
                    clusterScoresHash.put(currDocID,new ClusterScore(currIS,currScore));
                    continue;
                }
                //Update best cluster and score and remove document from previously best cluster
                if (currScore>=clusterScoresHash.get(currDocID).getClusterScore()){
                    myDocumentClusteringIndex.getRow(currIS).removeValueFromRow(currDocID);                    
                    clusterScoresHash.put(currDocID,new ClusterScore(currIS,currScore));
                }
            }                
        }        
    }
    
    
    public double calculateClusterDocumentScore(ItemSet is, String docID){
        double minSupportPercent=(double)MIN_CLUSTER_SUPPORT/100;
        double score=0;
        SupportRow currRow=myClusterSupportIndex.getRow(is);
        TermSupport currEntry=null;
        String currTerm=null;
        
        for (Iterator<TermSupport> se= currRow.getTermsIterator(); se.hasNext();) {
            currEntry=se.next();
            currTerm=currEntry.getTerm();
            //Term has minimum cluster support. Add it to score with TFIDF coefficient
            if (currEntry.getSupport()>minSupportPercent)
                score+=myTFIDFIndex.getTermTfidfInDoc(currTerm,docID)*currEntry.getSupport();
            //Term doesn't have minimum cluster support. Deduct it from score with TFIDF coefficient
            else
               score -=myTFIDFIndex.getTermTfidfInDoc(currTerm,docID)*getTermGlobalSupport(currTerm);               
        }        
        return score;
    }
    
    public double getTermGlobalSupport(String term){
        TermSupport tmp;
        for (int i = 0; i < myGlobalSupportIndex.size(); i++) {
            tmp=myGlobalSupportIndex.get(i);
            if (tmp.getTerm()==term)
                return tmp.getSupport();
        }
        return 0;
    }
    
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
    
    public void createInitialDocumentClusters(FrequentItemSets fis){
        String docId;

        ItemSet currItemSet;
        
        for (Iterator<String> docIter = myTFIDFIndex.getIndex().getIndexKeysIterator(); docIter.hasNext();) {
            docId=docIter.next();
            for (Iterator<ItemSet> itemIter = fis.getSetsIterator(); itemIter.hasNext();) {
                currItemSet=itemIter.next();
                if (containsItemSet(currItemSet,docId))
                    myDocumentClusteringIndex.addIndexValue(currItemSet,docId);
            }           
        }           
    }
    
    /**
     * Returns true if document contains all items in given ItemSet.
     * Otherwise, returns false
     * @param is
     * @param docId
     * @return
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
    
    public ClusteringDocumentIndex getClusterDocuments(){
        return myDocumentClusteringIndex;
    }
    
    public ClusteringSupportIndex getClusterSupport(){
        return myClusterSupportIndex;
    }
    
    public InvertedIndex getTFIDFIndex(){
        return myTFIDFIndex.getIndex();
    }
    
    public void doClustering(FrequentItemSets fis){
        createInitialDocumentClusters(fis);
        createInitialClusterSupport(fis);
        disjointClusters();
    }
}
