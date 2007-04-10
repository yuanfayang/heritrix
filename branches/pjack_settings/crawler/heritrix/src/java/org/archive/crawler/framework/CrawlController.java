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

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.EventObject;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.AttributeNotFoundException;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanException;
import javax.management.Notification;
import javax.management.ReflectionException;

import org.apache.commons.httpclient.URIException;
import org.archive.crawler.datamodel.CrawlOrder;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.openmbeans.annotations.Bean;
import org.archive.openmbeans.annotations.Emitter;
import org.archive.openmbeans.annotations.Operation;
import org.archive.processors.util.ServerCache;
import org.archive.crawler.event.CrawlStatusListener;
import org.archive.crawler.event.CrawlURIDispositionListener;
import org.archive.crawler.framework.exceptions.FatalConfigurationException;
import org.archive.crawler.url.CanonicalizationRule;
import org.archive.net.UURI;
import org.archive.net.UURIFactory;
import org.archive.state.DefaultDirectoryModule;
import org.archive.state.DirectoryModule;
import org.archive.processors.Processor;
import org.archive.processors.credential.CredentialStore;
import org.archive.settings.CheckpointRecovery;
import org.archive.settings.ListModuleListener;
import org.archive.settings.Sheet;
import org.archive.settings.SheetManager;
import org.archive.settings.file.BdbModule;
import org.archive.state.Expert;
import org.archive.state.Global;
import org.archive.state.Immutable;
import org.archive.state.Initializable;
import org.archive.state.Key;
import org.archive.state.KeyManager;
import org.archive.state.StateProvider;
import org.archive.util.ArchiveUtils;
import org.archive.util.Reporter;
import org.xbill.DNS.DClass;
import org.xbill.DNS.Type;
import org.xbill.DNS.dns;

import java.util.concurrent.locks.ReentrantLock;

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
public class CrawlController extends Bean 
implements Serializable, Reporter, StateProvider, Initializable, 
DirectoryModule {
 
    // be robust against trivial implementation changes
    private static final long serialVersionUID =
        ArchiveUtils.classnameBasedUID(CrawlController.class,1);

    
    private static String ACCEPTABLE_USER_AGENT =
        "\\S+.*\\(.*\\+http(s)?://\\S+\\.\\S+.*\\).*";

    /**
     * Regex for acceptable from address.
     */
    private static String ACCEPTABLE_FROM = "\\S+@\\S+\\.\\S+";

    
    @Immutable
    final public static Key<ServerCache> SERVER_CACHE = 
        Key.make(ServerCache.class, null);


    /**
     * The frontier to use for the crawl.
     */
    @Immutable
    final public static Key<Frontier> FRONTIER = Key.make(Frontier.class, null);


    /**
     * Ordered list of url canonicalization rules.  Rules are applied in the 
     * order listed from top to bottom.
     */
    @Global
    final public static Key<List<CanonicalizationRule>> URI_CANONICALIZATION_RULES = 
        Key.makeList(CanonicalizationRule.class);


    /**
     * Statistics tracking modules.  Any number of specialized statistics 
     * trackers that monitor a crawl and write logs, reports and/or provide 
     * information to the user interface.
     */
    @Global
    final public static Key<List<StatisticsTracking>> LOGGERS = 
        Key.makeList(StatisticsTracking.class);

    
    @Expert @Immutable
    final public static Key<CredentialStore> CREDENTIAL_STORE = 
        Key.make(new CredentialStore());


    final public static Key<Map<String,Processor>> PROCESSORS =
        Key.makeMap(Processor.class);

    
    @Immutable
    final public static Key<CrawlOrder> ORDER = Key.make(new CrawlOrder());
    
    
    @Immutable
    final public static Key<SheetManager> SHEET_MANAGER = 
        Key.make(SheetManager.class, null);

    
    @Immutable
    final public static Key<StatisticsTracker> STATISTICS_TRACKER = 
        Key.make(StatisticsTracker.class, null);
    
    final public static Key<CrawlerLoggerModule> LOGGER_MODULE =
        Key.make(CrawlerLoggerModule.class, null);
    
    static {
        KeyManager.addKeys(CrawlController.class);
    }
    
    /**
     * Messages from the crawlcontroller.
     *
     * They appear on console.
     */
    private final static Logger LOGGER =
        Logger.getLogger(CrawlController.class.getName());


    // key subcomponents which define and implement a crawl in progress
    //private transient CrawlOrder order;
//    private transient CrawlScope scope;
    
//    private transient Frontier frontier;

    private transient ToePool toePool;
    
    private transient ServerCache serverCache;
    

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

    // crawl state: as requested or actual
    
    /**
     * Crawl exit status.
     */
    private transient CrawlStatus sExit;

    
    private static enum State {
        NASCENT, RUNNING, PAUSED, PAUSING, CHECKPOINTING, 
        STOPPING, FINISHED, STARTED, PREPARING 
    }

    transient private State state = State.NASCENT;

    // disk paths
    transient private File disk;        // overall disk path
    
    /**
     * For temp files representing state of crawler (eg queues)
     */
    transient private File stateDisk;
    
    /**
     * For discardable temp files (eg fetch buffers).
     */
    transient private File scratchDisk;

    /**
     * Directory that holds checkpoint.
     */
    transient private File checkpointsDisk;
    
    /**
     * Checkpointer.
     * Knows if checkpoint in progress and what name of checkpoint is.  Also runs
     * checkpoints.
     */
    private Checkpointer checkpointer;
    
    private CrawlOrder order;


    // crawl limits
    private long maxBytes;
    private long maxDocument;
    private long maxTime;

    /**
     * A manifest of all files used/created during this crawl. Written to file
     * at the end of the crawl (the absolutely last thing done).
     */
    transient private StringBuffer manifest;




    protected StatisticsTracking statistics = null;


    // Since there is a high probability that there will only ever by one
    // CrawlURIDispositionListner we will use this while there is only one:
    private transient CrawlURIDispositionListener
        registeredCrawlURIDispositionListener;

    // And then switch to the array once there is more then one.
     protected transient ArrayList<CrawlURIDispositionListener> 
     registeredCrawlURIDispositionListeners;


    

    public CrawlController() {
    }
    
    public void initialTasks(StateProvider provider) {        
        this.sheetManager = provider.get(this, SHEET_MANAGER);
        this.order = provider.get(this, ORDER);
        this.statistics = provider.get(this, STATISTICS_TRACKER);
        this.loggerModule = provider.get(this, LOGGER_MODULE);
        sendCrawlStateChangeEvent(State.PREPARING, CrawlStatus.PREPARING);

        this.singleThreadLock = new ReentrantLock();
//        this.order = new CrawlOrder();
//        this.order.setController(this);
        sExit = null;
        this.manifest = new StringBuffer();
        String onFailMessage = "";
        try {
            onFailMessage = "You must set the User-Agent and From HTTP" +
            " header values to acceptable strings. \n" +
            " User-Agent: [software-name](+[info-url])[misc]\n" +
            " From: [email-address]\n";
            checkUserAgentAndFrom();

            onFailMessage = "Unable to setup disk";
            if (disk == null) {
                setupDisk();
            }
            
            // Figure if we're to do a checkpoint restore. If so, get the
            // checkpointRecover instance and then put into place the old bdb
            // log files. If any of the log files already exist in target state
            // diretory, WE DO NOT OVERWRITE (Makes for faster recovery).
            // CrawlController checkpoint recovery code manages restoration of
            // the old StatisticsTracker, any BigMaps used by the Crawler and
            // the moving of bdb log files into place only. Other objects
            // interested in recovery need to ask if
            // CrawlController#isCheckpointRecover is set to figure if in
            // recovery and then take appropriate recovery action
            // (These objects can call CrawlController#getCheckpointRecover
            // to get the directory that might hold files/objects dropped
            // checkpointing).  Such objects will need to use a technique other
            // than object serialization restoring settings because they'll
            // have already been constructed when comes time for object to ask
            // if its to recover itself. See ARCWriterProcessor for example.
//            onFailMessage = "Unable to test/run checkpoint recover";
//            this.checkpointRecover = getCheckpointRecover();
//            if (this.checkpointRecover == null) {
//                this.checkpointer =
//                    new Checkpointer(this, this.checkpointsDisk);
//            } else {
//                setupCheckpointRecover();
//            }
//            
//            onFailMessage = "Unable to setup bdb environment.";
//            
//            onFailMessage = "Unable to setup statistics";
//            setupStatTracking();
//            
//            onFailMessage = "Unable to setup crawl modules";
//            setupCrawlModules();
        } catch (Exception e) {
            String tmp = "On crawl: " + sheetManager.getCrawlName() + " " +
                onFailMessage;
            LOGGER.log(Level.SEVERE, tmp, e);
            throw new IllegalStateException(tmp, e);
        }

        // force creation of DNS Cache now -- avoids CacheCleaner in toe-threads group
        dns.getRecords("localhost", Type.A, DClass.IN);
        
        // setupToePool();
        setThresholds();
        
        reserveMemory = new LinkedList<char[]>();
        for(int i = 1; i < RESERVE_BLOCKS; i++) {
            reserveMemory.add(new char[RESERVE_BLOCK_SIZE]);
        }
        
//        processors = get(this, PROCESSORS);
    }

    
    public <T> T getOrderSetting(Key<T> key) {
        Sheet def = sheetManager.getDefault();
        return def.get(order, key);
    }
    
    
    /**
     * Does setup of checkpoint recover.
     * Copies bdb log files into state dir.
     * @throws IOException
     */
//    protected void setupCheckpointRecover()
//    throws IOException {
//        long started = System.currentTimeMillis();;
//        if (LOGGER.isLoggable(Level.FINE)) {
//            LOGGER.fine("Starting recovery setup -- copying into place " +
//                "bdbje log files -- for checkpoint named " +
//                this.checkpointRecover.getDisplayName());
//        }
//        // Mark context we're in a recovery.
//        this.checkpointer.recover(this);
//        this.loggerModule.getProgressStats().info("CHECKPOINT RECOVER " +
//            this.checkpointRecover.getDisplayName());
//        // Copy the bdb log files to the state dir so we don't damage
//        // old checkpoint.  If thousands of log files, can take
//        // tens of minutes (1000 logs takes ~5 minutes to java copy,
//        // dependent upon hardware).  If log file already exists over in the
//        // target state directory, we do not overwrite -- we assume the log
//        // file in the target same as one we'd copy from the checkpoint dir.
//        File bdbSubDir = CheckpointUtils.
//            getBdbSubDirectory(this.checkpointRecover.getDirectory());
//        FileUtils.copyFiles(bdbSubDir, CheckpointUtils.getJeLogsFilter(),
//            getStateDisk(), true,
//            false);
//        if (LOGGER.isLoggable(Level.INFO)) {
//            LOGGER.info("Finished recovery setup for checkpoint named " +
//                this.checkpointRecover.getDisplayName() + " in " +
//                (System.currentTimeMillis() - started) + "ms.");
//        }
//    }
    
    
//    public Environment getBdbEnvironment() {
//        return this.bdb.getEnvironment();
//    }
//    
//    public StoredClassCatalog getClassCatalog() {
//        return this.bdb.getClassCatalog();
//    }


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

//    private void setupCrawlModules() throws FatalConfigurationException,
//             AttributeNotFoundException, MBeanException, ReflectionException {
/*        if (scope == null) {
            scope = getOrderSetting(SCOPE);
            scope.initialize(this);
        }
        this.serverCache = get(this, SERVER_CACHE);
        
        if (this.frontier == null) {
            this.frontier = getOrderSetting(FRONTIER);
            try {
                frontier.initialize(this);
                frontier.pause(); // Pause until begun
                // Run recovery if recoverPath points to a file (If it points
                // to a directory, its a checkpoint recovery).
                // TODO: make recover path relative to job root dir.
                if (!isCheckpointRecover()) {
                    runFrontierRecover(get(this, CrawlOrder.RECOVER_PATH));
                }
            } catch (IOException e) {
                throw new FatalConfigurationException(
                    "unable to initialize frontier: " + e);
            }
        } */

//    }
    
    protected void runFrontierRecover(String recoverPath)
            throws AttributeNotFoundException, MBeanException,
            ReflectionException, FatalConfigurationException {
        if (recoverPath == null || recoverPath.length() <= 0) {
            return;
        }
        File f = new File(recoverPath);
        if (!f.exists()) {
            LOGGER.severe("Recover file does not exist " + recoverPath);
            return;
        }
        if (!f.isFile()) {
            // Its a directory if supposed to be doing a checkpoint recover.
            return;
        }
        boolean retainFailures = getOrderSetting(
                CrawlOrder.RECOVER_RETAIN_FAILURES);
        try {
            getFrontier().importRecoverLog(recoverPath, retainFailures);
        } catch (IOException e) {
            e.printStackTrace();
            throw (FatalConfigurationException) new FatalConfigurationException(
                "Recover.log " + recoverPath + " problem: " + e).initCause(e);
        }
    }

    private void setupDisk() {
        String diskPath = get(order, CrawlOrder.DISK_PATH);
        diskPath = sheetManager.toAbsolutePath(diskPath);
        this.disk = new File(diskPath);
        this.disk.mkdirs();
        this.checkpointsDisk = getSettingsDir(CrawlOrder.CHECKPOINTS_PATH);
        this.stateDisk = getSettingsDir(CrawlOrder.STATE_PATH);
        this.scratchDisk = getSettingsDir(CrawlOrder.SCRATCH_PATH);
    }
    
    
    /**
     * Return fullpath to the directory named by <code>key</code>
     * in settings.
     * If directory does not exist, it and all intermediary dirs
     * will be created.
     * @param key Key to use going to settings.
     * @return Full path to directory named by <code>key</code>.
     * @throws AttributeNotFoundException
     */
    private File getSettingsDir(Key<String> key) {
        String path = get(order, key);
        File f = new File(path);
        if (!f.isAbsolute()) {
            f = new File(disk.getPath(), path);
        }
        if (!f.exists()) {
            f.mkdirs();
        }
        return f;
    }

    /**
     * Setup the statistics tracker.
     * The statistics object must be created before modules can use it.
     * Do it here now so that when modules retrieve the object from the
     * controller during initialization (which some do), its in place.
     * @throws InvalidAttributeValueException
     * @throws FatalConfigurationException
     */
/*    private void setupStatTracking()
    throws InvalidAttributeValueException, FatalConfigurationException {
        List<StatisticsTracking> loggers = 
            getSheetManager().getDefault().check(this, LOGGERS);
        final String cstName = "crawl-statistics";
        if (loggers.isEmpty()) {
            if (!isCheckpointRecover() && this.statistics == null) {
                this.statistics = new StatisticsTracker(this);
            }
            loggers.add((StatisticsTracker)this.statistics);
        }
        
        if (isCheckpointRecover()) {
            restoreStatisticsTracker(loggers, cstName);
        }

        for (StatisticsTracking tracker: loggers) {
            tracker.initialize(this);
            if (this.statistics == null) {
                this.statistics = tracker;
            }
        }
        
    }
    */

    
    protected void restoreStatisticsTracker(List<StatisticsTracking> loggers,
        String replaceName)
    throws FatalConfigurationException {
        try {
            // Add the deserialized statstracker to the settings system.
            // FIXME: Remove old stattracker
            //loggers.removeElement(loggers.globalSettings(), replaceName);
            loggers.add(this.statistics);
         } catch (Exception e) {
             throw convertToFatalConfigurationException(e);
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
        maxBytes = getOrderSetting(CrawlOrder.MAX_BYTES_DOWNLOAD);
        maxDocument = getOrderSetting(CrawlOrder.MAX_DOCUMENT_DOWNLOAD);
        maxTime = getOrderSetting(CrawlOrder.MAX_TIME_SEC);
    }
   

    /**
     * @return Object this controller is using to track crawl statistics
     */
    public StatisticsTracking getStatistics() {
        return statistics;
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
     * Send the checkpoint event.
     * Has its own method apart from
     * {@link #sendCrawlStateChangeEvent(Object, String)} because checkpointing
     * throws an Exception (Didn't want to have to wrap all of the
     * sendCrawlStateChangeEvent in try/catches).
     * @param checkpointDir Where to write checkpoint state to.
     * @throws Exception
     */
/*    protected void sendCheckpointEvent(File checkpointDir) throws Exception {
        synchronized (this.registeredCrawlStatusListeners) {
            if (this.state != State.PAUSED) {
                throw new IllegalStateException("Crawler must be completly " +
                    "paused before checkpointing can start");
            }
            this.state = State.CHECKPOINTING;
            for (Iterator i = this.registeredCrawlStatusListeners.iterator();
                    i.hasNext();) {
                CrawlStatusListener l = (CrawlStatusListener)i.next();
                l.crawlCheckpoint(this, checkpointDir);
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.fine("Sent " + State.CHECKPOINTING + " to " + l);
                }
            }
            LOGGER.fine("Sent " + State.CHECKPOINTING);
        }
    }*/

    /** 
     * Operator requested crawl begin
     */
    @Operation(desc="Start the crawl.")
    public void requestCrawlStart() {
        setupToePool();
        runProcessorInitialTasks();

        sendCrawlStateChangeEvent(State.STARTED, CrawlStatus.PENDING);
        CrawlStatus jobState = CrawlStatus.RUNNING;
        state = State.RUNNING;
        sendCrawlStateChangeEvent(this.state, jobState);

        // A proper exit will change this value.
        this.sExit = CrawlStatus.FINISHED_ABNORMAL;
        
        Thread statLogger = new Thread(statistics);
        statLogger.setName("StatLogger");
        statLogger.start();
        
        getFrontier().start();
    }

    /**
     * Called when the last toethread exits.
     */
    protected void completeStop() {
        LOGGER.fine("Entered complete stop.");
        // Run processors' final tasks
        runProcessorFinalTasks();
        // Ok, now we are ready to exit.
        sendCrawlStateChangeEvent(State.FINISHED, this.sExit);
        
        List<Closeable> closeables = sheetManager.getCloseables();
        for (Closeable c: closeables) try {
            c.close();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Could not close " + c, e);
        }

        loggerModule.closeLogFiles();
        
        // Release reference to logger file handler instances.
        this.manifest = null;

        // Do cleanup.
        this.statistics = null;
//        this.frontier = null;
        this.disk = null;
        this.scratchDisk = null;
//        this.order = null;
//        this.scope = null;
        if (this.sheetManager !=  null) {
            this.sheetManager.cleanup();
        }
        this.sheetManager = null;
        this.reserveMemory = null;
        if (this.serverCache != null) {
            this.serverCache.cleanup();
            this.serverCache = null;
        }
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
        } else if (maxTime > 0 &&
                statistics.crawlDuration() >= maxTime * 1000) {
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
    @Operation(desc="Request a checkpoint.")
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
     * Get recover checkpoint.
     * Returns null if we're NOT in recover mode.
     * Looks at ATTR_RECOVER_PATH and if its a directory, assumes checkpoint
     * recover. If checkpoint mode, returns Checkpoint instance if
     * checkpoint was VALID (else null).
     * @return Checkpoint instance if we're in recover checkpoint
     * mode and the pointed-to checkpoint was valid.
     * @see #isCheckpointRecover()
     */
//    public synchronized Checkpoint getCheckpointRecover() {
//        if (this.checkpointRecover != null) {
//            return this.checkpointRecover;
//        }
//        return getCheckpointRecover(this);
//    }
    
    /*
    public static Checkpoint getCheckpointRecover(CrawlController c) {
        String path = c.get(c.order, CrawlOrder.RECOVER_PATH);
        if (path == null || path.length() <= 0) {
            return null;
        }
        File rp = new File(path);
        // Assume if path is to a directory, its a checkpoint recover.
        Checkpoint result = null;
        if (rp.exists() && rp.isDirectory()) {
            Checkpoint cp = new Checkpoint(rp);
            if (cp.isValid()) {
                // if valid, set as result.
                result = cp;
            }
        }
        return result;
    }
    
    public static boolean isCheckpointRecover(CrawlController c) {
        return getCheckpointRecover(c) != null;
    }
    */
    /**
     * @return True if we're in checkpoint recover mode. Call
     * {@link #getCheckpointRecover()} to get at Checkpoint instance
     * that has info on checkpoint directory being recovered from.
     */
//    public boolean isCheckpointRecover() {
//        return this.checkpointRecover != null;
//    }

    /**
     * Operator requested for crawl to stop.
     */
    @Operation(desc="Aborts the crawl.")
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
            frontier.unpause();
        }
        LOGGER.fine("Finished."); 
    }
    
    /**
     * Stop the crawl temporarly.
     */
    @Operation(desc="Stop the crawl temporarily.")
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
    @Operation(desc="Resume crawl from paused state.")
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
        LOGGER.info("Crawl resumed.");
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
        toePool.setSize(get(order, CrawlOrder.MAX_TOE_THREADS));
        toePool.waitForAll();
    }


    /**
     * @return The server cache instance.
     */
    public ServerCache getServerCache() {
        return get(this, SERVER_CACHE);
//        return serverCache;
    }

    /**
     * @param o
     */
//    public void setOrder(CrawlOrder o) {
//        order = o;
//    }


    /**
     * @return The frontier.
     */
    public Frontier getFrontier() {
        return get(this, FRONTIER);
//        return frontier;
    }

    /**
     * @return This crawl scope.
     */
//    public CrawlScope getScope() {
//        return get(this, SCOPE);
////        return scope;
//    }


    /**
     * Get the 'working' directory of the current crawl.
     * @return the 'working' directory of the current crawl.
     */
    public File getDisk() {
        return disk;
    }

    /**
     * @return Scratch disk location.
     */
    public File getScratchDisk() {
        return scratchDisk;
    }

    /**
     * @return State disk location.
     */
    public File getStateDisk() {
        return stateDisk;
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
    public void kickUpdate() {
        toePool.setSize(get(this, CrawlOrder.MAX_TOE_THREADS));
        
        // getScope().kickUpdate(this);
        getFrontier().kickUpdate();
        for (Processor p: get(this, PROCESSORS).values()) {
            p.kickUpdate(this);
        }
        
        // TODO: continue to generalize this, so that any major 
        // component can get a kick when it may need to refresh its data

        setThresholds();
    }

    /**
     * @return The settings handler.
     */
    public SheetManager getSheetManager() {
        return sheetManager;
    }

    /**
     * This method iterates through processor chains to run processors' initial
     * tasks.
     *
     */
    private void runProcessorInitialTasks(){
        for (Processor p: get(this, PROCESSORS).values()) {
            p.initialTasks(this);
        }
    }

    /**
     * This method iterates through processor chains to run processors' final
     * tasks.
     *
     */
    private void runProcessorFinalTasks(){
        for (Processor p: get(this, PROCESSORS).values()) {
            p.finalTasks(this);
        }
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
        stream.writeObject(order);
        
        stream.writeObject(disk);
        stream.writeObject(checkpointsDisk);
        stream.writeObject(scratchDisk);
        stream.writeObject(stateDisk);
        
        stream.defaultWriteObject();
    }
    
    
    private void readObject(ObjectInputStream stream)
    throws IOException, ClassNotFoundException {
        this.order = (CrawlOrder)stream.readObject();
        this.state = State.PAUSED;
        
        disk = (File)stream.readObject();
        checkpointsDisk = (File)stream.readObject();
        scratchDisk = (File)stream.readObject();
        stateDisk = (File)stream.readObject();
        this.manifest = new StringBuffer();
        
        if (stream instanceof CheckpointRecovery) {
            CheckpointRecovery cr = (CheckpointRecovery)stream;
            this.disk = translate(cr, CrawlOrder.DISK_PATH, this.disk);
            this.checkpointsDisk = translate(cr, 
                    CrawlOrder.CHECKPOINTS_PATH, this.checkpointsDisk);
            this.scratchDisk = translate(cr, 
                    CrawlOrder.SCRATCH_PATH, this.scratchDisk);
            this.stateDisk = translate(cr, 
                    CrawlOrder.STATE_PATH, this.stateDisk);
        }
            
        stream.defaultReadObject();

        // Ensure no holdover singleThreadMode
        singleThreadMode = false; 
    }
    
    
    private File translate(CheckpointRecovery cr, Key<String> key, File f) {
        String newPath = cr.translatePath(f.getAbsolutePath());
        cr.setState(order, key, newPath);
        File r = new File(newPath);
        r.mkdirs();
        return r;
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
     * Note that a ToeThread reached paused condition, possibly
     * completing the crawl-pause. 
     */
    public synchronized void toePaused() {
        releaseContinuePermission();
        if (state == State.PAUSING && toePool.getActiveToeCount() == 0) {
            completePause();
        }
    }
    
    /**
     * Note that a ToeThread ended, possibly completing the crawl-stop. 
     */
    public synchronized void toeEnded() {
        if (state == State.STOPPING && toePool.getActiveToeCount() == 0) {
            completeStop();
        }
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
    
    /**
     * Log a URIException from deep inside other components to the crawl's
     * shared log. 
     * 
     * @param e URIException encountered
     * @param u CrawlURI where problem occurred
     * @param l String which could not be interpreted as URI without exception
     */
    public void logUriError(URIException e, UURI u, CharSequence l) {
        if (e.getReasonCode() == UURIFactory.IGNORED_SCHEME) {
            // don't log those that are intentionally ignored
            return; 
        }
        Object[] array = {u, l};
        loggerModule.getUriErrors().log(Level.INFO, e.getMessage(), array);
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
                + ArchiveUtils.TIMESTAMP12.format(new Date())
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
     * Call this method to get instance of the crawler BigMap implementation.
     * A "BigMap" is a Map that knows how to manage ever-growing sets of
     * key/value pairs. If we're in a checkpoint recovery, this method will
     * manage reinstantiation of checkpointed bigmaps.
     * @param dbName Name to give any associated database.  Also used
     * as part of name serializing out bigmap.  Needs to be unique to a crawl.
     * @param keyClass Class of keys we'll be using.
     * @param valueClass Class of values we'll be using.
     * @return Map that knows how to carry large sets of key/value pairs or
     * if none available, returns instance of HashMap.
     * @throws Exception
     */
//    public <K,V> Map<K,V> getBigMap(final String dbName, 
//            final Class<? super K> keyClass,
//            final Class<? super V> valueClass)
//    throws DatabaseException {
//        return bdb.getBigMap(dbName, keyClass, valueClass);
//    }
    

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

    
    public File getRelative(String path) {
        File f = new File(path);
        if (f.isAbsolute()) {
            return f;
        }

    
        return new File(disk, path);
    }
    
    
    public File getCheckpointsDisk() {
        return this.checkpointsDisk;
    }
    
    
    public CredentialStore getCredentialStore() {
        return get(this, CREDENTIAL_STORE);
    }
    
    
    public <T> T get(Object module, Key<T> key) {
        return sheetManager.getDefault().get(module, key);
    }

    
    
    
    public void checkUserAgentAndFrom() throws FatalConfigurationException {
        Map<String,String> hh = get(order, CrawlOrder.HTTP_HEADERS);
        String userAgent = hh.get("user-agent");
        String from = hh.get("from");
        boolean valid = true;
        if (userAgent == null) {
            valid = false;
        }
        if (from == null) {
            valid = false;
        }
        if (valid && !userAgent.matches(ACCEPTABLE_USER_AGENT)) {
            valid = false;
        }
        if (valid && !from.matches(ACCEPTABLE_FROM)) {
            valid = false;
        }

        if (!valid) {
            throw new FatalConfigurationException("unacceptable user-agent " +
                    " or from (Reedit your order file).");
        }
    }

    
    public String getUserAgent(StateProvider p) {
        return p.get(order, CrawlOrder.HTTP_HEADERS).get("user-agent");
    }


    
    public CrawlOrder getOrder() {
        return order;
    }

    
    @Emitter(desc="Emitted when the crawl status changes (eg, when a crawl "
        + " goes from CRAWLING to ENDED)",         
        types={ "PAUSED", "RUNNING", "PAUSING", "STARTED", "STOPPING", 
            "FINISHED", "PREPARING" })
    void emit(Notification n) {
        sendNotification(n);
    }


//    public BdbModule getBdbModule() {
//        return bdb;
//    }


    public CrawlerLoggerModule getLoggerModule() {
        return loggerModule;
    }


    public File getDirectory() {
        return disk;
    }

    
    public String toAbsolutePath(String path) {
        return DefaultDirectoryModule.toAbsolutePath(getDirectory(), path);
    }


    public String toRelativePath(String path) {
        return DefaultDirectoryModule.toRelativePath(getDirectory(), path);
    }

}
