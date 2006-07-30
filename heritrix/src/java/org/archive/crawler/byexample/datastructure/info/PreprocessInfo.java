package org.archive.crawler.byexample.datastructure.info;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;




public class PreprocessInfo extends XMLInfo{
    
    protected static String ROOT_TAG_LABEL="preProcess-output";
    
    public static String TERMS_FILE_TAG_LABEL="termsIndexFN";
    private String termsFN;
    
    public static String DOCS_FILE_TAG_LABEL="documentListingFN";
    private String docsFN;
    
    public static String DOCS_NO_TAG_LABEL="preprocessDocsNo";
    private long docsNo;
    
    public static String TERMS_NO_TAG_LABEL="preprocessTermsNo";
    private int termsNo;
    
    
    public PreprocessInfo() throws Exception{
        super(ROOT_TAG_LABEL);
    }
    
    public PreprocessInfo(String termsFN, String docsFN, long docsNo, int termsNo) throws Exception{
        super(ROOT_TAG_LABEL);
        this.termsFN=termsFN;
        this.docsFN=docsFN;
        this.docsNo=docsNo;
        this.termsNo=termsNo;
    }

    public String getDocsFN() {
        return docsFN;
    }
    public void setDocsFN(String docsFN) {
        this.docsFN = docsFN;
    }
    public long getDocsNo() {
        return docsNo;
    }
    public void setDocsNo(long docsNo) {
        this.docsNo = docsNo;
    }
    public String getTermsFN() {
        return termsFN;
    }
    public void setTermsFN(String termsFN) {
        this.termsFN = termsFN;
    }
    public int getTermsNo() {
        return termsNo;
    }
    public void setTermsNo(int termsNo) {
        this.termsNo = termsNo;
    }

    public void toXML(String path, String filename) throws Exception{
        
        createNewXmlDoc();
        
        addElement(rootElement,TERMS_FILE_TAG_LABEL,termsFN);
        addElement(rootElement,DOCS_FILE_TAG_LABEL,docsFN);
        addElement(rootElement,DOCS_NO_TAG_LABEL,String.valueOf(docsNo));
        addElement(rootElement,TERMS_NO_TAG_LABEL,String.valueOf(termsNo));
        
        dumpToFile(path, filename);
    }
    
    public void fromXML(String path, String filename) throws Exception{
       readFromFile(path,filename);        
       NodeList children=rootElement.getChildNodes();
       Node child=null;
       Element iter=null;
       String key=null;
       String value=null;
       for (int i = 0; i < children.getLength(); i++) {
           child=children.item(i);
           if (child instanceof Element) {
               iter=(Element)child;
               key=iter.getTagName();
               value=iter.getTextContent();
               if (key.equals(TERMS_FILE_TAG_LABEL))
                   termsFN=value;
               else if (key.equals(DOCS_FILE_TAG_LABEL))
                   docsFN=value;
               else if (key.equals(TERMS_NO_TAG_LABEL))
                   termsNo=Integer.parseInt(value);
               else if (key.equals(DOCS_NO_TAG_LABEL))
                   docsNo=Long.parseLong(value);
           }               
       }        
    }
    
    
} //END OF CLASS
