/*
 * UriErrorFormatter.java
 * Created on Jul 7, 2003
 *
 * $Header$
 */
package org.archive.crawler.io;

import java.util.logging.Formatter;
import java.util.logging.LogRecord;

import org.archive.crawler.datamodel.CoreAttributeConstants;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.util.ArchiveUtils;

/**
 * @author gojomo
 *
 */
public class UriErrorFormatter extends Formatter implements CoreAttributeConstants {
	
	/* (non-Javadoc)
	 * @see java.util.logging.Formatter#format(java.util.logging.LogRecord)
	 */
	public String format(LogRecord lr) {
		CrawlURI curi = (CrawlURI) lr.getParameters()[0];
		String problem = (String) lr.getParameters()[1];
				
		return ArchiveUtils.get17DigitDate()
		+ " "
		+ ( (curi ==null) ? "n/a" : curi.getURIString() )
		+ " \""
		+ lr.getMessage()
		+ "\" "
		+ problem
		+ "\n";
	}
}

