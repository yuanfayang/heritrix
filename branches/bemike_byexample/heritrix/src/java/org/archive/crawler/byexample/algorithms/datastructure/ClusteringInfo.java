package org.archive.crawler.byexample.algorithms.datastructure;

import java.util.HashSet;
import java.util.Set;

import org.w3c.dom.Element;

public class ClusteringInfo extends XMLProperties{
    
    
    
    public class ClusterStruct{      
        private ItemSet clusterLabel;                   
        private int clusterDocNo;                        
        private boolean clusterRelevance;
        
        public ClusterStruct(ItemSet is, int docNo, boolean relevance){
            clusterLabel=is;
            clusterDocNo=docNo;
            clusterRelevance=relevance;
        }
        
        public long getClusterDocNo() {
            return clusterDocNo;
        }
        public void setClusterDocNo(int clusterDocNo) {
            this.clusterDocNo = clusterDocNo;
        }
        public ItemSet getClusterLabel() {
            return clusterLabel;
        }
        public void setClusterLabel(ItemSet clusterLabel) {
            this.clusterLabel = clusterLabel;
        }
        public boolean isClusterRelevance() {
            return clusterRelevance;
        }
        public void setClusterRelevance(boolean clusterRelevance) {
            this.clusterRelevance = clusterRelevance;
        }                                      
    }
    
    protected static String ROOT_LABEL="clustering-output";
    public static String CLUSTERING_GROUP_TAG_LABEL="cluster";
    public static String CLUSTER_TAG_LABEL="clusterLabel";
    public static String CLUSTER_DOCS_NO_TAG_LABEL="clusterDocsNo";
    public static String CLUSTER_RELEVANCE_RATION_TAG_LABEL="clusterRelevance";
    private Set<ClusterStruct> clusters;
    
    public ClusteringInfo() throws Exception{
        super(ROOT_LABEL);
        clusters=new HashSet<ClusterStruct>();
    }
    
    public Set<ClusterStruct> getClusters(){
        return clusters;
    }
    
    public void addCluster(ItemSet label, int docNo, boolean rel){
        clusters.add(new ClusterStruct(label, docNo, rel));
    }
    
    public void toXML(String path, String filename) throws Exception{
        
        Element clusterElement=null;
        createNewXmlDoc();
        
        for (ClusterStruct iter : clusters) {
            clusterElement=addElement(rootElement,CLUSTERING_GROUP_TAG_LABEL);
            addElement(clusterElement,CLUSTER_TAG_LABEL,iter.clusterLabel.toString());
            addElement(clusterElement,CLUSTER_DOCS_NO_TAG_LABEL,String.valueOf(iter.clusterDocNo));
            addElement(clusterElement,CLUSTER_RELEVANCE_RATION_TAG_LABEL,String.valueOf(iter.clusterRelevance));
        }
        
        dumpToFile(path,filename);
    }
    
} //END OF CLASS
