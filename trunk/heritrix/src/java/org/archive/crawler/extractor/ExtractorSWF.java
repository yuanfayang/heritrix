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
 * Created on Jul 9, 2003
 *
 */
package org.archive.crawler.extractor;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.logging.Logger;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.methods.GetMethod;
import org.archive.crawler.datamodel.CoreAttributeConstants;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.framework.Processor;
import org.archive.io.NullOutputStream;

import com.anotherbigidea.flash.readers.SWFReader;
import com.anotherbigidea.flash.readers.TagParser;
import com.anotherbigidea.flash.writers.SWFWriter;
import com.anotherbigidea.io.InStream;

/**
 * Extract URIs from SWF (flash/shockwave) files.
 * 
 * @author Parker Thompson
 *
 */
public class ExtractorSWF extends Processor implements CoreAttributeConstants {

    private static Logger logger = Logger.getLogger("org.archive.crawler.extractor.ExtractorSWF");

    protected long numberOfCURIsHandled = 0;
    protected long numberOfLinksExtracted = 0;

    /**
     * @param name
     */
    public ExtractorSWF(String name) {
        super(name, "Flash extractor");
    }

	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.Processor#process(org.archive.crawler.datamodel.CrawlURI)
	 */
	protected void innerProcess(CrawlURI curi) {

		ArrayList links  = new ArrayList();
		GetMethod get = null;
		InputStream documentStream = null;
		Writer out = null;
		
		// assumes swfs will be coming in through http
		//TODO make htis more general (currently we're only fetching via http so it doesn't matter)
		if(! curi.getAList().containsKey(A_HTTP_TRANSACTION)) {
			return;
		}
		 get = (GetMethod)curi.getAList().getObject(A_HTTP_TRANSACTION);

		Header contentType = get.getResponseHeader("Content-Type");		
		if ((contentType==null)||(!contentType.getValue().startsWith("application/x-shockwave-flash"))) {
			// nothing to extract for other types here
			return; 
		}
		
        numberOfCURIsHandled++;
        
		// get the swf as a File
		try{
			documentStream = get.getHttpRecorder().getRecordedInput().getContentReplayInputStream();
		}catch(IOException e){
			e.printStackTrace();
		}
		
		if (documentStream==null) {
			// TODO: note problem
			return;
		}
		
		InStream in = new InStream(documentStream);
		
		// the lib wants something to write to.  Since we don't want output we'll just
		// pass it a good 'ol NullOutputStream object.  In the long-term it may be better
		// to create our own classes for javaswf that more effeciently (and generally)
		// parse files, but this will work for now and should hopefully not prove to be
		// a performance bottleneck anyway.
		NullOutputStream nos = new NullOutputStream();
		SWFWriter tags = new SWFWriter(nos);
	
		IATagParser iatp = new IATagParser(tags);		
		TagParser parser = new TagParser( iatp );

		SWFReader reader = new SWFReader( parser, in );
        
		try {
			reader.readFile();
		} catch (IOException e) {
			// this direct writing to the crawlErrors log 
			// couldn't possibly work: the crawlError log 
			// currently expects a runtime error instance inside 
			// the problem crawlURI
//			Object array[] = { curi };
//			controller.crawlErrors.log(
//				Level.INFO,
//				curi.getUURI().getUri().toString(),
//				array);
			curi.addLocalizedError(getName(),e,null);
		} finally {
			try {
				documentStream.close();
			} catch (IOException ignored) {

			}
		}
		
		links = iatp.getLinks();
		if(links.size() > 0){
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
        ret.append("Processor: org.archive.crawler.extractor.ExtractorSWF\n");
        ret.append("  Function:          Link extraction on Shockwave Flash documents (.swf)\n");
        ret.append("  CrawlURIs handled: " + numberOfCURIsHandled + "\n");
        ret.append("  Links extracted:   " + numberOfLinksExtracted + "\n\n");
        
        return ret.toString();
    }

}
