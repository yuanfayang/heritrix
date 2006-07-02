package org.archive.crawler.byexample.algorithms.datastructure;

import org.archive.crawler.byexample.constants.OutputConstants;

public class ClusterScore{
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
        return clusterLabel.toString()+OutputConstants.ENTRY_SEPARATOR+clusterScore;
    }
    
}