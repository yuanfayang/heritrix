package org.archive.crawler.byexample.datastructure.info;


public class ClassificationInfo extends XMLInfo {

    protected static String ROOT_TAG_LABEL="classification-output";
    public static String BASED_ON_TAG_LABEL="based-on-job";
    private String basedOnFN=null;
    public static String FULL_LIST_TAG_LABEL="full-list";
    private String fullListFN=null;
    public static String DOCS_NO_TAG_LABEL="classifiedDocsNo";
    private long docsNo;
    
    public ClassificationInfo(String basedOn,String fullList,long docsNo) throws Exception{
        super(ROOT_TAG_LABEL);
        this.basedOnFN=basedOn;
        this.fullListFN=fullList;
        this.docsNo=docsNo;
    }
    
    public void toXML(String path, String filename) throws Exception{                
        createNewXmlDoc();        
        //Add info
        addElement(rootElement, BASED_ON_TAG_LABEL,basedOnFN);
        addElement(rootElement, FULL_LIST_TAG_LABEL, fullListFN);
        addElement(rootElement, DOCS_NO_TAG_LABEL, String.valueOf(docsNo));
                
        dumpToFile(path,filename);
    }
}
