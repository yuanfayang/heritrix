/* 
 * Processor.java
 * Created on Apr 16, 2003
 *
 * $Header$
 */
package org.archive.crawler.framework;

import org.archive.crawler.datamodel.*;
import org.archive.crawler.datamodel.CrawlURI;

/**
 * 
 * @author Gordon Mohr
 */
public class Processor {
	public void process(CrawlURI curi) {
		// by default, simply forward curi along
		curi.setNextProcessor(getDefaultNext());
	}
	
	/**
	 * 
	 */
	private Processor getDefaultNext() {
		return null;
		
	}
	
	public void init(Config conf, CrawlController controller) {
		// default do nothing
	}
}
