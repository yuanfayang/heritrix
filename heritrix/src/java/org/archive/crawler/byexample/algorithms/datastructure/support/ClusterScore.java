package org.archive.crawler.byexample.algorithms.datastructure.support;

import org.archive.crawler.byexample.algorithms.datastructure.itemset.ItemSet;
import org.archive.crawler.byexample.constants.OutputConstants;

public class ClusterScore implements Comparable{
    private ItemSet clusterLabel;
    private double clusterScore;
    
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
        return clusterLabel.toString()+OutputConstants.ENTRY_SEPARATOR+clusterScore;
    }
    
    public int compareTo(Object that){
        ClusterScore otherCS;
        if (that instanceof ClusterScore)
            otherCS = (ClusterScore)that;
        else
            return -1;
        if (this.clusterScore>otherCS.clusterScore)
            return 1;
        if (this.clusterScore<otherCS.clusterScore)
            return -1;
        else
            return 0;       
    }
    
} //END OF CLASS