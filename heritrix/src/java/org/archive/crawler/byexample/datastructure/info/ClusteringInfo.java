package org.archive.crawler.byexample.datastructure.info;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.archive.crawler.byexample.constants.OutputConstants;
import org.archive.crawler.byexample.datastructure.itemset.ItemSet;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * This class handles XMLInfo presentation of clustering results 
 * 
 * @author Michael Bendersky
 *
 */
public class ClusteringInfo extends XMLInfo{

    private class ClusterStruct{      
        private ItemSet clusterLabel;                   
        private int clusterDocNo;                        
        private boolean isRelevant;
        private String associatedTerms;
        
        public ClusterStruct(ItemSet is, int docNo, boolean relevance, String at){
            clusterLabel=is;
            clusterDocNo=docNo;
            isRelevant=relevance;
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
            return isRelevant;
        }
        public void setClusterRelevance(boolean clusterRelevance) {
            this.isRelevant = clusterRelevance;
        }          
        public String getAssociatedTerms(){
            return associatedTerms;
        }
        
        public String toString(){
            return  clusterLabel+OutputConstants.ENTRY_SEPARATOR+
                    clusterDocNo+OutputConstants.ENTRY_SEPARATOR+
                    isRelevant+OutputConstants.ENTRY_SEPARATOR;
        }
        
        public boolean equals(Object other){
            if (!(other instanceof ItemSet))
                return false;
            
            ItemSet otherIS=(ItemSet)other;
            if (otherIS.equals(this.clusterLabel))
                return true;
            
            return false;
        }
        
    }
    
    protected static String ROOT_LABEL="clustering-output";
    public static String CLUSTER_DOCS_LISTING_TAG_LABEL="clusterDocsListingFN";
    private String clusterDocsFN;
    public static String CLUSTER_TERM_SUPPORT_INDEX_TAG_LABEL="clusterTermSupportIndexFN";
    private String clusterTermSupportFN;
    public static String CLUSTERING_GROUP_TAG_LABEL="cluster";
    public static String CLUSTER_NAME_TAG_LABEL="clusterLabel";
    public static String CLUSTER_DOCS_NO_TAG_LABEL="clusterDocsNo";
    public static String CLUSTER_RELEVANCE_TAG_LABEL="clusterRelevance";
    public static String CLUSTER_ASSOCIATED_TERMS_TAG_LABEL="associatedTerms";
    private Map<ItemSet, ClusterStruct> clusters;
    

    public ClusteringInfo(String docList,String termSupportIndex) throws Exception{
        super(ROOT_LABEL);
        this.clusterDocsFN=docList;
        this.clusterTermSupportFN=termSupportIndex;
        clusters=new ConcurrentHashMap<ItemSet, ClusterStruct>();
    }

    public ClusteringInfo() throws Exception{
        super(ROOT_LABEL);
        clusters=new ConcurrentHashMap<ItemSet, ClusterStruct>();
    }
    
    public void addCluster(ItemSet label, int docNo, boolean rel, String associatedTermsList){
        clusters.put(label, new ClusterStruct(label, docNo, rel, associatedTermsList));
    }
    
            
    public boolean getClusterRelevance(ItemSet is){       
        return clusters.get(is).isRelevant;
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

    /**
     * Convert data to XML and dump xml contents to file
     * @param path
     * @param filename
     * @throws Exception
     */
    public void toXML(String path, String filename) throws Exception{
        
        ClusterStruct currStruct=null;
        Element clusterElement=null;
        createNewXmlDoc();
        
        //Add files info
        addElement(rootElement,CLUSTER_DOCS_LISTING_TAG_LABEL,clusterDocsFN);
        addElement(rootElement,CLUSTER_TERM_SUPPORT_INDEX_TAG_LABEL,clusterTermSupportFN);
        
        // Add clustering info
        for (Iterator<ItemSet> iter = clusters.keySet().iterator(); iter.hasNext();) {
            currStruct=clusters.get(iter.next());       
            clusterElement=addElement(rootElement,CLUSTERING_GROUP_TAG_LABEL);
            addElement(clusterElement,CLUSTER_NAME_TAG_LABEL,currStruct.clusterLabel.toString());
            addElement(clusterElement,CLUSTER_DOCS_NO_TAG_LABEL,String.valueOf(currStruct.clusterDocNo));
            addElement(clusterElement,CLUSTER_RELEVANCE_TAG_LABEL,String.valueOf(currStruct.isRelevant));
            addElement(clusterElement,CLUSTER_ASSOCIATED_TERMS_TAG_LABEL,currStruct.associatedTerms);
        }
        
        dumpToFile(path,filename);
    }
    
    /**
     * Read data from a file and create XML document if possible
     * @param path
     * @param filename
     * @throws Exception
     */
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
