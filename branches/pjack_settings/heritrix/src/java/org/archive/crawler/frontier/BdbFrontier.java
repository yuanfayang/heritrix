/* BdbFrontier
 * 
 * $Id$
* 
 * Created on Sep 24, 2004
 *
 *  Copyright (C) 2004 Internet Archive.
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
  */
package org.archive.crawler.frontier;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.UriUniqFilter;
import org.archive.crawler.framework.CrawlController;
import org.archive.crawler.framework.FrontierMarker;
import org.archive.crawler.framework.exceptions.FatalConfigurationException;
import org.archive.crawler.util.BdbUriUniqFilter;
import org.archive.crawler.util.CheckpointUtils;
import org.archive.state.Expert;
import org.archive.state.Key;
import org.archive.state.StateProvider;
import org.archive.util.ArchiveUtils;

import com.sleepycat.je.DatabaseException;

/**
 * A Frontier using several BerkeleyDB JE Databases to hold its record of
 * known hosts (queues), and pending URIs. 
 *
 * @author Gordon Mohr
 */
public class BdbFrontier extends WorkQueueFrontier implements Serializable {
    // be robust against trivial implementation changes
    private static final long serialVersionUID = ArchiveUtils
        .classnameBasedUID(BdbFrontier.class, 1);

    private static final Logger logger =
        Logger.getLogger(BdbFrontier.class.getName());

    /** all URIs scheduled to be crawled */
    protected transient BdbMultipleWorkQueues pendingUris;


    
    /**
     * Create the single object (within which is one BDB database)
     * inside which all the other queues live. 
     * 
     * @return the created BdbMultipleWorkQueues
     * @throws DatabaseException
     */
    private BdbMultipleWorkQueues createMultipleWorkQueues()
    throws DatabaseException {
        return new BdbMultipleWorkQueues(this.controller.getBdbEnvironment(),
            this.controller.getClassCatalog(),
            this.controller.isCheckpointRecover());
    }

    
     
    protected UriUniqFilter deserializeAlreadySeen(
            final Class<? extends UriUniqFilter> cls,
            final File dir)
    throws FileNotFoundException, IOException {
        UriUniqFilter uuf = null;
        try {
            logger.fine("Started deserializing " + cls.getName() +
                " of checkpoint recover.");
            uuf = CheckpointUtils.readObjectFromFile(cls, dir);
            logger.fine("Finished deserializing bdbje as part " +
                "of checkpoint recover.");
        } catch (ClassNotFoundException e) {
            throw new IOException("Failed to deserialize "  +
                cls.getName() + ": " + e.getMessage());
        }
        return uuf;
    }

    /**
     * Return the work queue for the given CrawlURI's classKey. URIs
     * are ordered and politeness-delayed within their 'class'.
     * 
     * @param curi CrawlURI to base queue on
     * @return the found or created BdbWorkQueue
     */
    protected WorkQueue getQueueFor(CrawlURI curi) {
        WorkQueue wq;
        String classKey = curi.getClassKey();
        synchronized (allQueues) {
            wq = (WorkQueue)allQueues.get(classKey);
            if (wq == null) {
                wq = new BdbWorkQueue(classKey, this);
                wq.setTotalBudget(curi.get(this, QUEUE_TOTAL_BUDGET));
                allQueues.put(classKey, wq);
            }
        }
        return wq;
    }
    
    /**
     * Return the work queue for the given classKey, or null
     * if no such queue exists.
     * 
     * @param classKey key to look for
     * @return the found WorkQueue
     */
    protected WorkQueue getQueueFor(String classKey) {
        WorkQueue wq; 
        synchronized (allQueues) {
            wq = (WorkQueue)allQueues.get(classKey);
        }
        return wq;
    }

    public FrontierMarker getInitialMarker(String regexpr,
            boolean inCacheOnly) {
        return pendingUris.getInitialMarker(regexpr);
    }

    /**
     * Return list of urls.
     * @param marker
     * @param numberOfMatches
     * @param verbose 
     * @return List of URIs (strings).
     */
    public ArrayList<String> getURIsList(FrontierMarker marker, 
            int numberOfMatches, final boolean verbose) {
        List curis;
        try {
            curis = pendingUris.getFrom(marker, numberOfMatches);
        } catch (DatabaseException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        ArrayList<String> results = new ArrayList<String>(curis.size());
        Iterator iter = curis.iterator();
        while(iter.hasNext()) {
            CrawlURI curi = (CrawlURI) iter.next();
            results.add("["+curi.getClassKey()+"] "+curi.singleLineReport());
        }
        return results;
    }
    
    protected void initQueue() throws IOException {
        try {
            this.pendingUris = createMultipleWorkQueues();
        } catch(DatabaseException e) {
            throw (IOException)new IOException(e.getMessage()).initCause(e);
        }
    }
    
    protected void closeQueue() {
        if (this.pendingUris != null) {
            this.pendingUris.close();
            this.pendingUris = null;
        }
    }
        
    protected BdbMultipleWorkQueues getWorkQueues() {
        return pendingUris;
    }

    protected boolean workQueueDataOnDisk() {
        return true;
    }

    
    /**
     * Constructor.
     */
    public BdbFrontier(CrawlController c,
            QueueAssignmentPolicy qap,
            UriUniqFilter alreadySeen)
    throws FatalConfigurationException, IOException {
        super(c, qap, alreadySeen);
//    }

    
//    public void initialize(CrawlController c)
//    throws FatalConfigurationException, IOException {
//        super.initialize(c);
        if (c.isCheckpointRecover()) {
            // If a checkpoint recover, copy old values from serialized
            // instance into this Frontier instance. Do it this way because 
            // though its possible to serialize BdbFrontier, its currently not
            // possible to set/remove frontier attribute plugging the
            // deserialized object back into the settings system.
            // The below copying over is error-prone because its easy
            // to miss a value.  Perhaps there's a better way?  Introspection?
            BdbFrontier f = null;
            try {
                f = (BdbFrontier)CheckpointUtils.
                    readObjectFromFile(this.getClass(),
                        this.controller.getCheckpointRecover().getDirectory());
            } catch (FileNotFoundException e) {
                throw new FatalConfigurationException("Failed checkpoint " +
                    "recover: " + e.getMessage());
            } catch (IOException e) {
                throw new FatalConfigurationException("Failed checkpoint " +
                    "recover: " + e.getMessage());
            } catch (ClassNotFoundException e) {
                throw new FatalConfigurationException("Failed checkpoint " +
                    "recover: " + e.getMessage());
            }

            this.nextOrdinal = f.nextOrdinal;
            this.totalProcessedBytes = f.totalProcessedBytes;
            this.disregardedUriCount = f.disregardedUriCount;
            this.failedFetchCount = f.failedFetchCount;
            this.processedBytesAfterLastEmittedURI =
                f.processedBytesAfterLastEmittedURI;
            this.queuedUriCount = f.queuedUriCount;
            this.succeededFetchCount = f.succeededFetchCount;
            this.lastMaxBandwidthKB = f.lastMaxBandwidthKB;
            this.readyClassQueues = f.readyClassQueues;
            this.inactiveQueues = f.inactiveQueues;
            this.retiredQueues = f.retiredQueues;
            this.snoozedClassQueues = f.snoozedClassQueues;
            this.inProcessQueues = f.inProcessQueues;
            wakeQueues();
        }
    }

    public void crawlCheckpoint(StateProvider context,
            File checkpointDir) throws Exception {
        super.crawlCheckpoint(context, checkpointDir);
        logger.fine("Started serializing already seen as part "
            + "of checkpoint. Can take some time.");
        // An explicit sync on the any deferred write dbs is needed to make the
        // db recoverable. Sync'ing the environment doesn't work.
        if (this.pendingUris != null) {
        	this.pendingUris.sync();
        }
        CheckpointUtils .writeObjectToFile(this.alreadyIncluded, checkpointDir);
        logger.fine("Finished serializing already seen as part "
            + "of checkpoint.");
        // Serialize ourselves.
        CheckpointUtils.writeObjectToFile(this, checkpointDir);
    }
    
    
}
