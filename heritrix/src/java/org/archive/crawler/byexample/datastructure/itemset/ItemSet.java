package org.archive.crawler.byexample.datastructure.itemset;

import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

import org.archive.crawler.byexample.constants.OutputConstants;
import org.archive.crawler.byexample.utils.HashCodeUtils;

/**
 * Class that represents a set containing one or more items.
 * It can be used, for example, to represent a k-frequent item set.
 * 
 * @see org.archive.crawler.byexample.datastructure.itemset.FrequentItemSets
 * @author Michael Bendersky
 *
 */
public class ItemSet {
        SortedSet<String> itemSet;
        
        /**
         * Default constructor
         *
         */
        public ItemSet(){
            itemSet=new TreeSet<String>();
        }
        
        /**
         * Constructor based on another ItemSet
         * @param is ItemSet
         */
        public ItemSet(ItemSet is){
            itemSet=new TreeSet<String>();
            itemSet.addAll(is.itemSet);
        }
        
        /**
         * Constructor based on a single item
         * @param item String represenation of an item
         */
        public ItemSet(String item){
            itemSet=new TreeSet<String>();
            itemSet.add(item);
        }
        
        /**
         * Constructor based on items array
         * @param items String array representing items array
         */
        public ItemSet(String[] items){
            itemSet=new TreeSet<String>();
            for (int i=0; i<items.length; i++){
                itemSet.add(items[i]);   
            }                       
        }
        
        /**
         * Insert item to set
         * @param term String represenation of an item
         */
        public void insertToSet(String term){
            itemSet.add(term);
        }
        
        /**
         * Removes item from set
         * @param term String represenation of an item
         */
        public void removeFromSet(String term){
            itemSet.remove(term);
        }
        
        /**
         * @return all items in ItemSet as String array
         */
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
        
        /**
         * @return number of items in ItemSet
         */
        public int getSize(){
            return itemSet.size();
        }
        
        /**
         * Get item at given position in ItemSet
         * @param position item position
         * @return String representation of an item at the position
         */
        public String getItemAtPosition(int position){
            return getItems()[position];
        }
        
        /**
         * 
         * @return TRUE if ItemSet contains the given item, FALSE otherwise
         */
        public boolean contains(String item){
           return itemSet.contains(item);
        }
        
        /**
         * 
         * @return TURE if ItemSet contains all items in the given ItemSet, FALSE otherwise
         */
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
        
        /**
         * ItemSet String representation 
         */
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
        
        /**
         * Parses string into ItemSet.
         * For example string <code>{item1;item2;item3;}</code> will be parsed into ItemSet containing 3 items
         * @param s String to parse
         * @return ItemSet
         */
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
