/* LaxURI
*
* $Id$
*
* Created on Aug 3, 2005
*
* Copyright (C) 2005 Internet Archive.
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

import java.util.Arrays;
import java.util.BitSet;

import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.util.EncodingUtil;

/**
 * URI subclass which allows partial/inconsistent encoding, matching
 * the URIs which will be relayed in requests from popular web
 * browsers (esp. Mozilla Firefox and MS IE).
 * 
 * @author gojomo
 */
public class LaxURI extends URI {
    final protected static char[] HTTP_SCHEME = {'h','t','t','p'};
    final protected static char[] HTTPS_SCHEME = {'h','t','t','p','s'};
    
    protected static final BitSet lax_rel_segment = new BitSet(256);
    // Static initializer for lax_rel_segment
    static {
        lax_rel_segment.or(rel_segment);
        lax_rel_segment.set(':'); // allow ':'
        // TODO: add additional allowances as need is demonstrated
    }

    protected static final BitSet lax_abs_path = new BitSet(256);
    static {
        lax_abs_path.or(abs_path);
        lax_abs_path.set('|'); // tests indicate Firefox (1.0.6) doesn't escape.
    }
    
    // passthrough initializers
    public LaxURI(String uri, boolean escaped, String charset)
    throws URIException {
        super(uri,escaped,charset);
    }
    public LaxURI(URI base, URI relative) throws URIException {
        super(base,relative);
    }
    public LaxURI(String uri, boolean escaped) throws URIException {
        super(uri,escaped);
    }
    public LaxURI() {
        super();
    }

    // overridden to use this class's static decode()
    public String getURI() throws URIException {
        return (_uri == null) ? null : decode(_uri, getProtocolCharset());
    }
    
    // overridden to use this class's static decode()
    public String getPath() throws URIException {
        char[] p = getRawPath();
        return (p == null) ? null : decode(p, getProtocolCharset());
    }

    // overridden to use this class's static decode()
    public String getPathQuery() throws URIException {
        char[] rawPathQuery = getRawPathQuery();
        return (rawPathQuery == null) ? null : decode(rawPathQuery,
                getProtocolCharset());
    }
    // overridden to use this class's static decode()
    protected static String decode(char[] component, String charset)
            throws URIException {
        if (component == null) {
            throw new IllegalArgumentException(
                    "Component array of chars may not be null");
        }
        return decode(new String(component), charset);
    }

    // overridden to use IA's LaxURLCodec, which never throws DecoderException
    protected static String decode(String component, String charset)
            throws URIException {
        if (component == null) {
            throw new IllegalArgumentException(
                    "Component array of chars may not be null");
        }
        byte[] rawdata = null;
        //     try {
        rawdata = LaxURLCodec.decodeUrlLoose(EncodingUtil
                .getAsciiBytes(component));
        //     } catch (DecoderException e) {
        //         throw new URIException(e.getMessage());
        //     }
        return EncodingUtil.getString(rawdata, charset);
    }
    
    // overidden to lax() the acceptable-char BitSet passed in
    protected boolean validate(char[] component, BitSet generous) {
        return super.validate(component, lax(generous));
    }

    // overidden to lax() the acceptable-char BitSet passed in
    protected boolean validate(char[] component, int soffset, int eoffset,
            BitSet generous) {
        return super.validate(component, soffset, eoffset, lax(generous));
    }
    
    /**
     * Given a BitSet -- typically one of the URI superclass's
     * predefined static variables -- possibly replace it with
     * a more-lax version to better match the character sets
     * actually left unencoded in web browser requests
     * 
     * @param generous original BitSet
     * @return (possibly more lax) BitSet to use
     */
    protected BitSet lax(BitSet generous) {
        if (generous == rel_segment) {
            // Swap in more lax allowable set
            return lax_rel_segment;
        }
        if (generous == abs_path) {
            return lax_abs_path;
        }
        // otherwise, leave as is
        return generous;
    }
    
    /** 
     * Coalesce the _host and _authority fields where 
     * possible.
     * 
     * In the web crawl/http domain, most URIs have an 
     * identical _host and _authority. (There is no port
     * or user info.) However, the superclass always 
     * creates two separate char[] instances. 
     * 
     * Notably, the lengths of these char[] fields are 
     * equal if and only if their values are identical.
     * This method makes use of this fact to reduce the
     * two instances to one where possible, slimming 
     * instances.  
     * 
     * @see org.apache.commons.httpclient.URI#parseAuthority(java.lang.String, boolean)
     */
    protected void parseAuthority(String original, boolean escaped)
            throws URIException {
        super.parseAuthority(original, escaped);
        if (_host != null && _authority != null
                && _host.length == _authority.length) {
            _host = _authority;
        }
    }
    
    
    /** 
     * Coalesce _scheme to existing instances, where appropriate.
     * 
     * In the web-crawl domain, most _schemes are 'http' or 'https',
     * but the superclass always creates a new char[] instance. For
     * these two cases, we replace the created instance with a 
     * long-lived instance from a static field, saving 12-14 bytes
     * per instance. 
     * 
     * @see org.apache.commons.httpclient.URI#setURI()
     */
    protected void setURI() {
        if (_scheme != null) {
            if (_scheme.length == 4 && Arrays.equals(_scheme, HTTP_SCHEME)) {
                _scheme = HTTP_SCHEME;
            } else if (_scheme.length == 5
                    && Arrays.equals(_scheme, HTTP_SCHEME)) {
                _scheme = HTTPS_SCHEME;
            }
        }
        super.setURI();
    }
}
