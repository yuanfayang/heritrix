/*
 * Created on Jul 9, 2003
 *
 */
package org.archive.crawler.extractor;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.logging.Level;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.methods.GetMethod;
import org.archive.crawler.datamodel.CoreAttributeConstants;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.framework.Processor;
import org.archive.util.IATagParser;
import org.archive.util.NullOutputStream;

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
		
		
		// get the swf as a File
		try{
			documentStream = get.getResponseBodyAsStream();
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
        
		try{
			reader.readFile();		
		}catch(IOException e){
			Object array[] = { curi };
			controller.crawlErrors.log(Level.INFO, curi.getUURI().getUri().toString(), array);
		}
		
		links = iatp.getLinks();
		if(links.size() > 0){
			curi.getAList().putObject("html-links", links);
		}
		
		logger.fine(curi + " has " + links.size() + " links.");
	}

}
