package org.archive.crawler.byexample.algorithms.datastructure.documents;

import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.logging.Logger;
import org.archive.crawler.byexample.algorithms.datastructure.support.ClusterScore;
import org.archive.crawler.byexample.constants.OutputConstants;
import org.archive.crawler.byexample.constants.ScopeDecisionConstants;
import org.archive.crawler.byexample.utils.FileUtils;

public class ClassificationDocumentsListing{
    
    public class DocumentClassification{
        String url;
        ClusterScore[] labeling;
        ScopeDecisionConstants scoping;
        
        public DocumentClassification(String url, ClusterScore[] labeling, ScopeDecisionConstants scoping){
            this.url=url;
            this.labeling=labeling;
            this.scoping=scoping;
        }
        
        public String toString(){
            
            StringBuffer labelingString=new StringBuffer();
            for (int i = 0; i < labeling.length; i++) {
                labelingString.append(labeling[i].getClusterLabel()).
                append(OutputConstants.ENTRY_SEPARATOR).append(labeling[i].getClusterScore()).append(";");
            }
            
            return url+OutputConstants.KEY_SEPARATOR+labelingString+OutputConstants.KEY_SEPARATOR+scoping;           
                    
        }
    }
    
    private ArrayList<DocumentClassification> docClasses;
    private static int MAX_ENTRIES_IN_MEMORY=1000;
    BufferedWriter dumpFile=null;
    private static Logger logger =
        Logger.getLogger(ClassificationDocumentsListing.class.getName());
    
    
    public ClassificationDocumentsListing(BufferedWriter bw) throws Exception{
        docClasses=new ArrayList<DocumentClassification>();
        dumpFile=bw;
    }
    
    public void addClassification(String crawledURL, ClusterScore[] scores, ScopeDecisionConstants scopeResult){        
        if (docClasses.size()>MAX_ENTRIES_IN_MEMORY && dumpFile!=null){
            try {
                dumpListingToFile();
                docClasses.clear();
            } catch (Exception e) {
                logger.info("Could not dump documents list from memory to file...");
            }
        }          
        docClasses.add(new DocumentClassification(crawledURL,scores,scopeResult));
    }
    
    public void dumpListingToFile() throws Exception{
        //No dump file defined - do nothing
        if (dumpFile==null)
            return;
        
        StringBuffer dump=new StringBuffer();
        for (DocumentClassification currKey : docClasses) {            
            dump.append(currKey.toString()+"\n");
        }
        FileUtils.dumpBufferToFile(dumpFile,dump);  
    }
    
    public String toString(){
        return docClasses.toString();
    }
   

    
} //END OF CLASS
