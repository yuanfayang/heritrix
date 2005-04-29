/* SeedFileIterator
*
* $Id$
*
* Created on Mar 28, 2005
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
package org.archive.crawler.scope;

import java.io.BufferedReader;

import org.apache.commons.httpclient.URIException;
import org.archive.crawler.datamodel.UURIFactory;
import org.archive.util.iterator.LineReadingIterator;
import org.archive.util.iterator.RegexpLineIterator;
import org.archive.util.iterator.TransformingIteratorWrapper;


/**
 * Iterator wrapper for seeds file on disk. 
 * 
 * @author gojomo
 */
public class SeedFileIterator extends TransformingIteratorWrapper {
    /*
     * Regexp to identify lines with a seed URI (or seed hostname).
     * (Like RegexpLineIterator.NONWHITESPACE_ENTRY_TRAILING_COMMENT.
     * except content must begin with a word char ("\\w") so that
     * "directive" lines with other characters may be ignored.
     */
    public static final String URI_OR_HOSTNAME_LINE = 
        "^\\s*(\\w\\S+)\\s*(#.*)?$";
    
    /**
     * 
     */
    public SeedFileIterator(BufferedReader reader) {
        super();
        inner = new RegexpLineIterator(
                new LineReadingIterator(reader),
                RegexpLineIterator.COMMENT_LINE,
                RegexpLineIterator.NONWHITESPACE_ENTRY_TRAILING_COMMENT,
                RegexpLineIterator.ENTRY);
    }
    
    /* (non-Javadoc)
     * @see org.archive.util.iterator.TransformingIteratorWrapper#transform(java.lang.Object)
     */
    protected Object transform(Object object) {
        String uri = (String)object;
        if(uri.matches("[\\w\\.]+")) {
            // all word chars and periods -- must be plain hostname
            uri = "http://"+uri;
        }
        try {
            // TODO: ignore lines beginning with non-word char
            return UURIFactory.getInstance(uri);
        } catch (URIException e) {
            return null;
        }
    }
}