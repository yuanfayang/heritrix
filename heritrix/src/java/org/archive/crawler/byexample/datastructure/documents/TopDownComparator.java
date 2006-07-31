package org.archive.crawler.byexample.datastructure.documents;

import java.util.Comparator;

/**
 * This class implements top-down comparator between 
 * DocumentClassificationEntry objects. List sorted by this comparator
 * will be sorted in descending order.
 * 
 * @author Michael Bendersky
 *
 */
public class TopDownComparator implements Comparator<DocumentClassificationEntry>{
    public int compare(DocumentClassificationEntry a, DocumentClassificationEntry b) {
        if (a.getClassificationRelevanceScore()>b.getClassificationRelevanceScore())
            return -1;
        if (a.getClassificationRelevanceScore()<b.getClassificationRelevanceScore())
            return 1;
        return 0;                
    }
}
