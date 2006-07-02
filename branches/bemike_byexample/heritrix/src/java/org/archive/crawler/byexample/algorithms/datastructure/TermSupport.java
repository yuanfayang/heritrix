package org.archive.crawler.byexample.algorithms.datastructure;

import org.archive.crawler.byexample.constants.OutputConstants;

public class TermSupport implements Comparable{
    
    private String myTerm;
    private double mySupport;
    
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
        return myTerm+OutputConstants.ENTRY_SEPARATOR+mySupport;
    }

    public double getSupport() {
        return mySupport;
    }

    public void setSupport(double mySupport) {
        this.mySupport = mySupport;
    }
    
    public int compareTo(Object anotherTS){
        if (this.mySupport>((TermSupport)anotherTS).mySupport)
            return 1;
        if (this.mySupport<((TermSupport)anotherTS).mySupport)
            return -1;
        return 0;
    }
}
