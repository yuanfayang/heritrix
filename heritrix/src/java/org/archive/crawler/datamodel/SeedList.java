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
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
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

import org.archive.util.DevUtils;


/**
 * Class to manage the seed list.
 * 
 * Manages cache of seeds.
 * 
 * @author stack
 * @version $Revision$, $Date$
 */
public class SeedList extends AbstractList {
    
    /**
     * Regexp for identifying URIs in seed input data
     */
    static final Pattern DEFAULT_SEED_EXTRACTOR =
        Pattern.compile("(?i:((https?://[a-zA-Z0-9-]+)|([a-zA-Z0-9-]+" +
            "\\.[a-zA-Z0-9-]+))(\\.[a-zA-Z0-9-]+)*(:\\d+)?(/\\S*)?)");

    /**
     * Cached name of seed file.  
     * 
     * Can be refreshed by passing an argument to {@link #refresh(File)}
     */
    private File seedfile = null;
    
    /**
     * Where to log.
     * 
     * Passed in to the constructor.
     */
    final Logger logger;
    
    /**
     * Cache of seeds.
     * 
     * Cache is instantiated on first call to {@link #refresh()}.  If refresh
     * method is never called, cache is never used (Null cache data member
     * is used as flag in iterator method; if null, return an iterator that 
     * goes against raw seed file, else return the cache iterator).
     */
    private List cache = null;

    
    /**
     * Constructor
     * 
     * @param seedfile Default seed file to use.
     * @param logger Logger to use logging seed problems.
     */
    public SeedList(File seedfile, Logger logger) {
        this.logger = logger;
        this.seedfile = seedfile;
    }
    
    /**
     * Blocked default onstructor.
     */
    private SeedList() {
        super();
        // Must initialize with something because logger is final.
        this.logger = null;
    }
    
    /**
     * @return Returns the current seedfile.
     */
    protected File getSeedfile() {
        return this.seedfile;
    }

    /**
     * @param seedfile The seedfile to set.
     */
    protected void setSeedfile(File seedfile) {
        this.seedfile = seedfile;
    }
    
    /* (non-Javadoc)
     * @see java.util.List#get(int)
     */
    public Object get(int index) {
        return (this.cache != null)? this.cache.get(index): null;
    }

    /* (non-Javadoc)
     * @see java.util.Collection#size()
     */
    public int size() {
        return (this.cache != null)? this.cache.size(): -1; 
    }
    
    /** 
     * Refreshes the cache.
     * 
     * The cache is instantiated on first call to this method and used
     * ever after.  Callers that do not want caching should not call this 
     * method.
     */
    public void refresh() {
        refresh(null);
    }
    
    /** 
     * Refreshes the cache.
     * 
     * @param file Seed file.  If null, we use the default.  Otherwise, we
     * freshen our seedfile with whats passed here.
     * 
     * The cache is instantiated on first call to this method and used
     * ever after.  Callers that do not want caching should not call this 
     * method.
     */
    public synchronized void refresh(File file) {
        if (file != null) {
            setSeedfile(file);
        }

        // Get iterator before we check on whether cache exists.  Null cache 
        // is signal inside in the iterator method that we should get iterator
        // over seed file rather than over cache.
        Iterator i = iterator();
        if (this.cache == null) {
            this.cache = Collections.synchronizedList(new ArrayList());
        } else {
            this.cache.clear();
        }
        synchronized (this.cache) {
            // 
            if (this.cache == null) {
                this.cache = Collections.synchronizedList(new ArrayList());
            } else {
                this.cache.clear();
            }
            while(i. hasNext()) {
                this.cache.add(i.next());
            }
        }
    }
    
    /**
     * Add a URI to the list of seeds. Includes adding the URI to the seed file.
     * 
     * @param newSeed The new seed to add.
     * @return True if we updated the seedlist.
     */
    public boolean add(Object newSeed) {
        
        boolean result = false;
        if (!(newSeed instanceof UURI)) {
            throw new IllegalArgumentException("Must pass a UURI: " + newSeed);
        }
        
        if (this.cache != null) {
            synchronized(this.cache) {
                // Recheck flag. May have changed since we came inside this 
                // sync block.
                if (this.cache != null) {
                    this.cache.add(newSeed);
                    result = true;
                }
            }
        }

        File f = getSeedfile();
        if (f != null) {
            try {
                FileWriter fw = new FileWriter(f, true);
                // Write to new (last) line the URL.
                fw.write("\n");
                fw.write(newSeed.toString());
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
     * to the end, there is a dangling open file descriptor till the finally 
     * on the iterator runs (if it ever runs).
     */
    public Iterator iterator() {
        Iterator iterator = null;
        try {
            iterator = (this.cache != null)?
                this.cache.iterator(): new SeedIterator();
        }
        catch (IOException e) {
            this.logger.severe("Failed to get iterator: " + e);
        }
        return iterator;
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
            if (!getSeedfile().exists() ||
                    !getSeedfile().canRead()) {
                throw new IOException(getSeedfile() + " does not" +
                    " exist or is not readable.");
            }
            this.reader =
                new BufferedReader(new FileReader(getSeedfile()));
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
            return (this.next != null)? true: loadNext();
        }
        
        /* (non-Javadoc)
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
         * next valid seed UURI. Return true if next is loaded,
         * false otherwise.
         *
         */
        private boolean loadNext() {
            try {
                String read;
                while ((read = this.reader.readLine()) != null) {
                    read = read.trim();
                    if (read.length() == 0 || read.startsWith("#")) {
                        continue;
                    }
                    Matcher m = this.seedExtractor.matcher(read);
                    while(m.find()) {
                        String candidate = m.group();
                        if(m.group(2)==null) {
                            // naked hostname without scheme
                            candidate = "http://" + candidate;
                        }
                        try {
                            this.next = UURI.createUURI(candidate);
                            // next loaded with next seed
                            return true;
                        } catch (URISyntaxException e1) {
                            Object[] array = { null, candidate };
                            SeedList.this.logger.log(Level.INFO,
                                "Reading seeds: " + e1.getMessage(), array );
                            this.next = null;
                            // keep reading for valid seeds
                            continue;
                        }
                    }
                    Object[] array = { null, read };
                    SeedList.this.logger.log(Level.INFO, "bad seed line",
                        array);
                }
                close();
                // no more seeds
                return false;
            } catch (IOException e) {
                DevUtils.warnHandle(e, "throw runtime error? log something?");
                return false;
            }
        }
        
        public void close() {
            if(this.reader != null) {
                try {
                    this.reader.close();
                    this.reader =  null;
                }
                catch (IOException e) {
                    SeedList.this.logger.severe(
                        "Failed close of the seed reader.");
                }
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
