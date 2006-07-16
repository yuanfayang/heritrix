package org.archive.crawler.byexample.algorithms.preprocessing;

import java.io.BufferedReader;
import java.util.Collections;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.archive.crawler.byexample.constants.OutputConstants;
import org.archive.crawler.byexample.utils.FileUtils;

/**
 * Loads file containing common stop words into memory
 * During pre-processing terms are compared against this file and stop-words are removed 
 * <p>
 * Stop words file resides under $HERITRIX_HOME/BYEXAMPLE_HOME/CONFIG_HOME directory 
 * and can be edited to add/remove/change stop-words.
 * <p>
 * Initial list of words was taken from a research paper: 
 * <p>
 * <i>Fox, Christopher, "A Stop List for General Text", SIGIR Forum, v 24, n 1-2, Fall 89/Winter 90, p 19-35</i>
 * 
 * @author Michael Bendersky
 */
public class StopWordsHandler {
    
    /**
     * Stop words file location
     */
    public static final String stopWordsFilePath=OutputConstants.CONFIG_HOME+OutputConstants.STOP_WORDS_FILENAME;
    
    private SortedSet<String> stopWordSet; 
    
    /**
     * Default constructor
     * @throws Exception if stop words file cannot be loaded
     */
    public StopWordsHandler() throws Exception{
        stopWordSet=createStopWordSet();
    }
    
    /**
     * Load the specified words list from a file into memory
     */
    public SortedSet<String> createStopWordSet() throws Exception{
        SortedSet<String> stopWordsHash=Collections.synchronizedSortedSet(new TreeSet<String>());
      
        BufferedReader in=FileUtils.readBufferFromFile(stopWordsFilePath);
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
     */
    public Set<String> getStopWordSet() {
        return stopWordSet;
    }
    
    /**
     * Determines whether a word is a stop-word.
     * As StopWords set is sorted in ascending order, lookup takes O(log(stop-words-set-size)) 
     * 
     * @param word
     * @return TRUE if word is a stop-word, FALSE else
     */
    public boolean isStopWord(String word){
        return stopWordSet.contains(word);        
    }
   
    
} //END OF CLASS
