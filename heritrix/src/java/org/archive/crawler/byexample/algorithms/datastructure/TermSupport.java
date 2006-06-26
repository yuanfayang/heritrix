package org.archive.crawler.byexample.algorithms.datastructure;

public class TermSupport{
    
    private String myTerm;
    private double mySupport;
    public static final String ENTRY_SEPARATOR=":";
    
    public TermSupport(String term, double support){
        myTerm=term;
        mySupport=support;
    }
    
    public void increaseSupport(){
        mySupport++;
    }
    
    public String getTerm(){
        return myTerm;
    }
    
    public String toString(){
        return myTerm+ENTRY_SEPARATOR+mySupport;
    }

    public double getSupport() {
        return mySupport;
    }

    public void setSupport(double mySupport) {
        this.mySupport = mySupport;
    }
}
