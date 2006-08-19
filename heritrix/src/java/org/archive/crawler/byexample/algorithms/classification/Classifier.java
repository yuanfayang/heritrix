package org.archive.crawler.byexample.algorithms.classification;

import org.archive.crawler.byexample.algorithms.preprocessing.PorterStemmer;
import org.archive.crawler.byexample.algorithms.preprocessing.StopWordsHandler;
import org.archive.crawler.byexample.constants.ByExampleProperties;
import org.archive.crawler.byexample.datastructure.documents.DocumentClassificationEntry;
import org.archive.crawler.byexample.datastructure.info.ClusteringInfo;
import org.archive.crawler.byexample.datastructure.itemset.ItemSet;
import org.archive.crawler.byexample.datastructure.support.ClusterScore;
import org.archive.crawler.byexample.datastructure.support.ClusterScoreListing;
import org.archive.crawler.byexample.datastructure.support.ClusterSupportIndex;
import org.archive.crawler.byexample.datastructure.support.TermSupportIndex;
import org.archive.crawler.byexample.utils.ParseUtils;

/**
 * This class implements classification algorirhm used by ClassifierProcessor
 * 
 * @see org.archive.crawler.byexample.processors.ClassifierProcessor
 * 
 * @author Michael Bendersky
 * 
 */
public class Classifier {

    // Term Support index on which classification is based
    private TermSupportIndex myTermsIndex;

    // Clustering relevance info
    private ClusteringInfo myRelevanceInfo;

    // Clusters with score below MIN_TERM_SCORE won't be associated with the
    // page
    private static final double MIN_TERM_SCORE = 0.01;

    /**
     * Default constructor
     * 
     * @param csi
     *            ClusterSupportIndex on which classification is based
     */
    public Classifier(ClusterSupportIndex csi, ClusteringInfo info) {
        myTermsIndex = new TermSupportIndex();
        myTermsIndex.fromCSI(csi);

        myRelevanceInfo = info;
    }

    /**
     * Parses CharSequence stream into array of string
     * 
     * @param cs
     *            CharSequence to parse
     * @return Strings array
     */
    public synchronized String[] parsePage(CharSequence cs){
        return ParseUtils.tokenizer(cs);
    }

    /**
     * Normalizes cluster scores to be in [0,1] range and removes loosely
     * connected clusters
     * 
     * @param scores
     *            ClusterScore array of scores to normalize
     * @return ClusterScore array with normalized scores
     */
    public synchronized ClusterScore[] normalizeRelevanceScores(
            ClusterScore[] scores) {
        ClusterScore[] unclassified = new ClusterScore[1];
        ClusterScore[] normalizedScores = new ClusterScore[scores.length];
        unclassified[0] = new ClusterScore(new ItemSet(
                ByExampleProperties.UNCLASSIFIED_LABEL), 1);
        double total = 0;

        for (int i = 0; i < scores.length; i++)
            total += scores[i].getClusterScore();
        if (total == 0)
            return unclassified;
        int j = 0;
        for (int i = 0; i < scores.length; i++) {
            double newScore = scores[i].getClusterScore() / total;
            // Remove loosely connected scores
            if (newScore > MIN_TERM_SCORE)
                normalizedScores[j++] = new ClusterScore(scores[i]
                        .getClusterLabel(), newScore);
        }

        ClusterScore[] resizedScoreArray = new ClusterScore[j];
        System.arraycopy(normalizedScores, 0, resizedScoreArray, 0, j);

        return resizedScoreArray;
    }

    /**
     * Classifies a given page. Classification contains following steps:
     * <p>
     * 1. Page parsing into tokens
     * <p>
     * 2. Stop words removal and stemming
     * <p>
     * 3. Assigning score to each cluster based on cluster-frequent terms
     * <p>
     * 4. Returning DocumentClassificationEntry object for the document
     * 
     * @see org.archive.crawler.byexample.constants.ByExampleProperties
     * @see org.archive.crawler.byexample.datastructure.documents.DocumentClassificationEntry
     * 
     * @param cs -
     *            CharSequence stream
     * @param swh -
     *            StopWordsHandler instance
     * @param stemmer -
     *            PorterStemmer instance
     * @return ClusterScore[] Array containing assignment probabilities
     */
    public synchronized DocumentClassificationEntry classify(String url,
            CharSequence cs, StopWordsHandler swh, PorterStemmer stemmer){
        String[] allTerms = parsePage(cs);
        String iter;
        ClusterScoreListing pageScores = new ClusterScoreListing();
        ClusterScoreListing termScores = null;

        // Process parsed page
        if (allTerms != null) {
            for (int i = 0; i < allTerms.length; i++) {
                iter = allTerms[i];
                // Ignore words with 2 or less characters and pre-defined stop
                // words
                if (iter.length() < 3 || swh.isStopWord(iter))
                    continue;
                // Stem the term
                iter = stemmer.stem(iter);
                // Get list of cluster scores for the term
                termScores = myTermsIndex.getRow(iter);
                // Add term score to term scores
                if (termScores != null)
                    pageScores.addListToRow(termScores);
            }
        }

        pageScores.sortListing();
        ClusterScore[] topScores = pageScores
                .getTopScores(ByExampleProperties.TOP_CLASSIFICATIONS);
        return new DocumentClassificationEntry(url,
                normalizeRelevanceScores(topScores),
                getRelevanceScore(topScores));
    }

    /**
     * Return relevance score for a document based on its top cluster scores.
     * Relevance score for a document is a weighted sum of it's cluster scores.
     * Cluster weight is determined by its relevance score 
     * 
     * @param cs
     *            ClusterScore[] - top cluster scores
     * @return relevance score for a document
     */
    public synchronized double getRelevanceScore(ClusterScore[] cs) {
        double relevanceScore = 0;        
        for (int i = 0; i < cs.length; i++) {       
            // Increase document relevance score for each cluster, 
            // according to cluster relevance
            relevanceScore += cs[i].getClusterScore()*
            myRelevanceInfo.getClusterRelevance(cs[i].getClusterLabel());
        }
        return relevanceScore;
    }

} // END OF CLASS
