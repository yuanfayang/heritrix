/*
 * CrawlOrder.java
 * Created on May 15, 2003
 *
 * $Header$
 */
package org.archive.crawler.datamodel;

import java.io.File;
import java.io.IOException;

import org.archive.crawler.framework.XMLConfig;
import org.archive.crawler.framework.exceptions.*;
import org.w3c.dom.Document;

/** Read and manipulate configuration (order) file.
 */
public class CrawlOrder extends XMLConfig {
	private static final String XP_CRAWL_ORDER_NAME = "//crawl-order/@name";
	private static final String XP_HTTP_USER_AGENT = "//http-headers/@User-Agent";
	private static final String XP_HTTP_FROM = "//http-headers/@From";
	private static final String XP_MAX_TOE_THREADS = "//behavior/@max-toe-threads";
	String caseFlattenedUserAgent;
	String name;
    String outputLocation;
	String crawlOrderFilename;
	
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
		
		//loadParents(pathToDoc);
		initialize();

	}
	
	/** Load common configuration variables from config file.  Should only 
	 *  be called on the primary (non-inherited from) crawl order.
	 */
	public void initialize(){
		name = getStringAt(XP_CRAWL_ORDER_NAME);
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
	
	/**
	 * @return
	 */
	public String getUserAgent() {
		if (caseFlattenedUserAgent==null) {
			caseFlattenedUserAgent =  getStringAt(XP_HTTP_USER_AGENT).toLowerCase();
		}
		return caseFlattenedUserAgent;
	}

	/**
	 * @return
	 */
	public String getFrom() {
		return getStringAt(XP_HTTP_FROM);
	}
	
	/**
		 * @return
	*/
	public int getMaxToes() {
		return getIntAt(XP_MAX_TOE_THREADS);
	}

	/**
	 * 
	 */
	public String getCrawlOrderFilename() {
		return crawlOrderFilename;
	}
}
