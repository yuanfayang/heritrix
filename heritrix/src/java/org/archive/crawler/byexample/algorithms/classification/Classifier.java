package org.archive.crawler.byexample.algorithms.classification;

import org.archive.crawler.byexample.algorithms.datastructure.itemset.ItemSet;
import org.archive.crawler.byexample.algorithms.datastructure.support.ClusterScore;
import org.archive.crawler.byexample.algorithms.datastructure.support.ClusterScoreListing;
import org.archive.crawler.byexample.algorithms.datastructure.support.ClusterSupportIndex;
import org.archive.crawler.byexample.algorithms.datastructure.support.TermSupportIndex;
import org.archive.crawler.byexample.algorithms.preprocessing.PorterStemmer;
import org.archive.crawler.byexample.algorithms.preprocessing.StopWordsHandler;
import org.archive.crawler.byexample.constants.AlgorithmConstants;
import org.archive.crawler.byexample.utils.ParseUtils;
import org.htmlparser.util.ParserException;

public class Classifier {
    
    private TermSupportIndex myTermsIndex;
    private static final double MIN_TERM_SCORE=0.01;
    
    public Classifier(ClusterSupportIndex csi){
        myTermsIndex=new TermSupportIndex();
        myTermsIndex.fromCSI(csi);
    }
    
    public String[] parsePage(CharSequence cs) throws ParserException{
        return ParseUtils.tokenizer(cs);        
    }
    
/**
 * Normalizes cluster scores to be in [0,1] range and removes loosely connected clusters
 * @param scores ClusterScore array of scores to normalize
 * @return ClusterScore array with normalized scores
 */ 
    public ClusterScore[] normalizeRelevanceScores (ClusterScore[] scores){
        ClusterScore[] unclassified=new ClusterScore[1];
        ClusterScore[] normalizedScores=new ClusterScore[scores.length];
        unclassified[0]=new ClusterScore(new ItemSet(AlgorithmConstants.UNCLASSIFIED_LABEL),1);
        double total=0;
        for (int i = 0; i < scores.length; i++)
            total+=scores[i].getClusterScore();
        if (total==0)
            return unclassified;
        int j=0;
        for (int i = 0; i < scores.length; i++){
            double newScore=scores[i].getClusterScore()/total;
            //Remove loosely connected scores
            if (newScore>MIN_TERM_SCORE)
                normalizedScores[j++]=new ClusterScore(scores[i].getClusterLabel(),newScore);
        }
        
        ClusterScore[] resizedScoreArray=new ClusterScore[j];
        System.arraycopy(normalizedScores,0,resizedScoreArray,0,j);
        return resizedScoreArray;
    }
    
    public ClusterScore[] classify(CharSequence cs, StopWordsHandler swh, PorterStemmer stemmer) throws ParserException{
        
        String[] allTerms=parsePage(cs);
        String iter;
        ClusterScoreListing pageScores=new ClusterScoreListing();
        ClusterScoreListing termScores=null;
        
        //Process parsed page
        if (allTerms.length > 0) {
          for (int i=0; i<allTerms.length; i++) {              
              iter=allTerms[i];              
              // Ignore words with 2 or less characters and pre-defined stop words
              if (iter.length()<3 || swh.isStopWord(iter))
                     continue;             
              // Stem the term
              iter=stemmer.stem(iter);
              // Get list of cluster scores for the term
              termScores=myTermsIndex.getRow(iter);
              // Add term score to term scores
              if (termScores!=null)
                  pageScores.addListToRow(termScores);
          }
        }
        
        pageScores.sortListing();
        return normalizeRelevanceScores(pageScores.getTopScores(AlgorithmConstants.TOP_CLASSIFICATIONS));      
    }
    
} // END OF CLASS
