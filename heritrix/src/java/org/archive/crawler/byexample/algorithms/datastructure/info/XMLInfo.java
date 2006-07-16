package org.archive.crawler.byexample.algorithms.datastructure.info;

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

public class XMLInfo {
    
    protected static String ROOT_TAG_LABEL="ROOT";
    
    protected Element rootElement;
    protected Document xmlDoc=null;
    
    public XMLInfo(String label) throws Exception{        
        ROOT_TAG_LABEL=label;
    }

    
    protected void addElement(Element parent,String elementName, String elementValue){
        Element childName=xmlDoc.createElement(elementName);
        childName.appendChild(xmlDoc.createTextNode(elementValue));
        parent.appendChild(childName);
    }
    
    protected Element addElement(Element parent,String elementName){
        Element child=xmlDoc.createElement(elementName);
        parent.appendChild(child);
        return child;
    }
    
    protected void createNewXmlDoc() throws Exception{
        xmlDoc=DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        rootElement=xmlDoc.createElement(ROOT_TAG_LABEL);
        xmlDoc.appendChild(rootElement);   
    }
    
    protected void dumpToFile(String path, String filename) throws Exception{
        BufferedWriter bw=FileUtils.createFileAtPath(path,filename);
        Transformer t=TransformerFactory.newInstance().newTransformer();
        t.transform(new DOMSource(xmlDoc),new StreamResult(bw));
        FileUtils.closeFile(bw);
    }
    
    protected void readFromFile(String path, String filename)throws Exception{
        DocumentBuilder builder=DocumentBuilderFactory.newInstance().newDocumentBuilder();
        xmlDoc=builder.parse(new InputSource(FileUtils.readBufferFromFile(path+filename)));
        rootElement=xmlDoc.getDocumentElement();
        checkRootLabel();
    }
    
    protected void checkRootLabel() throws Exception{
        if (!rootElement.getTagName().equalsIgnoreCase(ROOT_TAG_LABEL))
            throw new Exception("Wrong file type");
    }
    
} //END OF CLASS
