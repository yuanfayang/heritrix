package org.archive.crawler.byexample.algorithms.datastructure.itemset;

import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

import org.archive.crawler.byexample.constants.OutputConstants;
import org.archive.crawler.byexample.utils.HashCodeUtils;

public class ItemSet {
        SortedSet<String> itemSet;
        
        public ItemSet(){
            itemSet=new TreeSet<String>();
        }
        
        public ItemSet(ItemSet is){
            itemSet=new TreeSet<String>();
            itemSet.addAll(is.itemSet);
        }
        
        public ItemSet(String item){
            itemSet=new TreeSet<String>();
            itemSet.add(item);
        }
        
        public ItemSet(String[] items){
            itemSet=new TreeSet<String>();
            for (int i=0; i<items.length; i++){
                itemSet.add(items[i]);   
            }                       
        }
        
        public void insertToSet(String term){
            itemSet.add(term);
        }
        
        public void removeFromSet(String term){
            itemSet.remove(term);
        }
        
        
        public String[] getItems(){  
            
            //Return null for empty item set
            if (itemSet.size()==0)
                return null;
            
            int i=0;
            String[] items =new String[itemSet.size()];
            for (String s : itemSet) {
                items[i]=s;
                i++;
            }
            return items; 
        }
        
        public int getSize(){
            return itemSet.size();
        }
        
        public String getItemAtPosition(int position){
            return getItems()[position];
        }
        
        public boolean contains(String item){
           return itemSet.contains(item);
        }
        
        public boolean contains(ItemSet is) {
            short countContains=0;
            if (is.getSize()>this.getSize())
                return false;
            for (String s1 : this.itemSet) {
               for (String s2: is.itemSet){
                   if (s1.equals(s2))
                       countContains++;
               }
            }
            if (countContains==is.getSize())
                return true;
            return false;
        }
        
        public boolean equals(Object otherObj){
            if ( this == otherObj ) return true;
            if ( !(otherObj instanceof ItemSet) ) return false;
            
            ItemSet other=(ItemSet)otherObj;
            Iterator<String> iter1=this.itemSet.iterator();
            Iterator<String> iter2=other.itemSet.iterator();            
            //Find non-matchning items
            while (iter1.hasNext() && iter2.hasNext()){
                //Found non-equal items
                if (!iter1.next().equals(iter2.next()))
                    return false;                
            }
            //No non-matching items found and sets are of equal size
            if (!iter1.hasNext()&&!iter2.hasNext())
                return true;
            //No non-matching items found but one set is larger than other 
            return false;
        }
        
        public int hashCode(){
            int result = HashCodeUtils.SEED;
            String[] allItems=getItems();
            String separator=OutputConstants.ENTRY_SEPARATOR;
            for (int i = 0; i < allItems.length; i++) {
                result = HashCodeUtils.hash(result, allItems[i]);
                result = HashCodeUtils.hash(result, separator);
            }
            return result;            
        }
        
        public String toString(){
            StringBuffer sb=new StringBuffer();
            sb.append("{");
            for (String s : getItems()) {
                sb.append(s);
                sb.append(OutputConstants.LIST_SEPARATOR);
            }
            sb.append("}");
            return sb.toString();
        }
        
        public static ItemSet createfromString(String s){
            s=s.substring(1,s.length()-1);
            String[] items=s.split(";");
            ItemSet is=new ItemSet();
            for (int i=0; i<items.length; i++){
                is.insertToSet(items[i]);   
            }  
            return is;
        }

} //END OF CLASS
