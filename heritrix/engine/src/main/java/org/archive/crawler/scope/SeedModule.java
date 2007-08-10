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
 * CrawlScope.java
 * Created on Oct 1, 2003
 *
 * $Header$
 */
package org.archive.crawler.scope;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.io.Writer;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.httpclient.URIException;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.scope.SeedFileIterator;
import org.archive.crawler.scope.SeedListener;
import org.archive.crawler.scope.SeedRefreshListener;
import org.archive.net.UURI;
import org.archive.state.FileModule;
import org.archive.state.Expert;
import org.archive.state.Global;
import org.archive.state.Immutable;
import org.archive.state.Initializable;
import org.archive.state.Key;
import org.archive.state.KeyManager;
import org.archive.state.StateProvider;
import org.archive.util.DevUtils;

/**
 * Module that maintanis a list of seeds.
 *
 * @author gojomo
 *
 */
public class SeedModule implements Initializable, Serializable {

    private static final long serialVersionUID = 3L;

    private static final Logger logger =
        Logger.getLogger(SeedModule.class.getName());


    /**
     * File from which to extract seeds.
     */
    @Expert @Immutable
    final public static Key<FileModule> SEEDSFILE = 
        Key.make(FileModule.class, null);


    /**
     * Whether to reread the seeds specification, whether it has changed or not,
     * every time any configuration change occurs. If true, seeds are reread
     * even when (for example) new domain overrides are set. Rereading the seeds
     * can take a long time with large seed lists.
     */
    @Global @Expert
    final public static Key<Boolean> REREAD_SEEDS_ON_CONFIG = Key.make(true);


    protected Set<SeedListener> seedListeners = new HashSet<SeedListener>();

    protected Set<SeedRefreshListener> seedRefreshListeners = 
        new HashSet<SeedRefreshListener>();

    
    private FileModule seedsFile;

    
    static {
        KeyManager.addKeys(SeedModule.class);
    }
    

    /** 
     * Constructor.
     */
    public SeedModule() {
    }


    public void initialTasks(StateProvider provider) {
        this.seedsFile = provider.get(this, SEEDSFILE);
    }


    /**
     * Refresh seeds.
     *
     */
    public void refreshSeeds() {
        fireSeedsRefreshed();
    }
    
    
    protected void fireSeedsRefreshed() {
        for (SeedRefreshListener l: seedRefreshListeners) {
            l.seedsRefreshed(seedsIterator(null));
        }
    }

    /**
     * @return Seed list file or null if problem getting settings file.
     */
    public File getSeedfile() {
        return seedsFile.getFile();
    }

    /** Check if a URI is in the seeds.
     *
     * @param o the URI to check.
     * @return true if URI is a seed.
     */
    protected boolean isSeed(Object o) {
        return o instanceof CrawlURI && ((CrawlURI) o).isSeed();
    }

    /**
     * @param a First UURI of compare.
     * @param b Second UURI of compare.
     * @return True if UURIs are of same host.
     */
    protected boolean isSameHost(UURI a, UURI b) {
        boolean isSameHost = false;
        if (a != null && b != null) {
            // getHost can come back null.  See
            // "[ 910120 ] java.net.URI#getHost fails when leading digit"
            try {
                if (a.getReferencedHost() != null && b.getReferencedHost() != null) {
                    if (a.getReferencedHost().equals(b.getReferencedHost())) {
                        isSameHost = true;
                    }
                }
            }
            catch (URIException e) {
                logger.severe("Failed compare of " + a + " " + b + ": " +
                    e.getMessage());
            }
        }
        return isSameHost;
    }



    /* (non-Javadoc)
     * @see org.archive.crawler.settings.ModuleType#listUsedFiles(java.util.List)
     */
    public void listUsedFiles(List<String> list) {
        File file = getSeedfile();
        list.add(file.getAbsolutePath());
    }

    /**
     * Take note of a situation (such as settings edit) where
     * involved reconfiguration (such as reading from external
     * files) may be necessary.
     */
    public void kickUpdate(StateProvider context) {
        // TODO: further improve this so that case with hundreds of
        // thousands or millions of seeds works better without requiring
        // this specific settings check 
        if (context.get(this, REREAD_SEEDS_ON_CONFIG)) {
            refreshSeeds();
            this.fireSeedsRefreshed();
        }
    }

    /**
     * Gets an iterator over all configured seeds. Subclasses
     * which cache seeds in memory can override with more
     * efficient implementation. 
     *
     * @return Iterator, perhaps over a disk file, of seeds
     */
    public Iterator<UURI> seedsIterator() {
        return seedsIterator(null);
    }
    
    /**
     * Gets an iterator over all configured seeds. Subclasses
     * which cache seeds in memory can override with more
     * efficient implementation. 
     *
     * @param ignoredItemWriter optional writer to get ignored seed items report
     * @return Iterator, perhaps over a disk file, of seeds
     */
    public Iterator<UURI> seedsIterator(Writer ignoredItemWriter) {
        BufferedReader br;
        try {
            br = new BufferedReader(new FileReader(getSeedfile()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new SeedFileIterator(br,ignoredItemWriter);
    }
    
    /**
     * Convenience method to close SeedFileIterator, if appropriate.
     * 
     * @param iter Iterator to check if SeedFileIterator needing closing
     */
    protected void checkClose(Iterator iter) {
        if(iter instanceof SeedFileIterator) {
            ((SeedFileIterator)iter).close();
        }
    }
    
    /**
     * Add a new seed to scope. By default, simply appends
     * to seeds file, though subclasses may handle differently.
     *
     * <p>This method is *not* sufficient to get the new seed 
     * scheduled in the Frontier for crawling -- it only 
     * affects the Scope's seed record (and decisions which
     * flow from seeds). 
     *
     * @param curi CandidateUri to add
     * @return true if successful, false if add failed for any reason
     */
    public boolean addSeed(final CrawlURI curi) {
        File f = getSeedfile();
        if (f != null) {
            try {
                FileWriter fw = new FileWriter(f, true);
                // Write to new (last) line the URL.
                fw.write("\n");
                fw.write("# Heritrix added seed " +
                    ((curi.getVia() != null) ? "redirect from " + curi.getVia():
                        "(JMX)") + ".\n");
                fw.write(curi.toString());
                fw.flush();
                fw.close();
                Iterator iter = seedListeners.iterator();
                while(iter.hasNext()) {
                    ((SeedListener)iter.next()).addedSeed(curi);
                }
                return true;
            } catch (IOException e) {
                DevUtils.warnHandle(e, "problem writing new seed");
            }
        }
        return false; 
    }
    
    public void addSeedListener(SeedListener sl) {
        seedListeners.add(sl);
    }
}
