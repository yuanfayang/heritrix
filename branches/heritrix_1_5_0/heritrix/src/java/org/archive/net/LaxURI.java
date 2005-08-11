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
        lax_abs_path.set('|'); // tests indicate Firefox (1.0.6) doesn't escape
    }
    
    // passthrough initializers
    public LaxURI(String uri, boolean escaped, String charset) throws URIException {
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
        char[] path = getRawPath();
        return (path == null) ? null : decode(path, getProtocolCharset());
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
        if(generous == rel_segment) {
            // swap in more lax allowable set
            return lax_rel_segment;
        }
        if (generous == abs_path) {
            return lax_abs_path;
        }
        // otherwise, leave as is
        return generous;
    }
}
