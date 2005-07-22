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
package org.archive.net;

import java.io.Serializable;

import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;
import org.archive.crawler.datamodel.CandidateURI;
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
implements CharSequence, Serializable {
    /**
     * Consider URIs too long for IE as illegal.
     */
    public final static int MAX_URL_LENGTH = 2083;
    
    public static final String MASSAGEHOST_PATTERN = "^www\\d*\\.";

    /**
     * Cache of the host name.
     *
     * Super class calculates on every call.  Profiling shows us spend 30% of
     * total elapsed time in URI class.
     */
    private transient String cachedHost = null;
    
    /**
     * Cache of the host base name.
     */
    private transient String cachedHostBasename = null;

    /**
     * Cache of this uuri escaped as a string.
     *
     * Super class calculates on every call.  Profiling shows us spend 30% of
     * total elapsed time in URI class.
     */
    private transient String cachedEscapedURI = null;

    /**
     * Cache of this uuri escaped as a string.
     *
     * Super class calculates on every call.  Profiling shows us spend 30% of
     * total elapsed time in URI class.
     */
    private transient String cachedString = null;
    
    /**
     * Cached authority minus userinfo.
     */
    private transient String cachedAuthorityMinusUserinfo = null;

    /**
     * Cache of this uuri in SURT format
     */
    private transient String surtForm = null;
    
    // Technically, underscores are disallowed in the domainlabel
    // portion of hostname according to rfc2396 but we'll be more
    // loose and allow them. See: [ 1072035 ] [uuri] Underscore in
    // host messes up port parsing.
    static {
        hostname.set('_');
    }


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
     * @param e True if escaped.
     * @return UURI that uses this UURI as base.
     * @throws URIException
     */
    public UURI resolve(String uri, boolean e)
    throws URIException {
        return resolve(uri, e, this.getProtocolCharset());
    }
    
    /**
     * @param uri URI as string that is resolved relative to this UURI.
     * @param e True if uri is escaped.
     * @param charset Charset to use.
     * @return UURI that uses this UURI as base.
     * @throws URIException
     */
    public UURI resolve(String uri, boolean e, String charset)
    throws URIException {
        return new UURI(this, new UURI(uri, e, charset));
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
        if (this.cachedHostBasename == null) {
            cacheHostBasename();
        }
        return this.cachedHostBasename;
    }
    
    protected synchronized void cacheHostBasename() throws URIException {
        if (this.cachedHostBasename != null) {
            return;
        }
        if (this.getHost() != null) {
            this.cachedHostBasename = TextUtils.
                replaceFirst(MASSAGEHOST_PATTERN, this.getHost(),
                UURIFactory.EMPTY_STRING);
        }
    }

    /**
     * Override to cache result
     * @return String representation of this URI 
     */
    public synchronized String toString() {
        if (this.cachedString == null) {
            this.cachedString = super.toString();
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
        if (this.cachedAuthorityMinusUserinfo != null) {
            return this.cachedAuthorityMinusUserinfo;
        }
        String tmp = getAuthority();
        if (tmp != null && tmp.length() > 0) {
        	int index = tmp.indexOf('@');
            if (index >= 0 && index < tmp.length()) {
                tmp = tmp.substring(index + 1);
            }
        }
        this.cachedAuthorityMinusUserinfo = tmp;
        return this.cachedAuthorityMinusUserinfo;
	}

    /* (non-Javadoc)
     * @see java.lang.CharSequence#length()
     */
    public int length() {
        return getEscapedURI().length();
    }

    /* (non-Javadoc)
     * @see java.lang.CharSequence#charAt(int)
     */
    public char charAt(int index) {
        return getEscapedURI().charAt(index);
    }

    /* (non-Javadoc)
     * @see java.lang.CharSequence#subSequence(int, int)
     */
    public CharSequence subSequence(int start, int end) {
        return getEscapedURI().subSequence(start,end);
    }

    /* (non-Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(Object arg0) {
        return getEscapedURI().compareTo(arg0);
    }
    
    /**
     * Convenience method for finding the UURI inside an
     * Object likely to have one.
     * 
     * @param o Object that has a UURI
     * @return the UURI found
     */
    public static UURI from(Object o) {
        UURI u = null;
        if (o instanceof UURI) {
            u = (UURI)o;
        } else if (o instanceof CandidateURI) {
            u = ((CandidateURI) o).getUURI();
        } else {
            // TODO: build UURI from a String?
            // possibly fail with null rather than exception?
            if (o != null) {
                throw new IllegalArgumentException("Passed wrong type: " + o);
            }
        }
        return u;
    }
    
    /**
     * Overridden from superclass to apply fixes to the two 
     * marked lines, preventing the misinterpretation of URI
     * strings which begin with a ':' as absolute URIs. 
     *
     * See also HTTPClient bug #35148
     *  http://issues.apache.org/bugzilla/show_bug.cgi?id=35148
     * 
     * @see org.apache.commons.httpclient.URI#parseUriReference(java.lang.String, boolean)
     */
    protected void parseUriReference(String original, boolean escaped)
        throws URIException {

        // validate and contruct the URI character sequence
        if (original == null) {
            throw new URIException("URI-Reference required");
        }

        /* @
         *  ^(([^:/?#]+):)?(//([^/?#]*))?([^?#]*)(\?([^#]*))?(#(.*))?
         */
        String tmp = original.trim();
        
        /*
         * The length of the string sequence of characters.
         * It may not be equal to the length of the byte array.
         */
        int length = tmp.length();

        /*
         * Remove the delimiters like angle brackets around an URI.
         */
        if (length > 0) {
            char[] firstDelimiter = { tmp.charAt(0) };
            if (validate(firstDelimiter, delims)) {
                if (length >= 2) {
                    char[] lastDelimiter = { tmp.charAt(length - 1) };
                    if (validate(lastDelimiter, delims)) {
                        tmp = tmp.substring(1, length - 1);
                        length = length - 2;
                    }
                }
            }
        }

        /*
         * The starting index
         */
        int from = 0;

        /*
         * The test flag whether the URI is started from the path component.
         */
        boolean isStartedFromPath = false;
        int atColon = tmp.indexOf(':');
        int atSlash = tmp.indexOf('/');
// THIS NEXT LINE IS A CHANGE FROM SUPERCLASS
        if (atColon <= 0 || (atSlash >= 0 && atSlash < atColon)) {
            isStartedFromPath = true;
        }

        /*
         * <p><blockquote><pre>
         *     @@@@@@@@
         *  ^(([^:/?#]+):)?(//([^/?#]*))?([^?#]*)(\?([^#]*))?(#(.*))?
         * </pre></blockquote><p>
         */
        int at = indexFirstOf(tmp, isStartedFromPath ? "/?#" : ":/?#", from);
        if (at == -1) { 
            at = 0;
        }

        /*
         * Parse the scheme.
         * <p><blockquote><pre>
         *  scheme    =  $2 = http
         *              @
         *  ^(([^:/?#]+):)?(//([^/?#]*))?([^?#]*)(\?([^#]*))?(#(.*))?
         * </pre></blockquote><p>
         */
// THIS NEXT LINE IS A CHANGE FROM SUPERCLASS
        if (at > 0 && at < length && tmp.charAt(at) == ':') {
            char[] target = tmp.substring(0, at).toLowerCase().toCharArray();
            if (validate(target, scheme)) {
                _scheme = target;
            } else {
                throw new URIException("incorrect scheme");
            }
            from = ++at;
        }

        /*
         * Parse the authority component.
         * <p><blockquote><pre>
         *  authority =  $4 = jakarta.apache.org
         *                  @@
         *  ^(([^:/?#]+):)?(//([^/?#]*))?([^?#]*)(\?([^#]*))?(#(.*))?
         * </pre></blockquote><p>
         */
        // Reset flags
        _is_net_path = _is_abs_path = _is_rel_path = _is_hier_part = false;
        if (0 <= at && at < length && tmp.charAt(at) == '/') {
            // Set flag
            _is_hier_part = true;
            if (at + 2 < length && tmp.charAt(at + 1) == '/') {
                // the temporary index to start the search from
                int next = indexFirstOf(tmp, "/?#", at + 2);
                if (next == -1) {
                    next = (tmp.substring(at + 2).length() == 0) ? at + 2 
                        : tmp.length();
                }
                parseAuthority(tmp.substring(at + 2, next), escaped);
                from = at = next;
                // Set flag
                _is_net_path = true;
            }
            if (from == at) {
                // Set flag
                _is_abs_path = true;
            }
        }

        /*
         * Parse the path component.
         * <p><blockquote><pre>
         *  path      =  $5 = /ietf/uri/
         *                                @@@@@@
         *  ^(([^:/?#]+):)?(//([^/?#]*))?([^?#]*)(\?([^#]*))?(#(.*))?
         * </pre></blockquote><p>
         */
        if (from < length) {
            // rel_path = rel_segment [ abs_path ]
            int next = indexFirstOf(tmp, "?#", from);
            if (next == -1) {
                next = tmp.length();
            }
            if (!_is_abs_path) {
                if (!escaped 
                    && prevalidate(tmp.substring(from, next), disallowed_rel_path) 
                    || escaped 
                    && validate(tmp.substring(from, next).toCharArray(), rel_path)) {
                    // Set flag
                    _is_rel_path = true;
                } else if (!escaped 
                    && prevalidate(tmp.substring(from, next), disallowed_opaque_part) 
                    || escaped 
                    && validate(tmp.substring(from, next).toCharArray(), opaque_part)) {
                    // Set flag
                    _is_opaque_part = true;
                } else {
                    // the path component may be empty
                    _path = null;
                }
            }
            if (escaped) {
                setRawPath(tmp.substring(from, next).toCharArray());
            } else {
                setPath(tmp.substring(from, next));
            }
            at = next;
        }

        // set the charset to do escape encoding
        String charset = getProtocolCharset();

        /*
         * Parse the query component.
         * <p><blockquote><pre>
         *  query     =  $7 = <undefined>
         *                                        @@@@@@@@@
         *  ^(([^:/?#]+):)?(//([^/?#]*))?([^?#]*)(\?([^#]*))?(#(.*))?
         * </pre></blockquote><p>
         */
        if (0 <= at && at + 1 < length && tmp.charAt(at) == '?') {
            int next = tmp.indexOf('#', at + 1);
            if (next == -1) {
                next = tmp.length();
            }
            _query = (escaped) ? tmp.substring(at + 1, next).toCharArray() 
                : encode(tmp.substring(at + 1, next), allowed_query, charset);
            at = next;
        }

        /*
         * Parse the fragment component.
         * <p><blockquote><pre>
         *  fragment  =  $9 = Related
         *                                                   @@@@@@@@
         *  ^(([^:/?#]+):)?(//([^/?#]*))?([^?#]*)(\?([^#]*))?(#(.*))?
         * </pre></blockquote><p>
         */
        if (0 <= at && at + 1 <= length && tmp.charAt(at) == '#') {
            if (at + 1 == length) { // empty fragment
                _fragment = "".toCharArray();
            } else {
                _fragment = (escaped) ? tmp.substring(at + 1).toCharArray() 
                    : encode(tmp.substring(at + 1), allowed_fragment, charset);
            }
        }

        // set this URI.
        setURI();
    }
    
    
    
    /**
     * Test if passed String has likely URI scheme prefix.
     * @param possibleUrl URL string to examine.
     * @return True if passed string looks like it could be an URL.
     */
    public static boolean hasScheme(String possibleUrl) {
        boolean result = false;
        for (int i = 0; i < possibleUrl.length(); i++) {
            char c = possibleUrl.charAt(i);
            if (c == ':') {
                if (i != 0) {
                    result = true;
                }
                break;
            }
            if (!scheme.get(c)) {
                break;
            }
        }
        return result;
    }
}
