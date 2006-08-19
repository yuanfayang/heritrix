package org.archive.crawler.byexample.algorithms.clustering.fihc;


import java.util.Iterator;
import java.util.List;

import org.archive.crawler.byexample.algorithms.tfidf.DocumentIndexManipulator;
import org.archive.crawler.byexample.constants.ByExampleProperties;
import org.archive.crawler.byexample.datastructure.documents.ClusterDocumentsIndex;
import org.archive.crawler.byexample.datastructure.documents.DocumentListing;
import org.archive.crawler.byexample.datastructure.documents.IdListing;
import org.archive.crawler.byexample.datastructure.info.ClusteringInfo;
import org.archive.crawler.byexample.datastructure.itemset.FrequentItemSets;
import org.archive.crawler.byexample.datastructure.itemset.ItemSet;
import org.archive.crawler.byexample.datastructure.support.ClusterSupportIndex;
import org.archive.crawler.byexample.datastructure.support.TermSupport;
import org.archive.crawler.byexample.utils.TimerUtils;

/**
 * Receives as input clustering structure created by StructureBuilder, prunes
 * and merges redundant clusters and builds clustering structure including
 * relevance assignment to each cluster
 * 
 * @author Michael Bendersky
 * 
 */
public class TreeBuilder {

    private ClusterDocumentsIndex myDocumentClusteringIndex;

    private ClusterSupportIndex myClusterSupportIndex;

    private DocumentIndexManipulator myTFIDFIndex;

    private List<TermSupport> myGlobalSupportIndex;

    private DocumentListing allDocs;

    private long myDocCount;

    /**
     * Default constructor
     */
    public TreeBuilder(long docCount, DocumentListing dl,
            ClusterDocumentsIndex cdi, DocumentIndexManipulator mti,
            List<TermSupport> gsi, ClusterSupportIndex csi) {
        myDocumentClusteringIndex = cdi;
        myTFIDFIndex = mti;
        myGlobalSupportIndex = gsi;
        myClusterSupportIndex = csi;
        myDocCount = docCount;
        allDocs = dl;
    }

    /**
     * Merges most similar clusters between different levels, if clusters size
     * is less than MIN_SIZE_TO_PRUNE
     * 
     * @see org.archive.crawler.byexample.datastructure.ByExampleProperties
     * @param fis
     *            Tree levels structure
     */
    public void mergeLevels(FrequentItemSets[] fis) {

        if (fis.length < 2)
            return;

        ItemSet currParent = null;
        ItemSet currChild = null;
        ItemSet tempParent = null;
        int childClusterSize = 0;
        double currScore = 0;
        double tempScore = 0;
        long sizeToPrune = Math.round(myDocCount
                * ((double) ByExampleProperties.MIN_SIZE_TO_PRUNE / 100));
        for (Iterator<ItemSet> iter1 = fis[fis.length - 1].getSetsIterator(); iter1
                .hasNext();) {
            currChild = iter1.next();
            childClusterSize = myDocumentClusteringIndex.getRow(currChild)
                    .getListSize();
            // Merge all non-empty children of small size with their parents
            if (childClusterSize > 0 && childClusterSize < sizeToPrune) {
                for (Iterator<ItemSet> iter2 = fis[fis.length - 2]
                        .getSetsIterator(); iter2.hasNext();) {
                    currParent = iter2.next();
                    // No parent is still chosen
                    if (tempParent == null)
                        tempParent = currParent;
                    if (currChild.contains(currParent)) {
                        currScore = ScoreComputation.interClusterSimilarity(
                                currParent, currChild,
                                myDocumentClusteringIndex,
                                myClusterSupportIndex, myTFIDFIndex,
                                myGlobalSupportIndex);
                        if (currScore >= tempScore) {
                            tempScore = currScore;
                            tempParent = currParent;
                        }
                    }
                }
                myDocumentClusteringIndex.getRow(tempParent).addList(
                        myDocumentClusteringIndex.getRow(currChild));
                myDocumentClusteringIndex.getRow(currChild).removeAll();
                myClusterSupportIndex.getRow(tempParent).truncateAndMerge(
                        ByExampleProperties.MIN_CLUSTER_SUPPORT,
                        myClusterSupportIndex.getRow(currChild),
                        myDocumentClusteringIndex.getRow(tempParent)
                                .getListSize(),
                        myDocumentClusteringIndex.getRow(currChild)
                                .getListSize());
                myClusterSupportIndex.removeRow(currChild);
            }
        }

        FrequentItemSets[] newFIS = new FrequentItemSets[fis.length - 1];
        System.arraycopy(fis, 0, newFIS, 0, fis.length - 1);
        mergeLevels(newFIS);
    }

    /**
     * Removes empty clusters and terms that have no minimal support in clusters
     */
    public void pruneLevels() {
        FrequentItemSets removedIS = myDocumentClusteringIndex
                .removeEmptyRows();
        for (Iterator<ItemSet> iter = removedIS.getSetsIterator(); iter
                .hasNext();) {
            ItemSet is = iter.next();
            myClusterSupportIndex.removeRow(is);
        }
        myClusterSupportIndex
                .truncateAllRows(ByExampleProperties.MIN_CLUSTER_SUPPORT);
    }

    /**
     * Determines the "relevance score" for a cluster.
     * Cluter relevance score is determined by a ration of relevant documents inside the cluster.
     * @param clusterDocs
     *            IdListing of all cluster documents
     * @return double cluster relevance score.
     */
    public double determineClusterRelevance(IdListing clusterDocs) {
        double relevanceRatio = 0;

        for (int i = 0; i < clusterDocs.getListSize(); i++) {
            // Count all relevant documents
            if (allDocs.getEntryAtPos(
                    Integer.parseInt(clusterDocs.getValueAtPos(i)))
                    .isRelevant())
                relevanceRatio++;
        }

       return (relevanceRatio /= clusterDocs.getListSize());
    }

    /**
     * Writes clustering structure into XML
     * 
     * @param path
     *            file path
     * @param filename
     *            file name
     */
    public void createClusteringInfoOutput(String path, String filename,
            ClusteringInfo info){
        ItemSet currIS = null;
        String assocTerms = null;

        for (Iterator<ItemSet> iter = myDocumentClusteringIndex
                .getIndexKeysIterator(); iter.hasNext();) {
            currIS = iter.next();
            assocTerms = myClusterSupportIndex.getRow(currIS).termsToString();
            info.addCluster(currIS, myDocumentClusteringIndex.getRow(currIS)
                    .getListSize(),
                    determineClusterRelevance(myDocumentClusteringIndex
                            .getRow(currIS)), assocTerms);
        }
        
        info.toXML(path, filename);
    }

    /**
     * Builds clustering tree, prunes and merges levels. Outputs resulting tree
     * as an XML file and reports invocation times for each step. This is the
     * only method that should be invoked by outside classes
     * 
     * @param path
     *            file path
     * @param filename
     *            file name
     * @param fis
     *            FrequenItemSet based on which the tree will be built
     */
    public void buildTree(String path, String filename, FrequentItemSets fis,
            ClusteringInfo info){
        TimerUtils myTH = new TimerUtils();

        myTH.startTimer();
        mergeLevels(fis.splitLevelsByK(ByExampleProperties.MAX_DEPTH));
        myTH.reportActionTimer("MERGING SMALL CLUSTERS");

        myTH.startTimer();
        pruneLevels();
        myTH.reportActionTimer("PRUNING EMPTY CLUSTERS");

        myTH.startTimer();
        createClusteringInfoOutput(path, filename, info);
        myTH.reportActionTimer("CREATING CLUSTERING OUTPUT XML FILE");
    }

} //END OF CLASS
