/*
 * Created on Jul 11, 2003
 *
 */
package org.archive.crawler.extractor;

import org.archive.crawler.framework.Processor;
import org.archive.crawler.extractor.PDFParser;

import org.archive.crawler.datamodel.CoreAttributeConstants;
import org.archive.crawler.datamodel.CrawlURI;

import java.util.ArrayList;
import java.util.logging.Logger;
import java.io.IOException;

import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.Header;

/** Allows the caller to proccess a CrawlURI representing a PDF
 *  for the purpose of extracting URIs
 * @author Parker Thompson 
 *
 */
public class ExtractorPDF extends Processor implements CoreAttributeConstants {
	
	private static Logger logger = Logger.getLogger("org.archive.crawler.extractor.ExtractorPDF");
	
	protected void innerProcess(CrawlURI curi){

		GetMethod get = null;
		if(curi.getAList().containsKey(A_HTTP_TRANSACTION)) {
			get = (GetMethod)curi.getAList().getObject(A_HTTP_TRANSACTION);

		}else{
			return;
		}
		
		Header contentType = get.getResponseHeader("Content-Type");		
		if ((contentType==null)||(!contentType.getValue().startsWith("application/pdf"))) {
			// nothing to extract for other types here
			return;
		} 

		// TODO fix to use recordingStreams
		byte[] docBytes = get.getResponseBody();
			
		PDFParser parser;
		ArrayList uris;
		try{
			parser = new PDFParser(docBytes);		
			uris = parser.extractURIs();
						
		}catch(IOException e){
			e.printStackTrace();
			return;
		}
		
		if(uris.size()>0) {
			curi.getAList().putObject("html-links", uris);
		}
		
		logger.fine(curi+" has "+uris.size()+" links.");
	}
}
