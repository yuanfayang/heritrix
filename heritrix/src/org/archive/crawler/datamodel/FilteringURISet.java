/*
 * FilteringURISet.java
 * Created on Apr 21, 2003
 *
 * $Header$
 */
package org.archive.crawler.datamodel;

import org.archive.crawler.framework.UURIFilter;

/**
 * A URISet that also filters, for example by requiring
 * passed-in URIs to be extensions of the contained URIs.
 * 
 * @author gojomo
 *
 */
public interface FilteringURISet extends UURIFilter, UURISet {

}
