package org.archive.crawler.byexample.datastructure.invertedindex;

import java.util.Comparator;

/**
 * This class implements comparator between  
 * IndexEntry objects. According to this comparator,
 * IndexEntry objects will be sorted in ascending order by their values  
 * 
 * @author Michael Bendersky
 *
 */
public class ValueComparator implements Comparator<IndexEntry>{        
    public int compare(IndexEntry a, IndexEntry b) {
        if (a.getEntryValue()>a.getEntryValue())
            return 1;
        if (a.getEntryValue()<b.getEntryValue())
            return -1;
        return 0;                
    }
}

