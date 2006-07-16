package org.archive.crawler.byexample.algorithms.datastructure.info;

import java.util.HashSet;
import java.util.Set;

import org.archive.crawler.byexample.algorithms.datastructure.itemset.FrequentItemSets;
import org.archive.crawler.byexample.algorithms.datastructure.itemset.ItemSet;
import org.archive.crawler.byexample.constants.OutputConstants;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class ClusteringInfo extends XMLInfo{
    
    
    
    public class ClusterStruct{      
        private ItemSet clusterLabel;                   
        private int clusterDocNo;                        
        private boolean clusterRelevance;
        private String associatedTerms;
        
        public ClusterStruct(ItemSet is, int docNo, boolean relevance, String at){
            clusterLabel=is;
            clusterDocNo=docNo;
            clusterRelevance=relevance;
            associatedTerms=at;
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
        public String getAssociatedTerms(){
            return associatedTerms;
        }
        public String toString(){
            return  clusterLabel+OutputConstants.ENTRY_SEPARATOR+
                    clusterDocNo+OutputConstants.ENTRY_SEPARATOR+
                    clusterRelevance+OutputConstants.ENTRY_SEPARATOR;
        }
    }
    
    protected static String ROOT_LABEL="clustering-output";
    public static String CLUSTER_DOCS_LISTING_TAG_LABEL="clusterDocsListingFN";
    public String clusterDocsFN;
    public static String CLUSTER_TERM_SUPPORT_INDEX_TAG_LABEL="clusterTermSupportIndexFN";
    public String clusterTermSupportFN;
    public static String CLUSTERING_GROUP_TAG_LABEL="cluster";
    public static String CLUSTER_NAME_TAG_LABEL="clusterLabel";
    public static String CLUSTER_DOCS_NO_TAG_LABEL="clusterDocsNo";
    public static String CLUSTER_RELEVANCE_TAG_LABEL="clusterRelevance";
    public static String CLUSTER_ASSOCIATED_TERMS_TAG_LABEL="associatedTerms";
    private Set<ClusterStruct> clusters;
    
    public ClusteringInfo(String docList,String termSupportIndex) throws Exception{
        super(ROOT_LABEL);
        this.clusterDocsFN=docList;
        this.clusterTermSupportFN=termSupportIndex;
        clusters=new HashSet<ClusterStruct>();
    }

    public ClusteringInfo() throws Exception{
        super(ROOT_LABEL);
        clusters=new HashSet<ClusterStruct>();
    }
    
    public void addCluster(ItemSet label, int docNo, boolean rel, String associatedTermsList){
        clusters.add(new ClusterStruct(label, docNo, rel, associatedTermsList));
    }
    
   /**
    * Returns FrequentItemSets containing all cluster labels item sets
    */
    public FrequentItemSets getClusterLabels(){
        FrequentItemSets fis=new FrequentItemSets();
        for (ClusterStruct struct : clusters) {
            fis.insertToSet(struct.clusterLabel);
        }
        return fis;
    }
        
    
    public String getClusterDocsFN() {
        return clusterDocsFN;
    }

    public void setClusterDocsFN(String clusterDocsFN) {
        this.clusterDocsFN = clusterDocsFN;
    }

    public String getClusterTermSupportFN() {
        return clusterTermSupportFN;
    }

    public void setClusterTermSupportFN(String clusterTermSupportFN) {
        this.clusterTermSupportFN = clusterTermSupportFN;
    }

    public void toXML(String path, String filename) throws Exception{
        
        Element clusterElement=null;
        createNewXmlDoc();
        
        //Add files info
        addElement(rootElement,CLUSTER_DOCS_LISTING_TAG_LABEL,clusterDocsFN);
        addElement(rootElement,CLUSTER_TERM_SUPPORT_INDEX_TAG_LABEL,clusterTermSupportFN);
        
        // Add clustering info
        for (ClusterStruct iter : clusters) {
            clusterElement=addElement(rootElement,CLUSTERING_GROUP_TAG_LABEL);
            addElement(clusterElement,CLUSTER_NAME_TAG_LABEL,iter.clusterLabel.toString());
            addElement(clusterElement,CLUSTER_DOCS_NO_TAG_LABEL,String.valueOf(iter.clusterDocNo));
            addElement(clusterElement,CLUSTER_RELEVANCE_TAG_LABEL,String.valueOf(iter.clusterRelevance));
            addElement(clusterElement,CLUSTER_ASSOCIATED_TERMS_TAG_LABEL,iter.associatedTerms);
        }
        
        dumpToFile(path,filename);
    }
    
    public void fromXML(String path, String filename) throws Exception{
        readFromFile(path,filename);        
        NodeList rootChildren=rootElement.getChildNodes();
        Node rootChild=null;
        Element topElement=null;
        NodeList clusterChildren=null;
        Node clusterChild=null;
        Element iter=null;
        String key=null;
        String value=null;
        String labelIter=null;
        int docNoIter=0;
        boolean relevanceIter=false;
        String assocTermsIter=null;
        
        //Find all children for each cluster group
        for (int i = 0; i < rootChildren.getLength(); i++) {
            rootChild=rootChildren.item(i);
            if (rootChild instanceof Element){
                topElement=(Element)rootChild;
                //Get clustering files
                if (topElement.getTagName().equals(CLUSTER_DOCS_LISTING_TAG_LABEL))
                    clusterDocsFN=topElement.getTextContent();
                else if (topElement.getTagName().equals(CLUSTER_TERM_SUPPORT_INDEX_TAG_LABEL))
                    clusterTermSupportFN=topElement.getTextContent();
                //Get clustering tree
                else if (topElement.getTagName().equals(CLUSTERING_GROUP_TAG_LABEL)){
                    clusterChildren=((Element)rootChild).getChildNodes();
                    //Create a ClusterStruct from XML tags 
                    for (int j = 0; j < clusterChildren.getLength(); j++) {
                        clusterChild=clusterChildren.item(j);
                        if (clusterChild instanceof Element){
                            iter=(Element)clusterChild;
                            key=iter.getTagName();
                            value=iter.getTextContent();
                            if (key.equals(CLUSTER_NAME_TAG_LABEL))
                                labelIter=value;                            
                            else if (key.equals(CLUSTER_DOCS_NO_TAG_LABEL))
                                docNoIter=Integer.parseInt(value);
                            else if (key.equals(CLUSTER_RELEVANCE_TAG_LABEL))
                                relevanceIter=Boolean.parseBoolean(value);    
                            else if (key.equals(CLUSTER_ASSOCIATED_TERMS_TAG_LABEL))
                                assocTermsIter=value;   
                        }                    
                    }
                    addCluster(ItemSet.createfromString(labelIter),docNoIter,relevanceIter,assocTermsIter);
                }
            }
        }
    }
    
} //END OF CLASS
