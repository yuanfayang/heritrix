/* 
 * Processor.java
 * Created on Apr 16, 2003
 *
 * $Header$
 */
package org.archive.crawler.framework;

import org.archive.crawler.datamodel.CrawlURI;
import org.w3c.dom.Node;

/**
 * 
 * @author Gordon Mohr
 */
public class Processor {
	CrawlController controller;
	
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
	
	public void init(Node xNode, CrawlController c) {
		controller = c;
	}
}
