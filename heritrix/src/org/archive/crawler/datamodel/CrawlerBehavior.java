/*
 * CrawlerBehavior.java
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
public class CrawlerBehavior extends XMLConfig {

	/**
	 * @param node
	 */
	public CrawlerBehavior(Node node) {
		xNode = nodeOrSrc(node);
	}

	/**
	 * @return
	 */
	public int getMaxToes() {
		return getIntAt("limits/max-toe-threads@value");
	}

}
