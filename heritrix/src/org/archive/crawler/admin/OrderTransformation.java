package org.archive.crawler.admin;
/*
 * Created on Sep 22, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */

import java.io.File;
import java.io.FileReader;
import java.io.StringWriter;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.archive.crawler.framework.XMLConfig;

/**
 * @author Administrator
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class OrderTransformation extends XMLConfig {
	public void setNodeValue(Node n, String value) {
		n.setNodeValue(value);

	}
	public void setNodeValue(String xPath, String value) {
		Node node = getNodeAt(xPath);
		if (node != null) {
			// Change element text node
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				// If element node has one child and it is a text node
				// replace it with new value
				if (node.getChildNodes() != null) {
					if ((node.getChildNodes().getLength() == 1)
						&& (node.getChildNodes().item(0).getNodeType()
							== Node.TEXT_NODE)) {
						node.getChildNodes().item(0).setNodeValue(value);
					} else {
						if (node.getChildNodes().getLength() > 1) {
							// If element node has children that are all text nodes
							// replace them with a single text node that contains new value
							NodeList nodeList = node.getChildNodes();
							boolean replace = true;
							for (int i = 0; i < nodeList.getLength(); i++) {
								if (nodeList.item(i).getNodeType()
									!= Node.TEXT_NODE) {
									replace = false;
								}
							}
							if (replace) {
								nodeList.item(0).setNodeValue(value);
								for (int i = 1;
									i < nodeList.getLength();
									i++) {
									node.removeChild(nodeList.item(i));
								}
							} else {
								// TODO: Report error
							}
						}
					}
				}
			}
			// Set attribute node values
			node.setNodeValue(value);
		}
	}

	public void serializeToXMLFile(String outputXMLFile) {
		try {
			File f = new File(outputXMLFile);
			Transformer xsltTransformer =
				TransformerFactory.newInstance().newTransformer();
			DOMSource domSource = new DOMSource(xNode);
			StreamResult sr = new StreamResult(f);

			xsltTransformer.transform(domSource, sr);
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}

	public void transformXMLToHTML(
		String outputHTMLfile,
		String orderFile,
		String xslFile) {
		transformXMLtoHTML(outputHTMLfile, orderFile, xslFile);
	}

	public void transformXMLToHTML(String outputHTMLfile) {
		// TODO: Default xml and xslt files should be constants
		transformXMLtoHTML(
			outputHTMLfile,
			"WebUIResources/webapps/admin/order.xml",
			"WebUIResources/webapps/admin/order-conf.xsl");
	}

	public String transformXMLToString() {
		// TODO: Default xml and xslt files should be constants
		return transformXMLToString(
			"WebUIResources/webapps/admin/order.xml",
			"WebUIResources/webapps/admin/order-conf.xsl");
	}

	public String transformXMLToString(String aXMLFile, String aXSLTemplate) {
		try {
			Transformer xsltTransformer =
				TransformerFactory.newInstance().newTransformer(
					new StreamSource(aXSLTemplate));
			StringWriter sw = new StringWriter();
			xsltTransformer.transform(
				new StreamSource(new FileReader(aXMLFile)),
				new StreamResult(sw));
			return sw.toString();
		} catch (Exception e) {
			e.printStackTrace(System.out);
			// TODO: handle exception
		}
		return "";
	}

	public void transformXMLtoHTML(
		String outputHTMLfile,
		String aXMLFile,
		String aXSLTemplate) {
		try {
			Transformer xsltTransformer =
				TransformerFactory.newInstance().newTransformer(
					new StreamSource(aXSLTemplate));
			xsltTransformer.transform(
				new StreamSource(new FileReader(aXMLFile)),
				new StreamResult(new File(outputHTMLfile)));

		} catch (Exception e) {
			e.printStackTrace(System.out);
			// TODO: handle exception
		}

	}
	public static void main(String[] args) {
		OrderTransformation test = new OrderTransformation();
		try {
			test.setNode(
				readDocumentFromFile("WebUIresources/webapps/admin/order.xml"));
			test.setNodeValue("//limits/max-link-depth/@value", "103");
			test.serializeToXMLFile("WebUIresources/webapps/admin/test.xml");
			test.transformXMLToHTML("c:/test.html");
			System.out.println(test.transformXMLToString());
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace(System.out);
		}

	}
}
