/*
 * CrawlOrder.java
 * Created on May 15, 2003
 *
 * $Header$
 */
package org.archive.crawler.datamodel;

import java.io.IOException;

import javax.xml.transform.TransformerException;

import org.apache.xpath.XPathAPI;
import org.archive.crawler.framework.*;
import org.w3c.dom.Document;

import java.io.File;

/**
 * @author gojomo
 *
 */
public class CrawlOrder extends XMLConfig {
	String name;
	CrawlerBehavior behavior;
	String outputLocation;
	public String crawlOrderFilename;
	/**
	 * @param crawlOrderFile
	 * @return
	 */
	public static CrawlOrder readFromFile(String crawlOrderFile) throws IOException {
		
		Document doc = readDocumentFromFile(crawlOrderFile);

		CrawlOrder co = new CrawlOrder(doc);
				
		// try to extract a default path for later file reading (e.g. seeds.txt)
		int pathEnd = crawlOrderFile.lastIndexOf(File.separatorChar);
		if(pathEnd >=0){
			String path = crawlOrderFile.substring(0,pathEnd);
			
			co.setDefaultFileLocation(path);
			co.getBehavior().setDefaultFileLocation(path);
		}
		
		return co;
	}

	/**
	 * @param doc
	 */
	public CrawlOrder(Document doc) {
		xNode = doc;
		try {
			name 		= getNodeAt("/crawl-order/@name").getNodeValue();
			behavior 	= new CrawlerBehavior(XPathAPI.selectSingleNode(doc,"//crawler-behavior"));
			
			outputLocation 	= getStringAt("//disk/@path");
			
		} catch (TransformerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * 
	 */
	public CrawlerBehavior getBehavior() {
		return behavior;
		
	}

	/**
	 * @return
	 */
	public String getName() {
		return getNodeAt("/crawl-order/@name").getNodeValue();
		//return name;
	}

	public String getOutputLocation(){
		return getStringAt("//disk/@path");
		//return outputLocation;
	}
}
