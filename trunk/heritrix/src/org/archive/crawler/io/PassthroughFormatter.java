/*
 * Created on Nov 21, 2003
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.archive.crawler.io;

import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * Formatter which merely prints the passed-in message, with no other
 * adornment. 
 * 
 * @author gojomo
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class PassthroughFormatter extends Formatter {

	/* (non-Javadoc)
	 * @see java.util.logging.Formatter#format(java.util.logging.LogRecord)
	 */
	public String format(LogRecord record) {
		return record.getMessage();
	}

}
