/*
 * CrawlOrder.java
 * Created on May 15, 2003
 *
 * $Header$
 */
package org.archive.crawler.datamodel;

import java.io.File;
import java.io.IOException;

import javax.xml.transform.TransformerException;

import org.apache.xpath.XPathAPI;
import org.archive.crawler.framework.XMLConfig;
import org.w3c.dom.Document;

/** Read and manipulate configuration (order) file.
 */
public class CrawlOrder extends XMLConfig {
	protected String name;
	protected CrawlerBehavior behavior;
	protected String outputLocation;
	public String crawlOrderFilename;
	//protected CrawlOrder parentConfigurationFile;
	
	/**
	 * @param crawlOrderFile
	 * @return
	 */
	public static CrawlOrder readFromFile(String crawlOrderFile) throws InitializationException {
		
		String pathToDoc = null;
		Document doc = null;
		
		// try to extract a default path for later file reading
		int pathEnd = crawlOrderFile.lastIndexOf(File.separatorChar);
		if(pathEnd >=0){
			pathToDoc = crawlOrderFile.substring(0,pathEnd);
		}
		
		try{
			doc = readDocumentFromFile(crawlOrderFile);
		}catch(IOException e){
			throw new InitializationException("Can't read from '" + crawlOrderFile + "': " + e.toString(), e);
		}
		
		
		CrawlOrder co = new CrawlOrder(doc, pathToDoc);				
//		XMLConfig behave = co.getBehavior();
//		if(behave != null){
//			behave.setDefaultFileLocation(pathToDoc);
//		}
		
		return co;
	}
	
	/** Construct a CrawlOrder instance given a Document */
	public CrawlOrder(Document doc) throws InitializationException {
		this(doc, ".");
	}

	/** Construct a CrawlOrder instance given a Document and a path to that
	 *  document.  The path is necessary in the event that the config file inherits
	 *  from another config file (so we can locate that on the file sytem).
	 * @param doc
	 * @param pathToDoc
	 * @throws InitializationException
	 */
	public CrawlOrder(Document doc, String pathToDoc)
		throws InitializationException {
		xNode = doc;

		if (pathToDoc != null) {
			defaultFilePath = pathToDoc;
		} else {
			defaultFilePath = ".";
		}
		
		loadParents(pathToDoc);
		initialize();

	}
	
	/** Load common configuration variables from config file.  Should only 
	 *  be called on the primary (non-inherited from) crawl order.
	 */
	public void initialize(){
		try {
			name = getStringAt("//crawl-order/@name");

			// ignore null pointers here, it may just mean this file inherited from
			// another and we can find the behavior there.			
			try {
				behavior =
					new CrawlerBehavior(
						XPathAPI.selectSingleNode(xNode, "//crawler-behavior"));
				behavior.setDefaultFileLocation(this.defaultFilePath);
				behavior.setParentConfig(this.parentConfigurationFile);
			} catch (NullPointerException e) {
			}

			//outputLocation = getStringAt("//disk/@path");

		} catch (TransformerException e) {
			e.printStackTrace();
		}
	}
	
	protected void loadParents(String pathToDoc) throws InitializationException {
		
		// try to read any configuration files which this extends
		// if this fails let the Initialization exception bubble up
		String parentFileName = getStringAt("//crawl-order/@extends");
		//TODO check for infinite loop (parent == self)
		if (parentFileName != null && parentFileName.length() != 0) {
			try {
				if(isAbsoluteFilePath(parentFileName)){
					parentConfigurationFile = 
						readFromFile(parentFileName);
				}else{
					parentConfigurationFile =
						readFromFile(pathToDoc + File.separator + parentFileName);
				}
				// can't read parent, proceed as best we can.
			} catch (InitializationException e) {
				System.out.println(
					"Unable to read config file '"
						+ pathToDoc
						+ File.separator
						+ parentFileName
						+ "' (non fatal), continuing...");
			}
		}
	}

	/**
	 * 
	 */
	public CrawlerBehavior getBehavior() {
		// if this node doesn't have it but we have a parent conf file check that
		if(behavior == null && parentConfigurationFile != null){ 
			return ((CrawlOrder)parentConfigurationFile).getBehavior();
		}
		return behavior;
	}

	/**
	 * @return
	 */
	public String getName() {
		// if this node doesn't have it but we have a parent conf file check that
		if(name == null && parentConfigurationFile != null){
			return ((CrawlOrder)parentConfigurationFile).getName();
		}
		return name;
	}

	public String getOutputLocation(){
		if(outputLocation == null && parentConfigurationFile != null){
			return ((CrawlOrder)parentConfigurationFile).getOutputLocation();
		}
		return outputLocation;
	}
}
