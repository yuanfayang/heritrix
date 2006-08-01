package org.archive.crawler.byexample.datastructure.info;

import java.io.BufferedWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.archive.crawler.byexample.utils.FileUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

/**
 * Basic info class used to output any algorithm result to an XML file. 
 * Class provides methods for adding elements to XML document
 * as well as methods for reading/writing XML document from/to file. 
 * This class is never used directly, but rather is extended by other info classes under 
 * org.archive.crawler.byexample.datastructure.info package
 * 
 * @author Michael Bendersky
 *
 */
public class XMLInfo {
    
    /**
     * XML document root label.
     * This definition must be overriden by any extending class 
     */
    protected static String ROOT_TAG_LABEL="ROOT";
    
    /**
     * XML document root element
     */
    protected Element rootElement;
    /**
     * XML document
     */
    protected Document xmlDoc=null;
    
    /**
     * Defaul constructor. 
     * Only assigns given label to ROOT_TAG_LABEL
     * @param label XML document root label 
     */
    public XMLInfo(String label){        
        ROOT_TAG_LABEL=label;
    }

    
    /**
     * Add element with given value under a given parent element
     * @param parent paremt element, under which new element will be added
     * @param elementName new element to add
     * @param elementValue String representing new element value
     */
    protected void addElement(Element parent,String elementName, String elementValue){
        Element childName=xmlDoc.createElement(elementName);
        childName.appendChild(xmlDoc.createTextNode(elementValue));
        parent.appendChild(childName);
    }
    
    /**
     * Add element under a given parent element
     * @param parent paremt element, under which new element will be added
     * @param elementName new element to add
     * @return added element
     */
    protected Element addElement(Element parent,String elementName){
        Element child=xmlDoc.createElement(elementName);
        parent.appendChild(child);
        return child;
    }
    
    /**
     * Creates new XML document
     * @throws Exception
     */
    protected void createNewXmlDoc() throws Exception{
        xmlDoc=DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        rootElement=xmlDoc.createElement(ROOT_TAG_LABEL);
        xmlDoc.appendChild(rootElement);   
    }
    
    /**
     * Writes the XML document to a file
     * @param path file path 
     * @param filename file name
     * @throws Exception
     */
    protected void dumpToFile(String path, String filename) throws Exception{
        BufferedWriter bw=FileUtils.createFileAtPath(path,filename);
        Transformer t=TransformerFactory.newInstance().newTransformer();
        t.transform(new DOMSource(xmlDoc),new StreamResult(bw));
        FileUtils.closeFile(bw);
    }
    
    /**
     * Reads the XML document from a file
     * @param path file path 
     * @param filename file name
     * @throws Exception
     */
    protected void readFromFile(String path, String filename)throws Exception{
        DocumentBuilder builder=DocumentBuilderFactory.newInstance().newDocumentBuilder();
        xmlDoc=builder.parse(new InputSource(FileUtils.readBufferFromFile(path+filename)));
        rootElement=xmlDoc.getDocumentElement();
        checkRootLabel();
    }
    
    /**
     * Check whether the root label is correct (ie, file was not corrupted during read/write procedure) 
     * @throws Exception
     */
    protected void checkRootLabel() throws Exception{
        if (!rootElement.getTagName().equalsIgnoreCase(ROOT_TAG_LABEL))
            throw new Exception("Wrong file type");
    }
    
} //END OF CLASS
