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
 * CrawlController.java
 * Created on May 14, 2003
 *
 * $Id$
 */
package org.archive.crawler.framework;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.Notification;

import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.event.CrawlStatusListener;
import org.archive.crawler.event.CrawlURIDispositionListener;
import org.archive.crawler.framework.exceptions.FatalConfigurationException;
import org.archive.modules.Processor;
import org.archive.modules.net.ServerCache;
import org.archive.openmbeans.annotations.Bean;
import org.archive.openmbeans.annotations.Emitter;
import org.archive.settings.KeyChangeEvent;
import org.archive.settings.KeyChangeListener;
import org.archive.settings.ListModuleListener;
import org.archive.settings.SheetManager;
import org.archive.settings.SingleSheet;
import org.archive.state.Expert;
import org.archive.state.Global;
import org.archive.state.Immutable;
import org.archive.state.Initializable;
import org.archive.state.Key;
import org.archive.state.KeyManager;
import org.archive.state.Path;
import org.archive.state.StateProvider;
import org.archive.util.ArchiveUtils;
import org.archive.util.Reporter;
import org.xbill.DNS.DClass;
import org.xbill.DNS.Lookup;

/**
 * CrawlController collects all the classes which cooperate to
 * perform a crawl and provides a high-level interface to the
 * running crawl.
 *
 * As the "global context" for a crawl, subcomponents will
 * often reach each other through the CrawlController.
 *
 * @author Gordon Mohr
 */
public class CrawlControllerImpl extends Bean implements 
    Serializable, 
    Reporter, 
    StateProvider, 
    Initializable,
    KeyChangeListener,
    CrawlController {
 
    // be robust against trivial implementation changes
    private static final long serialVersionUID =
        ArchiveUtils.classnameBasedUID(CrawlControllerImpl.class,1);


    @Immutable
    final public static Key<ServerCache> SERVER_CACHE = 
        Key.makeAuto(ServerCache.class);


    /**
     * The frontier to use for the crawl.
     */
    @Immutable
    final public static Key<Frontier> FRONTIER = Key.makeAuto(Frontier.class);

    
    @Immutable
    final public static Key<Path> SCRATCH_DIR = 
        Key.make(new Path("scratch"));
    
    @Immutable
    final public static Key<Path> CHECKPOINTS_DIR =
        Key.make(new Path("checkpoints"));


    /**
     * Statistics tracking modules.  Any number of specialized statistics 
     * trackers that monitor a crawl and write logs, reports and/or provide 
     * information to the user interface.
     */
    @Global
    final public static Key<List<StatisticsTracking>> LOGGERS = 
        Key.makeList(StatisticsTracking.class);

    final public static Key<Map<String,Processor>> PROCESSORS =
        Key.makeMap(Processor.class);

    
    /**
     * Maximum number of bytes to download. Once this number is exceeded 
     * the crawler will stop. A value of zero means no upper limit.
     */
    @Global
    final public static Key<Long> MAX_BYTES_DOWNLOAD = Key.make(0L);


    /**
     * Maximum number of documents to download. Once this number is exceeded the 
     * crawler will stop. A value of zero means no upper limit.
     */
    @Global
    final public static Key<Long> MAX_DOCUMENT_DOWNLOAD = Key.make(0L);


    /**
     * Maximum amount of time to crawl (in seconds). Once this much time has 
     * elapsed the crawler will stop. A value of zero means no upper limit.
     */
    @Global
    final public static Key<Long> MAX_TIME_SEC = Key.make(0L);


    /**
     * Maximum number of threads processing URIs at the same time.
     */
    final public static Key<Integer> MAX_TOE_THREADS = Key.make(25);

    /** whether to pause, rather than finish, when crawl appears done */
    final public static Key<Boolean> PAUSE_AT_FINISH = Key.make(false);
    
    /** whether to pause at crawl start */
    final public static Key<Boolean> PAUSE_AT_START = Key.make(false);

    /**
     * Size in bytes of in-memory buffer to record outbound traffic. One such 
     * buffer is reserved for every ToeThread. 
     */
    @Expert @Immutable
    final public static Key<Integer> RECORDER_OUT_BUFFER_BYTES = Key.make(4096);


    /**
     * Size in bytes of in-memory buffer to record inbound traffic. One such 
     * buffer is reserved for every ToeThread.
     */
    @Expert @Immutable
    final public static Key<Integer> RECORDER_IN_BUFFER_BYTES = 
        Key.make(65536);
    
    
    @Immutable
    final public static Key<SheetManager> SHEET_MANAGER = 
        Key.makeAuto(SheetManager.class);

    final public static Key<CrawlerLoggerModule> LOGGER_MODULE =
        Key.makeAuto(CrawlerLoggerModule.class);

    @Immutable
    final public static Key<Integer> CHECKPOINTER_PERIOD = Key.make(-1);

    static {
        KeyManager.addKeys(CrawlControllerImpl.class);
    }
    
    /**
     * Messages from the crawlcontroller.
     *
     * They appear on console.
     */
    private final static Logger LOGGER =
        Logger.getLogger(CrawlControllerImpl.class.getName());

    private transient ToePool toePool;

    private Frontier frontier;

    /**
     * Sheet manager for context (SURT) based settings.  Passed to the
     * constructor.
     */
    private SheetManager sheetManager;

    private CrawlerLoggerModule loggerModule;
    
    // Used to enable/disable single-threaded operation after OOM
    private volatile transient boolean singleThreadMode = false; 
    private ReentrantLock singleThreadLock = null;

    // emergency reserve of memory to allow some progress/reporting after OOM
    private transient LinkedList<char[]> reserveMemory;
    private static final int RESERVE_BLOCKS = 1;
    private static final int RESERVE_BLOCK_SIZE = 6*2^20; // 6MB

    transient ThreadGroup alertThreadGroup;
    
    // crawl state: as requested or actual

    
    /**
     * Crawl exit status.
     */
    private transient CrawlStatus sExit;

    
    // FIXME: Make this an outer class.
    public static enum State {
        NASCENT, RUNNING, PAUSED, PAUSING, CHECKPOINTING, 
        STOPPING, FINISHED, STARTED, PREPARING 
    }

    transient private State state = State.NASCENT;

    /**
     * For discardable temp files (eg fetch buffers).
     */
    private Path scratchDir;

    /**
     * Directory that holds checkpoint.
     */
    private Path checkpointsDir;
    
    /**
     * Checkpointer.
     * Knows if checkpoint in progress and what name of checkpoint is.  Also runs
     * checkpoints.
     */
    private Checkpointer checkpointer;


    // crawl limits
    private long maxBytes;
    private long maxDocument;
    private long maxTime;

    /**
     * A manifest of all files used/created during this crawl. Written to file
     * at the end of the crawl (the absolutely last thing done).
     */
    transient private StringBuffer manifest;




//    protected StatisticsTracking statistics = null;


    // Since there is a high probability that there will only ever by one
    // CrawlURIDispositionListner we will use this while there is only one:
    private transient CrawlURIDispositionListener
        registeredCrawlURIDispositionListener;

    // And then switch to the array once there is more then one.
     protected transient ArrayList<CrawlURIDispositionListener> 
     registeredCrawlURIDispositionListeners;


    

    public CrawlControllerImpl() {
        super(CrawlController.class);
    }
    
    public void initialTasks(StateProvider provider) {        
        this.sheetManager = provider.get(this, SHEET_MANAGER);
        this.loggerModule = provider.get(this, LOGGER_MODULE);
        this.scratchDir = provider.get(this, SCRATCH_DIR);
        this.checkpointsDir = provider.get(this, CHECKPOINTS_DIR);
        this.checkpointer = new Checkpointer(this, this.checkpointsDir.toFile());
        this.frontier = provider.get(this, FRONTIER);

        this.singleThreadLock = new ReentrantLock();
        sExit = null;
        this.manifest = new StringBuffer();

        // force creation of DNS Cache now -- avoids CacheCleaner in toe-threads group
        // also cap size at 1 (we never wanta cached value; 0 is non-operative)
        Lookup.getDefaultCache(DClass.IN).setMaxEntries(1);
        //dns.getRecords("localhost", Type.A, DClass.IN);
        
        // setupToePool();
        setThresholds();
        
        reserveMemory = new LinkedList<char[]>();
        for(int i = 1; i < RESERVE_BLOCKS; i++) {
            reserveMemory.add(new char[RESERVE_BLOCK_SIZE]);
        }

        // Can't call setupToePool yet due to circular dependency with Frontier
        // So, remember current ThreadGroup so we can create the ToePool in 
        // it later.
        alertThreadGroup = Thread.currentThread().getThreadGroup();
    }

    /**
     * Register for CrawlURIDisposition events.
     *
     * @param cl a class implementing the CrawlURIDispostionListener interface
     *
     * @see CrawlURIDispositionListener
     */
    public void addCrawlURIDispositionListener(CrawlURIDispositionListener cl) {
        registeredCrawlURIDispositionListener = null;
        if (registeredCrawlURIDispositionListeners == null) {
            // First listener;
            registeredCrawlURIDispositionListener = cl;
            //Only used for the first one while it is the only one.
            registeredCrawlURIDispositionListeners 
             = new ArrayList<CrawlURIDispositionListener>(1);
            //We expect it to be very small.
        }
        registeredCrawlURIDispositionListeners.add(cl);
    }

    /**
     * Allows an external class to raise a CrawlURIDispostion
     * crawledURISuccessful event that will be broadcast to all listeners that
     * have registered with the CrawlController.
     *
     * @param curi - The CrawlURI that will be sent with the event notification.
     *
     * @see CrawlURIDispositionListener#crawledURISuccessful(CrawlURI)
     */
    public void fireCrawledURISuccessfulEvent(CrawlURI curi) {
        if (registeredCrawlURIDispositionListener != null) {
            // Then we'll just use that.
            registeredCrawlURIDispositionListener.crawledURISuccessful(curi);
        } else {
            // Go through the list.
            if (registeredCrawlURIDispositionListeners != null
                && registeredCrawlURIDispositionListeners.size() > 0) {
                Iterator it = registeredCrawlURIDispositionListeners.iterator();
                while (it.hasNext()) {
                    (
                        (CrawlURIDispositionListener) it
                            .next())
                            .crawledURISuccessful(
                        curi);
                }
            }
        }
    }

    /**
     * Allows an external class to raise a CrawlURIDispostion
     * crawledURINeedRetry event that will be broadcast to all listeners that
     * have registered with the CrawlController.
     *
     * @param curi - The CrawlURI that will be sent with the event notification.
     *
     * @see CrawlURIDispositionListener#crawledURINeedRetry(CrawlURI)
     */
    public void fireCrawledURINeedRetryEvent(CrawlURI curi) {
        if (registeredCrawlURIDispositionListener != null) {
            // Then we'll just use that.
            registeredCrawlURIDispositionListener.crawledURINeedRetry(curi);
            return;
        }
        
        // Go through the list.
        if (registeredCrawlURIDispositionListeners != null
                && registeredCrawlURIDispositionListeners.size() > 0) {
            for (Iterator i = registeredCrawlURIDispositionListeners.iterator();
                    i.hasNext();) {
                ((CrawlURIDispositionListener)i.next()).crawledURINeedRetry(curi);
            }
        }
    }

    /**
     * Allows an external class to raise a CrawlURIDispostion
     * crawledURIDisregard event that will be broadcast to all listeners that
     * have registered with the CrawlController.
     * 
     * @param curi -
     *            The CrawlURI that will be sent with the event notification.
     * 
     * @see CrawlURIDispositionListener#crawledURIDisregard(CrawlURI)
     */
    public void fireCrawledURIDisregardEvent(CrawlURI curi) {
        if (registeredCrawlURIDispositionListener != null) {
            // Then we'll just use that.
            registeredCrawlURIDispositionListener.crawledURIDisregard(curi);
        } else {
            // Go through the list.
            if (registeredCrawlURIDispositionListeners != null
                && registeredCrawlURIDispositionListeners.size() > 0) {
                Iterator it = registeredCrawlURIDispositionListeners.iterator();
                while (it.hasNext()) {
                    (
                        (CrawlURIDispositionListener) it
                            .next())
                            .crawledURIDisregard(
                        curi);
                }
            }
        }
    }

    /**
     * Allows an external class to raise a CrawlURIDispostion crawledURIFailure event
     * that will be broadcast to all listeners that have registered with the CrawlController.
     *
     * @param curi - The CrawlURI that will be sent with the event notification.
     *
     * @see CrawlURIDispositionListener#crawledURIFailure(CrawlURI)
     */
    public void fireCrawledURIFailureEvent(CrawlURI curi) {
        if (registeredCrawlURIDispositionListener != null) {
            // Then we'll just use that.
            registeredCrawlURIDispositionListener.crawledURIFailure(curi);
        } else {
            // Go through the list.
            if (registeredCrawlURIDispositionListeners != null
                && registeredCrawlURIDispositionListeners.size() > 0) {
                Iterator it = registeredCrawlURIDispositionListeners.iterator();
                while (it.hasNext()) {
                    ((CrawlURIDispositionListener)it.next())
                        .crawledURIFailure(curi);
                }
            }
        }
    }


    
    protected FatalConfigurationException
            convertToFatalConfigurationException(Exception e) {
        FatalConfigurationException fce =
            new FatalConfigurationException("Converted exception: " +
               e.getMessage());
        fce.setStackTrace(e.getStackTrace());
        return fce;
    }

    

    /**
     * Sets the values for max bytes, docs and time based on crawl order. 
     */
    private void setThresholds() {
        maxBytes = sheetManager.get(this, MAX_BYTES_DOWNLOAD);
        maxDocument = sheetManager.get(this, MAX_DOCUMENT_DOWNLOAD);
        maxTime = sheetManager.get(this, MAX_TIME_SEC);
    }
   

    /**
     * @return Object this controller is using to track crawl statistics
     */
    public StatisticsTracking getStatistics() {
        List<StatisticsTracking> statTrackers = sheetManager.get(this, LOGGERS);
        if (statTrackers == null) {
            return null;
        }
        if (statTrackers.isEmpty()) {
            return null;
        }
        return statTrackers.get(0);
    }
    
    /**
     * Send crawl change event to all listeners.
     * @param newState State change we're to tell listeners' about.
     * @param message Message on state change.
     * @see #sendCheckpointEvent(File) for special case event sending
     * telling listeners to checkpoint.
     */
    protected void sendCrawlStateChangeEvent(State newState, 
            CrawlStatus status) {
        this.state = newState;
        List<CrawlStatusListener> registeredCrawlStatusListeners = 
            ListModuleListener.get(sheetManager, CrawlStatusListener.class);
        for (CrawlStatusListener l: registeredCrawlStatusListeners) {
            switch (newState) {
                case PAUSED:
                    l.crawlPaused(status.getDescription());
                    break;
                case RUNNING:
                    l.crawlResuming(status.getDescription());
                    break;
                case PAUSING:
                    l.crawlPausing(status.getDescription());
                    break;
                case STARTED:
                    l.crawlStarted(status.getDescription());
                    break;
                case STOPPING:
                    l.crawlEnding(status.getDescription());
                    break;
                case FINISHED:
                    l.crawlEnded(status.getDescription());
                    break;
                case PREPARING:
                    l.crawlResuming(status.getDescription());
                    break;
                default:
                    throw new RuntimeException("Unknown state: " + newState);
            }
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine("Sent " + newState + " to " + l);
            }
        }
        sendNotification(newState.toString(), "");
        // FIXME: Should we ONLY use JMX notifications, and eliminate
        // the old event framework?
        LOGGER.fine("Sent " + newState);
    }
    


    /** 
     * Operator requested crawl begin
     */
    public void requestCrawlStart() {
        sendCrawlStateChangeEvent(State.PREPARING, CrawlStatus.PREPARING);
        frontier.loadSeeds();
        
        setupToePool();

        sendCrawlStateChangeEvent(State.STARTED, CrawlStatus.PENDING);
        CrawlStatus jobState = CrawlStatus.RUNNING;
        state = State.RUNNING;
        sendCrawlStateChangeEvent(this.state, jobState);

        // A proper exit will change this value.
        this.sExit = CrawlStatus.FINISHED_ABNORMAL;
        
        Thread statLogger = new Thread(getStatistics());
        statLogger.setName("StatLogger");
        statLogger.start();
        
        if (get(this,PAUSE_AT_START)) {
            requestCrawlPause();
        } else {
            getFrontier().start();
        }
    }

    /**
     * Called when the last toethread exits.
     */
    protected void completeStop() {
        LOGGER.fine("Entered complete stop.");

        sheetManager.closeModules();
        loggerModule.closeLogFiles();
        
        // Release reference to logger file handler instances.
        this.manifest = null;

//        this.frontier = null;
//        this.disk = null;
//        this.scratchDisk = null;
//        this.order = null;
//        this.scope = null;
        if (this.sheetManager !=  null) {
            this.sheetManager.cleanup();
        }
        this.reserveMemory = null;
        if (this.checkpointer != null) {
            this.checkpointer.cleanup();
            this.checkpointer = null;
        }
        if (this.toePool != null) {
            this.toePool.cleanup();
            // I played with launching a thread here to do cleanup of the
            // ToePool ThreadGroup (making sure the cleanup thread was not
            // in the ToePool ThreadGroup).  Did this because ToePools seemed
            // to be sticking around holding references to CrawlController at
            // least.  Need to spend more time looking to see that this is
            // still the case even after adding the above toePool#cleanup call.
        }
        this.toePool = null;


        LOGGER.fine("Finished crawl.");


        // Ok, now we are ready to exit.
        sendCrawlStateChangeEvent(State.FINISHED, this.sExit);
        this.sheetManager = null;
    }
    
    synchronized void completePause() {
        // Send a notifyAll. At least checkpointing thread may be waiting on a
        // complete pause.
        notifyAll();
        sendCrawlStateChangeEvent(State.PAUSED, CrawlStatus.PAUSED);
    }

    private boolean shouldContinueCrawling() {
        Frontier frontier = getFrontier();
        if (frontier.isEmpty()) {
            this.sExit = CrawlStatus.FINISHED;
            return false;
        }

        if (maxBytes > 0 && frontier.totalBytesWritten() >= maxBytes) {
            // Hit the max byte download limit!
            sExit = CrawlStatus.FINISHED_DATA_LIMIT;
            return false;
        } else if (maxDocument > 0
                && frontier.succeededFetchCount() >= maxDocument) {
            // Hit the max document download limit!
            this.sExit = CrawlStatus.FINISHED_DOCUMENT_LIMIT;
            return false;
        } else if (maxTime > 0 && getStatistics().crawlDuration() >= maxTime * 1000) {
            // Hit the max byte download limit!
            this.sExit = CrawlStatus.FINISHED_TIME_LIMIT;
            return false;
        }
        return state == State.RUNNING;
    }

    /**
     * Request a checkpoint.
     * Sets a checkpointing thread running.
     * @throws IllegalStateException Thrown if crawl is not in paused state
     * (Crawl must be first paused before checkpointing).
     */
    public synchronized void requestCrawlCheckpoint()
    throws IllegalStateException {
        if (this.checkpointer == null) {
            return;
        }
        if (this.checkpointer.isCheckpointing()) {
            throw new IllegalStateException("Checkpoint already running.");
        }
        this.checkpointer.checkpoint();
    }   
    
    /**
     * @return True if checkpointing.
     */
    public boolean isCheckpointing() {
        return this.state == State.CHECKPOINTING;
    }


    /**
     * Operator requested for crawl to stop.
     */
    public synchronized void requestCrawlStop() {
        requestCrawlStop(CrawlStatus.ABORTED);
    }
    
    /**
     * Operator requested for crawl to stop.
     * @param message 
     */
    public synchronized void requestCrawlStop(CrawlStatus message) {
        if (state == State.STOPPING || state == State.FINISHED) {
            return;
        }
        if (message == null) {
            throw new IllegalArgumentException("Message cannot be null.");
        }
        this.sExit = message;
        beginCrawlStop();
    }

    /**
     * Start the process of stopping the crawl. 
     */
    public void beginCrawlStop() {
        LOGGER.fine("Started.");
        sendCrawlStateChangeEvent(State.STOPPING, this.sExit);
        Frontier frontier = getFrontier();
        if (frontier != null) {
            frontier.terminate();
        }
        LOGGER.fine("Finished."); 
    }
    
    /**
     * Stop the crawl temporarly.
     */
    public synchronized void requestCrawlPause() {
        if (state == State.PAUSING || state == State.PAUSED) {
            // Already about to pause
            return;
        }
        sExit = CrawlStatus.WAITING_FOR_PAUSE;
        getFrontier().pause();
        sendCrawlStateChangeEvent(State.PAUSING, this.sExit);
        if (toePool.getActiveToeCount() == 0) {
            // if all threads already held, complete pause now
            // (no chance to trigger off later held thread)
            completePause();
        }
    }

    /**
     * Tell if the controller is paused
     * @return true if paused
     */
    public boolean isPaused() {
        return state == State.PAUSED;
    }
    
    public boolean isPausing() {
        return state == State.PAUSING;
    }
    
    public boolean isRunning() {
        return state == State.RUNNING;
    }

    /**
     * Resume crawl from paused state
     */
    public void requestCrawlResume() {
        if (this.toePool == null) {
            this.setupToePool();
        }

        if (state != State.PAUSING && state != State.PAUSED && state != State.CHECKPOINTING) {
            // Can't resume if not been told to pause or if we're in middle of
            // a checkpoint.
            return;
        }
        
        // ToePool might be null if we are resuming after a checkpoint recovery
        multiThreadMode();
        Frontier f = getFrontier();
        f.unpause();
        sendCrawlStateChangeEvent(State.RUNNING, CrawlStatus.RUNNING);
    }

    /**
     * @return Active toe thread count.
     */
    public int getActiveToeCount() {
        if (toePool == null) {
            return 0;
        }
        return toePool.getActiveToeCount();
    }

    public void setupToePool() {
        toePool = new ToePool(this);
        // TODO: make # of toes self-optimizing
        int max = sheetManager.get(this, MAX_TOE_THREADS);
        toePool.setSize(max);
        toePool.waitForAll();
    }


    /**
     * @return The server cache instance.
     */
    ServerCache getServerCache() {
        return get(this, SERVER_CACHE);
    }


    /**
     * @return The frontier.
     */
    public Frontier getFrontier() {
        return frontier;
    }


    /**
     * @return The number of ToeThreads
     *
     * @see ToePool#getToeCount()
     */
    public int getToeCount() {
        return this.toePool == null? 0: this.toePool.getToeCount();
    }

    /**
     * @return The ToePool
     */
    public ToePool getToePool() {
        return toePool;
    }
    
	/**
	 * @return toepool one-line report
	 */
	public String oneLineReportThreads() {
		// TODO Auto-generated method stub
		return toePool.singleLineReport();
	}

    /**
     * While many settings will update automatically when the SettingsHandler is
     * modified, some settings need to be explicitly changed to reflect new
     * settings. This includes, number of toe threads and seeds.
     */
    public void keyChanged(KeyChangeEvent event) {
        if (event.getKey() == MAX_TOE_THREADS) {
            int max = sheetManager.get(this, MAX_TOE_THREADS);
            toePool.setSize(max);
        }
        
        setThresholds();
    }

    /**
     * @return The settings handler.
     */
    public SheetManager getSheetManager() {
        return sheetManager;
    }



    /**
     * Kills a thread. For details see
     * {@link org.archive.crawler.framework.ToePool#killThread(int, boolean)
     * ToePool.killThread(int, boolean)}.
     * @param threadNumber Thread to kill.
     * @param replace Should thread be replaced.
     * @see org.archive.crawler.framework.ToePool#killThread(int, boolean)
     */
    public void killThread(int threadNumber, boolean replace){
        toePool.killThread(threadNumber, replace);
    }


    /**
     * Evaluate if the crawl should stop because it is finished.
     */
    public void checkFinish() {
        if(atFinish()) {
            beginCrawlStop();
        }
    }

    /**
     * Evaluate if the crawl should stop because it is finished,
     * without actually stopping the crawl.
     * 
     * @return true if crawl is at a finish-possible state
     */
    public boolean atFinish() {
        return state == State.RUNNING && !shouldContinueCrawling();
    }
    
    
    private void writeObject(ObjectOutputStream stream) throws IOException {
        // In an ideal world, this wouldn't be necessary:  Modules would
        // declare formal dependencies on the objects they require, and would
        // not go through the CrawlController to find each other.  However,
        // since many modules still do use the CrawlController to find each
        // other, we must ensure that critical things are deserialized in
        // order.  In particular, CrawlOrder and BdbModule must be deserialized
        // first since Modules that are constructed during defaultReadObject
        // may require those things.
//        stream.writeObject(order);
        stream.defaultWriteObject();
    }
    
    
    private void readObject(ObjectInputStream stream)
    throws IOException, ClassNotFoundException {
//        this.order = (CrawlOrder)stream.readObject();
        this.state = State.PAUSED;

        this.manifest = new StringBuffer();

        stream.defaultReadObject();

        // Ensure no holdover singleThreadMode
        singleThreadMode = false;
        alertThreadGroup = Thread.currentThread().getThreadGroup();
    }


    /**
     * Go to single thread mode, where only one ToeThread may
     * proceed at a time. Also acquires the single lock, so 
     * no further threads will proceed past an 
     * acquireContinuePermission. Caller mush be sure to release
     * lock to allow other threads to proceed one at a time. 
     */
    public void singleThreadMode() {
        this.singleThreadLock.lock();
        singleThreadMode = true; 
    }

    /**
     * Go to back to regular multi thread mode, where all
     * ToeThreads may proceed at once
     */
    public void multiThreadMode() {
        this.singleThreadLock.lock();
        singleThreadMode = false; 
        while(this.singleThreadLock.isHeldByCurrentThread()) {
            this.singleThreadLock.unlock();
        }
    }
    
    /**
     * Proceed only if allowed, giving CrawlController a chance
     * to enforce single-thread mode.
     */
    public void acquireContinuePermission() {
        if (singleThreadMode) {
            this.singleThreadLock.lock();
            if(!singleThreadMode) {
                // If changed while waiting, ignore
                while(this.singleThreadLock.isHeldByCurrentThread()) {
                    this.singleThreadLock.unlock();
                }
            }
        } // else, permission is automatic
    }

    /**
     * Relinquish continue permission at end of processing (allowing
     * another thread to proceed if in single-thread mode). 
     */
    public void releaseContinuePermission() {
        if (singleThreadMode) {
            while(this.singleThreadLock.isHeldByCurrentThread()) {
                this.singleThreadLock.unlock();
            }
        } // else do nothing; 
    }
    
    public void freeReserveMemory() {
        if(!reserveMemory.isEmpty()) {
            reserveMemory.removeLast();
            System.gc();
        }
    }
    
    /**
     * Note that a ToeThread ended 
     */
    public synchronized void toeEnded() {

    }

    /**
     * Add order file contents to manifest.
     * Write configuration files and any files managed by CrawlController to
     * it - files managed by other classes, excluding the settings framework,
     * are responsible for adding their files to the manifest themselves.
     * by calling addToManifest.
     * Call before writing out reports.
     */
    public void addOrderToManifest() {
        // FIXME: Figure out how to let modules advertise their files
        /*
        for (Iterator it = getSettingsHandler().getListOfAllFiles().iterator();
                it.hasNext();) {
            addToManifest((String)it.next(),
                CrawlController.MANIFEST_CONFIG_FILE, true);
        }
        */
    }
    
    // 
    // Reporter
    //
    public final static String PROCESSORS_REPORT = "processors";
    public final static String MANIFEST_REPORT = "manifest";
    protected final static String[] REPORTS = {PROCESSORS_REPORT, MANIFEST_REPORT};
    
    /* (non-Javadoc)
     * @see org.archive.util.Reporter#getReports()
     */
    public String[] getReports() {
        return REPORTS;
    }

    /* (non-Javadoc)
     * @see org.archive.util.Reporter#reportTo(java.io.Writer)
     */
    public void reportTo(PrintWriter writer) {
        reportTo(null,writer);
    }

    public String singleLineReport() {
        return ArchiveUtils.singleLineReport(this);
    }

    public void reportTo(String name, PrintWriter writer) {
        if(PROCESSORS_REPORT.equals(name)) {
            reportProcessorsTo(writer);
            return;
        } else if (MANIFEST_REPORT.equals(name)) {
            reportManifestTo(writer);
            return;
        } else if (name!=null) {
            writer.println("requested report unknown: "+name);
        }
        singleLineReportTo(writer);
    }

    /**
     * @param writer Where to write report to.
     */
    protected void reportManifestTo(PrintWriter writer) {
        writer.print(manifest.toString());
    }

    /**
     * Compiles and returns a human readable report on the active processors.
     * @param writer Where to write to.
     * @see org.archive.crawler.framework.Processor#report()
     */
    protected void reportProcessorsTo(PrintWriter writer) {
        writer.print(
            "Processors report - "
                + ArchiveUtils.get12DigitDate()
                + "\n");
        writer.print("  Job being crawled:    " + sheetManager.getCrawlName()
                + "\n");

        writer.print("  Number of Processors: " + get(this, PROCESSORS).size() + "\n");
        writer.print("  NOTE: Some processors may not return a report!\n\n");

        for (Processor p: get(this, PROCESSORS).values()) {
            writer.print(p.report());
        }
    }

    public void singleLineReportTo(PrintWriter writer) {
        // TODO: imrpvoe to be summary of crawl state
        writer.write("[Crawl Controller]\n");
    }

    public String singleLineLegend() {
        // TODO improve
        return "nothingYet";
    }

    /**
     * Called whenever progress statistics logging event.
     * @param e Progress statistics event.
     */
    public void progressStatisticsEvent(final EventObject e) {
        // Default is to do nothing.  Subclass if you want to catch this event.
        // Later, if demand, add publisher/listener support.  Currently hacked
        // in so the subclass in CrawlJob added to support JMX can send
        // notifications of progressStatistics change.
    }
    
    /**
     * Log to the progress statistics log.
     * @param msg Message to write the progress statistics log.
     */
    public void logProgressStatistics(final String msg) {
        loggerModule.getProgressStats().info(msg);
    }

    /**
     * @return CrawlController state.
     */
    public Object getState() {
        return this.state;
    }


    public <T> T get(Object module, Key<T> key) {
        SingleSheet def = sheetManager.getGlobalSheet();
        return def.get(module, key);
    }


    File getScratchDir() {
        return scratchDir.toFile();
    }
    
    
    public File getCheckpointsDir() {
        return checkpointsDir.toFile();
    }
    
    @Emitter(desc="Emitted when the crawl status changes (eg, when a crawl "
        + " goes from CRAWLING to ENDED)",         
        types={ "PAUSED", "RUNNING", "PAUSING", "STARTED", "STOPPING", 
            "FINISHED", "PREPARED" })
    void emit(Notification n) {
        sendNotification(n);
    }


    public String getCrawlStatusString() {
        return this.state.toString();
    }

    public String getToeThreadReport() {
        StringWriter sw = new StringWriter();
        toePool.reportTo(new PrintWriter(sw));
        return sw.toString();
    }

    public String getToeThreadReportShort() {
        return (toePool == null) ? "" : toePool.singleLineReport();
    }

    public String getFrontierReport() {
        StringWriter sw = new StringWriter();
        try {
            getFrontier().reportTo(new PrintWriter(sw));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return sw.toString();
    }

    public String getFrontierReportShort() {
        return getFrontier().singleLineReport();
    }

    public String getProcessorsReport() {
        StringWriter sw = new StringWriter();
        this.reportTo(CrawlControllerImpl.PROCESSORS_REPORT,new PrintWriter(sw));
        return sw.toString();
    }

    /**
     * Receive notification from the frontier, in the frontier's own 
     * manager thread, that the frontier has reached a new state. 
     * 
     * @param reachedState the state the frontier has reached
     */
    public void noteFrontierState(Frontier.State reachedState) {
        switch (reachedState) {
        case RUN: 
            LOGGER.info("Crawl resumed.");
            sendCrawlStateChangeEvent(State.RUNNING, CrawlStatus.RUNNING);
        case PAUSE:
            if (state == State.PAUSING) {
                completePause();
                break;
            }
            if(atFinish()) { // really, "just reached finish"
                if (get(this,PAUSE_AT_FINISH)) {
                    requestCrawlPause();
                } else {
                    beginCrawlStop();
                }
                break;
            }
            if(state == State.STOPPING || state == State.FINISHED) {
                frontier.requestState(Frontier.State.FINISH);
            }
            break;
        case FINISH:
            completeStop();
            break;
        default:
            // do nothing
        }
    }

}
