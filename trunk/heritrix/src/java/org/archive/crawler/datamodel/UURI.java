/* UURI
 *
 * $Id$
 *
 * Created on Apr 18, 2003
 *
 * Copyright (C) 2003 Internet Archive.
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
 */
package org.archive.crawler.datamodel;

import java.io.Serializable;

import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;
import org.archive.util.SURT;
import org.archive.util.TextUtils;


/**
 * Usable URI.
 * 
 * This class wraps {@link org.apache.commons.httpclient.URI} adding caching
 * and methods. It cannot be instantiated directly.  Go via UURIFactory.
 * 
 *  <p>We used to use {@link java.net.URI} for parsing URIs but ran across
 * quirky behaviors and bugs.  {@link java.net.URI} is not subclassable --
 * its final -- and its unlikely that java.net.URI will change any time soon
 * (See Gordon's considered petition here:
 * <a href="http://developer.java.sun.com/developer/bugParade/bugs/4939847.html">java.net.URI
 * should have loose/tolerant/compatibility option (or allow reuse)</a>).
 *
 * <p>This class tries to cache calculated strings such as the extracted host
 * and this class as a string rather than have the parent class rerun its
 * calculation everytime.
 *
 * @author gojomo
 * @author stack
 *
 * @see org.apache.commons.httpclient.URI
 */
public class UURI extends URI
implements Serializable {
    
    public static final String MASSAGEHOST_PATTERN = "^www\\d*\\.";

    /**
     * Cache of the host name.
     *
     * Super class calculates on every call.  Profiling shows us spend 30% of
     * total elapsed time in URI class.
     */
    private String cachedHost = null;

    /**
     * Cache of this uuri escaped as a string.
     *
     * Super class calculates on every call.  Profiling shows us spend 30% of
     * total elapsed time in URI class.
     */
    private String cachedEscapedURI = null;

    /**
     * Cache of this uuri escaped as a string.
     *
     * Super class calculates on every call.  Profiling shows us spend 30% of
     * total elapsed time in URI class.
     */
    private String cachedString = null;

    /**
     * Cache of this uuri in SURT format
     */
    private String surtForm = null;


    /**
     * Shutdown access to default constructor.
     */
    protected UURI() {
        super();
    }
    
    /**
     * @param uri String representation of an absolute URI.
     * @param escaped If escaped.
     * @param charset Charset to use.
     * @throws org.apache.commons.httpclient.URIException
     */
    protected UURI(String uri, boolean escaped, String charset)
    throws URIException {
        super(uri, escaped, charset);
        normalize();
    }
    
    /**
     * @param relative String representation of URI.
     * @param base Parent UURI to use derelativizing.
     * @throws org.apache.commons.httpclient.URIException
     */
    protected UURI(UURI base, UURI relative) throws URIException {
        super(base, relative);
        normalize();
    }
    
    /**
     * @param uri URI as string that is resolved relative to this UURI.
     * @param charset Charset to use.
     * @return UURI that uses this UURI as base.
     * @throws URIException
     */
    public UURI resolve(String uri)
    throws URIException {
        return resolve(uri, UURIFactory.isEscaped(uri),
            this.getProtocolCharset());
    }

    /**
     * @param uri URI as string that is resolved relative to this UURI.
     * @param charset Charset to use.
     * @return UURI that uses this UURI as base.
     * @throws URIException
     */
    public UURI resolve(String uri, boolean escaped)
    throws URIException {
        return resolve(uri, escaped, this.getProtocolCharset());
    }
    
    /**
     * @param uri URI as string that is resolved relative to this UURI.
     * @param escaped True if uri is escaped.
     * @param charset Charset to use.
     * @return UURI that uses this UURI as base.
     * @throws URIException
     */
    public UURI resolve(String uri, boolean escaped, String charset)
    throws URIException {
        return new UURI(this, new UURI(uri, escaped, charset));
    }

    /**
     * Test an object if this UURI is equal to another.
     *
     * @param obj an object to compare
     * @return true if two URI objects are equal
     */
    public boolean equals(Object obj) {

        // normalize and test each components
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof UURI)) {
            return false;
        }
        UURI another = (UURI) obj;
        // scheme
        if (!equals(this._scheme, another._scheme)) {
            return false;
        }
        // is_opaque_part or is_hier_part?  and opaque
        if (!equals(this._opaque, another._opaque)) {
            return false;
        }
        // is_hier_part
        // has_authority
        if (!equals(this._authority, another._authority)) {
            return false;
        }
        // path
        if (!equals(this._path, another._path)) {
            return false;
        }
        // has_query
        if (!equals(this._query, another._query)) {
            return false;
        }
        // UURIs do not have fragments
        return true;
    }

    /**
     * Strips www variants from the host.
     *
     * Strips www[0-9]*\. from the host.  If calling getHostBaseName becomes a
     * performance issue we should consider adding the hostBasename member that
     * is set on initialization.
     *
     * @return Host's basename.
     * @throws URIException
     */
    public String getHostBasename() throws URIException {
        if (this.getHost() != null) {
            return TextUtils.replaceFirst(MASSAGEHOST_PATTERN, this.getHost(),
                UURIFactory.EMPTY_STRING);
        }
        return null;
    }

    /**
     * Override to cache result
     * @return String representation of this URI 
     */
    public String toString() {
        if (this.cachedString == null) {
            synchronized (this) {
                if (this.cachedString == null) {
                    this.cachedString = super.toString();
                }
            }
        }
        return this.cachedString;
    }

    public String getEscapedURI() {
        if (this.cachedEscapedURI == null) {
            synchronized (this) {
                if (this.cachedEscapedURI == null) {
                    this.cachedEscapedURI = super.getEscapedURI();
                }
            }
        }
        return this.cachedEscapedURI;
    }

    public synchronized String getHost() throws URIException {
        if (this.cachedHost == null) {
            // If this._host is null, 3.0 httpclient throws
            // illegalargumentexception.  Don't go there.
            if (this._host != null) {
            	this.cachedHost = super.getHost();
            }
        }
        return this.cachedHost;
    }
    
    /**
     * Return the referenced host in the UURI, if any, also extracting the 
     * host of a DNS-lookup URI where necessary. 
     * 
     * @return the target or topic host of the URI
     * @throws URIException
     */
    public String getReferencedHost() throws URIException {
        String referencedHost = this.getHost();
        if(referencedHost==null && this.getScheme().equals("dns")) {
            // extract target domain of DNS lookup
            String possibleHost = this.getCurrentHierPath();
            if(possibleHost != null && possibleHost.matches("[-_\\w\\.:]+")) {
                referencedHost = possibleHost;
            }
        }
        return referencedHost;
    }

    /**
     * @return Return the 'SURT' format of this UURI
     * 
     * @throws URIException
     */
    public String getSurtForm() {
        if (surtForm == null) {
            surtForm = SURT.fromURI(this.toString());
        }
        return surtForm;
    }
    
    /**
     * Return the authority minus userinfo (if any).
     * 
     * If no userinfo present, just returns the authority.
     * 
     * @return The authority stripped of any userinfo if present.
     * @throws URIException
     */
	public String getAuthorityMinusUserinfo()
    throws URIException {
        String result = getAuthority();
        if (result != null && result.length() > 0) {
        	int index = result.indexOf('@');
            if (index >= 0 && index < result.length()) {
            	result = result.substring(index + 1);
            }
        }
        return result;
	}
}
