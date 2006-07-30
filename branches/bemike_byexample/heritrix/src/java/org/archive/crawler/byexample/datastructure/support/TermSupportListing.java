package org.archive.crawler.byexample.datastructure.support;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.archive.crawler.byexample.constants.OutputConstants;


public class TermSupportListing{
    private ArrayList<TermSupport> termList;
    
    public TermSupportListing(){
        termList=new ArrayList<TermSupport>();
    }
    
    public void insertInitialTerms(List<TermSupport> terms){
        for (TermSupport iter : terms) {
            addValueToRow(new TermSupport(iter.getTerm(),0));
        }
    }
    
    public void addValueToRow(TermSupport value){
        termList.add(value);
    }
    
    public String toString(){
        return termList.toString();
    }
    
    public String termsToString(){
        StringBuffer sb=new StringBuffer();
        for (TermSupport ts : termList) {
            sb.append(ts.getTerm());
            sb.append(OutputConstants.LIST_SEPARATOR);
        }
        return sb.toString();
    }
    
    public Iterator<TermSupport> getTermsIterator(){
        return termList.iterator();
    }
    
    public TermSupport getEntry(String term){
        for (TermSupport se : termList) {
            if (se.getTerm().equals(term))
                return se;                    
        }
        return null;
    }
    
    public TermSupport getEntryAtIndex(int i){
        return termList.get(i);
    }
    
    /**
     * Truncates all terms in both rows with support less than given percentage.
     * Afterwards, merges the other row with this row.
     * @param percentage minimal support
     * @param otherRow SupportRow to merge 
     * @param thisClusterSize size of the current cluster
     * @param otherClusterSize size of the cluster to merge
     */
    public void truncateAndMerge(int percentage, TermSupportListing otherRow, int thisClusterSize, int otherClusterSize){
        this.truncateByPercentage(percentage);
        otherRow.truncateByPercentage(percentage);
        for (TermSupport i : this.termList) {
            for (TermSupport j : otherRow.termList) {
                //Merge identical terms support
                if (i.getTerm().equals(j.getTerm()))
                    i.setSupport(
                            (i.getSupport()*thisClusterSize+j.getSupport()*otherClusterSize)/
                            (thisClusterSize+otherClusterSize));
            }
        }
    }
    
    /**
     * Truncates all terms in a row with support less than given percentage
     */
    public void truncateByPercentage(int percentage){
        TermSupport currSup=null;    
        ArrayList<TermSupport> toRemove=new ArrayList<TermSupport>();
        int i=0;
        while(i<termList.size()){
            currSup=termList.get(i);
            if (currSup.getSupport()<((double)percentage/100))
                toRemove.add(currSup);
            i++;              
        }
        termList.removeAll(toRemove);
    }
    
    public int getSize(){
        return termList.size();
    }
    
} //END OF CLASS
