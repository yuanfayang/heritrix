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
import org.archive.crawler.framework.Processor;

/**
 *  This class allows the caller to extract href style links from word97-format word documents.
 * 
 * @author Parker Thompson
 *
 */
public class ExtractorDOC extends Processor implements CoreAttributeConstants {

    private static Logger logger = Logger.getLogger("org.archive.crawler.extractor.ExtractorDOC");

    private long numberOfCURIsHandled = 0;
    private long numberOfLinksExtracted = 0;

    /**
     * @param name
     */
    public ExtractorDOC(String name) {
        super(name, "DOC Extractor");
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
        
        numberOfCURIsHandled++;		
		
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
            numberOfLinksExtracted += links.size();
            curi.getAList().putObject(A_HTML_LINKS, links);
		}
		curi.linkExtractorFinished(); // Set flag to indicate that link extraction is completed.
		logger.fine(curi + " has " + links.size() + " links.");
	}

    /* (non-Javadoc)
     * @see org.archive.crawler.framework.Processor#report()
     */
    public String report() {
        StringBuffer ret = new StringBuffer();
        ret.append("Processor: org.archive.crawler.extractor.ExtractorDOC\n");
        ret.append("  Function:          Link extraction on MS Word documents (.doc)\n");
        ret.append("  CrawlURIs handled: " + numberOfCURIsHandled + "\n");
        ret.append("  Links extracted:   " + numberOfLinksExtracted + "\n\n");
        
        return ret.toString();
    }
}
