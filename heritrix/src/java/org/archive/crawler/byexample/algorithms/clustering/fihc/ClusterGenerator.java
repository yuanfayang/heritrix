package org.archive.crawler.byexample.algorithms.clustering.fihc;

import java.util.List;

import org.archive.crawler.byexample.algorithms.datastructure.ClusteringDocumentIndex;
import org.archive.crawler.byexample.algorithms.datastructure.ClusteringSupportIndex;
import org.archive.crawler.byexample.algorithms.datastructure.DocumentListing;
import org.archive.crawler.byexample.algorithms.datastructure.FrequentItemSets;
import org.archive.crawler.byexample.algorithms.datastructure.InvertedIndex;
import org.archive.crawler.byexample.algorithms.datastructure.TermSupport;
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
    private ClusteringDocumentIndex myDocumentClusteringIndex;
    private ClusteringSupportIndex myClusterSupportIndex;
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
        myDocumentClusteringIndex=new ClusteringDocumentIndex();
        myClusterSupportIndex=new ClusteringSupportIndex();
        myGlobalSupportIndex=termSupport;
        myDocCount=docCount;
        
        structureBuilder=new StructureBuilder(myDocCount, termsIndex, termSupport, allDocs);        
    }    
    
    /**
     * @return current document clustering index
     * 
     * @see org.archive.crawler.byexample.algorithms.datastructure.ClusteringDocumentIndex
     */
    public ClusteringDocumentIndex getClusterDocuments(){
        return myDocumentClusteringIndex;
    }
    
    /**
     * @return current support index
     * 
     * @see org.archive.crawler.byexample.algorithms.datastructure.ClusteringSupportIndex
     * 
     */
    public ClusteringSupportIndex getClusterSupport(){
        return myClusterSupportIndex;
    }
    
    /**
     * @return current TFIDF index
     * 
     * @see org.archive.crawler.byexample.algorithms.datastructure.InvertedIndex
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
    public void doClustering(FrequentItemSets fis, String path, String filename) throws Exception{        

        structureBuilder.buildStructure(fis);
        
        myClusterSupportIndex=structureBuilder.getClusterSupport();
        myDocumentClusteringIndex=structureBuilder.getClusterDocuments();
        
        treeBuilder=new TreeBuilder(myDocCount,myDocumentClusteringIndex,myTFIDFIndex,myGlobalSupportIndex,
                                    myClusterSupportIndex); 

        treeBuilder.buildTree(path,filename);       
    }
    
} //END OF CLASS
