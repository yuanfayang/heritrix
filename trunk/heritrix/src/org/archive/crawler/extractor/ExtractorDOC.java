/*
 * Created on Jul 7, 2003
 *
 */
package org.archive.crawler.extractor;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.logging.Logger;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.poi.hdf.extractor.WordDocument;
import org.archive.crawler.datamodel.CoreAttributeConstants;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.framework.CrawlController;
import org.archive.crawler.framework.Processor;

/**
 *  This class allows the caller to extract href style links from word97-format word documents.
 * 
 * @author Parker Thompson
 *
 */
public class ExtractorDOC extends Processor implements CoreAttributeConstants {

	private static Logger logger = Logger.getLogger("org.archive.crawler.extractor.ExtractorDOC");

	/**
	 *  Initialize this processor module.
	 */
	public void initialize(CrawlController c){
		super.initialize(c);
	}
	
	/**
	 *  Processes a word document and extracts any hyperlinks from it.
	 *  This only extracts href style links, and does not examine the actual
	 *  text for valid URIs.
	 */
	protected void innerProcess(CrawlURI curi){
		
		ArrayList links  = new ArrayList();
		GetMethod get = null;
		InputStream documentStream = null;
		Writer out = null;
		
		// assumes docs will be coming in through http
		//TODO make htis more general (currently we're only fetching via http so it doesn't matter)
		if(! curi.getAList().containsKey(A_HTTP_TRANSACTION)) {
			return;
		}
		 get = (GetMethod)curi.getAList().getObject(A_HTTP_TRANSACTION);

		Header contentType = get.getResponseHeader("Content-Type");		
		if ((contentType==null)||(!contentType.getValue().startsWith("application/msword"))) {
			// nothing to extract for other types here
			return; 
		}
		
		// get the doc as a File
		try{
		  	documentStream = get.getHttpRecorder().getRecordedInput().getContentReplayInputStream();
		
			if (documentStream==null) {
				// TODO: note problem
				return;
			}	
			// extract the text from the doc and write it to a stream we
			// can then process
			out = new StringWriter();
			WordDocument w = null;

			w = new WordDocument( documentStream );
			w.writeAllText(out);
		}catch(Exception e){
			curi.addLocalizedError(getName(),e,"ExtractorDOC Exception");
			return;
		} finally {
			try {
				documentStream.close();
			} catch (IOException ignored) {

			}
		}

		// get doc text out of stream
		String page = out.toString();
		
		// find HYPERLINKs
		int currentPos = -1;
		int linkStart = -1;
		int linkEnd = -1;
		char quote = '\"';
		
		currentPos = page.indexOf("HYPERLINK");
		while(currentPos >= 0){

			linkStart = page.indexOf(quote, currentPos) + 1;
			linkEnd = page.indexOf(quote, linkStart);
			
			String hyperlink = page.substring(linkStart, linkEnd);
			
			//System.out.println("link: '" + hyperlink + "'");	
			links.add(hyperlink);
			currentPos = page.indexOf("HYPERLINK", linkEnd + 1);
		}
		
		// if we found any links add them to the curi for later processing
		if(links.size()>0) {
			curi.getAList().putObject(A_HTML_LINKS, links);
		}
		
		logger.fine(curi + " has " + links.size() + " links.");
	}
}
