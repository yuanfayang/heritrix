/* 
 * Filter.java
 * Created on Apr 16, 2003
 *
 * $Header$
 */
package org.archive.crawler.framework;

/**
 * 
 * @author Gordon Mohr
 */
public interface Filter {
	public boolean accepts(CrawlURI curi);
	public boolean accepts(NormalizedURIString curi);
}
