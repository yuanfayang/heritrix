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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import org.archive.crawler.datamodel.CoreAttributeConstants;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.event.CrawlStatusListener;
import org.archive.crawler.writer.MetadataProvider;
import org.archive.io.ObjectPlusFilesInputStream;
import org.archive.io.WriterPool;
import org.archive.io.WriterPoolMember;
import org.archive.io.WriterPoolSettings;
import org.archive.processors.Processor;
import org.archive.processors.fetcher.CrawlHost;
import org.archive.state.ExampleStateProvider;
import org.archive.state.Key;
import org.archive.state.KeyMaker;
import org.archive.state.StateProvider;

/**
 * Abstract implementation of a file pool processor.
 * Subclass to implement for a particular {@link WriterPoolMember} instance.
 * @author Parker Thompson
 * @author stack
 */
public abstract class WriterPoolProcessor extends Processor
implements CoreAttributeConstants, CrawlStatusListener {
    
    
    private final Logger logger = Logger.getLogger(this.getClass().getName());


    /**
     * Compress files when "writing to disk.
     */
    final public static Key<Boolean> COMPRESS = Key.makeFinal(true);

    
    /**
     * File prefix. The text supplied here will be used as a prefix naming
     * writer files. For example if the prefix is 'IAH', then file names will
     * look like IAH-20040808101010-0001-HOSTNAME.arc.gz ...if writing ARCs (The
     * prefix will be separated from the date by a hyphen).
     */
    final public static Key<String> PREFIX = 
        Key.makeFinal(WriterPoolMember.DEFAULT_PREFIX);


    /**
     * Where to save files. Supply absolute or relative path. If relative, files
     * will be written relative to the order.disk-path setting. If more than one
     * path specified, we'll round-robin dropping files to each. This setting is
     * safe to change midcrawl (You can remove and add new dirs as the crawler
     * progresses).
     */
    final public static Key<List<String>> PATH = 
        Key.makeFinal(Collections.singletonList("crawl-store"));


    /**
     * Suffix to tag onto files. If value is '${HOSTNAME}', will use hostname
     * for suffix. If empty, no suffix will be added.
     */
    final public static Key<String> SUFFIX = 
        Key.makeFinal(WriterPoolMember.DEFAULT_SUFFIX);


    /**
     * Max size of each file.
     */
    final public static Key<Integer> MAX_SIZE_BYTES = Key.makeFinal(100000000);

    
    /**
     * Maximum active files in pool. This setting cannot be varied over the life
     * of a crawl.
     */
    final public static Key<Integer> POOL_MAX_ACTIVE = 
        Key.makeFinal(WriterPool.DEFAULT_MAX_ACTIVE);


    /**
     * Maximum time to wait on pool element (milliseconds). This setting cannot
     * be varied over the life of a crawl.
     */
    final public static Key<Integer> POOL_MAX_WAIT = 
        Key.makeFinal(WriterPool.DEFAULT_MAXIMUM_WAIT);


    /**
     * Total file bytes to write to disk. Once the size of all files on disk has
     * exceeded this limit, this processor will stop the crawler. A value of
     * zero means no upper limit.
     */
    final public static Key<Long> MAX_BYTES_WRITTEN = Key.makeExpertFinal(0L);


    final public static Key<MetadataProvider> METADATA_PROVIDER = 
        Key.makeNull(MetadataProvider.class); 

    /**
     * Reference to pool.
     */
    transient private WriterPool pool = null;
    
    /**
     * Total number of bytes written to disc.
     */
    private long totalBytesWritten = 0;

    
    final private CrawlController controller;
    

    /**
     * @param name Name of this processor.
     * @param description Description for this processor.
     */
    public WriterPoolProcessor(CrawlController controller) {
        this.controller = controller;
    }


    @Override
    public synchronized void initialTasks(StateProvider context) {
        // Add this class to crawl state listeners and setup pool.
        controller.addCrawlStatusListener(this);
        setupPool(context, new AtomicInteger());
        // Run checkpoint recovery code.
        if (controller.isCheckpointRecover()) {
            checkpointRecover();
        }
    }
    
    
    
    protected AtomicInteger getSerialNo() {
        return ((WriterPool)getPool()).getSerialNo();
    }

    /**
     * Set up pool of files.
     */
    protected abstract void setupPool(StateProvider context, 
            final AtomicInteger serialNo);

    
    protected void checkBytesWritten(StateProvider context) {
        long max = context.get(this, MAX_BYTES_WRITTEN);
        if (max <= 0) {
            return;
        }
        if (max <= this.totalBytesWritten) {
            controller.requestCrawlStop(CrawlStatus.FINISHED_WRITE_LIMIT);
        }
    }
    
    protected String getHostAddress(CrawlURI curi) {
        CrawlHost h = curi.getCrawlHost();
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
    


	public void crawlEnding(String sExitMessage) {
		this.pool.close();
	}

	public void crawlEnded(String sExitMessage) {
        // sExitMessage is unused.
	}

    /* (non-Javadoc)
     * @see org.archive.crawler.event.CrawlStatusListener#crawlStarted(java.lang.String)
     */
    public void crawlStarted(String message) {
        // TODO Auto-generated method stub
    }
    
    protected String getCheckpointStateFile() {
    	return this.getClass().getName() + ".state";
    }
    
    public void crawlCheckpoint(StateProvider context, File checkpointDir) 
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
        saveCheckpointSerialNumber(checkpointDir, serial);
        // Close all ARCs on checkpoint.
        try {
            this.pool.close();
        } finally {
            // Reopen on checkpoint.
            setupPool(context, new AtomicInteger(serial));
        }
    }
    
	public void crawlPausing(String statusMessage) {
        // sExitMessage is unused.
	}

	public void crawlPaused(String statusMessage) {
        // sExitMessage is unused.
	}

	public void crawlResuming(String statusMessage) {
        // sExitMessage is unused.
	}
	
    private void readObject(ObjectInputStream stream)
    throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        ObjectPlusFilesInputStream coistream =
            (ObjectPlusFilesInputStream)stream;
        coistream.registerFinishTask( new Runnable() {
            public void run() {
                // FIXME: Figure out checkpointing in new settings system
            	setupPool(new ExampleStateProvider(), new AtomicInteger());
            }
        });
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
	
    /**
     * Called out of {@link #initialTasks()} when recovering a checkpoint.
     * Restore state.
     */
    protected void checkpointRecover() {
        int serialNo = loadCheckpointSerialNumber();
        if (serialNo != -1) {
            getSerialNo().set(serialNo);
        }
    }

    /**
     * @return Serial number from checkpoint state file or if unreadable, -1
     * (Client should check for -1).
     */
    protected int loadCheckpointSerialNumber() {
        int result = -1;
        
        // If in recover mode, read in the Writer serial number saved
        // off when we checkpointed.
        File stateFile = new File(controller.getCheckpointRecover().getDirectory(),
                getCheckpointStateFile());
        if (!stateFile.exists()) {
            logger.info(stateFile.getAbsolutePath()
                    + " doesn't exist so cannot restore Writer serial number.");
        } else {
            DataInputStream dis = null;
            try {
                dis = new DataInputStream(new FileInputStream(stateFile));
                result = dis.readShort();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (dis != null) {
                        dis.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }
    
    protected void saveCheckpointSerialNumber(final File checkpointDir,
            final int serialNo)
    throws IOException {
        // Write out the current state of the ARCWriter serial number.
        File f = new File(checkpointDir, getCheckpointStateFile());
        DataOutputStream dos = new DataOutputStream(new FileOutputStream(f));
        try {
            dos.writeShort(serialNo);
        } finally {
            dos.close();
        }
    }
    
    
    protected List<String> getMetadata(StateProvider context) {
        MetadataProvider provider = context.get(this, METADATA_PROVIDER);
        return provider.getMetadata();
    }

    
    protected abstract  Key<List<String>> getPathKey();
    
    private List<File> getOutputDirs(StateProvider context) {
        List<String> list = context.get(this, getPathKey());
        ArrayList<File> results = new ArrayList<File>();
        for (String path: list) {
            File f = new File(path);
            if (!f.isAbsolute()) {
                f = new File(controller.getDisk(), path);
            }
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
    
    protected WriterPoolSettings getWriterPoolSettings(
            final StateProvider context) {
        final int maxSize = context.get(this, MAX_SIZE_BYTES);
        final List<String> metadata = getMetadata(context);
        final List<File> output = getOutputDirs(context);
        final String prefix = context.get(this, PREFIX);
        final String suffix = context.get(this, SUFFIX);
        final boolean compressed = context.get(this, COMPRESS);
        return new WriterPoolSettings() {

            public int getMaxSize() {
                return maxSize;
            }

            public List<String> getMetadata() {
                return metadata;
            }

            public List<File> getOutputDirs() {
                return output;
            }

            public String getPrefix() {
                return prefix;
            }

            public String getSuffix() {
                return suffix;
            }

            public boolean isCompressed() {
                return compressed;
            }
            
        };
    }
    
    protected static Key<List<String>> makePath(String defaultPath) {
        KeyMaker<List<String>> km = KeyMaker.makeList(String.class);
        km.overrideable = false;
        km.def = Collections.singletonList(defaultPath);
        return km.toKey();
    }

}