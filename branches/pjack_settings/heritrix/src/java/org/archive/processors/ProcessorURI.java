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
import java.util.Map;
import java.util.Set;

import org.apache.commons.httpclient.HttpMethod;
import org.archive.net.UURI;
import org.archive.processors.credential.CredentialAvatar;
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
    long getFetchBeginTime();
    void setFetchBeginTime(long time);
    
    // Used to be a map attribute
    long getFetchCompletedTime();
    void setFetchCompletedTime(long time);
    
    String getDNSServerIPLabel();
    void setDNSServerIPLabel(String label);
    
    FetchType getFetchType();
    
    Recorder getRecorder();

    Map<String,Object> getData();
    
    String getUserAgent();
    String getFrom();

    void setContentSize(long size);
    void setContentDigest(String algorithm, byte[] digest);

    String getContentType();
    void setContentType(String mimeType);

    Collection<String> getAnnotations();
    Collection<Throwable> getNonFatalFailures();
    
    // Give a better name?
    int getFetchStatus();
    void setFetchStatus(int status);
    
    // Used to be a map attribute. May still want to be one.
    void setHttpMethod(HttpMethod method);
    

    /* OK, the Heritrix implementation of this will need to merge 
     * CredentialAvatars from the server with CredentialAvatars from
     * this instance itself.  CredentialAvatars from this instnace itself
     * must be promoted to the proper CrawlServer.
     * 
     * However, the non-Heritrix implementation might not do any of that.
     * In particular I'm trying to free FetchHTTP from needing to know about
     * CrawlServers.
     * 
     * See the old versions of FetchHTTP.populateCredentials and
     * FetchHTTP.promoteCredentials for the Heritrix implementation of this
     * method.
     * 
     * @param method
     */
    boolean populateCredentials(HttpMethod method);
    void promoteCredentials();
    
    Set<CredentialAvatar> getCredentialAvatars();

    /**
     * Detaches an Rfc2617Credential if one exists.
     * 
     * Heritrix implementation lives in FetchHTTP.handle401
     * 
     * @return  true if a Rfc2617Credential existed and was detached
     */
    boolean detachRfc2617Credential(String realm);
    
    
    /**
     * Search this curi's environment for an RFC 2617 credential that is 
     * appropriate for the realm.
     * 
     * Heritrix implementation lives in FetchHTTP.handle401
     * 
     * @param realm
     * @return
     */
    boolean attachRfc2617Credential(String realm);


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
    boolean passedDNS();

    
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

    
    void skipToPostProcessing();
}
