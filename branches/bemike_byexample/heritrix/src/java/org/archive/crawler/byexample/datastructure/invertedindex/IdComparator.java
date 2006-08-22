package org.archive.crawler.byexample.datastructure.invertedindex;

import java.util.Comparator;

/**
 * This class implements comparator between  
 * IndexEntry objects. According to this comparator,
 * IndexEntry objects will be sorted lexicographically by their Id's (in ascending order)  
 * 
 * @author Michael Bendersky
 *
 */
public class IdComparator implements Comparator<IndexEntry>{        
    public int compare(IndexEntry a, IndexEntry b) {
        if (a.getEntryId().compareTo(b.getEntryId())>0)
            return 1;
        if (a.getEntryId().compareTo(b.getEntryId())<0)
            return -1;
        return 0;                
    }
}
