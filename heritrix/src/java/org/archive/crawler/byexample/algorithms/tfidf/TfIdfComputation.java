package org.archive.crawler.byexample.algorithms.tfidf;


public class TfIdfComputation {
    
    /**
     * Returns TF according to formula:
     * 1+log(1+log(num_of_occurences(doc,term))
     * 
     * @param doc
     * @param term
     * @param index
     */
    public static double computeTF(double numOccursInDoc){
        return 1+Math.log(1+Math.log(numOccursInDoc));        
    }
    
    
    /**
     * Returns IDF according to formula:
     * log((1+num_of_term_documents)/total_num_of_documents)
     * 
     * @param dtSize
     * @param dSize
     * @return
     */
    public static double computeIDF(int dtSize, long dSize){
        return Math.log((double)(1+dSize)/(double)dtSize);
    }
    
    /**
     * Returns combined TF*IDF rank, according to TF and IDF formulas
     * 
     * @param numOccursInDoc
     * @param dtSize
     * @param dSize
     * @return
     */
    public static double computeTFIDF(double numOccursInDoc, int dtSize, long dSize){
        return computeTF(numOccursInDoc)*computeIDF(dtSize,dSize);
    }
    
    /**
     * Returns cosine distance between lexicographically sorted string arrays a and b according to formula:
     * (Number of common strings to arrays a and b)/(length(a)*length(b)) 
     * 
     * @param a - Lexicographically sorted string array
     * @param b - Lexicographically sorted string array
     * @return
     */
    public static double computeCosineDistance(String[] a, String[] b){
        int i=0,j=0, commonSum=0;
        while (i<a.length && j<b.length){
            if(a[i].compareTo(b[j])>0){
                j++;
                continue;
            }
            if (a[i].compareTo(b[j])<0){
                i++;
                continue;
            }
            commonSum++;
            i++;
            j++;
        }
        return commonSum/(a.length*b.length);
    }
}
