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
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.EventObject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.AttributeNotFoundException;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanException;
import javax.management.ReflectionException;

import org.apache.commons.httpclient.URIException;
import org.archive.crawler.datamodel.CandidateURI;
import org.archive.crawler.datamodel.Checkpoint;
import org.archive.crawler.datamodel.CrawlOrder;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.openmbeans.annotations.Bean;
import org.archive.openmbeans.annotations.Operation;
import org.archive.processors.util.ServerCache;
import org.archive.crawler.event.CrawlStatusListener;
import org.archive.crawler.event.CrawlURIDispositionListener;
import org.archive.crawler.framework.exceptions.FatalConfigurationException;
import org.archive.crawler.framework.exceptions.InitializationException;
import org.archive.crawler.io.LocalErrorFormatter;
import org.archive.crawler.io.RuntimeErrorFormatter;
import org.archive.crawler.io.StatisticsLogFormatter;
import org.archive.crawler.io.UriErrorFormatter;
import org.archive.crawler.io.UriProcessingFormatter;
import org.archive.crawler.url.CanonicalizationRule;
import org.archive.crawler.util.CheckpointUtils;
import org.archive.io.GenerationFileHandler;
import org.archive.net.UURI;
import org.archive.net.UURIFactory;
import org.archive.processors.Processor;
import org.archive.processors.credential.CredentialStore;
import org.archive.settings.Sheet;
import org.archive.settings.SheetManager;
import org.archive.settings.file.BdbModule;
import org.archive.state.Dependency;
import org.archive.state.Expert;
import org.archive.state.Global;
import org.archive.state.Immutable;
import org.archive.state.Key;
import org.archive.state.KeyManager;
import org.archive.state.StateProvider;
import org.archive.surt.SURTTokenizer;
import org.archive.util.ArchiveUtils;
import org.archive.util.CachedBdbMap;
import org.archive.util.FileUtils;
import org.archive.util.Reporter;
import org.xbill.DNS.DClass;
import org.xbill.DNS.Type;
import org.xbill.DNS.dns;

import com.sleepycat.bind.serial.StoredClassCatalog;
import com.sleepycat.je.CheckpointConfig;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.DbInternal;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.dbi.EnvironmentImpl;
import com.sleepycat.je.utilint.DbLsn;

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
implements Serializable, Reporter, StateProvider {
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

    
    @Expert @Immutable
    final public static Key<CrawlScope> SCOPE = 
        Key.make(CrawlScope.class, null);

    
    final public static Key<Map<String,Processor>> PROCESSORS =
        Key.makeMap(Processor.class);

    
    @Dependency
    final public static Key<CrawlOrder> ORDER = Key.make(new CrawlOrder());
    
    
    @Dependency
    final public static Key<SheetManager> SHEET_MANAGER = 
        Key.make(SheetManager.class, null);
    
    @Dependency
    final public static Key<BdbModule> BDB =
        Key.make(BdbModule.class, null);
    
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

    // manifest support
    /** abbrieviation label for config files in manifest */
    public static final char MANIFEST_CONFIG_FILE = 'C';
    /** abbrieviation label for report files in manifest */
    public static final char MANIFEST_REPORT_FILE = 'R';
    /** abbrieviation label for log files in manifest */
    public static final char MANIFEST_LOG_FILE = 'L';

    // key log names
    private static final String LOGNAME_PROGRESS_STATISTICS =
        "progress-statistics";
    private static final String LOGNAME_URI_ERRORS = "uri-errors";
    private static final String LOGNAME_RUNTIME_ERRORS = "runtime-errors";
    private static final String LOGNAME_LOCAL_ERRORS = "local-errors";
    private static final String LOGNAME_CRAWL = "crawl";

    // key subcomponents which define and implement a crawl in progress
    //private transient CrawlOrder order;
//    private transient CrawlScope scope;
    
//    private transient Frontier frontier;

    private transient ToePool toePool;
    
    private transient ServerCache serverCache;
    

    /**
     * Sheet manager for context (SURT) based settings.  Passed to the
     * initialization method.
     */
    private transient SheetManager sheetManager;

    // Used to enable/disable single-threaded operation after OOM
    private volatile transient boolean singleThreadMode = false; 
    private transient ReentrantLock singleThreadLock = null;

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
/*
    private static final Object NASCENT = "NASCENT".intern();
    private static final Object RUNNING = "RUNNING".intern();
    private static final Object PAUSED = "PAUSED".intern();
    private static final Object PAUSING = "PAUSING".intern();
    private static final Object CHECKPOINTING = "CHECKPOINTING".intern();
    private static final Object STOPPING = "STOPPING".intern();
    private static final Object FINISHED = "FINISHED".intern();
    private static final Object STARTED = "STARTED".intern();
    private static final Object PREPARING = "PREPARING".intern();
*/
    transient private State state = State.NASCENT;

    // disk paths
    private transient File disk;        // overall disk path
    private transient File logsDisk;    // for log files
    
    /**
     * For temp files representing state of crawler (eg queues)
     */
    private transient File stateDisk;
    
    /**
     * For discardable temp files (eg fetch buffers).
     */
    private transient File scratchDisk;

    /**
     * Directory that holds checkpoint.
     */
    private transient File checkpointsDisk;
    
    /**
     * Checkpointer.
     * Knows if checkpoint in progress and what name of checkpoint is.  Also runs
     * checkpoints.
     */
    private Checkpointer checkpointer;
    
    /**
     * Gets set to checkpoint we're in recovering if in checkpoint recover
     * mode.  Gets setup by {@link #getCheckpointRecover()}.
     */
    private transient Checkpoint checkpointRecover = null;

    // crawl limits
    private long maxBytes;
    private long maxDocument;
    private long maxTime;

    /**
     * A manifest of all files used/created during this crawl. Written to file
     * at the end of the crawl (the absolutely last thing done).
     */
    private StringBuffer manifest;

    /**
     * Record of fileHandlers established for loggers,
     * assisting file rotation.
     */
    transient private Map<Logger,FileHandler> fileHandlers;

    /** suffix to use on active logs */
    public static final String CURRENT_LOG_SUFFIX = ".log";

    /**
     * Crawl progress logger.
     *
     * No exceptions.  Logs summary result of each url processing.
     */
    public transient Logger uriProcessing;

    /**
     * This logger contains unexpected runtime errors.
     *
     * Would contain errors trying to set up a job or failures inside
     * processors that they are not prepared to recover from.
     */
    public transient Logger runtimeErrors;

    /**
     * This logger is for job-scoped logging, specifically errors which
     * happen and are handled within a particular processor.
     *
     * Examples would be socket timeouts, exceptions thrown by extractors, etc.
     */
    public transient Logger localErrors;

    /**
     * Special log for URI format problems, wherever they may occur.
     */
    public transient Logger uriErrors;

    /**
     * Statistics tracker writes here at regular intervals.
     */
    private transient Logger progressStats;

    /**
     * Logger to hold job summary report.
     *
     * Large state reports made at infrequent intervals (e.g. job ending) go
     * here.
     */
    public transient Logger reports;

    protected StatisticsTracking statistics = null;

    /**
     * List of crawl status listeners.
     *
     * All iterations need to synchronize on this object if they're to avoid
     * concurrent modification exceptions.
     * See {@link java.util.Collections#synchronizedList(List)}.
     */
    private transient List<CrawlStatusListener> registeredCrawlStatusListeners =
        Collections.synchronizedList(new ArrayList<CrawlStatusListener>());
    
    // Since there is a high probability that there will only ever by one
    // CrawlURIDispositionListner we will use this while there is only one:
    private transient CrawlURIDispositionListener
        registeredCrawlURIDispositionListener;

    // And then switch to the array once there is more then one.
     protected transient ArrayList<CrawlURIDispositionListener> 
     registeredCrawlURIDispositionListeners;
    
    
    /**
     * Keep a list of all BigMap instance made -- shouldn't be many -- so that
     * we can checkpoint.
     */
    private transient Map<String,CachedBdbMap<?,?>> bigmaps = null;

    
    

//    private transient Map<String,Processor> processors;
    

    final private CrawlOrder order;

    final private BdbModule bdb;
    

    public CrawlController(SheetManager manager, CrawlOrder order, 
            BdbModule environment) 
    throws InitializationException {
        this.sheetManager = manager;
        this.order = order;
        this.bdb = environment;
        sendCrawlStateChangeEvent(State.PREPARING, CrawlStatus.PREPARING);

        this.singleThreadLock = new ReentrantLock();
//        this.order = new CrawlOrder();
//        this.order.setController(this);
        this.bigmaps = new Hashtable<String,CachedBdbMap<?,?>>();
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

            onFailMessage = "Unable to create log file(s)";
            setupLogs();
            
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
            onFailMessage = "Unable to test/run checkpoint recover";
            this.checkpointRecover = getCheckpointRecover();
            if (this.checkpointRecover == null) {
                this.checkpointer =
                    new Checkpointer(this, this.checkpointsDisk);
            } else {
                setupCheckpointRecover();
            }
            
            onFailMessage = "Unable to setup bdb environment.";
            
//            onFailMessage = "Unable to setup statistics";
//            setupStatTracking();
            
            onFailMessage = "Unable to setup crawl modules";
            setupCrawlModules();
        } catch (Exception e) {
            String tmp = "On crawl: " + sheetManager.getCrawlName() + " " +
                onFailMessage;
            LOGGER.log(Level.SEVERE, tmp, e);
            throw new InitializationException(tmp, e);
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
    protected void setupCheckpointRecover()
    throws IOException {
        long started = System.currentTimeMillis();;
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("Starting recovery setup -- copying into place " +
                "bdbje log files -- for checkpoint named " +
                this.checkpointRecover.getDisplayName());
        }
        // Mark context we're in a recovery.
        this.checkpointer.recover(this);
        this.progressStats.info("CHECKPOINT RECOVER " +
            this.checkpointRecover.getDisplayName());
        // Copy the bdb log files to the state dir so we don't damage
        // old checkpoint.  If thousands of log files, can take
        // tens of minutes (1000 logs takes ~5 minutes to java copy,
        // dependent upon hardware).  If log file already exists over in the
        // target state directory, we do not overwrite -- we assume the log
        // file in the target same as one we'd copy from the checkpoint dir.
        File bdbSubDir = CheckpointUtils.
            getBdbSubDirectory(this.checkpointRecover.getDirectory());
        FileUtils.copyFiles(bdbSubDir, CheckpointUtils.getJeLogsFilter(),
            getStateDisk(), true,
            false);
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.info("Finished recovery setup for checkpoint named " +
                this.checkpointRecover.getDisplayName() + " in " +
                (System.currentTimeMillis() - started) + "ms.");
        }
    }
    
    protected boolean getCheckpointCopyBdbjeLogs() {
        return getOrderSetting(CrawlOrder.CHECKPOINT_COPY_BDBJE_LOGS);
    }
    
    
    public Environment getBdbEnvironment() {
        return this.bdb.getEnvironment();
    }
    
    public StoredClassCatalog getClassCatalog() {
        return this.bdb.getClassCatalog();
    }

    /**
     * Register for CrawlStatus events.
     *
     * @param cl a class implementing the CrawlStatusListener interface
     *
     * @see CrawlStatusListener
     */
    public void addCrawlStatusListener(CrawlStatusListener cl) {
        synchronized (this.registeredCrawlStatusListeners) {
            this.registeredCrawlStatusListeners.add(cl);
        }
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

    private void setupCrawlModules() throws FatalConfigurationException,
             AttributeNotFoundException, MBeanException, ReflectionException {
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

    }
    
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
        File file = new File(diskPath);
        if (file.isAbsolute()) {
            this.disk = file;
        } else {
            File workDir = sheetManager.getWorkingDirectory();
            this.disk = new File(workDir, diskPath);
        }
        this.disk.mkdirs();
        this.logsDisk = getSettingsDir(CrawlOrder.LOGS_PATH);
        this.checkpointsDisk = getSettingsDir(CrawlOrder.CHECKPOINTS_PATH);
        this.stateDisk = getSettingsDir(CrawlOrder.STATE_PATH);
        this.scratchDisk = getSettingsDir(CrawlOrder.SCRATCH_PATH);
    }
    
    /**
     * @return The logging directory or null if problem reading the settings.
     */
    public File getLogsDir() {
        return getSettingsDir(CrawlOrder.LOGS_PATH);
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
    public File getSettingsDir(Key<String> key) {
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
        List<StatisticsTracking> loggers = getOrderSetting(LOGGERS);
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
            // tracker.initialize(this);
            if (this.statistics == null) {
                this.statistics = tracker;
            }
        }
    } */
    
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

    private void setupLogs() throws IOException {
        String logsPath = logsDisk.getAbsolutePath() + File.separatorChar;
        uriProcessing = Logger.getLogger(LOGNAME_CRAWL + "." + logsPath);
        runtimeErrors = Logger.getLogger(LOGNAME_RUNTIME_ERRORS + "." +
            logsPath);
        localErrors = Logger.getLogger(LOGNAME_LOCAL_ERRORS + "." + logsPath);
        uriErrors = Logger.getLogger(LOGNAME_URI_ERRORS + "." + logsPath);
        progressStats = Logger.getLogger(LOGNAME_PROGRESS_STATISTICS + "." +
            logsPath);

        this.fileHandlers = new HashMap<Logger,FileHandler>();

        setupLogFile(uriProcessing,
            logsPath + LOGNAME_CRAWL + CURRENT_LOG_SUFFIX,
            new UriProcessingFormatter(), true);

        setupLogFile(runtimeErrors,
            logsPath + LOGNAME_RUNTIME_ERRORS + CURRENT_LOG_SUFFIX,
            new RuntimeErrorFormatter(), true);

        setupLogFile(localErrors,
            logsPath + LOGNAME_LOCAL_ERRORS + CURRENT_LOG_SUFFIX,
            new LocalErrorFormatter(), true);

        setupLogFile(uriErrors,
            logsPath + LOGNAME_URI_ERRORS + CURRENT_LOG_SUFFIX,
            new UriErrorFormatter(), true);

        setupLogFile(progressStats,
            logsPath + LOGNAME_PROGRESS_STATISTICS + CURRENT_LOG_SUFFIX,
            new StatisticsLogFormatter(), true);

    }

    private void setupLogFile(Logger logger, String filename, Formatter f,
            boolean shouldManifest) throws IOException, SecurityException {
        GenerationFileHandler fh = new GenerationFileHandler(filename, true,
            shouldManifest);
        fh.setFormatter(f);
        logger.addHandler(fh);
        addToManifest(filename, MANIFEST_LOG_FILE, shouldManifest);
        logger.setUseParentHandlers(false);
        this.fileHandlers.put(logger, fh);
    }
    
    protected void rotateLogFiles(String generationSuffix)
    throws IOException {
        if (this.state != State.PAUSED && this.state != State.CHECKPOINTING) {
            throw new IllegalStateException("Pause crawl before requesting " +
                "log rotation.");
        }
        for (Iterator i = fileHandlers.keySet().iterator(); i.hasNext();) {
            Logger l = (Logger)i.next();
            GenerationFileHandler gfh =
                (GenerationFileHandler)fileHandlers.get(l);
            GenerationFileHandler newGfh =
                gfh.rotate(generationSuffix, CURRENT_LOG_SUFFIX);
            if (gfh.shouldManifest()) {
                addToManifest((String) newGfh.getFilenameSeries().get(1),
                    MANIFEST_LOG_FILE, newGfh.shouldManifest());
            }
            l.removeHandler(gfh);
            l.addHandler(newGfh);
            fileHandlers.put(l, newGfh);
        }
    }

    /**
     * Close all log files and remove handlers from loggers.
     */
    public void closeLogFiles() {
       for (Iterator i = fileHandlers.keySet().iterator(); i.hasNext();) {
            Logger l = (Logger)i.next();
            GenerationFileHandler gfh =
                (GenerationFileHandler)fileHandlers.get(l);
            gfh.close();
            l.removeHandler(gfh);
        }
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
        try {
            return statistics==null ?
                    new StatisticsTracker(this): this.statistics;
        } catch (FatalConfigurationException e) {
            throw new RuntimeException(e); // FIXME
        }
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
        synchronized (this.registeredCrawlStatusListeners) {
            this.state = newState;
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
            LOGGER.fine("Sent " + newState);
        }
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
    protected void sendCheckpointEvent(File checkpointDir) throws Exception {
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
    }

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
        synchronized (this.registeredCrawlStatusListeners) {
            // Remove all listeners now we're done with them.
            this.registeredCrawlStatusListeners.
                removeAll(this.registeredCrawlStatusListeners);
            this.registeredCrawlStatusListeners = null;
        }
        
        closeLogFiles();
        
        // Release reference to logger file handler instances.
        this.fileHandlers = null;
        this.uriErrors = null;
        this.uriProcessing = null;
        this.localErrors = null;
        this.runtimeErrors = null;
        this.progressStats = null;
        this.reports = null;
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
        this.bigmaps = null;
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
     * Run checkpointing.
     * CrawlController takes care of managing the checkpointing/serializing
     * of bdb, the StatisticsTracker, and the CheckpointContext.  Other
     * modules that want to revive themselves on checkpoint recovery need to
     * save state during their {@link CrawlStatusListener#crawlCheckpoint(File)}
     * invocation and then in their #initialize if a module,
     * or in their #initialTask if a processor, check with the CrawlController
     * if its checkpoint recovery. If it is, read in their old state from the
     * pointed to  checkpoint directory.
     * <p>Default access only to be called by Checkpointer.
     * @throws Exception
     */
    void checkpoint()
    throws Exception {
        // Tell registered listeners to checkpoint.
        sendCheckpointEvent(this.checkpointer.
            getCheckpointInProgressDirectory());
        
        // Rotate off crawler logs.
        LOGGER.fine("Rotating log files.");
        rotateLogFiles(CURRENT_LOG_SUFFIX + "." +
            this.checkpointer.getNextCheckpointName());

        // Sync the BigMap contents to bdb, if their bdb bigmaps.
        LOGGER.fine("BigMaps.");
        checkpointBigMaps(this.checkpointer.getCheckpointInProgressDirectory());

        // Note, on deserialization, the super CrawlType#parent
        // needs to be restored. Parent is '/crawl-order/loggers'.
        // The settings handler for this module also needs to be
        // restored. Both of these fields are private in the
        // super class. Adding the restored ST to crawl order should take
        // care of this.

        // Checkpoint bdb environment.
        LOGGER.fine("Bdb environment.");
        checkpointBdb(this.checkpointer.getCheckpointInProgressDirectory());

        // Make copy of order, seeds, and settings.
        LOGGER.fine("Copying settings.");
        copySettings(this.checkpointer.getCheckpointInProgressDirectory());

        // Checkpoint this crawlcontroller.
        CheckpointUtils.writeObjectToFile(this,
            this.checkpointer.getCheckpointInProgressDirectory());
    }
    
    /**
     * Copy off the settings.
     * @param checkpointDir Directory to write checkpoint to.
     * @throws IOException 
     */
    protected void copySettings(final File checkpointDir) throws IOException {
        // FIXME: Overhaul checkpointing
/*        final List files = this.settingsHandler.getListOfAllFiles();
        boolean copiedSettingsDir = false;
        final File settingsDir = new File(this.disk, "settings");
        for (final Iterator i = files.iterator(); i.hasNext();) {
            File f = new File((String)i.next());
            if (f.getAbsolutePath().startsWith(settingsDir.getAbsolutePath())) {
                if (copiedSettingsDir) {
                    // Skip.  We've already copied this member of the
                    // settings directory.
                    continue;
                }
                // Copy 'settings' dir all in one lump, not a file at a time.
                copiedSettingsDir = true;
                FileUtils.copyFiles(settingsDir,
                    new File(checkpointDir, settingsDir.getName()));
                continue;
            }
            FileUtils.copyFiles(f, f.isDirectory()? checkpointDir:
                new File(checkpointDir, f.getName()));
        } */
    }
    
    /**
     * Checkpoint bdb.
     * I used do a call to log cleaning as suggested in je-2.0 javadoc but takes
     * way too much time (20minutes for a crawl of 1million items). Assume
     * cleaner is keeping up. Below was log cleaning loop .
     * <pre>int totalCleaned = 0;
     * for (int cleaned = 0; (cleaned = this.bdbEnvironment.cleanLog()) != 0;
     *  totalCleaned += cleaned) {
     *      LOGGER.fine("Cleaned " + cleaned + " log files.");
     * }
     * </pre>
     * <p>I also used to do a sync. But, from Mark Hayes, sync and checkpoint
     * are effectively same thing only sync is not configurable.  He suggests
     * doing one or the other:
     * <p>MS: Reading code, Environment.sync() is a checkpoint.  Looks like
     * I don't need to call a checkpoint after calling a sync?
     * <p>MH: Right, they're almost the same thing -- just do one or the other,
     * not both.  With the new API, you'll need to do a checkpoint not a
     * sync, because the sync() method has no config parameter.  Don't worry
     * -- it's fine to do a checkpoint even though you're not using.
     * @param checkpointDir Directory to write checkpoint to.
     * @throws DatabaseException 
     * @throws IOException 
     * @throws RuntimeException Thrown if failed setup of new bdb environment.
     */
    protected void checkpointBdb(File checkpointDir)
    throws DatabaseException, IOException, RuntimeException {
        EnvironmentConfig envConfig = bdb.getEnvironment().getConfig();
        final List bkgrdThreads = Arrays.asList(new String []
            {"je.env.runCheckpointer", "je.env.runCleaner",
                "je.env.runINCompressor"});
        try {
            // Disable background threads
            setBdbjeBkgrdThreads(envConfig, bkgrdThreads, "false");
            // Do a force checkpoint.  Thats what a sync does (i.e. doSync).
            CheckpointConfig chkptConfig = new CheckpointConfig();
            chkptConfig.setForce(true);
            
            // Mark Hayes of sleepycat says:
            // "The default for this property is false, which gives the current
            // behavior (allow deltas).  If this property is true, deltas are
            // prohibited -- full versions of internal nodes are always logged
            // during the checkpoint. When a full version of an internal node
            // is logged during a checkpoint, recovery does not need to process
            // it at all.  It is only fetched if needed by the application,
            // during normal DB operations after recovery. When a delta of an
            // internal node is logged during a checkpoint, recovery must
            // process it by fetching the full version of the node from earlier
            // in the log, and then applying the delta to it.  This can be
            // pretty slow, since it is potentially a large amount of
            // random I/O."
            chkptConfig.setMinimizeRecoveryTime(true);
            bdb.getEnvironment().checkpoint(chkptConfig);
            LOGGER.fine("Finished bdb checkpoint.");
            
            // From the sleepycat folks: A trick for flipping db logs.
            EnvironmentImpl envImpl = 
                DbInternal.envGetEnvironmentImpl(bdb.getEnvironment());
            long firstFileInNextSet =
                DbLsn.getFileNumber(envImpl.forceLogFileFlip());
            // So the last file in the checkpoint is firstFileInNextSet - 1.
            // Write manifest of all log files into the bdb directory.
            final String lastBdbCheckpointLog =
                getBdbLogFileName(firstFileInNextSet - 1);
            processBdbLogs(checkpointDir, lastBdbCheckpointLog);
            LOGGER.fine("Finished processing bdb log files.");
        } finally {
            // Restore background threads.
            setBdbjeBkgrdThreads(envConfig, bkgrdThreads, "true");
        }
    }
    
    protected void processBdbLogs(final File checkpointDir,
            final String lastBdbCheckpointLog) throws IOException {
        File bdbDir = CheckpointUtils.getBdbSubDirectory(checkpointDir);
        if (!bdbDir.exists()) {
            bdbDir.mkdir();
        }
        PrintWriter pw = new PrintWriter(new FileOutputStream(new File(
             checkpointDir, "bdbje-logs-manifest.txt")));
        try {
            // Don't copy any beyond the last bdb log file (bdbje can keep
            // writing logs after checkpoint).
            boolean pastLastLogFile = false;
            Set<String> srcFilenames = null;
            final boolean copyFiles = getCheckpointCopyBdbjeLogs();
            do {
                FilenameFilter filter = CheckpointUtils.getJeLogsFilter();
                srcFilenames =
                    new HashSet<String>(Arrays.asList(
                            getStateDisk().list(filter)));
                List tgtFilenames = Arrays.asList(bdbDir.list(filter));
                if (tgtFilenames != null && tgtFilenames.size() > 0) {
                    srcFilenames.removeAll(tgtFilenames);
                }
                if (srcFilenames.size() > 0) {
                    // Sort files.
                    srcFilenames = new TreeSet<String>(srcFilenames);
                    int count = 0;
                    for (final Iterator i = srcFilenames.iterator();
                            i.hasNext() && !pastLastLogFile;) {
                        String name = (String) i.next();
                        if (copyFiles) {
                            FileUtils.copyFiles(new File(getStateDisk(), name),
                                new File(bdbDir, name));
                        }
                        pw.println(name);
                        if (name.equals(lastBdbCheckpointLog)) {
                            // We're done.
                            pastLastLogFile = true;
                        }
                        count++;
                    }
                    if (LOGGER.isLoggable(Level.FINE)) {
                        LOGGER.fine("Copied " + count);
                    }
                }
            } while (!pastLastLogFile && srcFilenames != null &&
                srcFilenames.size() > 0);
        } finally {
            pw.close();
        }
    }
 
    protected String getBdbLogFileName(final long index) {
        String lastBdbLogFileHex = Long.toHexString(index);
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < (8 - lastBdbLogFileHex.length()); i++) {
            buffer.append('0');
        }
        buffer.append(lastBdbLogFileHex);
        buffer.append(".jdb");
        return buffer.toString();
    }
    
    protected void setBdbjeBkgrdThreads(final EnvironmentConfig config,
            final List threads, final String setting) {
        for (final Iterator i = threads.iterator(); i.hasNext();) {
            config.setConfigParam((String)i.next(), setting);
        }
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
    public synchronized Checkpoint getCheckpointRecover() {
        if (this.checkpointRecover != null) {
            return this.checkpointRecover;
        }
        return getCheckpointRecover(this);
    }
    
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
    
    /**
     * @return True if we're in checkpoint recover mode. Call
     * {@link #getCheckpointRecover()} to get at Checkpoint instance
     * that has info on checkpoint directory being recovered from.
     */
    public boolean isCheckpointRecover() {
        return this.checkpointRecover != null;
    }

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
    public synchronized void requestCrawlResume() {
        if (state != State.PAUSING && state != State.PAUSED && state != State.CHECKPOINTING) {
            // Can't resume if not been told to pause or if we're in middle of
            // a checkpoint.
            return;
        }
        multiThreadMode();
        getFrontier().unpause();
        LOGGER.fine("Crawl resumed.");
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
    public CrawlScope getScope() {
        return get(this, SCOPE);
//        return scope;
    }


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
        
        getScope().kickUpdate(this);
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
     * Add a file to the manifest of files used/generated by the current
     * crawl.
     * 
     * TODO: Its possible for a file to be added twice if reports are
     * force generated midcrawl.  Fix.
     *
     * @param file The filename (with absolute path) of the file to add
     * @param type The type of the file
     * @param bundle Should the file be included in a typical bundling of
     *           crawler files.
     *
     * @see #MANIFEST_CONFIG_FILE
     * @see #MANIFEST_LOG_FILE
     * @see #MANIFEST_REPORT_FILE
     */
    public void addToManifest(String file, char type, boolean bundle) {
        manifest.append(type + (bundle? "+": "-") + " " + file + "\n");
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
    
    private void readObject(ObjectInputStream stream)
    throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        // Setup status listeners
        this.registeredCrawlStatusListeners =
            Collections.synchronizedList(new ArrayList<CrawlStatusListener>());
        // Ensure no holdover singleThreadMode
        singleThreadMode = false; 
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
        uriErrors.log(Level.INFO, e.getMessage(), array);
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
    public <K,V> Map<K,V> getBigMap(final String dbName, 
            final Class<? super K> keyClass,
            final Class<? super V> valueClass)
    throws Exception {
        CachedBdbMap<K,V> result = new CachedBdbMap<K,V>(dbName);
        if (isCheckpointRecover()) {
            File baseDir = getCheckpointRecover().getDirectory();
            @SuppressWarnings("unchecked")
            CachedBdbMap<K,V> temp = CheckpointUtils.
                readObjectFromFile(result.getClass(), dbName, baseDir);
            result = temp;
        }
        result.initialize(getBdbEnvironment(), keyClass, valueClass,
                getClassCatalog());
        // Save reference to all big maps made so can manage their
        // checkpointing.
        this.bigmaps.put(dbName, result);
        return result;
    }
    
    protected void checkpointBigMaps(final File cpDir)
    throws Exception {
        for (final Iterator i = this.bigmaps.keySet().iterator(); i.hasNext();) {
            Object key = i.next();
            Object obj = this.bigmaps.get(key);
            // TODO: I tried adding sync to custom serialization of BigMap
            // implementation but data member counts of the BigMap
            // implementation were not being persisted properly.  Look at
            // why.  For now, do sync in advance of serialization for now.
            ((CachedBdbMap)obj).sync();
            CheckpointUtils.writeObjectToFile(obj, (String)key, cpDir);
        }
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
        this.progressStats.info(msg);
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

    
    public void setStateProvider(CandidateURI uri) {
        if (uri.getStateProvider() == null) {
            StateProvider sp = findConfig(uri.getUURI());
            uri.setStateProvider(sp);
        }
    }
    
    /**
     * Returns the configuration for the given URI.
     * 
     * @param uri
     * @return
     */
    public StateProvider findConfig(UURI uri) {
        SURTTokenizer st;
        try {
            st = new SURTTokenizer(uri.toString());
        } catch (URIException e) {
            throw new AssertionError();
        }
        
        SheetList list = null;
        for (String s = st.nextSearch(); s != null; s = st.nextSearch()) {
            Sheet sheet = sheetManager.getAssociation(s);
            if (sheet != null) {
                if (list == null) {
                    list = new SheetList();
                }
                list.add(sheet);
            }
        }
        if (list == null) {
            return sheetManager.getDefault();
        }
        
        list.add(sheetManager.getDefault());
        return list;
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
    
}
