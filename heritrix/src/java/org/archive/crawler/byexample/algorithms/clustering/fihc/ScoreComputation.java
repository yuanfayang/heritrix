package org.archive.crawler.byexample.algorithms.clustering.fihc;

import java.util.Iterator;
import java.util.List;

import org.archive.crawler.byexample.algorithms.datastructure.documents.ClusterDocumentsIndex;
import org.archive.crawler.byexample.algorithms.datastructure.itemset.ItemSet;
import org.archive.crawler.byexample.algorithms.datastructure.support.ClusterSupportIndex;
import org.archive.crawler.byexample.algorithms.datastructure.support.TermSupport;
import org.archive.crawler.byexample.algorithms.datastructure.support.TermSupportListing;
import org.archive.crawler.byexample.algorithms.tfidf.DocumentIndexManipulator;
import org.archive.crawler.byexample.constants.AlgorithmConstants;

/**
 * Class that provides static methods for similarity scores computation between documents and clusters
 * 
 * @author Michael Bendersky
 *
 */
public class ScoreComputation {

    /**
     * Calculate score of item for given ItemSet.
     * The higher the score, the higher the probability that document belongs to cluster, labeled by this set
     */
    public static double calculateClusterDocumentScore(ItemSet is, String docID,
                                                ClusterSupportIndex myClusterSupportIndex, 
                                                DocumentIndexManipulator myTFIDFIndex,
                                                List<TermSupport> myGlobalSupportIndex){
        double minSupportPercent=(double)AlgorithmConstants.MIN_CLUSTER_SUPPORT/100;
        double score=0;
        TermSupportListing currRow=myClusterSupportIndex.getRow(is);
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
               score -=myTFIDFIndex.getTermTfidfInDoc(currTerm,docID)*getTermGlobalSupport(currTerm, myGlobalSupportIndex);               
        }        
        return score;
    }
    
    /**
     * Calculate similarity score between two clusters.
     * The higher the score, the higher the probability that two clusters can be merged into one
     */
    public static double interClusterSimilarity(ItemSet is1, ItemSet is2,
                                                ClusterDocumentsIndex myDocumentClusterIndex,
                                                ClusterSupportIndex myClusterSupportIndex, 
                                                DocumentIndexManipulator myTFIDFIndex,
                                                List<TermSupport> myGlobalSupportIndex){
        String[] docsToCompare=myDocumentClusterIndex.getRow(is2).toArray();
        double totalSimilarity=0;
        for (int i = 0; i < docsToCompare.length; i++) {
            totalSimilarity=calculateClusterDocumentScore(is1,docsToCompare[i],myClusterSupportIndex,myTFIDFIndex,myGlobalSupportIndex);            
        }
        //Normalize the similarity by clusters size
        return normalizeSimilarity(totalSimilarity,myDocumentClusterIndex.getRow(is1).getRowSize(), 
                                   myDocumentClusterIndex.getRow(is2).getRowSize());
    }
   
    //Normalize similarity by clusters size
    private static double normalizeSimilarity(double sim, int size1, int size2){
        return sim/=Math.pow(Math.pow(size1,2)+Math.pow(size2,2),0.5);
    }
    
    private static double getTermGlobalSupport(String term, List<TermSupport> myGlobalSupportIndex){
        TermSupport tmp;
        for (int i = 0; i < myGlobalSupportIndex.size(); i++) {
            tmp=myGlobalSupportIndex.get(i);
            if (tmp.getTerm()==term)
                return tmp.getSupport();
        }
        return 0;
    }
    
} //End of class
