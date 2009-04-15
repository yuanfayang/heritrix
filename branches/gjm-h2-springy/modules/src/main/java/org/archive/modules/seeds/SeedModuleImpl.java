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
package org.archive.modules.seeds;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Serializable;
import java.io.Writer;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.httpclient.URIException;
import org.archive.modules.ProcessorURI;
import org.archive.net.UURI;
import org.archive.openmbeans.annotations.Operation;
import org.archive.settings.CheckpointRecovery;
import org.archive.settings.RecoverAction;
import org.archive.settings.file.Checkpointable;
import org.archive.spring.ConfigFile;
import org.archive.spring.ReadSource;
import org.archive.spring.WriteTarget;
import org.archive.util.DevUtils;
import org.archive.util.FileUtils;

/**
 * Module that maintains a list of seeds.
 *
 * TODO: Rename to SeedModule in absence of separate interface
 * 
 * @contributor gojomo
 */
public class SeedModuleImpl implements 
    ReadSource,
    Serializable, 
    Checkpointable {

    private static final long serialVersionUID = 3L;

    private static final Logger logger =
        Logger.getLogger(SeedModuleImpl.class.getName());

    /**
     * File from which to extract seeds.
     */
    protected ReadSource seedsSource = 
        (ReadSource) new ConfigFile("seeds file","seeds.txt");
    public ReadSource getSeedsSource() {
        return seedsSource;
    }
    public void setSeedsSource(ReadSource seedsSource) {
        this.seedsSource = seedsSource;
    }

    /**
     * Whether to reread the seeds specification, whether it has changed or not,
     * every time any configuration change occurs. If true, seeds are reread
     * even when (for example) new domain overrides are set. Rereading the seeds
     * can take a long time with large seed lists.
     */
    protected boolean rereadSeedsOnConfig = true;
    public boolean getRereadSeedsOnConfig() {
        return rereadSeedsOnConfig;
    }
    public void setRereadSeedsOnConfig(boolean rereadSeedsOnConfig) {
        this.rereadSeedsOnConfig = rereadSeedsOnConfig;
    }


    protected Set<SeedListener> seedListeners = new HashSet<SeedListener>();

    protected Set<SeedRefreshListener> seedRefreshListeners = 
        new HashSet<SeedRefreshListener>();

    /** 
     * Constructor.
     */
    public SeedModuleImpl() {
        super();
    }

    /**
     * Refresh seeds.
     *
     */
    @Operation(desc="Notify all listeners that the seeds file has changed.")
    public void refreshSeeds() {
        fireSeedsRefreshed();
    }
    
    
    protected void fireSeedsRefreshed() {
        for (SeedRefreshListener l: seedRefreshListeners) {
            l.seedsRefreshed();
        }
    }

    /** Check if a URI is in the seeds.
     *
     * @param o the URI to check.
     * @return true if URI is a seed.
     */
    protected boolean isSeed(Object o) {
        return o instanceof ProcessorURI && ((ProcessorURI) o).isSeed();
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

    /**
     * Take note of a situation (such as settings edit) where
     * involved reconfiguration (such as reading from external
     * files) may be necessary.
     */
    public void noteReconfiguration(/*KeyChangeEvent event*/) {
        // TODO: further improve this so that case with hundreds of
        // thousands or millions of seeds works better without requiring
        // this specific settings check 
        if (getRereadSeedsOnConfig()) {
            refreshSeeds();
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
        BufferedReader br = new BufferedReader(seedsSource.getReader());
        return new SeedFileIterator(br,ignoredItemWriter);
    }
    
    /**
     * Convenience method to close SeedFileIterator, if appropriate.
     * 
     * @param iter Iterator to check if SeedFileIterator needing closing
     */
    protected void checkClose(Iterator<?> iter) {
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
    public boolean addSeed(final ProcessorURI curi) {
        if(!(seedsSource instanceof WriteTarget)) {
            // TODO: do something else to log seed update
            
        } else {
            try {
                Writer fw = ((WriteTarget)seedsSource).getWriter(true);
                // Write to new (last) line the URL.
                fw.write("\n");
                fw.write("# Heritrix added seed " +
                    ((curi.getVia() != null) ? "redirect from " + curi.getVia():
                        "(JMX)") + ".\n");
                fw.write(curi.toString());
                fw.flush();
                fw.close();
                Iterator<SeedListener> iter = seedListeners.iterator();
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
    
    public void addSeedRefreshListener(SeedRefreshListener srl) {
        seedRefreshListeners.add(srl);
    }


    public void checkpoint(File dir, List<RecoverAction> actions)
            throws IOException {
        int id = System.identityHashCode(this);
        String backup = "seeds" + id + " .txt";
        //TODO:SPRINGY
//        FileUtils.copyFile(getSeedsFile().getFile(), new File(dir, backup));
//        actions.add(new SeedModuleRecoverAction(backup, getSeedsFile().getFile()));
    }

    
    private static class SeedModuleRecoverAction implements RecoverAction {

        private static final long serialVersionUID = 1L;

        private File target;
        private String backup;
        
        public SeedModuleRecoverAction(String backup, File target) {
            this.target = target;
            this.backup = backup;
        }
        
        public void recoverFrom(File checkpointDir, CheckpointRecovery cr)
                throws Exception {
            target = new File(cr.translatePath(target.getAbsolutePath()));
            FileUtils.copyFile(new File(checkpointDir, backup), target); 
        }
        
    }

    public Reader getReader() {
        return seedsSource.getReader();
    }
}
