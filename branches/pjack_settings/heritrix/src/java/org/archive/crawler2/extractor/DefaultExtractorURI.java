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
 * ContentExtractor.java
 * Created on October 5, 2006
 *
 * $Header$
 */
package org.archive.crawler2.extractor;


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.httpclient.URIException;
import org.archive.crawler.datamodel.RobotsHonoringPolicy;
import org.archive.state.Constraint;
import org.archive.io.CharSubSequence;
import org.archive.io.ReplayCharSequence;
import org.archive.io.SeekInputStream;
import org.archive.net.UURI;
import org.archive.net.UURIFactory;
import org.archive.state.Key;

public class DefaultExtractorURI implements ExtractorURI, Cloneable {

    
    /**
     * An exception and an error message.  Used for keeping list of errors.
     */
    public static class ExceptionHolder {

        /** The exception. */
        final public Throwable exception;
        
        /** The error message. */
        final public String description;
        
        /**
         * Constructor.
         * 
         * @param e      the exception
         * @param desc   the description
         */
        public ExceptionHolder(Throwable e, String desc) {
            this.exception = e;
            this.description = desc;
        }
    }
    
    private UURI uuri;
    private UURI base;
    private LinkContext via;
    private RobotsHonoringPolicy rhp;
    
    private StringBuilder annotations = new StringBuilder();
    private List<ExceptionHolder> local = new ArrayList<ExceptionHolder>();
    private List<ExceptionHolder> uriErrors = new ArrayList<ExceptionHolder>();
    private Set<Link> outlinks = new HashSet<Link>(); 
    private Map<Key,Object> properties = new HashMap<Key,Object>();

    private boolean finished;

    private byte[] content;
    private Charset contentEncoding;
    private String contentType;
    
    
    /**
     * Constructor.
     * 
     * @param uuri   the URI 
     * @param via    the context of the URI's discovery
     */
    public DefaultExtractorURI(UURI uuri, LinkContext via) {
        this.uuri = uuri;
        this.via = via;
    }

    
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
    
    
    public DefaultExtractorURI duplicate() {
        return (DefaultExtractorURI)clone();
    }

    // Documented in ExtractorURI
    public void addAnnotation(CharSequence msg) {
        annotations.append(msg).append(';');
    }

    
    /**
     * Returns the annotations as a string.
     * 
     * @return  the annotations as a string
     */
    public String getAnnotations() {
        return annotations.toString();
    }


    // Documented in ExtractorURI
    public void addLocalizedError(Throwable e, String msg) {
        local.add(new ExceptionHolder(e, msg));
    }
    
    
    /**
     * Returns the reported localized errors.
     * 
     * @return  the localized errors
     */
    public List<ExceptionHolder> getLocalizedErrors() {
        return local;
    }


    // Documented in ExtractorURI
    public void addUriError(URIException e, String badURI) {
        uriErrors.add(new ExceptionHolder(e, badURI));
    }
    
    
    /**
     * Returns the reported URI errors.
     * 
     * @return  the reported URI errors
     */
    public List<ExceptionHolder> getUriErrors() {
        return uriErrors;
    }


    // Documented in ExtractorURI
    public UURI getBaseURI() {
        if (base == null) {
            return uuri;
        }
        return base;
    }


    // Documented in ExtractorURI
    public ReplayCharSequence getCharSequence() throws IOException {
        ByteArrayInputStream baip = new ByteArrayInputStream(content);
        InputStreamReader r = new InputStreamReader(baip, contentEncoding);
        final StringBuilder sb = new StringBuilder();
        for (int ch = r.read(); ch >= 0; ch = r.read()) {
            sb.append((char)ch);
        }
        return new ReplayCharSequence() {
            public int length() {
                return sb.length();
            }
            
            public char charAt(int index) {
                return sb.charAt(index);
            }
            
            public CharSequence subSequence(int start, int end) {
                return new CharSubSequence(this, start, end);
            }
            
            public void close() {
            }
            
            public String toString() {
                return sb.toString();
            }
        };
    }


    // Documented in ExtractorURI
    public long getContentLength() {
        return content.length;
    }

    
    // Documented in ExtractorURI
    public String getContentType() {
        return contentType;
    }

    
    // Documented in ExtractorURI
    public SeekInputStream getInputStream() throws IOException {
        // TODO Auto-generated method stub
        return null;
    }
    
    
    /**
     * Sets the content for this ExtractorURI.
     * 
     * @param content   the content as a byte array
     * @param enc       the content encoding
     * @param contentType   the content mime type
     */
    public void setContent(byte[] content, Charset enc, String contentType) {
        this.content = content;
        this.contentType = contentType;
        this.contentEncoding = enc;
    }
    

    /**
     * Sets the content for this ExtractorURI.
     * 
     * @param content       the content as a string
     * @param contentType   the content mime type
     */
    public void setContent(String content, String contentType) {
        try {
            this.content = content.getBytes("UTF-8");
            this.contentType = contentType;
            this.contentEncoding = Charset.forName("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    
    // Documented in ExtractorURI
    public Set<Link> getOutLinks() {
        return outlinks;
    }

    
    // Documented in ExtractorURI
    public RobotsHonoringPolicy getRobotsHonoringPolicy() {
        return rhp;
    }

    
    /**
     * Sets the RobotsHonoringPolicy for this ExtractorURI.
     * 
     * @param rhp  the new robots honoring policy
     */
    public void setRobotsHonoringPolicy(RobotsHonoringPolicy rhp) {
        this.rhp = rhp;
    }


    // Documented in ExtractorURI
    public UURI getUURI() {
        return uuri;
    }


    // Documented in ExtractorURI
    public LinkContext getViaContext() {
        return via;
    }


    // Documented in ExtractorURI
    public boolean isLinkExtractionFinished() {
        return finished;
    }


    // Documented in ExtractorURI
    public void linkExtractionFinished() {
        finished = true;
    }


    // Documented in ExtractorURI
    public void setBaseURI(String s) throws URIException {
        this.base = UURIFactory.getInstance(s);        
    }

    
    // Documented in ExtractorURI
    public <T> T get(Key<T> key) {
        Object o = properties.get(key);
        if (o == null) {
            return key.getDefaultValue();
        }
        return key.getType().cast(o);
    }    


    /**
     * Sets a value for the given key.
     * 
     * @param <T> the type of key
     * @param key   the key whose value to set
     * @param value   the value for that key
     */
    public <T> void set(Key<T> key, T value) {
        for (Constraint<T> c: key.getConstraints()) {
            if (!c.allowed(value)) {
                throw new IllegalArgumentException();
            }
        }
        properties.put(key, value);
    }
}
