/*
 * CrawlErrorFormatter.java
 * Created on Jul 7, 2003
 *
 * $Header$
 */
package org.archive.crawler.io;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.LogRecord;

import org.archive.crawler.datamodel.CoreAttributeConstants;
import org.archive.crawler.datamodel.CrawlURI;

/**
 * @author gojomo
 *
 */
public class CrawlErrorFormatter extends UriProcessingFormatter implements CoreAttributeConstants {
	
	/* (non-Javadoc)
	 * @see java.util.logging.Formatter#format(java.util.logging.LogRecord)
	 */
	public String format(LogRecord lr) {
		CrawlURI curi = (CrawlURI) lr.getParameters()[0];
		RuntimeException e = (RuntimeException)curi.getAList().getObject(A_RUNTIME_EXCEPTION);
		StringWriter sw = new StringWriter();
		e.printStackTrace(new PrintWriter(sw));
		
		return super.format(lr) + sw.toString();
	}
}


