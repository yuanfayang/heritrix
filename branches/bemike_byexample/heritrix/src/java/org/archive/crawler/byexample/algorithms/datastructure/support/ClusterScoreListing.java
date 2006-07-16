package org.archive.crawler.byexample.algorithms.datastructure.support;

import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.Collections;

import org.archive.crawler.byexample.algorithms.datastructure.itemset.ItemSet;

public class ClusterScoreListing {
        
    private ArrayList<ClusterScore> scoreList;
    
    public ClusterScoreListing(){
        scoreList=new ArrayList<ClusterScore>();
    }
    
    public void addValueToRow(ClusterScore value){
        scoreList.add(value);
    }
    
    public int getListPos(ItemSet iS){
        for (int i = 0; i < getRowSize(); i++){
            if (scoreList.get(i).getClusterLabel().equals(iS))
                return i;
        }
        return -1;
    }
    
    public void addListToRow(ClusterScoreListing aRow){
        int itemPos=0;
        ClusterScore newScore=null;
        ClusterScore currScore=null;
        for (int i = 0; i < aRow.getRowSize(); i++) {
            newScore=aRow.scoreList.get(i);
            itemPos=getListPos(newScore.getClusterLabel());
            if (itemPos!=-1){
                currScore=this.scoreList.get(i);
                currScore.setClusterScore(currScore.getClusterScore()+newScore.getClusterScore());
            }
            else
                addValueToRow(newScore);
        }
    }
    
    public void sortListing(){
        Collections.sort(scoreList);
    }
    
    public ClusterScore[] getTopScores(int k){
        ClusterScore[] topScoresArray;
        if (scoreList.size()>k){
            topScoresArray=new ClusterScore[k];
            scoreList.subList(scoreList.size()-1-k,scoreList.size()-1).toArray(topScoresArray);           
        }
        else{
            topScoresArray=new ClusterScore[scoreList.size()];
            scoreList.toArray(topScoresArray);
        }
        return topScoresArray;
    }
    
    public void removeValueFromRow(ClusterScore value){
        scoreList.remove(value);
    }
    
    public void removeAll(){
        scoreList.clear();
    }
                
    public int getRowSize(){
        return scoreList.size();
    }
    
    public String toString(){
        return scoreList.toString();
    }
    
} // END OF CLASS
