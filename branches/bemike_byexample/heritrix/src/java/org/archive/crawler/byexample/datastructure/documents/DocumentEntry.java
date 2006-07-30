package org.archive.crawler.byexample.datastructure.documents;

import org.archive.crawler.byexample.constants.OutputConstants;

/**
 * Datastructure class implementing record of crawled url features 
 * Features include:
 * <p>
 * - Page id
 * <p>
 * - Page url
 * <page>
 * - Page relevance flag (TRUE/FALSE)
 * 
 * @author Michael Bendersky
 *
 */
public class DocumentEntry{
    long id;
    String url;
    boolean isRelevant;
    
    /**
     * Default constructor
     * @param id
     * @param url
     * @param isAutoIn
     */
    public DocumentEntry(long id, String url, boolean isAutoIn){
        this.id=id;
        this.url=url;
        this.isRelevant=isAutoIn;
    }
    
    public String toString(){
        return id+OutputConstants.ENTRY_SEPARATOR+url+OutputConstants.ENTRY_SEPARATOR+isRelevant;
    }
    
    /**
     * @return page id
     */
    public long getId(){
        return this.id;
    }
    
    /**
     * 
     * @return page relevance flag (TRUE/FALSE)
     */
    public boolean isRelevant(){
        return isRelevant;
    }
}
