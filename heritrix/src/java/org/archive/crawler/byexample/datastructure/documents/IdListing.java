package org.archive.crawler.byexample.datastructure.documents;

import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.logging.Logger;

import org.archive.crawler.byexample.constants.OutputConstants;
import org.archive.crawler.byexample.utils.FileUtils;

/**
 * Datastructure class providing implementation of simple ID's list 
 * Each ID represents some identification String value.
 * @author Michael Bendersky
 *
 */
public class IdListing{
    
        private ArrayList<String> idList;
        BufferedWriter dumpFile=null;
        private static Logger logger =
            Logger.getLogger(IdListing.class.getName());
        
        /**
         * Default constructor
         *
         */
        public IdListing(){
            idList=new ArrayList<String>();
        }
        
        /**
         * Constructor with specified output file
         * @param bw BufferedWriter for the output file
         */
        public IdListing(BufferedWriter bw){
            idList=new ArrayList<String>();
            dumpFile=bw;
        }
        
        /**
         * Check if list size is above OutputConstants.MAX_ENTRIES_IN_MEMORY
         *       
         */
        public void checkMaxSize(){
            if (idList.size()>OutputConstants.MAX_ENTRIES_IN_MEMORY && dumpFile!=null){
                try {
                    dumpListingToFile();
                    idList.clear();
                } catch (Exception e) {
                    logger.info("Could not dump documents list from memory to file...");
                }
            }
        }
        
        /**
         * Add value to list
         * @param value 
         */
        public void addValue(String value){            
            checkMaxSize();                        
            idList.add(value);
        }
        
        /**
         * Merge two lists
         * @param aRow
         */
        public void addList(IdListing aRow){
            checkMaxSize();   
            idList.addAll(aRow.idList);
        }
        
        /**
         * Remove value from list
         * @param value
         */
        public void removeValue(String value){
            idList.remove(value);
        }
        
        /**
         * Clear the entire list
         *
         */
        public void removeAll(){
            idList.clear();
        }
        
        /**
         * Get value at position <i>i</i>
         */
        public String getValueAtPos(int i){
            return idList.get(i);
        }
        
        /**
         * 
         * @return String[] representation of this list
         */
        public String[] toArray(){
            String[] s=new String[idList.size()];
            return idList.toArray(s);
        }
        
        /**
         * @return list size
         */
        public int getListSize(){
            return idList.size();
        }
        
        public String toString(){
            return idList.toString();
        }
        
        /**
         * Write listing to designated output file
         * @throws Exception
         */
        public void dumpListingToFile() throws Exception{
            //No dump file defined - do nothing
            if (dumpFile==null)
                return;
            
            StringBuffer dump=new StringBuffer();
            for (String currKey : idList) {            
                dump.append(currKey+"\n");
            }
            FileUtils.dumpBufferToFile(dumpFile,dump);  
        }
        
        
        
} //END OF CLASS
