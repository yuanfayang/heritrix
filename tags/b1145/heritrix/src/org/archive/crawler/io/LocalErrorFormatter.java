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
import org.archive.crawler.datamodel.LocalizedError;

/**
 * @author gojomo
 *
 */
public class LocalErrorFormatter extends UriProcessingFormatter implements CoreAttributeConstants {
	
	/* (non-Javadoc)
	 * @see java.util.logging.Formatter#format(java.util.logging.LogRecord)
	 */
	public String format(LogRecord lr) {
		LocalizedError err = (LocalizedError) lr.getParameters()[1];
		Exception ex = (Exception)err.exception;
		StringWriter sw = new StringWriter();
		ex.printStackTrace(new PrintWriter(sw));
		
		return super.format(lr) + " " + sw.toString();
	}
}


