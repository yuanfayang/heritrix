/* Copyright (C) 2003 Internet Archive.
 *
 * This file is part of the Heritrix web crawler (crawler.archive.org).
 *
 * Heritrix is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * any later version.
 *
 * Heritrix is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser Public License for more details.
 *
 * You should have received a copy of the GNU Lesser Public License
 * along with Heritrix; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * Created on Sep 22, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package org.archive.crawler.admin;

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
		String outputHTMLFile,
		String aXMLFile,
		String aXSLTemplate){
		transformXMLtoHTML(outputHTMLFile, aXMLFile, aXSLTemplate, "./");
}

	public void transformXMLtoHTML(
		String outputHTMLFile,
		String aXMLFile,
		String aXSLTemplate,
		String workingDirectory) {
		try {
			Transformer xsltTransformer =
				TransformerFactory.newInstance().newTransformer(
					new StreamSource(aXSLTemplate));
					xsltTransformer.setParameter("orderFilePath", workingDirectory);
			xsltTransformer.transform(
				new StreamSource(new FileReader(aXMLFile)),
				new StreamResult(new File(outputHTMLFile)));

		} catch (Exception e) {
			e.printStackTrace(System.out);
			// TODO: handle exception
		}

	}
}
