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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;
import org.archive.util.TextUtils;


/**
 * Usable URI: A heritrix spin on {@link java.net.URI}.
 *
 * This class is an implementation of RFC2396 codifying our experience of
 * URIs out in the wild.  Often the below is a looser implementation of RFC2396
 * {@link java.net.URI}.  At other times it fixes quirky {@link java.net.URI}
 * behaviors.  We've made this class because {@link java.net.URI} is not
 * subclassable -- its final -- and its unlikely URI will change any time soon 
 * (See Gordon's considered petition here:
 * <a href="http://developer.java.sun.com/developer/bugParade/bugs/4939847.html">java.net.URI
 * should have loose/tolerant/compatibility option (or allow reuse)</a>).
 * 
 * <p>Instances of this class are always normalized -- massaged in
 * ways that by spec and in practice, do not change the URI's meaning nor
 * function -- i.e. we call {@link #normalize()} is called in the constructor --
 * and URIs are rehabilitated: i.e. patched in riskless or necessary ways to be
 * legal, eg escaping spaces).  Other things done are the removal of any 
 * '..' if its first thing in the path as per IE, removal of trailing
 * whitespace, conversion of backslash to forward slash. This class will also
 * fail URIs if they are longer than IE's allowed maximum length.
 * 
 * <p>This class tries to cache calculated strings such as the extracted host
 * and this class as a string rather than have the parent class rerun its 
 * calculation everytime.
 * 
 * <p>See <a href="http://sourceforge.net/tracker/?func=detail&aid=910120&group_id=73833&atid=539099">[ 910120 ]
 * java.net.URI#getHost fails when leading digit</a>,
 * <a href="http://sourceforge.net/tracker/?func=detail&aid=788277&group_id=73833&atid=539099">[ 788277 ]
 * Doing separate DNS lookup for same host</a>,
 * <a href="http://sourceforge.net/tracker/?func=detail&aid=808270&group_id=73833&atid=539099">[ 808270 ]
 * java.net.URI chokes on hosts_with_underscores</a>,
 * <a href="http://sourceforge.net/tracker/?func=detail&aid=874220&group_id=73833&atid=539099">[ 874220 ]
 * NPE in java.net.URI.encode</a>,
 * <a href="http://sourceforge.net/tracker/?func=detail&aid=927940&group_id=73833&atid=539099">[ 927940 ]
 * java.net.URI parses %20 but getHost null</a> to mention a few of the problems
 * we've had with native java URI class.
 *
 * @author gojomo
 * @author stack
 * 
 * @see org.apache.commons.httpclient.URI
 */
public class UURI extends URI {
    
    /**
     * RFC 2396 regex.
     * 
     * From the RFC Appendix B:
     * <pre>
     * URI Generic Syntax                August 1998
     *
     * B. Parsing a URI Reference with a Regular Expression
     *
     * As described in Section 4.3, the generic URI syntax is not sufficient
     * to disambiguate the components of some forms of URI.  Since the
     * "greedy algorithm" described in that section is identical to the
     * disambiguation method used by POSIX regular expressions, it is
     * natural and commonplace to use a regular expression for parsing the
     * potential four components and fragment identifier of a URI reference.
     *
     * The following line is the regular expression for breaking-down a URI
     * reference into its components.
     *
     * ^(([^:/?#]+):)?(//([^/?#]*))?([^?#]*)(\?([^#]*))?(#(.*))?
     * 12            3  4          5       6  7        8 9
     *
     * The numbers in the second line above are only to assist readability;
     * they indicate the reference points for each subexpression (i.e., each
     * paired parenthesis).  We refer to the value matched for subexpression
     * <n> as $<n>.  For example, matching the above expression to
     * 
     * http://www.ics.uci.edu/pub/ietf/uri/#Related
     * 
     * results in the following subexpression matches:
     * 
     * $1 = http:
     * $2 = http
     * $3 = //www.ics.uci.edu
     * $4 = www.ics.uci.edu
     * $5 = /pub/ietf/uri/
     * $6 = <undefined>
     * $7 = <undefined>
     * $8 = #Related
     * $9 = Related
     * 
     * where <undefined> indicates that the component is not present, as is
     * the case for the query component in the above example.  Therefore, we
     * can determine the value of the four components and fragment as
     * 
     * scheme    = $2
     * authority = $4
     * path      = $5
     * query     = $7
     * fragment  = $9
     * </pre>
     * 
     * <p>Below differs from the rfc regex in that it has java escaping of
     * regex characters and we allow a URI made of a fragment only (Added extra
     * group so indexing is off by one after scheme).
     */
    final static Pattern RFC2396REGEX = Pattern.compile(
        "^(([^:/?#]+):)?((//([^/?#]*))?([^?#]*)(\\?([^#]*))?)?(#(.*))?");
    
    public static final String DOTDOT = "^(/\\.\\.)+";
    public static final String SLASH = "/";
    public static final String HTTP = "http";
    public static final String HTTP_PORT = ":80";
    public static final String HTTPS = "https";
    public static final String HTTPS_PORT = ":443";
    public static final String DOT = ".";
    public static final String EMPTY_STRING = "";
    public static final String NBSP = "\\xA0";
    public static final String SPACE = " ";
    public static final String ESCAPED_SPACE = "%20";
    public static final String PIPE = "|";
    public static final String PIPE_PATTERN = "\\|";
    public static final String ESCAPED_PIPE = "%7C";
    public static final String CIRCUMFLEX = "^";
    public static final String CIRCUMFLEX_PATTERN = "\\^";
    public static final String ESCAPED_CIRCUMFLEX = "%5E";
    public static final String QUOT = "\"";
    public static final String ESCAPED_QUOT = "%22";
    public static final String SQUOT = "'";
    public static final String ESCAPED_SQUOT = "%27";
    public static final String APOSTROPH = "`";
    public static final String ESCAPED_APOSTROPH = "%60";
    public static final String LSQRBRACKET = "[";
    public static final String LSQRBRACKET_PATTERN = "\\[";
    public static final String ESCAPED_LSQRBRACKET = "%5B";
    public static final String RSQRBRACKET = "]";
    public static final String RSQRBRACKET_PATTERN = "\\]";
    public static final String ESCAPED_RSQRBRACKET = "%5D";
    public static final String LCURBRACKET = "{";
    public static final String LCURBRACKET_PATTERN = "\\{";
    public static final String ESCAPED_LCURBRACKET = "%7B";
    public static final String RCURBRACKET = "}";
    public static final String RCURBRACKET_PATTERN = "\\}";
    public static final String ESCAPED_RCURBRACKET = "%7D";
    public static final String BACKSLASH = "\\";
    public static final String BACKSLASH_PATTERN = "\\\\";
    public static final String ESCAPED_BACKSLASH = "%5C";
    public static final String NEWLINE = "\n+|\r+";
    public static final String IMPROPERESC_REPLACE = "%25$1";
    public static final String IMPROPERESC = 
        "%((?:[^\\p{XDigit}])|(?:.[^\\p{XDigit}])|(?:\\z))";
    
    public static final String MASSAGEHOST_PATTERN = "^www\\d*\\.";
    /**
     * Authority port number regex.
     */
    final static Pattern PORTREGEX = Pattern.compile(".*:([0-9]+)$");
    
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
     * Cache of this uuri escaped with fragment if present as a string.
     * 
     * Super class calculates on every call.  Profiling shows us spend 30% of
     * total elapsed time in URI class.
     */
    private String cachedString = null;

    
    /**
     * Shutdown default constructor.
     */
    private UURI() {
        super();
    }
    
    /**
     * @param uri String representation of an absolute URI.
     * @throws org.apache.commons.httpclient.URIException
     */
    public UURI(String uri) throws URIException {
        super(patchEscape(uri, null));
        normalize();
    }
    
    /**
     * @param relative String representation of URI.
     * @param base Parent UURI to use derelativizing.
     * @throws org.apache.commons.httpclient.URIException
     */
    public UURI(UURI base, String relative) throws URIException {
        super(base, patchEscape(relative, base));
        normalize();
    }
    
    /**
     * Escape passed uri string.
     *
     * Does heritrix escaping; usually escaping done to make our behavior align
     * with IEs.  This method codifies our experience pulling URIs from the
     * wilds.  Its does esacaping NOT done in superclass.
     * 
     * @param uri URI as string.
     * @param base May be null.
     * @return An URI escaped string.
     * @throws URIException
     */
    private static String patchEscape(String uri, URI base)
            throws URIException {
        if (uri == null) {
            throw new NullPointerException();
        } else if (uri.length() == 0 && base == null){
            throw new URIException("URI length is zero (and not relative).");
        }

        // Replace nbsp with normal spaces (so that they get stripped if at
        // ends, or encoded if in middle)
        uri = TextUtils.replaceAll(NBSP, uri, SPACE);
        
        // IE actually converts backslashes to slashes rather than to %5C.
        // Since URIs that have backslashes usually work only with IE, we will
        // convert backslashes to slashes as well.
        // TODO: Maybe we can first convert backslashes by specs and than by IE
        // so that we fetch both versions. 
        if (uri.indexOf(BACKSLASH) >= 0) {
            uri = TextUtils.replaceAll(BACKSLASH_PATTERN, uri, SLASH);
        }
        
        // Escape improper escape codes; eg any '%' followed by non-hex-digits.
        uri = TextUtils.replaceAll(IMPROPERESC, uri, IMPROPERESC_REPLACE);
        // Twice just to be sure (actually, to handle multiple %% in a row)
        uri = TextUtils.replaceAll(IMPROPERESC, uri, IMPROPERESC_REPLACE);
        // Kill newlines etc
        uri = TextUtils.replaceAll(NEWLINE, uri, EMPTY_STRING);
        
        // For further processing, get uri elements.  See the RFC2396REGEX
        // comment above for explaination of group indices used in the below.
        Matcher matcher = RFC2396REGEX.matcher(uri);
        if (!matcher.matches()) {
            throw new URIException("Failed parse of " + uri);
        }
        String uriScheme = checkUriElementAndLowerCase(matcher.group(2));
        String uriSchemeSpecificPart = checkUriElement(matcher.group(3));
        String uriAuthority = checkUriElement(matcher.group(5));
        String uriPath = checkUriElement(matcher.group(6));
        String uriQuery = checkUriElement(matcher.group(8));
        String uriFragment = checkUriElement(matcher.group(10));
        
        // Lowercase the host part of the uriAuthority; don't destroy any
        // userinfo capitalizations.
        int index = (uriAuthority != null)? uriAuthority.indexOf('@'): -1;
        if (index > 0) {
            uriAuthority = uriAuthority.substring(0, index) +
                uriAuthority.substring(index).toLowerCase();
        }
        
        // Test if relative URI.  If so, need a base to resolve against.
        if (uriScheme == null && base == null) {
            throw new URIException("Relative URI but no base: " + uri);
        }
        
        // Do some checks if absolute path.
        if (uriSchemeSpecificPart != null &&
                uriSchemeSpecificPart.startsWith(SLASH)) {
            if (uriPath != null) {
                // Eliminate '..' if its first thing in the path.  IE does this.
                uriPath = TextUtils.replaceFirst(DOTDOT, uriPath,
                    EMPTY_STRING);
            }   
            // Ensure root URLs end with '/': browsers always send "/"
            // on the request-line, so we should consider "http://host" 
            // to be "http://host/".
            if (uriPath == null || EMPTY_STRING.equals(uriPath)) {
                uriPath = SLASH;
            }
        }        
        
        if (uriAuthority != null) {
            if (uriScheme != null && uriScheme.length() > 0 &&
                    uriScheme.equals(HTTP)) {
                uriAuthority = stripTail(uriAuthority, HTTP_PORT);
                checkPort(uriAuthority);
            } else if (uriScheme != null && uriScheme.length() > 0 &&
                    uriScheme.equals(HTTPS)) {
                uriAuthority = stripTail(uriAuthority, HTTPS_PORT);
                checkPort(uriAuthority);
            }
            // Strip any prefix dot or tail dots from the authority.
            uriAuthority = stripTail(uriAuthority, DOT);
            uriAuthority = stripPrefix(uriAuthority, DOT);
        }

        return reassemble(uriScheme, uriAuthority, uriPath, uriQuery,
            uriFragment);
    }
    
    /**
     * Check port on passed http authority.  Make sure the size is not larger
     * than allowed: See the 'port' definition on this
     * page, http://www.kerio.com/manual/wrp/en/418.htm.
     * 
     * @param uriAuthority
     */
    private static void checkPort(String uriAuthority) throws URIException {
        Matcher m = PORTREGEX.matcher(uriAuthority);
        if (m.matches()) {
            String no = m.group(1);
            if (no != null && no.length() > 0) {
                int portNo = Integer.parseInt(no);
                if (portNo <= 0 || portNo > 65535) {
                    throw new URIException("Port out of bounds: " +
                        uriAuthority);
                }
            }
        }
    }

    /**
     * Reassemble URI from passed pieces.
     * @param uriScheme Scheme.
     * @param uriAuthority Authority.
     * @param uriPath Path.
     * @param uriQuery Query.
     * @param uriFragment Fragment.
     * @return Reassembled URI.
     * throws URIException
     */
    private static String reassemble(String uriScheme, String uriAuthority,
                String uriPath, String uriQuery, String uriFragment) {
        // Put the URI back together for return as a string.
        StringBuffer buffer = new StringBuffer();
        appendNonNull(buffer, uriScheme, ":", true);
        appendNonNull(buffer, uriAuthority, "//", false);
        appendNonNull(buffer, uriPath, "", false);
        appendNonNull(buffer, uriQuery, "?", false);
        appendNonNull(buffer, uriFragment, "#", false);
        return buffer.toString();
    }
    
    /**
     * @param b Buffer to append to.
     * @param str String to append if not null.
     * @param substr Suffix or prefix to use if <code>str</code> is not null.
     * @param suffix True if <code>substr</code> is a suffix.
     */
    private static void appendNonNull(StringBuffer b, String str, String substr,
            boolean suffix) {
        if (str != null && str.length() > 0) {
            if (!suffix) {
                b.append(substr);
            }
            b.append(str);
            if (suffix) {
                b.append(substr);
            }
        }
    }

    /**
     * @param str String to work on.
     * @param prefix Prefix to strip if present.
     */
    private static String stripPrefix(String str, String prefix) {
        return str.startsWith(prefix)?
            str.substring(prefix.length(), str.length()):
            str;
    }   
            
    /**
     * @param str String to work on.
     * @param tail Tail to strip if present.
     */
    private static String stripTail(String str, String tail) {
        return str.endsWith(tail)?
            str.substring(0, str.length() - tail.length()):
            str;
    }

    /**
     * @param element to examine.
     * @return Null if passed null or an empty string otherwise
     * <code>element</code>.
     */
    private static String checkUriElement(String element) {
        return (element == null || element.length() <= 0)? null: element;
    }
    
    /**
     * @param element to examine and lowercase if non-null.
     * @return Null if passed null or an empty string otherwise
     * <code>element</code> lowercased.
     */
    private static String checkUriElementAndLowerCase(String element) {
        String tmp = checkUriElement(element);
        return (tmp != null)? tmp.toLowerCase(): tmp;
    }
    
    /**
     * @param uri URI as string that is resolved relative to this UURI.
     * @return UURI that uses this UURI as base.
     * @throws URIException
     */
    public UURI resolve(String uri) throws URIException {
        return new UURI(this, uri);
    }
    
    /**
     * Calls {@link URI#URI(String)} and converts any URIException to
     * {@link  IllegalArgumentException}.
     * @param uri URI as a string.
     * @return UURI.
     * @throws IllegalArgumentException if an URIException.
     */
    public UURI create(String uri) {
        UURI uuri = null;
        try {
            uuri = new UURI(uri);
        }
        catch (URIException e) {
            throw new IllegalArgumentException(uri + ": " + e.getMessage());
        }
        return uuri;
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
        // has_fragment?  should be careful of the only fragment case.
        if (!equals(this._fragment, another._fragment)) {
            return false;
        }
        return true;
    }
    
    /**
     * Strips www[0-9]*\. from the host.
     * 
     * If calling getHostBaseName becomes a performance issue we should 
     * consider adding the hostBasename member that is set on initialization.
     * 
     * @return Host's basename.
     * @throws URIException
     */
    public String getHostBasename() throws URIException {
        if (this.getHost() != null) {            
            return TextUtils.replaceFirst(MASSAGEHOST_PATTERN, this.getHost(),
                        EMPTY_STRING);
        }
        return null;
    }
    
    /**
     * Override because superclass drops fragment if present.
     * @return String representation of this URI WITH the fragment if present.
     */
    public String toString() {
        if (this.cachedString == null) {
            synchronized (this) {
                if (this.cachedString == null) {
                    String frgmnt = getEscapedFragment();
                    this.cachedString = super.toString() +
                        ((frgmnt == null || frgmnt.length() <= 0)? "":
                            "#" + frgmnt);
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
    
    public String getHost() throws URIException {
        if (this.cachedHost == null) {
            synchronized (this) {
                if (this.cachedHost == null) {
                    this.cachedHost = super.getHost();
                }
            }
        }
        return this.cachedHost;
    }
}
