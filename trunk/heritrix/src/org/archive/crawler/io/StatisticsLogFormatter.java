/*
 * Created on Jul 22, 2003
 *
 */
package org.archive.crawler.io;

import java.util.logging.Formatter;
import java.util.logging.LogRecord;

import java.text.DecimalFormat;

import org.apache.commons.httpclient.methods.GetMethod;
import org.archive.crawler.datamodel.CoreAttributeConstants;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.util.ArchiveUtils;


/**
 * @author Parker Thompson
 *
 */
public class StatisticsLogFormatter extends Formatter {

	/**
	 * 
	 */
	public StatisticsLogFormatter() {
		super();
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see java.util.logging.Formatter#format(java.util.logging.LogRecord)
	 */
	public String format(LogRecord record) {
		return record.getMessage() + "\n";
	}
}
