/*
 * CrawlScope.java
 * Created on May 21, 2003
 *
 * $Header$
 */
package org.archive.crawler.datamodel;

import org.w3c.dom.Node;

/**
 * @author gojomo
 *
 */
public class CrawlScope extends XMLConfig {
	
	/**
	 * @param node
	 */
	public CrawlScope(Node node) {
		xNode = nodeOrSrc(node);
	}

}
