/*
 * BasicScope.java
 * Created on Oct 1, 2003
 *
 * $Header$
 */
package org.archive.crawler.basic;

import org.archive.crawler.framework.CrawlScope;

/**
 * A core CrawlScope suitable for the most common
 * crawl needs.
 * 
 * Roughly, its logic is that a URI is included if:
 * 
 *    ( isSeed(uri) || focusFilter.accepts(uri) ) 
 *     && ! excludeFilter.accepts(uri)
 * 
 * @author gojomo
 *
 */
public class BasicScope extends CrawlScope {
	
}
