/*
 * XMLConfig.java
 * Created on May 22, 2003
 *
 * $Header$
 */
package org.archive.crawler.datamodel;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.xpath.XPathAPI;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.traversal.NodeIterator;
import org.xml.sax.SAXException;

/**
 * @author gojomo
 *
 */
public class XMLConfig {
	Node xNode;
	
	public static Document readDocumentFromFile(String filename) {
		File f = new File(filename);
		try {
			DocumentBuilderFactory factory =
				DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document document = builder.parse(f);
			return document;
		} catch (FactoryConfigurationError e) {
			// unable to get a document builder factory
		} catch (ParserConfigurationException e) {
			// parser was unable to be configured
		} catch (SAXException e) {
			// parsing error
		} catch (IOException e) {
			// i/o error
		}
		return null;
	}
	
	public static Node nodeOrSrc(Node node) {
		try {
			Node srcNode = XPathAPI.selectSingleNode(node,"@src");
			if (srcNode != null ) {
				return (Node)readDocumentFromFile(srcNode.getNodeValue());
			}
		} catch (TransformerException te) {
			// TODO something maybe
		}
		return node;

	}

	protected int getIntAt(String xpath) {

			try {
				return Integer.parseInt(XPathAPI.selectSingleNode(xNode,xpath).getNodeValue());
			} catch (NumberFormatException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (DOMException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (TransformerException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return -1;
	}
	
	
	/**
	 * @param string
	 * @return
	 */
	public Object instantiate(String string) {
		try {
			return instantiate(XPathAPI.selectSingleNode(xNode,string));
		} catch (DOMException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TransformerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	public Object instantiate(Node n) {
		try {
			Class c = Class.forName(n.getAttributes().getNamedItem("class").getNodeValue());
			return c.newInstance();
		} catch (DOMException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * @param string
	 * @param processors
	 * @return
	 */
	public Object instantiateAllInto(String xpath, HashMap results) {
		Object first = null;
		NodeIterator iter;
		try {
			iter = XPathAPI.selectNodeIterator(xNode, xpath);
		} catch (TransformerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		Node currentNode;
		while ((currentNode=iter.nextNode())!=null) {
			Object currentObject = instantiate(currentNode);
			if (first==null) first = currentObject;
			String name = currentNode.getAttributes().getNamedItem("name").getNodeValue();
			results.put(name,currentNode);
		}
		return first;
	}
}
