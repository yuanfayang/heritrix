package org.archive.crawler.byexample.datastructure.documents;

import org.archive.crawler.byexample.constants.OutputConstants;
import org.archive.crawler.byexample.constants.ScopeDecisionConstants;
import org.archive.crawler.byexample.datastructure.support.ClusterScore;

/**
 *  Datastructure class implementing document classification record.
 *  Structure of the record is as follows:
 * <p>
 * - URL
 * <p>
 * - Array of ClusterScores for each possible cluster of the url
 * <p>
 * - ScopeDecisionConstant indicating the scoping decision for the url
 * 
 * @see org.archive.crawler.byexample.datastructure.support.ClusterScore
 * @see org.archive.crawler.byexample.constants.ScopeDecisionConstants
 * 
 * @author Michael Bendersky
 *
 */
public class DocumentClassification{
    String url;
    ClusterScore[] labeling;
    ScopeDecisionConstants scoping;
    
    /**
     * Default construct
     * @param url
     * @param labeling
     * @param scoping
     */
    public DocumentClassification(String url, ClusterScore[] labeling, ScopeDecisionConstants scoping){
        this.url=url;
        this.labeling=labeling;
        this.scoping=scoping;
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
        
        return url+OutputConstants.KEY_SEPARATOR+labelingString+OutputConstants.KEY_SEPARATOR+scoping;                           
    }
    
} //END OF CLASS
