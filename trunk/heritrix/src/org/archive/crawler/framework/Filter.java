/* 
 * Filter.java
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
public interface Filter {
	public void setName(String name);
	public String getName();
	public boolean accepts(CrawlURI curi);
	public boolean accepts(UURI uuri);
}
