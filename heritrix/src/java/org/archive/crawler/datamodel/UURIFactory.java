/* UURIFactory
 *
 * $Id$
 *
 * Created on July 16, 2004
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
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;
import org.archive.util.TextUtils;


/**
 * Factory that returns UURIs.
 * 
 * Does escaping and fixup on URIs massaging in accordance with RFC2396
 * and to match browser practice. For example, it removes any
 * '..' if first thing in the path as per IE,  converts backslashes to forward
 * slashes, and discards any 'fragment'/anchor portion of the URI. This
 * class will also fail URIs if they are longer than IE's allowed maximum
 * length.
 * 
 * <p>TODO: Test logging.
 * 
 * @author stack
 */
public class UURIFactory extends URI {
    
    /**
     * Logging instance.
     */
    private static Logger logger =
        Logger.getLogger(UURIFactory.class.getName());
    
    /**
     * The single instance of this factory.
     */
    private static final UURIFactory factory = new UURIFactory();
    
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
    public static final String NBSP = "\u00A0";
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
    public static final String COMMERCIAL_AT = "@";
    public static final char PERCENT_SIGN = '%';
    
    /**
     * First percent sign in string followed by two hex chars.
     */
    public static final String URI_HEX_ENCODING =
        "^[^%]*%[\\p{XDigit}][\\p{XDigit}].*";
    
    /**
     * Authority port number regex.
     */
    final static Pattern PORTREGEX = Pattern.compile(".*:([0-9]+)$");
    
    /**
     * Legal characters in the domain label part of a uri authority.
     * 
     * We're looser than the spec. allowing underscores.
     */
    final static String LEGAL_DOMAINLABEL_REGEX =
        "^(?:[a-zA-Z0-9_-]++(?:\\.)?)++(:[0-9]+)?$";
    
    /**
     * Pattern that looks for case of three or more slashes after the 
     * scheme.  If found, we replace them with two only as mozilla does.
     */
    final static Pattern HTTP_SCHEME_SLASHES =
        Pattern.compile("^(https?://)/+(.*)");

    /**
     * Protected constructor.
     */
    private UURIFactory() {
        super();
    }
    
    /**
     * @param uri URI as string.
     * @return An instance of UURI
     * @throws URIException
     */
    public static UURI getInstance(String uri) throws URIException {
        return UURIFactory.factory.create(uri);
    }
    
    /**
     * @param uri URI as string.
     * @param charset Character encoding of the passed uri string.
     * @return An instance of UURI
     * @throws URIException
     */
    public static UURI getInstance(String uri, String charset)
    		throws URIException {
        return UURIFactory.factory.create(uri, charset);
    }
    
    /**
     * @param base Base uri to use resolving passed relative uri.
     * @param relative URI as string.
     * @return An instance of UURI
     * @throws URIException
     */
    public static UURI getInstance(UURI base, String relative)
    		throws URIException {
        return UURIFactory.factory.create(base, relative);
    }

    /**
     * @param uri URI as string.
     * @return Instance of UURI.
     * @throws URIException
     */
    private UURI create(String uri) throws URIException {
        return create(uri, UURI.getDefaultProtocolCharset());
    }
    
    /**
     * @param uri URI as string.
     * @param charset Original encoding of the string.
     * @return Instance of UURI.
     * @throws URIException
     */
    private UURI create(String uri, String charset) throws URIException {
        UURI uuri = isEscaped(uri)? 
            new UURIImpl(fixup(uri, null).toCharArray(), charset):
            new UURIImpl(fixup(uri, null), charset);
         if (logger.isLoggable(Level.FINE)) {
             logger.fine("URI " + uri +
                 " PRODUCT " + uuri.toString() +
                 " ESCAPED " + escaped +
                 " CHARSET " + charset);
            }
        return uuri;
    }
    
    /**
     * @param base UURI to us as a base resolving <code>relative</code>.
     * @param relative Relative URI.
     * @return Instance of UURI.
     * @throws URIException
     */
    private UURI create(UURI base, String relative) throws URIException {
        UURI uuri = isEscaped(relative)? 
            new UURIImpl(base,
                    new UURI(fixup(relative, base).toCharArray(),
                    base.getProtocolCharset())):
            new UURIImpl(base, new UURI(fixup(relative, base),
                    base.getProtocolCharset()));
        if (logger.isLoggable(Level.FINE)) {
            logger.fine(" URI " + relative +
                " PRODUCT " + uuri.toString() +
                " ESCAPED " + escaped +
                " CHARSET " + base.getProtocolCharset() +
                " BASE " + base);
        }
        return uuri;
    }
    
    /**
     * If first <code>%</code> found is URI encoded, then its
     * escaped.
     * @return True if the passed URI exhibits 'escapedness'.
     */
    protected boolean isEscaped(String uri) {
        return (uri == null || uri.length() <= 0)? false:
            TextUtils.getMatcher(URI_HEX_ENCODING, uri).matches();
    }
    
    /**
     * Do heritrix fix-up on passed uri string.
     *
     * Does heritrix escaping; usually escaping done to make our behavior align
     * with IEs.  This method codifies our experience pulling URIs from the
     * wilds.  Its does escaping NOT done in superclass.
     *
     * @param uri URI as string.
     * @param base May be null.
     * @return An URI escaped string.
     * @throws URIException
     */
    private String fixup(String uri, URI base)
            throws URIException {
        if (uri == null) {
            throw new NullPointerException();
        } else if (uri.length() == 0 && base == null){
            throw new URIException("URI length is zero (and not relative).");
        }

        // Replace nbsp with normal spaces (so that they get stripped if at
        // ends, or encoded if in middle)
        uri = TextUtils.replaceAll(NBSP, uri, SPACE);
        
        // Get rid of any trailing spaces or new-lines. 
        uri = uri.trim();
        
        // IE actually converts backslashes to slashes rather than to %5C.
        // Since URIs that have backslashes usually work only with IE, we will
        // convert backslashes to slashes as well.
        // TODO: Maybe we can first convert backslashes by specs and than by IE
        // so that we fetch both versions.
        if (uri.indexOf(BACKSLASH) >= 0) {
            uri = TextUtils.replaceAll(BACKSLASH_PATTERN, uri, SLASH);
        }
        
        // Kill newlines etc
        uri = TextUtils.replaceAll(NEWLINE, uri, EMPTY_STRING);
        
        // Test for the case of more than two slashes after the http(s) scheme.
        // Replace with two slashes as mozilla does if found.
        // See [ 788219 ] URI Syntax Errors stop page parsing.
        Matcher matcher = HTTP_SCHEME_SLASHES.matcher(uri);
        if (matcher.matches()){
            uri = matcher.group(1) + matcher.group(2);
        }
        
        // For further processing, get uri elements.  See the RFC2396REGEX
        // comment above for explaination of group indices used in the below.
        matcher = RFC2396REGEX.matcher(uri);
        if (!matcher.matches()) {
            throw new URIException("Failed parse of " + uri);
        }
        String uriScheme = checkUriElementAndLowerCase(matcher.group(2));
        String uriSchemeSpecificPart = checkUriElement(matcher.group(3));
        String uriAuthority = checkUriElement(matcher.group(5));
        String uriPath = checkUriElement(matcher.group(6));
        String uriQuery = checkUriElement(matcher.group(8));
        // UNUSED String uriFragment = checkUriElement(matcher.group(10));
        
        // Test if relative URI.  If so, need a base to resolve against.
        if ((uriScheme == null || uriScheme.length() <= 0)  && base == null) {
            throw new URIException("Relative URI but no base: " + uri);
        }
        
        // Lowercase the host part of the uriAuthority; don't destroy any
        // userinfo capitalizations.  Make sure no illegal characters in
        // domainlabel substring of the uri authority.
        if (uriAuthority != null) {
            int index = uriAuthority.indexOf(COMMERCIAL_AT);
            if (index < 0) {
                uriAuthority = checkDomainlabel(uriAuthority.toLowerCase());
            } else {
                uriAuthority = uriAuthority.substring(0, index) +
                	COMMERCIAL_AT + checkDomainlabel(
                	    uriAuthority.substring(index + 1).toLowerCase());
            }
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

        // Preallocate big.
        StringBuffer buffer = new StringBuffer(1024 * 4);
        appendNonNull(buffer, uriScheme, ":", true);
        appendNonNull(buffer, uriAuthority, "//", false);
        appendNonNull(buffer, uriPath, "", false);
        appendNonNull(buffer, uriQuery, "?", false);
        return buffer.toString();
    }

    /**
     * Check the domain label part of the authority.
     * 
     * We're more lax than the spec. in that we allow underscores.
     * 
     * @param label Domain label to check.
     * @return Return passed domain label.
     * @throws URIException
     */
    private String checkDomainlabel(String label)
    		throws URIException {
        if (!TextUtils.matches(LEGAL_DOMAINLABEL_REGEX, label)) {
            throw new URIException("Illegal domain label: " + label);
        }
        return label;
    }

    /**
     * Check port on passed http authority.  Make sure the size is not larger
     * than allowed: See the 'port' definition on this
     * page, http://www.kerio.com/manual/wrp/en/418.htm.
     *
     * @param uriAuthority
     */
    private void checkPort(String uriAuthority) throws URIException {
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
     * @param b Buffer to append to.
     * @param str String to append if not null.
     * @param substr Suffix or prefix to use if <code>str</code> is not null.
     * @param suffix True if <code>substr</code> is a suffix.
     */
    private void appendNonNull(StringBuffer b, String str, String substr,
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
    private String stripPrefix(String str, String prefix) {
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
    private String checkUriElement(String element) {
        return (element == null || element.length() <= 0)? null: element;
    }

    /**
     * @param element to examine and lowercase if non-null.
     * @return Null if passed null or an empty string otherwise
     * <code>element</code> lowercased.
     */
    private String checkUriElementAndLowerCase(String element) {
        String tmp = checkUriElement(element);
        return (tmp != null)? tmp.toLowerCase(): tmp;
    }
    
    /**
     * Implementation of UURI protected class used by enclosing factory.
     */
    private class UURIImpl extends UURI  implements Serializable {
        /**
         * @param uri String representation of an absolute URI.
         * @throws org.apache.commons.httpclient.URIException
         */
        protected UURIImpl(String uri, String charset) throws URIException {
            super(uri, charset);
            normalize();
        }
        
        /**
         * @param uri String representation of an absolute URI.
         * @throws org.apache.commons.httpclient.URIException
         */
        protected UURIImpl(char [] uri, String charset) throws URIException {
            super(uri, charset);
            normalize();
        }
        
        /**
         * @param relative String representation of URI.
         * @param base Parent UURI to use derelativizing.
         * @throws org.apache.commons.httpclient.URIException
         */
        protected UURIImpl(UURI base, UURI relative) throws URIException {
            super(base, relative);
            normalize();
        }
    }
}
