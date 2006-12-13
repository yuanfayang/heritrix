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
 * ProcessorURI.java
 * Created on October 5, 2006
 *
 * $Header$
 */
package org.archive.processors;


import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.URIException;
import org.archive.net.UURI;
import org.archive.processors.credential.CredentialAvatar;
import org.archive.processors.extractor.Link;
import org.archive.processors.extractor.LinkContext;
import org.archive.processors.util.RobotsHonoringPolicy;
import org.archive.state.StateProvider;
import org.archive.util.Recorder;


/**
 * A URI to be processed.
 * 
 * @author pjack
 */
public interface ProcessorURI extends StateProvider {

    
    public static enum FetchType { HTTP_GET, HTTP_POST, UNKNOWN };
    
    /**
     * Returns the URI being processed.
     * 
     * @return  the URI
     */
    UURI getUURI();

    
    UURI getVia();
    
    boolean isPrerequisite();
    void setPrerequisite(boolean prereq);

    // Used to be a map attribute.
    void setError(String msg);
    
    // Used to be a map attribute.
    long getFetchBeginTime();
    void setFetchBeginTime(long time);
    
    // Used to be a map attribute
    long getFetchCompletedTime();
    void setFetchCompletedTime(long time);
    
    String getDNSServerIPLabel();
    void setDNSServerIPLabel(String label);
    
    FetchType getFetchType();
    void setFetchType(FetchType type);
    
    Recorder getRecorder();

    Map<String,Object> getData();
    
    String getUserAgent();
    String getFrom();

    long getContentSize();
    void setContentSize(long size);
    void setContentDigest(String algorithm, byte[] digest);

    String getContentType();
    void setContentType(String mimeType);

    long getContentLength();
    
    Collection<String> getAnnotations();
    Collection<Throwable> getNonFatalFailures();
    
    // Give a better name?
    int getFetchStatus();
    void setFetchStatus(int status);
    
    // Used to be a map attribute. May still want to be one.
    HttpMethod getHttpMethod();
    void setHttpMethod(HttpMethod method);
    
    
    boolean hasCredentialAvatars();
    Set<CredentialAvatar> getCredentialAvatars();



    /*
     * True if the DNS lookup occurred and worked.
     * 
     * Heritrix implementation should be:
     * 
     *          CrawlHost host = getController().getServerCache().getHostFor(curi);
         // make sure the dns lookup succeeded
         if (host.getIP() == null && host.hasBeenLookedUp()) {
             curi.setFetchStatus(S_DOMAIN_PREREQUISITE_FAILURE);
             return false;
         }
         return true;

     * 
     * 
     */
    
    void requestCrawlPause();
    RobotsHonoringPolicy getRobotsHonoringPolicy();
    void skipToPostProcessing();

    
    // Eliminate CrawlURI.hasCredentialAvatars


    /*
     * Host name as determined by DNS? Maybe? Needed by Credential.rootUriMatch
     * 
     * Heritrix implementation is:
     * 
     *   String serverName = controller.getServerCache().getServerFor(curi).
     *       getName();
     */
    String getResolvedName();

    


    String getPathFromSeed();
    boolean isSeed();
    void setSeed(boolean seed);
    
    boolean isLocation();

    
    List<Link> getOutLinks();
    
    UURI getBaseURI();
    void setBaseURI(UURI base);

    boolean hasBeenLinkExtracted();
    void linkExtractorFinished();

    void addUriError(URIException e, String uri);

    LinkContext getViaContext();

}
