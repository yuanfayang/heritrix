/* Copyright (C) 2006 Internet Archive.
 *
 * This file is part of the Heritrix web crawler (crawler.archive.org).
 *
 * Heritrix is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * any later version.
 *
 * Heritrix is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser Public License for more details.
 *
 * You should have received a copy of the GNU Lesser Public License
 * along with Heritrix; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * ExtractorURI.java
 * Created on October 5, 2006
 *
 * $Header$
 */
package org.archive.crawler2.extractor;


import java.io.IOException;
import java.util.Set;

import org.apache.commons.httpclient.URIException;
import org.archive.crawler.datamodel.RobotsHonoringPolicy;
import org.archive.crawler2.framework.ProcessorURI;
import org.archive.io.ReplayCharSequence;
import org.archive.io.SeekInputStream;
import org.archive.net.UURI;


/**
 * A fetched URI whose links should be extracted.
 * 
 * @author pjack
 */
public interface ExtractorURI extends ProcessorURI {
    
    
    /**
     * The content-type of the fetched content for the URI.
     * 
     * @return  the content-type
     */
    String getContentType();


    /**
     * The context that led to this URI.  Can be useful for determining 
     * whether or not your extractor should run.  For instance, 
     * {@link ExtractorJS} uses the context to see if the URI was
     * discovered via a &lt;SCRIPT&gt; tag.
     * 
     * @return  the link context
     */
    LinkContext getViaContext();
    
    
    /**
     * The length in bytes of the fetched content for the URI.
     * 
     * @return  the content length
     */
    long getContentLength();
    
    
    /**
     * The set of links extracted from the fetched content.  Extractor 
     * implementations should populate this set as they discover new links.
     * 
     * @return  the set of extracted links
     */
    Set<Link> getOutLinks();


    /**
     * Returns the fetched content as a CharSequence.
     * 
     * @return   the fetched content
     * @throws IOException   if an IO error occurs
     */
    ReplayCharSequence getCharSequence() throws IOException;
    
    
    /**
     * Returns the fetched content as an InputStream.
     * 
     * @return   the fetched content as an InputStream
     * @throws IOException   if an IO error occurs
     */
    SeekInputStream getInputStream() throws IOException;
    
    
    /**
     * This method is in dire need of a better name.
     * FIXME: Better yet, shouldn't this just be a custom Logger level?
     * 
     * @param e     the exception to log
     * @param msg   a message to log with the exception
     */
    void addLocalizedError(Throwable e, String msg);
    
    
    /**
     * Logs a problematic URI.
     * 
     * FIXME: Shouldn't this just be a custom Logger level?
     * FIXME: Better yet, shouldn't UURIFactory do this automatically?
     * 
     * @param e   the exception to log
     * @param badURI   the URI that caused the exception
     */
    void addUriError(URIException e, String badURI);

    
    /**
     * Returns true if some Extractor has invoked 
     * {@link #linkExtractionFinished()}.  Generally, other Extractors 
     * should not process URIs when this flag is set.  The exception is for
     * things like ExtractorHTTP, which extracts links from the headers and
     * not from the content.
     * 
     * FIXME: Should we then have 2 flags, one for content extraction and
     * one for header extraction?
     * 
     * @return   true if some Extractor has set the finished flag
     */
    boolean isLinkExtractionFinished();


    /**
     * Indicates that link extraction was completed successfully on this URI.
     * This flag prevents other extractors (which are potentially costly) from
     * processing this URI.
     */
    void linkExtractionFinished();

    
    /**
     * Returns the "base" URI for this URI.  If no explicit base URI was set
     * using {@link #setBaseURI(String)}, then this method will return 
     * {@link #getUURI()}.
     * 
     * @return   the base URI
     */
    UURI getBaseURI();
    
    
    /**
     * Sets the base URI.  This is essentially for handling the HTML META
     * BASE tag, which redefines the root of HREF elements in the HTML 
     * document.
     * 
     * <p>If set to null, then {@link #getUURI()} will be considered the
     * base URI.
     * 
     * @param s   the new base URI
     * @throws URIException    if the given string is not a valid URI
     */
    void setBaseURI(String s) throws URIException;
    
    
    /**
     * Returns the robots honoring policy for this URI.  This is essentially
     * for handling the HTML META ROBOTS tag.  The policy may indicate that
     * such crawler guidelines are to be ignored.
     * 
     * @return  the robots honoring policy
     */
    RobotsHonoringPolicy getRobotsHonoringPolicy();
    
    
    /**
     * Adds an annotation to this URI.  Annotations are short messages that
     * appear in crawl logs.
     * 
     * @param msg  a short annotation
     */
    void addAnnotation(CharSequence msg);


}
