/*
 * FetchFormatter.java
 * Created on Jun 10, 2003
 *
 * $Header$
 */
package org.archive.crawler.io;

import java.net.URI;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * @author gojomo
 *
 */
public class FetchFormatter extends Formatter {

	/* (non-Javadoc)
	 * @see java.util.logging.Formatter#format(java.util.logging.LogRecord)
	 */
	public String format(LogRecord lr) {
		int threadNumber = ((Integer)lr.getParameters()[0]).intValue();
		int statusCode = ((Integer)lr.getParameters()[1]).intValue();
		String length = (String)lr.getParameters()[2];
		URI uri = (URI)lr.getParameters()[3];
		return threadNumber + " " + statusCode+" "+length+" "+uri+"\n";
	}

}
