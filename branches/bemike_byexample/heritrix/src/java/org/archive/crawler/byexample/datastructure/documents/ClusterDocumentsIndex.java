package org.archive.crawler.byexample.datastructure.documents;

import java.io.BufferedWriter;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.archive.crawler.byexample.constants.OutputConstants;
import org.archive.crawler.byexample.datastructure.itemset.FrequentItemSets;
import org.archive.crawler.byexample.datastructure.itemset.ItemSet;
import org.archive.crawler.byexample.utils.FileUtils;

/**
 * Datastructure class providng high-level definition of a Cluster-Document Index.
 * This index is a mapping between ItemSet representing a cluster and IdListing representing a documents 
 * list in this cluster 
 * 
 * @see org.archive.crawler.byexample.datastructure.itemset.ItemSet
 * @see org.archive.crawler.byexample.datastructure.documents.IdListing
 * 
 * @author Michael Bendersky
 *
 */
public class ClusterDocumentsIndex {

    private Map<ItemSet,IdListing> myClusteringHash;
    
    /**
     * Default constructor
     *
     */
    public ClusterDocumentsIndex(){
        myClusteringHash=new ConcurrentHashMap<ItemSet,IdListing>();
    }
    
    /**
     * Add new key to the index
     * @param key ItemSet to add as a new key
     */
    public void addIndexKey(ItemSet key){
        myClusteringHash.put(key,new IdListing());
    }
    
    /**
     * Add a new value to the key.
     * If a key doesn't exist when this method is called, it is added.
     * @param key ItemSet to add as a new key
     * @param value String representing document id to add to cluster documents list
     */
    public void addIndexValue(ItemSet key, String value){
        if (!myClusteringHash.containsKey(key))
            addIndexKey(key);        
        myClusteringHash.get(key).addValue(value);       
    }
    
    /**
     * Returns iterator over index keys
     */
    public Iterator<ItemSet> getIndexKeysIterator(){
        return myClusteringHash.keySet().iterator();
    }
    
    /**
     * Returns string representation of this index
     */
    public String toString(){
        return myClusteringHash.toString();
    }
    
    /**
     * Get index row, representing all documents associated with a key
     * @param key ItemSet key representing a cluster
     * @return IdListing of all document id's associated with the key
     */
    public IdListing getRow(ItemSet key){
        return myClusteringHash.get(key);
    }
    
    /**
     * Removes empty rows from index
     * @return FrequentItemSets all removed ItemSets 
     */
    public FrequentItemSets removeEmptyRows(){
        FrequentItemSets removedIS=new FrequentItemSets();
        ItemSet currIS;        
        for (Iterator<ItemSet> iter = getIndexKeysIterator(); iter.hasNext();) {
            currIS=iter.next();
            if (getRow(currIS).getListSize()==0){
                myClusteringHash.remove(currIS);
                removedIS.insertToSet(currIS);
            }
        }
        return removedIS;
    }
    
    /**
     * 
     * @return index size
     */
    public int getSize(){
        return myClusteringHash.size();
    }
    
    /**
     * Write index to designated output file
     * @param bw BufferedWriter for the output file
     * @throws Exception
     */
    public void dumpIndexToFile(BufferedWriter bw)throws Exception{
        StringBuffer dump=new StringBuffer();
        ItemSet currKey;
        for (Iterator<ItemSet> iter = getIndexKeysIterator(); iter.hasNext();) {
            currKey=iter.next();
            dump.append(currKey);
            dump.append(OutputConstants.KEY_SEPARATOR);
            dump.append(getRow(currKey).toString());
            dump.append("\n");
        }
        FileUtils.dumpBufferToFile(bw,dump);        
    }   
    
} //END OF CLASS
