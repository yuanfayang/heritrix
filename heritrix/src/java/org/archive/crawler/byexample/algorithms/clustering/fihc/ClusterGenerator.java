package org.archive.crawler.byexample.algorithms.clustering.fihc;

import java.util.List;

import org.archive.crawler.byexample.algorithms.datastructure.documents.ClusterDocumentsIndex;
import org.archive.crawler.byexample.algorithms.datastructure.documents.DocumentListing;
import org.archive.crawler.byexample.algorithms.datastructure.info.ClusteringInfo;
import org.archive.crawler.byexample.algorithms.datastructure.invertedindex.InvertedIndex;
import org.archive.crawler.byexample.algorithms.datastructure.itemset.FrequentItemSets;
import org.archive.crawler.byexample.algorithms.datastructure.support.ClusterSupportIndex;
import org.archive.crawler.byexample.algorithms.datastructure.support.TermSupport;
import org.archive.crawler.byexample.algorithms.preprocessing.TermIndexManipulator;
import org.archive.crawler.byexample.algorithms.tfidf.DocumentIndexManipulator;

/**
 * Cluster Generation class.
 * Receives as input pre-process files and k-frequent item sets and generates clustering according to FIHC algorithm 
 * 
 * @author Michael Bendersky
 */
public class ClusterGenerator {
    
    private TermIndexManipulator myTermsIndex;
    private DocumentIndexManipulator myTFIDFIndex;   
    private ClusterDocumentsIndex myDocumentClusteringIndex;
    private ClusterSupportIndex myClusterSupportIndex;
    private List<TermSupport> myGlobalSupportIndex;
    private long myDocCount;
    
    private StructureBuilder structureBuilder;
    private TreeBuilder treeBuilder;
    
    /**
     * Defaul constructor
     * @param docCount number of documents in collection
     * @param termsIndex terms InvertedIndex
     * @param termSupport TermSupport list
     * @param allDocs list of documents in collection
     */
    public ClusterGenerator(long docCount, InvertedIndex termsIndex, List<TermSupport> termSupport, DocumentListing allDocs){
        myTermsIndex=new TermIndexManipulator(termsIndex);
        myTFIDFIndex=new DocumentIndexManipulator(termsIndex, docCount);
        myTFIDFIndex.createSortedByIdTFIDFIndex();
        myDocumentClusteringIndex=new ClusterDocumentsIndex();
        myClusterSupportIndex=new ClusterSupportIndex();
        myGlobalSupportIndex=termSupport;
        myDocCount=docCount;
        
        structureBuilder=new StructureBuilder(myDocCount, termsIndex, termSupport, allDocs);        
    }    
    
    /**
     * @return current document clustering index
     * 
     * @see org.archive.crawler.byexample.algorithms.datastructure.documents.ClusterDocumentsIndex
     */
    public ClusterDocumentsIndex getClusterDocuments(){
        return myDocumentClusteringIndex;
    }
    
    /**
     * @return current support index
     * 
     * @see org.archive.crawler.byexample.algorithms.datastructure.support.ClusterSupportIndex
     * 
     */
    public ClusterSupportIndex getClusterSupport(){
        return myClusterSupportIndex;
    }
    
    /**
     * @return current TFIDF index
     * 
     * @see org.archive.crawler.byexample.algorithms.datastructure.invertedindex.InvertedIndex
     */
    public InvertedIndex getTFIDFIndex(){
        return myTFIDFIndex.getIndex();
    }
    
    /**
     * Creates initial clusters, builds structure and builds clusters tree.
     * 
     * @param fis k-frequent Item Sets
     * @param path path where clustering XML file will be created
     * @param filename name of clustering XML file
     * @throws Exception
     */
    public void doClustering(FrequentItemSets fis, String path, String filename, ClusteringInfo info) throws Exception{        

        structureBuilder.buildStructure(fis);
        
        myClusterSupportIndex=structureBuilder.getClusterSupport();
        myDocumentClusteringIndex=structureBuilder.getClusterDocuments();
        
        treeBuilder=new TreeBuilder(myDocCount,myDocumentClusteringIndex,myTFIDFIndex,myGlobalSupportIndex,
                                    myClusterSupportIndex); 

        treeBuilder.buildTree(path,filename,fis, info);       
    }
    
} //END OF CLASS
