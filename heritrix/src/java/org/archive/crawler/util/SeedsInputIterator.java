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
 * SeedsInputIterator.java
 * Created on Oct 30, 2003
 *
 * $Header$
 */
package org.archive.crawler.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.archive.crawler.datamodel.UURI;
import org.archive.crawler.framework.CrawlController;
import org.archive.util.DevUtils;

/**
 *
 * Read seeds from the given BufferedReader. Any lines where the
 * first non-whitespace character is '#' are considered
 * comments and ignored.
 *
 * Otherwise, anything on the line that looks like a URI or
 * URI fragment (such as a dotted hostname, with or without
 * a path-fragment) will be taken as a URI. If necessary,
 * "http://" will be prepended to URI-like strings lacking
 * it.
 *
 * @author gojomo
 *
 */
public class SeedsInputIterator implements Iterator {
    //  regexp for identifying URIs in seed input data
    public static final Pattern DEFAULT_SEED_EXTRACTOR =
        Pattern.compile("(?i:((https?://[a-zA-Z0-9-]+)|([a-zA-Z0-9-]+\\.[a-zA-Z0-9-]+))(\\.[a-zA-Z0-9-]+)*(:\\d+)?(/\\S*)?)");
        // pattern to extract seeds
    Pattern seedExtractor = DEFAULT_SEED_EXTRACTOR;

    CrawlController controller;
    BufferedReader reader;
    UURI next;

    /**
     * @param reader
     * @param controller
     */
    public SeedsInputIterator(BufferedReader reader, CrawlController controller) {
        this.reader = reader;
        this.controller = controller;
    }

    /* (non-Javadoc)
     * @see java.util.Iterator#remove()
     */
    public void remove() {
        throw new UnsupportedOperationException();
    }

    /* (non-Javadoc)
     * @see java.util.Iterator#hasNext()
     */
    public boolean hasNext() {
        if(next!=null) {
            return true;
        }
        return loadNext();
    }

    /* (non-Javadoc)
     * @see java.util.Iterator#next()
     */
    public Object next() {
        if(next==null) {
            if (!loadNext()) {
                throw new NoSuchElementException();
            }
        }
        // next is guaranteed set by a loadNext which returned true
        UURI retVal = next;
        next = null;
        return retVal;
    }

    /**
     * Scanning the reader, load the 'next' field with the
     * next valid seed UURI. Return true if next is loaded,
     * false otherwise.
     *
     */
    private boolean loadNext() {
        try {
            String read;
            while ((read = reader.readLine()) != null) {
                read = read.trim();
                if (read.length() == 0 || read.startsWith("#")) {
                    continue;
                }
                Matcher m = seedExtractor.matcher(read);
                while(m.find()) {
                    String candidate = m.group();
                    if(m.group(2)==null) {
                        // naked hostname without scheme
                        candidate = "http://" + candidate;
                    }
                    try {
                        next = UURI.createUURI(candidate);
                        // next loaded with next seed
                        return true;
                    } catch (URISyntaxException e1) {
                        Object[] array = { null, candidate };
                        controller.uriErrors.log(Level.INFO,"reading seeds: "+e1.getMessage(), array );
                        next = null;
                        // keep reading for valid seeds
                        continue;
                    }
                }
                Object[] array = { null, read };
                controller.uriErrors.log(Level.INFO, "bad seed line", array);
            }
            reader.close();
            // no more seeds
            return false;
        } catch (IOException e) {
            DevUtils.warnHandle(e, "throw runtime error? log something?");
            return false;
        }
    }

    /* (non-Javadoc)
     * @see java.lang.Object#finalize()
     */
    protected void finalize() throws Throwable {
        super.finalize();
        if(reader!=null) {
            reader.close();
        }
    }

}
