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
public class Processor extends XMLConfig {
	CrawlController controller;
	Processor defaultNext;
	
	public void process(CrawlURI curi) {
		// by default, simply forward curi along
		curi.setNextProcessor(getDefaultNext());
	}
	
	/**
	 * 
	 */
	private Processor getDefaultNext() {
		return defaultNext;
		
	}
	
	public void initialize(CrawlController c) {
		controller = c;
		Node n;
		if ((n=xNode.getAttributes().getNamedItem("next"))!=null) {
			defaultNext = (Processor)c.getProcessors().get(n.getNodeValue());
		}
	}
}
