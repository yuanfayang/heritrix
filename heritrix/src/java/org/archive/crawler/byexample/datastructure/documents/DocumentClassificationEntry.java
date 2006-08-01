package org.archive.crawler.byexample.datastructure.documents;

import org.archive.crawler.byexample.constants.OutputConstants;
import org.archive.crawler.byexample.datastructure.support.ClusterScore;

/**
 *  Datastructure class implementing document classification record.
 *  Structure of the record is as follows:
 * <p>
 * - URL
 * <p>
 * - Array of ClusterScores for each possible cluster of the url
 * <p>
 * - Relevance score of the url
 * 
 * @see org.archive.crawler.byexample.datastructure.support.ClusterScore
 * @see org.archive.crawler.byexample.constants.ScopeDecisionConstants
 * 
 * @author Michael Bendersky
 *
 */
public class DocumentClassificationEntry{
    private String url;
    private ClusterScore[] labeling;
    private double classificationRelevanceScore;
    
    /**
     * Default construct
     * @param url
     * @param labeling
     * @param relScore
     */
    public DocumentClassificationEntry(String url, ClusterScore[] labeling, double relScore){
        this.url=url;
        this.labeling=labeling;
        this.classificationRelevanceScore=relScore;
    }
    
    /**
     * String representation of this class
     */
    public String toString(){
        
        StringBuffer labelingString=new StringBuffer();
        for (int i = 0; i < labeling.length; i++) {
            labelingString.append(labeling[i].getClusterLabel()).
            append(OutputConstants.ENTRY_SEPARATOR).append(labeling[i].getClusterScore()).append(";");
        }
        
        return url+OutputConstants.KEY_SEPARATOR+labelingString+OutputConstants.KEY_SEPARATOR+classificationRelevanceScore;                           
    }

    public double getClassificationRelevanceScore() {
        return classificationRelevanceScore;
    }

    public ClusterScore[] getLabeling() {
        return labeling;
    }

    public String getUrl() {
        return url;
    }
    
} //END OF CLASS
