package org.archive.crawler.byexample.datastructure.invertedindex;

import java.util.Comparator;

public class IdComparator implements Comparator<IndexEntry>{        
    public int compare(IndexEntry a, IndexEntry b) {
        if (a.getEntryId().compareTo(b.getEntryId())>0)
            return 1;
        if (a.getEntryId().compareTo(b.getEntryId())<0)
            return -1;
        return 0;                
    }
}
