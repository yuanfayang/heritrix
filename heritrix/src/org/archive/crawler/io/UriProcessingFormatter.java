/*
 * UriProcessingFormatter.java
 * Created on Jun 10, 2003
 *
 * $Header$
 */
package org.archive.crawler.io;

import java.text.DecimalFormat;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

import org.apache.commons.httpclient.methods.GetMethod;
import org.archive.crawler.datamodel.CandidateURI;
import org.archive.crawler.datamodel.CoreAttributeConstants;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.UURI;
import org.archive.util.ArchiveUtils;

/**
 * Formmatter for 'crawl.log'. Expects completed CrawlURI as parameter.  
 *   
 * @author gojomo
 *
 */
public class UriProcessingFormatter extends Formatter implements CoreAttributeConstants {
	static DecimalFormat STATUS_FORMAT = new DecimalFormat("-####");
	static DecimalFormat LENGTH_FORMAT = new DecimalFormat("#,##0");
	
	/* (non-Javadoc)
	 * @see java.util.logging.Formatter#format(java.util.logging.LogRecord)
	 */
	public String format(LogRecord lr) {
		CrawlURI curi = (CrawlURI) lr.getParameters()[0];

		String length = ".";
		String mime = ".";
		String uri = curi.getUURI().getUri().toASCIIString();
		if ( curi.getAList().containsKey(A_HTTP_TRANSACTION)) {
			GetMethod get = (GetMethod) curi.getAList().getObject(A_HTTP_TRANSACTION);
				
			if(curi.getContentLength()>=0) {
				length = Long.toString(curi.getContentLength());
			} else if (curi.getContentSize()>0) {
				length = Long.toString(curi.getContentSize());
			}
			
			if (get.getResponseHeader("Content-Type")!=null) {
				mime = get.getResponseHeader("Content-Type").getValue();
			}
		} else {
			if (curi.getContentSize()>0) {
				length = Long.toString(curi.getContentSize());
 
			}
			if (curi.getContentType() != null) {
				mime = curi.getContentType();
			} 
		}
		long time;
		if(curi.getAList().containsKey(A_FETCH_COMPLETED_TIME)) {
			time = curi.getAList().getLong(A_FETCH_COMPLETED_TIME);
		} else {
			time = System.currentTimeMillis();
		}
		
		Object via = curi.getVia();
		if (via instanceof CandidateURI) {
			via = ((CandidateURI)via).getUURI().getUri().toASCIIString();
		}
		if (via instanceof UURI) {
			via = ((UURI)via).getUri().toASCIIString();
		}
		
		// allow get to be GC'd
		curi.getAList().remove(A_HTTP_TRANSACTION);

		return ArchiveUtils.get17DigitDate(time)
			+ " "
			+ ArchiveUtils.padTo(curi.getFetchStatus(),4)
			+ " "
			+ ArchiveUtils.padTo(length,10)
			+ " "
			+ "#"
			+ curi.getThreadNumber()
			+ " "
			+ uri
			+ " "
			+ mime
			+ "\n"
			+ "  "
			+ curi.getPathFromSeed()
			+ " "
			+ via
			+ "\n";
	}

}


