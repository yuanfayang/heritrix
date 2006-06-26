package org.archive.crawler.byexample.algorithms.preprocessing;

import java.io.BufferedReader;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.archive.crawler.byexample.utils.FileHandler;


public class StopWordsHandler {
    
    //TO DO: take the value from some configuration file
    public static final String stopWordsFilePath="D:\\Java Projects/ArchiveOpenCrawler/src/java/org/archive/crawler/byexample/stopwords.txt";
    private Set<String> stopWordSet; 
    
    public StopWordsHandler() throws Exception{
        stopWordSet=createStopWordSet();
    }
    
    /**
     * Load the specified words list from a file into memory
     */
    public Set<String> createStopWordSet() throws Exception{
        Set<String> stopWordsHash=Collections.synchronizedSet(new HashSet<String>());
      
        BufferedReader in=FileHandler.readBufferFromFile(stopWordsFilePath);
        String iter = in.readLine();
        
        //Stopwords file is empty
        if (in==null) 
            return null;
        
        while (!(iter==null)){         
            //Check for comments and blank lines
            if (!iter.equals("")){
                if (iter.charAt(0)!='#')
                    stopWordsHash.add(iter);
            }
           iter=in.readLine();
        }
        
        in.close();
        
        return stopWordsHash;
            
    }
    
    /**
     * Get stopWordSet instance
     * @return
     */
    public Set<String> getStopWordSet() {
        return stopWordSet;
    }
    
    public boolean isStopWord(String word){
        return stopWordSet.contains(word);        
    }
   
    
} //END OF CLASS
