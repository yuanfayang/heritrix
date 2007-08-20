/* WriterPoolProcessor
 *
 * $Id$
 *
 * Created on July 19th, 2006
 *
 * Copyright (C) 2006 Internet Archive.
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
package org.archive.crawler.framework;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.archive.crawler.datamodel.CrawlURI;
import static org.archive.crawler.datamodel.CoreAttributeConstants.*;

import org.archive.crawler.recrawl.IdenticalDigestDecideRule;
import org.archive.crawler.writer.MetadataProvider;
import org.archive.io.DefaultWriterPoolSettings;
import org.archive.io.WriterPool;
import org.archive.io.WriterPoolMember;
import org.archive.io.WriterPoolSettings;
import org.archive.state.FileModule;
import org.archive.modules.ProcessResult;
import org.archive.modules.Processor;
import org.archive.modules.ProcessorURI;
import org.archive.modules.net.CrawlHost;
import org.archive.modules.net.ServerCache;
import org.archive.modules.net.ServerCacheUtil;

import static org.archive.modules.fetcher.FetchStatusCodes.*;

import org.archive.settings.RecoverAction;
import org.archive.state.Expert;
import org.archive.state.Immutable;
import org.archive.state.Key;
import org.archive.state.KeyMaker;
import org.archive.state.KeyManager;
import org.archive.state.StateProvider;

/**
 * Abstract implementation of a file pool processor.
 * Subclass to implement for a particular {@link WriterPoolMember} instance.
 * @author Parker Thompson
 * @author stack
 */
public abstract class WriterPoolProcessor extends Processor 
implements Closeable {
    
    
//    private static final Logger logger = 
//        Logger.getLogger(WriterPoolProcessor.class.getName());


    /**
     * Compress files when "writing to disk.
     */
    @Immutable
    final public static Key<Boolean> COMPRESS = Key.make(true);

    
    /**
     * File prefix. The text supplied here will be used as a prefix naming
     * writer files. For example if the prefix is 'IAH', then file names will
     * look like IAH-20040808101010-0001-HOSTNAME.arc.gz ...if writing ARCs (The
     * prefix will be separated from the date by a hyphen).
     */
    @Immutable
    final public static Key<String> PREFIX = 
        Key.make(WriterPoolMember.DEFAULT_PREFIX);


    /**
     * Where to save files. Supply absolute or relative path. If relative, files
     * will be written relative to the order.disk-path setting. If more than one
     * path specified, we'll round-robin dropping files to each. This setting is
     * safe to change midcrawl (You can remove and add new dirs as the crawler
     * progresses).
     */
//    final public static Key<List<String>> PATH = 
//        Key.makeFinal(Collections.singletonList("crawl-store"));


    /**
     * Suffix to tag onto files. If value is '${HOSTNAME}', will use hostname
     * for suffix. If empty, no suffix will be added.
     */
    @Immutable
    final public static Key<String> SUFFIX = 
        Key.make(WriterPoolMember.DEFAULT_SUFFIX);


    /**
     * Max size of each file.
     */
    @Immutable
    final public static Key<Long> MAX_SIZE_BYTES = Key.make(100000000L);

    
    /**
     * Maximum active files in pool. This setting cannot be varied over the life
     * of a crawl.
     */
    @Immutable
    final public static Key<Integer> POOL_MAX_ACTIVE = 
        Key.make(WriterPool.DEFAULT_MAX_ACTIVE);


    /**
     * Maximum time to wait on pool element (milliseconds). This setting cannot
     * be varied over the life of a crawl.
     */
    @Immutable
    final public static Key<Integer> POOL_MAX_WAIT = 
        Key.make(WriterPool.DEFAULT_MAXIMUM_WAIT);

    
    /**
     * Whether to skip the writing of a record when URI history information is
     * available and indicates the prior fetch had an identical content digest.
     * Default is false.
     */
    final public static Key<Boolean> SKIP_IDENTICAL_DIGESTS = Key.make(false);


    /**
     * CrawlURI annotation indicating no record was written.
     */
    protected static final String ANNOTATION_UNWRITTEN = "unwritten";

    /**
     * Total file bytes to write to disk. Once the size of all files on disk has
     * exceeded this limit, this processor will stop the crawler. A value of
     * zero means no upper limit.
     */
    @Immutable @Expert
    final public static Key<Long> TOTAL_BYTES_TO_WRITE = Key.make(0L);

    @Immutable
    final public static Key<MetadataProvider> METADATA_PROVIDER = 
        Key.makeAuto(MetadataProvider.class);

    @Immutable
    final public static Key<ServerCache> SERVER_CACHE = 
        Key.makeAuto(ServerCache.class);

    @Immutable
    final public static Key<FileModule> DIRECTORY =
        Key.make(FileModule.class, null);
    
    
    static {
        KeyManager.addKeys(WriterPoolProcessor.class);
    }
    
    /**
     * Reference to pool.
     */
    transient private WriterPool pool = null;
    
    /**
     * Total number of bytes written to disc.
     */
    private long totalBytesWritten = 0;

    
    private ServerCache serverCache;
    private FileModule directory;
    private WriterPoolSettings settings;
    private int maxActive;
    private int maxWait;
    private AtomicInteger serial = new AtomicInteger();
    

    /**
     * @param name Name of this processor.
     * @param description Description for this processor.
     */
    public WriterPoolProcessor() {
        super();
    }


    @Override
    public synchronized void initialTasks(StateProvider context) {
        this.serverCache = context.get(this, SERVER_CACHE);
        this.directory = context.get(this, DIRECTORY);
        this.maxActive = context.get(this, POOL_MAX_ACTIVE);
        this.maxWait = context.get(this, POOL_MAX_WAIT);
        this.settings = getWriterPoolSettings(context);
        setupPool(serial);
    }
    
    
    
    protected AtomicInteger getSerialNo() {
        return ((WriterPool)getPool()).getSerialNo();
    }

    /**
     * Set up pool of files.
     */
    protected abstract void setupPool(final AtomicInteger serial);

    
    protected ProcessResult checkBytesWritten(StateProvider context) {
        long max = context.get(this, TOTAL_BYTES_TO_WRITE);
        if (max <= 0) {
            return ProcessResult.PROCEED;
        }
        if (max <= this.totalBytesWritten) {
            return ProcessResult.FINISH; // FIXME: Specify reason
//            controller.requestCrawlStop(CrawlStatus.FINISHED_WRITE_LIMIT);
        }
        return ProcessResult.PROCEED;
    }
    
    /**
     * Whether the given CrawlURI should be written to archive files.
     * Annotates CrawlURI with a reason for any negative answer.
     * 
     * @param curi CrawlURI
     * @return true if URI should be written; false otherwise
     */
    protected boolean shouldWrite(CrawlURI curi) {
        if (curi.get(this, SKIP_IDENTICAL_DIGESTS)
            && IdenticalDigestDecideRule.hasIdenticalDigest(curi)) {
            curi.getAnnotations().add(ANNOTATION_UNWRITTEN 
                    + ":identicalDigest");
            return false;
        }
        
        boolean retVal;
        String scheme = curi.getUURI().getScheme().toLowerCase();
        // TODO: possibly move this sort of isSuccess() test into CrawlURI
        if (scheme.equals("dns")) {
            retVal = curi.getFetchStatus() == S_DNS_SUCCESS;
        } else if (scheme.equals("http") || scheme.equals("https")) {
            retVal = curi.getFetchStatus() > 0 && curi.isHttpTransaction();
        } else if (scheme.equals("ftp")) {
            retVal = curi.getFetchStatus() == 200;
        } else {
            curi.getAnnotations().add(ANNOTATION_UNWRITTEN
                    + ":scheme");
            return false;
        }
        
        if (retVal == false) {
            // status not deserving writing
            curi.getAnnotations().add(ANNOTATION_UNWRITTEN + ":status");
            return false;
        }
        
        return true; 
    }
    
    /**
     * Return IP address of given URI suitable for recording (as in a
     * classic ARC 5-field header line).
     * 
     * @param curi CrawlURI
     * @return String of IP address
     */
    protected String getHostAddress(CrawlURI curi) {
        // special handling for DNS URIs: want address of DNS server
        if (curi.getUURI().getScheme().toLowerCase().equals("dns")) {
            return (String)curi.getData().get(A_DNS_SERVER_IP_LABEL);
        }
        // otherwise, host referenced in URI
        CrawlHost h = ServerCacheUtil.getHostFor(serverCache, curi.getUURI());
        if (h == null) {
            throw new NullPointerException("Crawlhost is null for " +
                curi + " " + curi.getVia());
        }
        InetAddress a = h.getIP();
        if (a == null) {
            throw new NullPointerException("Address is null for " +
                curi + " " + curi.getVia() + ". Address " +
                ((h.getIpFetched() == CrawlHost.IP_NEVER_LOOKED_UP)?
                     "was never looked up.":
                     (System.currentTimeMillis() - h.getIpFetched()) +
                         " ms ago."));
        }
        return h.getIP().getHostAddress();
    }

    
    public void checkpoint(File checkpointDir, List<RecoverAction> actions) 
    throws IOException {
        int serial = getSerialNo().get();
        if (this.pool.getNumActive() > 0) {
            // If we have open active Archive files, up the serial number
            // so after checkpoint, we start at one past current number and
            // so the number we serialize, is one past current serialNo.
            // All this serial number manipulation should be fine in here since
            // we're paused checkpointing (Revisit if this assumption changes).
            serial = getSerialNo().incrementAndGet();
        }

        // Close all ARCs on checkpoint.
        try {
            this.pool.close();
        } finally {
            // Reopen on checkpoint.
            this.serial = new AtomicInteger(serial);
            setupPool(this.serial);
        }
    }
  
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
    }
    
    
    private void readObject(ObjectInputStream stream) 
    throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        this.setupPool(serial);
    }

    protected WriterPool getPool() {
        return pool;
    }

    protected void setPool(WriterPool pool) {
        this.pool = pool;
    }

    protected long getTotalBytesWritten() {
        return totalBytesWritten;
    }

    protected void setTotalBytesWritten(long totalBytesWritten) {
        this.totalBytesWritten = totalBytesWritten;
    }
	
    
    protected abstract List<String> getMetadata(StateProvider context);


    
    protected abstract  Key<List<String>> getPathKey();
    
    private List<File> getOutputDirs(StateProvider context) {
        List<String> list = context.get(this, getPathKey());
        ArrayList<File> results = new ArrayList<File>();
        for (String path: list) {
            String p = directory.toAbsolutePath(path);
            File f = new File(p);
            if (!f.exists()) {
                try {
                    f.mkdirs();
                } catch (Exception e) {
                    e.printStackTrace();
                    continue;
                }
            }
            results.add(f);
        }
        return results;        
    }
    
    
    protected WriterPoolSettings getWriterPoolSettings() {
        return settings;
    }
    
    
    protected int getMaxActive() {
        return maxActive;
    }
    
    
    protected int getMaxWait() {
        return maxWait;
    }
    
    
    private WriterPoolSettings getWriterPoolSettings(
            final StateProvider context) {
        DefaultWriterPoolSettings result = new DefaultWriterPoolSettings();
        result.setMaxSize(context.get(this, MAX_SIZE_BYTES));
        result.setMetadata(getMetadata(context));
        result.setOutputDirs(getOutputDirs(context));
        result.setPrefix(context.get(this, PREFIX));
        result.setSuffix(context.get(this, SUFFIX));
        result.setCompressed(context.get(this, COMPRESS));
        return result;
    }
    
    protected static Key<List<String>> makePath(String defaultPath) {
        KeyMaker<List<String>> km = KeyMaker.makeList(String.class);
        km.def = Collections.singletonList(defaultPath);
        return km.toKey();
    }

    
    @Override
    protected void innerProcess(ProcessorURI puri) {
        throw new AssertionError();
    }


    @Override
    protected abstract ProcessResult innerProcessResult(ProcessorURI uri);

    
    public void close() {
        this.pool.close();
    }


    protected boolean shouldProcess(ProcessorURI uri) {
        if (!(uri instanceof CrawlURI)) {
            return false;
        }
        
        CrawlURI curi = (CrawlURI)uri;
        // If failure, or we haven't fetched the resource yet, return
        if (curi.getFetchStatus() <= 0) {
            return false;
        }
        
        // If no recorded content at all, don't write record.
        long recordLength = curi.getContentSize();
        if (recordLength <= 0) {
            // getContentSize() should be > 0 if any material (even just
            // HTTP headers with zero-length body is available.
            return false;
        }
        
        return true;
    }


}
