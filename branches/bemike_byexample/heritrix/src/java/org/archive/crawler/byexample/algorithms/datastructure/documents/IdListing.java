package org.archive.crawler.byexample.algorithms.datastructure.documents;

import java.util.ArrayList;

public class IdListing{
    
        private ArrayList<String> idList;
        
        public IdListing(){
            idList=new ArrayList<String>();
        }
        
        public void addValueToRow(String value){
            idList.add(value);
        }
        
        public void addListToRow(IdListing aRow){
            idList.addAll(aRow.idList);
        }
        
        public void removeValueFromRow(String value){
            idList.remove(value);
        }
        
        public void removeAll(){
            idList.clear();
        }
        
        public String getValueAtPos(int i){
            return idList.get(i);
        }
                
        public String[] toArray(){
            String[] s=new String[idList.size()];
            return idList.toArray(s);
        }
        
        public int getRowSize(){
            return idList.size();
        }
        
        public String toString(){
            return idList.toString();
        }
} //END OF CLASS
