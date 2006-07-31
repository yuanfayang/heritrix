package org.archive.crawler.byexample.datastructure.documents;

import java.io.BufferedWriter;
import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;

import org.archive.crawler.byexample.utils.FileUtils;

public class TopClassificationSet {
    private SortedSet<DocumentClassificationEntry> topClassificationSet;
    private int maxSize;
    private BufferedWriter dumpFile=null;
    
    /**
     * Default constructor 
     * @param c Comparator for set construction
     * @param maxSize set maximum allowed size
     * @param dumpFile BufferedWriter for dump file
     */
    public TopClassificationSet(Comparator c, int maxSize, BufferedWriter dumpFile){
        topClassificationSet=new TreeSet<DocumentClassificationEntry>(c);
        this.maxSize=maxSize; 
        this.dumpFile=dumpFile;
    }
    
    /**
     * Add set entry
     */
    public void addEntry(DocumentClassificationEntry dce){
        topClassificationSet.add(dce);
        //Makes sure set size never exceeds maxSize
        if (topClassificationSet.size()>maxSize)
            topClassificationSet.remove(topClassificationSet.last());                
    }
    
    /**
     * Write set to designated output file
     * @throws Exception
     */
    public void dumpListingToFile() throws Exception{
        //No dump file defined - do nothing
        if (dumpFile==null)
            return;
        
        StringBuffer dump=new StringBuffer();
        for (DocumentClassificationEntry dce : topClassificationSet) {
            dump.append(dce.toString()+"\n");
        }
        FileUtils.dumpBufferToFile(dumpFile,dump);  
    }
    
} // END OF CLASS
