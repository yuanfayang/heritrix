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
import org.archive.crawler.datamodel.CoreAttributeConstants;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.util.ArchiveUtils;

/**
 * Expects parameters 
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
			// allow get to be GC'd
			curi.getAList().remove(A_HTTP_TRANSACTION);
				
			if (get.getResponseHeader("Content-Length")!=null) {
				length = get.getResponseHeader("Content-Length").getValue();
			}
			if (get.getResponseHeader("Content-Type")!=null) {
				mime = get.getResponseHeader("Content-Type").getValue();
			}
		}
		long time;
		if(curi.getAList().containsKey(A_FETCH_COMPLETED_TIME)) {
			time = curi.getAList().getLong(A_FETCH_COMPLETED_TIME);
		} else {
			time = System.currentTimeMillis();
		}
		
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
			+ "\n";
	}

}


