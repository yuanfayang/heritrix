/* SeedList
 *
 * Created on Apr 29, 2004
 *
 * Copyright (C) 2004 Internet Archive.
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.httpclient.URIException;
import org.archive.util.DevUtils;
import org.archive.util.IoUtils;


/**
 * Class to manage the seed list.
 *
 * Manages cache of seeds.
 *
 * @author stack
 * @version $Revision$, $Date$
 */
public class SeedList
extends AbstractList implements Serializable {
    /**
     * SeedList logging instance.
     */
    private static final Logger logger =
        Logger.getLogger(SeedList.class.getName());

    /**
     * Regexp for identifying URIs in seed input data
     */
    static final Pattern DEFAULT_SEED_EXTRACTOR =
        Pattern.compile("(?i:((([a-z]+)://[a-zA-Z0-9-@]+)|" +
            "(([a-z]+):[a-zA-Z0-9-@]+)|" +
            "([a-zA-Z0-9-]+\\.[a-zA-Z0-9-]+))" +
            "(\\.[a-zA-Z0-9-]+)*(:\\d+)?(/\\S*)?)");
    

    /**
     * Cached name of seed file.
     *
     * Can be refreshed by passing an argument to {@link #refresh(File)}
     */
    private File seedfile = null;

    /**
     * Cache of seeds.
     */
    private List cache = null;


    /**
     * Constructor
     *
     * @param seedfile Default seed file to use.
     * @param caching True if we're to cache the seed list.
     */
    public SeedList(File seedfile, boolean caching) {
        //this.logger = logger;
        this.seedfile = seedfile;
        if (caching) {
            this.cache = Collections.synchronizedList(new ArrayList());
            refresh(seedfile);
        }
    }

    /**
     * Blocked default constructor.
     */
    private SeedList() {
        super();
    }

    /**
     * @return Returns the current seedfile.
     */
    protected File getSeedfile() {
        return this.seedfile;
    }
    
    /**
     * Return an seeds as a stream.
     * This method will work for case where seeds are on disk or on classpath.
     * @return InputStream on current seeds file.
     * @throws IOException
     */
    public InputStream getSeedStream() throws IOException {
        InputStream is = null;
        if (!getSeedfile().exists()) {
            // Is the file on the CLASSPATH?
            is = SeedIterator.class.
                getResourceAsStream(IoUtils.getClasspathPath(getSeedfile()));
        } else if(getSeedfile().canRead()) {
            is = new FileInputStream(getSeedfile());
        }
        if (is == null) {
            throw new IOException(getSeedfile() + " does not" +
            " exist -- neither on disk nor on CLASSPATH -- or is not" +
            " readable.");
        }
        return is;
    }

    /**
     * @param seedfile The seedfile to set.
     */
    protected void setSeedfile(File seedfile) {
        this.seedfile = seedfile;
    }

    /** (non-Javadoc)
     * @see java.util.List#get(int)
     */
    public Object get(int index) {
        return (this.cache != null)? this.cache.get(index): null;
    }

    /** (non-Javadoc)
     * @see java.util.Collection#size()
     */
    public int size() {
        return (this.cache != null)? this.cache.size(): -1;
    }

    /**
     * Refreshes the cache, if caching in operation.
     *
     * If no cache, this is a noop.
     */
    public void refresh() {
        refresh(null);
    }

    /**
     * Refresh the seed file and the cache, if caching in operation.
     *
     * @param file Seed file.  If null, we use the default.  Otherwise, we
     * freshen our seedfile with whats passed here.
     */
    public void refresh(File file) {
        if (file != null) {
            setSeedfile(file);
        }

        // Get iterator before we check on whether cache exists.  Null cache
        // is signal inside in the iterator method that we should get iterator
        // over seed file rather than over cache.
        if (this.cache != null) {
            Iterator i = null;
            try {
                i = new SeedIterator();
            }
            catch (IOException e) {
                logger.severe("Failed to get seed iterator: " + e);
            }

            if (i != null) {
                synchronized (this.cache) {
                    this.cache.clear();
                    while(i. hasNext()) {
                        this.cache.add(i.next());
                    }
                }
            }
        }
    }

    /**
     * Add a URI to the list of seeds. Includes adding the URI to the seed file.
     *
     * Assumption is that the caller is worrying about synchronization.
     *
     * @param newSeed The new seed to add.
     * @return True if we updated the seedlist.
     */
    public boolean add(Object newSeed) {

        boolean result = false;
        if (!(newSeed instanceof UURI)) {
            throw new IllegalArgumentException("Must pass UURI: " + newSeed);
        }

        UURI uuri = (UURI)newSeed;

        if (this.cache != null) {
            this.cache.add(uuri);
             result = true;
        }

        File f = getSeedfile();
        if (f != null) {
            try {
                FileWriter fw = new FileWriter(f, true);
                // Write to new (last) line the URL.
                fw.write("\n");
                fw.write(uuri.toString());
                fw.flush();
                fw.close();
                result = true;
            } catch (IOException e) {
                DevUtils.warnHandle(e, "problem writing new seed");
            }
        }

        return result;
    }

    /**
     * @return Iterator over the seeds.  If you do not read the iterator
     * to the end, and this you are not caching the seedlist, there is a
     * dangling open file descriptor till the finally on the iterator runs
     * (if it ever runs).
     */
    public Iterator iterator() {
        Iterator iterator = null;
        try {
            iterator = (this.cache != null)?
                this.cache.iterator(): new SeedIterator();
        }
        catch (IOException e) {
            logger.severe("Failed to get iterator: " + e);
        }
        return iterator;
    }
    
    /**
     * @return Returns the logger.
     */
    protected static Logger getLogger() {
        return logger;
    }

    /**
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
    private class SeedIterator implements Iterator {

        /**
         * Pattern to extract seeds
         */
        private Pattern seedExtractor = DEFAULT_SEED_EXTRACTOR;

        UURI next = null;

        private BufferedReader reader;


        private SeedIterator() throws IOException {
            super();
            this.reader =
                new BufferedReader(new InputStreamReader(getSeedStream()));
        }

        /** (non-Javadoc)
         * @see java.util.Iterator#remove()
         */
        public void remove() {
            throw new UnsupportedOperationException();
        }

        /** (non-Javadoc)
         * @see java.util.Iterator#hasNext()
         */
        public boolean hasNext() {
            return (this.next != null)? true: loadNext();
        }

        /** (non-Javadoc)
         * @see java.util.Iterator#next()
         */
        public Object next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            // Next is guaranteed set by a loadNext which returned true
            UURI retVal = this.next;
            this.next = null;
            return retVal;
        }

        /**
         * Scanning the reader, load the 'next' field with the
         * next valid seed UURI.
         * @return True if next is loaded, false otherwise.
         */
        private boolean loadNext() {
            try {
                String read;
                nextline: while ((read = this.reader.readLine()) != null) {
                    read = read.trim();
                    if (read.length() == 0 || read.startsWith("#")) {
                        continue;
                    }
                    
                    Matcher m = this.seedExtractor.matcher(read);
                    while(m.find()) {
                        String candidate = m.group();
                        // Group #3 OR #5 has the scheme if any.
                        if(m.group(3) == null && m.group(5) == null) {
                            // This is a naked hostname without a scheme
                            candidate = "http://" + candidate;
                        }
                        try {
                            this.next = UURIFactory.getInstance(candidate);
                            // next loaded with next seed
                            return true;
                        } catch (URIException e1) {
                            Object[] array = {null, candidate};
                            getLogger().log(Level.INFO,
                                "Reading seeds: " + e1.getMessage(), array);
                            this.next = null;
                            // keep reading for valid seeds
                            continue nextline;
                        }
                    }
                    Object[] array = {null, read};
                    getLogger().log(Level.INFO, "bad seed line", array);
                }
                close();
                // no more seeds
                return false;
            } catch (IOException e) {
                DevUtils.warnHandle(e, "throw runtime error? log something?");
                return false;
            }
        }

        /**
         * As this iterator is backed by a reader, it should
         * receive a close() so that it can close the reader.
         * @throws IOException
         */
        public void close() throws IOException {
            if(this.reader != null) {
                this.reader.close();
                this.reader =  null;
            }
        }

        /* (non-Javadoc)
         * @see java.lang.Object#finalize()
         */
        protected void finalize() throws Throwable {
            super.finalize();
            close();
        }
    }
}
