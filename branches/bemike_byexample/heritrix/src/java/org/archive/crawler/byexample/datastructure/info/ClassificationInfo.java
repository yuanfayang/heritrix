package org.archive.crawler.byexample.datastructure.info;

/**
 * This class handles XMLInfo presentation of classification results 
 * 
 * @author Michael Bendersky
 *
 */
public class ClassificationInfo extends XMLInfo {

    protected static String ROOT_TAG_LABEL="classification-output";
    public static String BASED_ON_TAG_LABEL="based-on-job";
    private String basedOnFN=null;
    public static String CLASSIFIED_LIST_TAG_LABEL="classified-list";
    private String classifiedFN=null;
    public static String UNCLASSIFIED_LIST_TAG_LABEL="unclassified-list";
    private String unclassifiedFN=null;         
    public static String MOST_RELEVANT_LIST_TAG_LABEL="most-relevant-list";
    private String mostRelevantFN=null;
    public static String LEAST_RELEVANT_LIST_TAG_LABEL="least-relevant-list";
    private String leastRelevantFN=null;
    public static String DOCS_NO_TAG_LABEL="classifiedDocsNo";
    private long docsNo;
    
    public ClassificationInfo(String basedOn,String classifiedList,
            String unclassifiedList, String mostRelevantList, 
            String leastRelevantList, long docsNo) throws Exception{
        super(ROOT_TAG_LABEL);
        this.basedOnFN=basedOn;
        this.classifiedFN=classifiedList;
        this.unclassifiedFN=unclassifiedList;
        this.mostRelevantFN=mostRelevantList;
        this.leastRelevantFN=leastRelevantList;
        this.docsNo=docsNo;
    }
    
    /**
     * Convert data to XML and dump xml contents to file
     * @param path
     * @param filename
     * @throws Exception
     */
    public void toXML(String path, String filename) throws Exception{                
        createNewXmlDoc();        
        //Add info
        addElement(rootElement, BASED_ON_TAG_LABEL,basedOnFN);
        addElement(rootElement, CLASSIFIED_LIST_TAG_LABEL, classifiedFN);
        addElement(rootElement, UNCLASSIFIED_LIST_TAG_LABEL, unclassifiedFN);
        addElement(rootElement, MOST_RELEVANT_LIST_TAG_LABEL, mostRelevantFN);
        addElement(rootElement, LEAST_RELEVANT_LIST_TAG_LABEL, leastRelevantFN);
        addElement(rootElement, DOCS_NO_TAG_LABEL, String.valueOf(docsNo));        
        dumpToFile(path,filename);
    }
    
}
