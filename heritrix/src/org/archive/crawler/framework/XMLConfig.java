/*
 * XMLConfig.java
 * Created on May 22, 2003
 *
 * $Header$
 */
package org.archive.crawler.framework;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.xpath.XPathAPI;
import org.w3c.dom.Attr;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.traversal.NodeIterator;
import org.xml.sax.SAXException;

/**
 * Parent class for objects which are configured via a 
 * backing XML node (possibly a full document, possibly
 * just a part of a full document). 
 * 
 * @author gojomo
 *
 */
public class XMLConfig {
	/**
	 * Backing node; default origin of any XPath accesses.
	 */
	protected Node xNode;
	
	protected HashMap cachedPathNodes = new HashMap(); // String -> Node
	protected HashMap cachedIntegers = new HashMap(); // String -> Integer
	protected HashMap cachedStrings = new HashMap(); // String -> String

	
	/**
	 * Convenience method for reading an XML file into a Document instance
	 * 
	 * @param filename
	 * @return
	 */
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
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			// parser was unable to be configured
			e.printStackTrace();
		} catch (SAXException e) {
			// parsing error
			e.printStackTrace();

		} catch (IOException e) {
			// i/o error
			e.printStackTrace();

		}
		return null;
	}
	
	/**
	 * If the supplied node has a 'src' attribute, treat that
	 * as a filename, and return the document in that file.
	 * Otherwise, return the supplied node. Simulates in-line
	 * inclusion of the other file, if specified.
	 * 
	 * @param node
	 * @return
	 */
	public static Node nodeOrSrc(Node node) {
		try {
			Node srcNode = XPathAPI.selectSingleNode(node,"@src");
			if (srcNode != null ) {
				return (Node)readDocumentFromFile(srcNode.getNodeValue());
			}
		} catch (TransformerException te) {
			// TODO something maybe
			te.printStackTrace();

		}
		return node;

	}

	/**
	 * Get either the contents at the specified xpath, or the
	 * contents of the file given by the src attribute at the
	 * specified xpath. Useful for reading large blocks of text
	 * from within an XML element or in a referenced helper file. 
	 * (For example, long seed URI lists.)
	 * 
	 * @param xpath
	 * @return
	 */
	public BufferedReader nodeValueOrSrcReader(String xpath) {
		return nodeValueOrSrcReader(getNodeAt(xpath));
	}

	/**
	 * Return the node at the specified xpath, starting from
	 * the local origin node. 
	 * 
	 * @param xpath
	 * @return
	 */
	public Node getNodeAt(String xpath) {
		Node cacheNode = null;
		if (! cachedPathNodes.containsKey(xpath)) {
			try {
				cacheNode = XPathAPI.selectSingleNode(xNode,xpath);
			} catch (TransformerException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				cacheNode = null;
			}
			cachedPathNodes.put(xpath,cacheNode);
		}
		return (Node)cachedPathNodes.get(xpath);
	}

	/**
	 * Get either the contents of the specified node, or the
	 * contents of the file given by the src attribute of the
	 * specified node. Useful for reading large blocks of text
	 * from within an XML element or in a referenced helper file. 
	 * (For example, long seed URI lists.)
	 * 
	 * @param node
	 * @return
	 */
	public static BufferedReader nodeValueOrSrcReader(Node node) {

		try {
			Node srcNode = XPathAPI.selectSingleNode(node,"@src");
			if (srcNode != null ) {
				return new BufferedReader(new FileReader(srcNode.getNodeValue()));
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (DOMException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TransformerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return new BufferedReader(new StringReader(node.getNodeValue()));

	}

	/**
	 * Retrieve a (positive) integer value from the given xpath;
	 * return -1 if none found or other error occurs. 
	 * 
	 * @param xpath
	 * @return
	 */
	public int getIntAt(String xpath) {
		return getIntAt(xpath,-1);
	}


	/**
	 * Retrieve a (positive) integer value from the given xpath;
	 * return the supplied default if none found or other error occurs. 
	 * 
	 * @param xpath
	 * @param defaultValue
	 * @return
	 */
	public int getIntAt(String xpath, int defaultValue) {
		Integer cacheInteger = null;
		if (! cachedIntegers.containsKey(xpath)) {
			try {
				String n = getStringAt(xpath);
				if (n!=null) {
					cacheInteger = new Integer(getStringAt(xpath));
				} else {
					cacheInteger = new Integer(defaultValue);
				}
			} catch (NumberFormatException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				cacheInteger = new Integer(defaultValue);
			} catch (DOMException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				cacheInteger = new Integer(defaultValue);
			}
			cachedIntegers.put(xpath,cacheInteger);
		}
		return ((Integer)cachedIntegers.get(xpath)).intValue();
	}

	/**
	 * Return the text of the given node: the value if an
	 * attribute node, the concatenation of all text children
	 * if an element node.
	 * 
	 * @param node
	 * @return
	 */
	private String textOf(Node node) {
		if (node == null) {
			return null;
		}
		if (node instanceof Attr) {
			return node.getNodeValue();
		}
		String value = ""; 
		NodeList children = node.getChildNodes(); 
		for(int i = 0; i < children.getLength(); i++ ) { 
		  Node ci = children.item(i); 
		  if( ci.getNodeType() == Node.TEXT_NODE ) { 
			value = value + ci.getNodeValue(); 
		  }
		}
		return value;
	}

	/**
	 * Return the text at the specified xpath. 
	 * 
	 * @param xpath
	 * @return
	 */
	protected String getStringAt(String xpath) {
		String cacheString = null;
		if (!cachedStrings.containsKey(xpath)) {
			try {
				cacheString = textOf(getNodeAt(xpath));
			} catch (NumberFormatException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (DOMException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			cachedStrings.put(xpath,cacheString);
		}
		return (String) cachedStrings.get(xpath);
	}
	
	
	/**
	 * Using the node at the specified xpath as a guide,
	 * create a Java instance. The node must supply a 
	 * 'class' attribute. 
	 * 
	 * @param xpath
	 * @return
	 */
	public Object instantiate(String xpath) {
		try {
			return instantiate(XPathAPI.selectSingleNode(xNode,xpath));
		} catch (DOMException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TransformerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Using the specified node, create a Java instance. 
	 * The node must supply a 'class' attribute. If the
	 * given class itself extends XMLConfig, the instance
	 * will be handed to node to serve as its own backing 
	 * XML. 
	 * 
	 * @param n
	 * @return
	 */
	public Object instantiate(Node n) {
		try {
			Class c = Class.forName(n.getAttributes().getNamedItem("class").getNodeValue());
			Object instance = c.newInstance();
			if (instance instanceof XMLConfig) {
				((XMLConfig)instance).setNode(n);
			}
			return instance;
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
	 * Set the backing XML node.
	 * 
	 * @param n
	 */
	public void setNode(Node n) {
		// TODO: probably shouldn't be public method
		xNode = n;
	}

	/**
	 * Instantiate each node selected by the xpath using the
	 * included class specifications. If a results object is
	 * supplied, add all instantiated objects to it. (If it
	 * is a hashmap, add them under their 'name' attribute.)
	 * Return the first item instatiated. 
	 * 
	 * @param xpath
	 * @param results
	 * @return
	 */
	public Object instantiateAllInto(String xpath, Object results) {
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
			if (first==null) {
				first = currentObject;
			}
			if (results instanceof HashMap) {
				// if supplied hashmap, look for 'name' key
				if (currentNode.getAttributes().getNamedItem("name")!=null) {
					String name = currentNode.getAttributes().getNamedItem("name").getNodeValue();
					((HashMap)results).put(name,currentObject);
				}
			} else if (results instanceof Collection){
				// otherwise, just add to results
				((Collection)results).add(currentObject);
			}
		}
		return first;
	}
}
