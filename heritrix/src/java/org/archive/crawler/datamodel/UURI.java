/* Copyright (C) 2003 Internet Archive.
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
 * URI.java
 * Created on Apr 18, 2003
 *
 * $Header$
 */
package org.archive.crawler.datamodel;

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Logger;

import org.archive.util.TextUtils;

/**
 * Usable URI: a legal URI for our purposes.
 *
 * These instances will always have been normalized
 * (massaged in ways that by spec and in practice,
 * do not change the URI's meaning or function) and
 * rehabilitated (patched in riskless or necessary
 * ways to be legal, eg escaping spaces).
 *
 * @author gojomo
 *
 */
public class UURI implements Serializable {
    // for now, consider URIs too long for IE as illegal
    // TODO: move this policy elsewhere
    private static int DEFAULT_MAX_URI_LENGTH = 2083;

    private static Logger logger =
        Logger.getLogger("org.archive.crawler.datamodel.UURI");

    protected java.net.URI uri;
    protected String uriString;

    public static UURI createUURI(String s) throws URISyntaxException {
        return new UURI(normalize(s));
    }

    /**
     * @param u
     */
    private UURI(URI u) throws URISyntaxException {
        uri = u;
        try {
            uriString = u.toASCIIString();
        } catch (NullPointerException npe) {
            throw new URISyntaxException(u.toString(),"URI.encode NPE");
        }
        if (uriString.length()>DEFAULT_MAX_URI_LENGTH) {
            throw new URISyntaxException(uriString,"Too Long");
        }
    }



    /**
     * Return a "normalized" String for the given String.
     * This is NOT the same as Alexa's canonicalization.
     *
     * Normalization cleans a URI to the maximum extent
     * possible without regard to what it would return
     * if fetched, or any special-casing based on past
     * observed behavior.
     *
     * For example, the URI scheme is case-flattened,
     * hostnames are case-flattened, default ports are
     * removed, and path-info is regularized.
     * @param u
     * @return A "normalized" String for the given String.
     * This is NOT the same as Alexa's canonicalization.
     * @throws URISyntaxException
     */
     public static URI normalize(String u) throws URISyntaxException {
        return normalize(u,null);
    }

    static final String DOTDOT = "^(/\\.\\.)+";

    static final String SLASH = "/";
    static final String HTTP = "http";
    static final String HTTP_PORT = ":80";
    static final String HTTPS = "https";
    static final String HTTPS_PORT = ":443";
    static final String DOT = ".";
    static final String EMPTY_STRING = "";
     
    /**
     * Normalize and derelativize
     *
     * @param s absolute or relative URI string
     * @param parent URI to use for derelativizing; may be null
     * @return A normalized and derelativized URL.
     * @throws URISyntaxException
     */
    public static URI normalize(String s, URI parent)
        throws URISyntaxException {

        if (s==null) {
            throw new URISyntaxException("n/a","Is null");
        }
        // TODO: stop creating temporary instances like a drunken sailor
        String es = patchEscape(s);
        URI u = new URI(es);
        if (!u.isAbsolute()) {
            if (parent==null) {
                throw new URISyntaxException(
                    s, "No parent supplied for relative URI.");
            }
            u = parent.resolve(es);
        }
        

        String scheme = u.getScheme().toLowerCase();
        if (u.getRawSchemeSpecificPart().startsWith(SLASH)) {
            // hierarchical URI
            // factor out path cruft, according to official spec
            u = u.normalize(); 
            // now, go further and eliminate extra '..' segments
            String fixedPath = 
                TextUtils.replaceFirst(DOTDOT, u.getPath(), EMPTY_STRING);
                
            if (EMPTY_STRING.equals(fixedPath)) {
                // ensure root URLs end with '/'
                fixedPath = SLASH;
            }
            
            String canonizedAuthority = u.getAuthority();
            if(canonizedAuthority==null) {
                //logger.warning("bad URI: "+s+" relative to "+parent);
                //return null;
                throw new URISyntaxException(
                    s, "uninterpretable relative to " + parent);
            }

            // TODO: fix the fact that this might clobber case-sensitive
            // user-info
            if (scheme.equals(HTTP)) {
                // case-flatten host, remove default port
                canonizedAuthority = canonizedAuthority.toLowerCase();
                // strip default port
                if (canonizedAuthority.endsWith(HTTP_PORT)) {
                    canonizedAuthority =
                        canonizedAuthority.substring(
                            0,
                            canonizedAuthority.length() - 3);
                }
                // chop trailing '.'
                if (canonizedAuthority.endsWith(DOT)) {
                    canonizedAuthority =
                        canonizedAuthority.substring(
                            0,
                            canonizedAuthority.length() - 1);
                }
                // chop leading '.'
                if (canonizedAuthority.startsWith(DOT)) {
                    canonizedAuthority =
                        canonizedAuthority.substring(
                            1,
                            canonizedAuthority.length());
                }

            } else if (scheme.equals(HTTPS)) {
                // case-flatten host, remove default port
                canonizedAuthority = canonizedAuthority.toLowerCase();
                if (canonizedAuthority.endsWith(HTTPS_PORT)) {
                    canonizedAuthority =
                        canonizedAuthority.substring(
                            0,
                            canonizedAuthority.length() - 4);
                }
            }
            u = new URI(scheme, // case-flatten scheme
                        canonizedAuthority, // case and port flatten
                        fixedPath, // leave alone
                        u.getQuery(), // leave alone
                        null); // drop fragment
        } else {
            // opaque URI
            u = new URI(scheme, // case-flatten scheme
                        u.getSchemeSpecificPart(), // leave alone
                        null); // drop fragment
        }

        return u;
    }

    static final String HTML_AMP_ENTITY = "&amp;";
    static final String AMP = "&";   
    static final String NBSP = "\\xA0";
    static final String SPACE = " ";
    static final String ESCAPED_SPACE = "%20";
    static final String PIPE = "\\|";
    static final String ESCAPED_PIPE = "%7C";
    static final String CIRCUMFLEX = "\\^";
    static final String ESCAPED_CIRCUMFLEX = "%5E";
    static final String QUOT = "\"";
    static final String ESCAPED_QUOT = "%22";
    static final String SQUOT = "'";
    static final String ESCAPED_SQUOT = "%27";
    static final String APOSTROPH = "`";
    static final String ESCAPED_APOSTROPH = "%60";
    static final String LSQRBRACKET = "\\[";
    static final String ESCAPED_LSQRBRACKET = "%5B";
    static final String RSQRBRACKET = "\\]";
    static final String ESCAPED_RSQRBRACKET = "%5D";
    static final String LCURBRACKET = "\\{";
    static final String ESCAPED_LCURBRACKET = "%7B";
    static final String RCURBRACKET = "\\}";
    static final String ESCAPED_RCURBRACKET = "%7D";
    static final String BACKSLASH = "\\\\";
    static final String ESCAPED_BACKSLASH = "%5C";
    static final String NEWLINE = "\n+|\r+";
    static final String IMPROPERESC_REPLACE = "%25$1";
    static final String IMPROPERESC = 
        "%((?:[^\\p{XDigit}])|(?:.[^\\p{XDigit}])|(?:\\z))";
        

    /** apply URI escaping where necessary
     *
     * @param s
     * @return A URI escaped string.
     */
    private static String patchEscape(String s) {
        // in a perfect world, s would already be escaped
        // but it may only be partially escaped, so patch it
        // up where necessary

        // replace nbsp with normal spaces (so that they get
        // stripped if at ends, or encoded if in middle)
        s = TextUtils.replaceAll(NBSP, s, SPACE);
        // strip ends whitespaces
        s = s.trim();
        // patch spaces
        if (s.indexOf(SPACE) >= 0) {
            s = TextUtils.replaceAll(SPACE, s, ESCAPED_SPACE);
        }        
        
        // Replace HTML ampersand entity.
        // TODO: decode more entities.
        if (s.indexOf(HTML_AMP_ENTITY) >= 0){
            s = TextUtils.replaceAll(HTML_AMP_ENTITY, s, AMP);
        }
        // escape  | ^ " ' ` [ ] { } \
        // (IE actually sends these unescaped, but they can't
        // be put into a java.net.URI instance)
        if (s.indexOf(PIPE) >= 0) {
            s = TextUtils.replaceAll(PIPE, s, ESCAPED_PIPE);
        }
        if (s.indexOf(CIRCUMFLEX) >= 0) {
            s = TextUtils.replaceAll(CIRCUMFLEX, s, ESCAPED_CIRCUMFLEX);
        }
        if (s.indexOf(QUOT) >= 0) {
            s = TextUtils.replaceAll(QUOT, s, ESCAPED_QUOT);
        }
        if (s.indexOf(SQUOT) >= 0) {
            s = TextUtils.replaceAll(SQUOT, s, ESCAPED_SQUOT);
        }
        if (s.indexOf(APOSTROPH) >= 0) {
            s = TextUtils.replaceAll(APOSTROPH, s, ESCAPED_APOSTROPH);
        }
        if (s.indexOf(LSQRBRACKET) >= 0) {
            s = TextUtils.replaceAll(LSQRBRACKET, s, ESCAPED_LSQRBRACKET);
        }
        if (s.indexOf(RSQRBRACKET) >= 0) {
            s = TextUtils.replaceAll(RSQRBRACKET, s, ESCAPED_RSQRBRACKET);
        }
        if (s.indexOf(LCURBRACKET) >= 0) {
            s = TextUtils.replaceAll(LCURBRACKET, s, ESCAPED_LCURBRACKET);
        }
        if (s.indexOf(RCURBRACKET) >= 0) {
            s = TextUtils.replaceAll(RCURBRACKET, s, ESCAPED_RCURBRACKET);
        }
        // IE acutally converts backslashes to slashes rather than to %5C.
        // Since URIs that have backslases usually work only with IE, we will
        // convert backslases to slases as well.
        // TODO: Maybe we can first convert backslashes by specs and than by IE
        // so that we fetch both versions. 
        if (s.indexOf(BACKSLASH) >= 0) {
            s = TextUtils.replaceAll(BACKSLASH, s, SLASH);
        }
        // escape improper escape codes; eg any '%' followed
        // by non-hex-digits or
        s = TextUtils.replaceAll(IMPROPERESC, s, IMPROPERESC_REPLACE);
        // twice just to be sure (actually, to handle multiple %% in a row)
        s = TextUtils.replaceAll(IMPROPERESC, s, IMPROPERESC_REPLACE);
        // kill newlines etc
        s = TextUtils.replaceAll(NEWLINE, s, EMPTY_STRING);

        return s;
    }


    public String toExternalForm(){
        return uri.toString();
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return "UURI<"+uri+">";
    }


    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object arg0) {
        if(! (arg0 instanceof UURI)) {
            return false;
        }
        return uri.equals(((UURI)arg0).getUri());
    }

    protected URI getUri() {
        return uri;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        return uri.hashCode();
    }

    /**
     * @param string
     * @param uri
     * @throws URISyntaxException
     * @return Created UURI.
     */
    public static UURI createUURI(String string, URI uri) throws URISyntaxException {
        return new UURI(normalize(string,uri));
    }

    static final String UNUSABLE_SCHEMES = "(?i)^(javascript:)|(aim:)";

    /**
     * @param string
     * @return True if a useable scheme.
     */
    private static boolean isUnusableScheme(String string) {
        if (TextUtils.matches(UNUSABLE_SCHEMES, string)) {
            return true;
        }
        return false;
    }

    /**
     * @return The uri scheme.
     */
    public String getScheme() {
        return uri.getScheme();
    }

    /**
     * @return The uri path.
     */
    public String getPath() {
        return uri.getPath();
    }

    /**
     * @return The uri as a string.
     */
    public String getURIString() {
        return uriString;
    }

    /**
     * Avoid casual use; java.net.URI may be phased out of crawler
     * for memory performance reasons
     *
     * @return URI
     */
    public URI getRawUri() {
        return getUri();
    }

    /**
     * @return The uri host.
     */
    public String getHost() {
        return uri.getHost();
    }
}
