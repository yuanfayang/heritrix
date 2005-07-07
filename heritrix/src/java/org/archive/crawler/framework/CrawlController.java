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
 * $Header$
 */
package org.archive.crawler.framework;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.AttributeNotFoundException;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanException;
import javax.management.ObjectInstance;
import javax.management.ReflectionException;

import org.apache.commons.httpclient.URIException;
import org.archive.crawler.Heritrix;
import org.archive.crawler.admin.Alert;
import org.archive.crawler.admin.CrawlJob;
import org.archive.crawler.admin.StatisticsTracker;
import org.archive.crawler.checkpoint.Checkpoint;
import org.archive.crawler.checkpoint.CheckpointContext;
import org.archive.crawler.datamodel.BigMapFactory;
import org.archive.crawler.datamodel.CrawlOrder;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.ServerCache;
import org.archive.crawler.datamodel.UURI;
import org.archive.crawler.datamodel.UURIFactory;
import org.archive.crawler.event.CrawlStatusListener;
import org.archive.crawler.event.CrawlURIDispositionListener;
import org.archive.crawler.framework.exceptions.FatalConfigurationException;
import org.archive.crawler.framework.exceptions.InitializationException;
import org.archive.crawler.io.LocalErrorFormatter;
import org.archive.crawler.io.RuntimeErrorFormatter;
import org.archive.crawler.io.StatisticsLogFormatter;
import org.archive.crawler.io.UriErrorFormatter;
import org.archive.crawler.io.UriProcessingFormatter;
import org.archive.crawler.settings.MapType;
import org.archive.crawler.settings.SettingsHandler;
import org.archive.io.GenerationFileHandler;
import org.archive.io.ObjectPlusFilesInputStream;
import org.archive.io.ObjectPlusFilesOutputStream;
import org.archive.io.arc.ARCWriter;
import org.archive.util.ArchiveUtils;
import org.archive.util.FileUtils;
import org.archive.util.JEApplicationMBean;
import org.archive.util.Reporter;
import org.xbill.DNS.DClass;
import org.xbill.DNS.Type;
import org.xbill.DNS.dns;

import EDU.oswego.cs.dl.util.concurrent.ReentrantLock;

import com.sleepycat.bind.serial.StoredClassCatalog;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;

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
public class CrawlController implements Serializable, Reporter {
    // be robust against trivial implementation changes
    private static final long serialVersionUID =
        ArchiveUtils.classnameBasedUID(CrawlController.class,1);

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
    private CrawlOrder order;
    private CrawlScope scope;
    private ProcessorChainList processorChains;
    private transient ToePool toePool;
    private Frontier frontier;
    private transient ServerCache serverCache;
    private SettingsHandler settingsHandler;


    // used to enable/disable single-threaded operation after OOM
    volatile boolean singleThreadMode = false; 
    private static final ReentrantLock SINGLE_THREAD_LOCK = new ReentrantLock();

    // emergency reserve of memory to allow some progress/reporting after OOM
    private LinkedList reserveMemory;
    private static final int RESERVE_BLOCKS = 1;
    private static final int RESERVE_BLOCK_SIZE = 6*2^20; // 6MB

    // crawl state: as requested or actual
    
    /**
     * Crawl exit status.
     */
    private String sExit;
    
    /**
     * Whether controller should start in PAUSED state.
     */
    private boolean beginPaused = false;

    private static final Object NASCENT = "NASCENT".intern();
    private static final Object RUNNING = "RUNNING".intern();
    private static final Object PAUSING = "PAUSING".intern();
    private static final Object PAUSED = "PAUSED".intern();
    private static final Object CHECKPOINTING = "CHECKPOINTING".intern();
    private static final Object STOPPING = "STOPPING".intern();
    private static final Object FINISHED = "FINISHED".intern();
    private static final Object STARTED = "STARTED".intern();
    private static final Object PREPARING = "PREPARING".intern();

    transient private Object state = NASCENT;

    // disk paths
    private File disk;        // overall disk path
    private File logsDisk;    // for log files
    private File checkpointsDisk;    // for checkpoint files
    private File stateDisk;   // for temp files representing state of crawler (eg queues)
    private File scratchDisk; // for discardable temp files (eg fetch buffers)

    /**
     * Checkpoint context.
     * Knows if checkpoint in progress, what name of checkpoint is, etc.
     */
    private CheckpointContext cpContext;

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
    transient private Map fileHandlers;

    /** suffix to use on active logs */
    public static final String CURRENT_LOG_SUFFIX = ".log";

    /**
     * Crawl progress logger.
     *
     * No exceptions.  Logs summary result of each url processing.
     */
    transient public Logger uriProcessing;

    /**
     * This logger contains unexpected runtime errors.
     *
     * Would contain errors trying to set up a job or failures inside
     * processors that they are not prepared to recover from.
     */
    transient public Logger runtimeErrors;

    /**
     * This logger is for job-scoped logging, specifically errors which
     * happen and are handled within a particular processor.
     *
     * Examples would be socket timeouts, exceptions thrown by extractors, etc.
     */
    transient public Logger localErrors;

    /**
     * Special log for URI format problems, wherever they may occur.
     */
    transient public Logger uriErrors;

    /**
     * Statistics tracker writes here at regular intervals.
     */
    transient public Logger progressStats;

    /**
     * Logger to hold job summary report.
     *
     * Large state reports made at infrequent intervals (e.g. job ending) go
     * here.
     */
    transient public Logger reports;

    // create a statistic tracking object and have it write to the log every
    protected StatisticsTracking statistics = null;

    /**
     * List of crawl status listeners.
     *
     * All iterations need to synchronize on this object if they're to avoid
     * concurrent modification exceptions.
     * See {@link java.util.Collections#synchronizedList(List)}.
     */
    transient private List registeredCrawlStatusListeners =
        Collections.synchronizedList(new ArrayList());
    
    // Since there is a high probability that there will only ever by one
    // CrawlURIDispositionListner we will use this while there is only one:
    transient CrawlURIDispositionListener registeredCrawlURIDispositionListener;
    // And then switch to the array once there is more then one.
    transient protected ArrayList registeredCrawlURIDispositionListeners;
    
    /** Shared bdb Environment for Frontier subcomponents */
    // TODO: investigate using multiple environments to split disk accesses
    // across separate physical disks
    private transient Environment bdbEnvironment = null;
    
    /**
     * Shared class catalog database.  Used by the
     * {@link #classCatalog}.
     */
    private transient Database classCatalogDB = null;
    
    /**
     * Class catalog instance.
     * Used by bdb serialization.
     */
    private transient StoredClassCatalog classCatalog = null;
    
    private transient ObjectInstance jeRegisteredMBeanInstance = null;

    
    /**
     * Gets set to checkpoint instance if we're in recover checkpoint mode.
     * Go via {@link #getCheckpointRecover()}.  It will take care of
     * construction if we're in recover checkpoint mode.
     */
    private Checkpoint checkpointRecover = null;
    
    /**
     * Default constructor
     */
    public CrawlController() {
        super();
        // defer most setup to initialize methods
    }

    /**
     * Starting from nothing, set up CrawlController and associated
     * classes to be ready for a first crawl.
     *
     * @param sH Settings handler.
     * @throws InitializationException
     */
    public void initialize(SettingsHandler sH)
    throws InitializationException {
        sendCrawlStateChangeEvent(PREPARING, CrawlJob.STATUS_PREPARING);
        
        this.settingsHandler = sH;
        this.order = settingsHandler.getOrder();
        this.order.setController(this);
        sExit = "";
        this.manifest = new StringBuffer();
        String onFailMessage = "";
        try {
            onFailMessage = "You must set the User-Agent and From HTTP" +
            " header values to acceptable strings. \n" +
            " User-Agent: [software-name](+[info-url])[misc]\n" +
            " From: [email-address]\n";
            order.checkUserAgentAndFrom();

            onFailMessage = "Unable to setup disk";
            if (disk == null) {
                setupDisk();
            }

            onFailMessage = "Unable to create log file(s)";
            setupLogs();
            
            // Do checkpointing restore if called for.
            onFailMessage = "Unable to test/run checkpoint recover";
            this.checkpointRecover = getCheckpointRecover();
            if (this.checkpointRecover == null) {
                this.cpContext = new CheckpointContext(this.checkpointsDisk);
            } else {
                this.cpContext = restoreCheckpointContext();
                setupCheckpointRecover();
            }
            
            onFailMessage = "Unable to setup bdb environment.";
            setupBdb();
            
            onFailMessage = "Unable to setup statistics";
            setupStatTracking();
            
            onFailMessage = "Unable to setup crawl modules";
            setupCrawlModules();
        } catch (Exception e) {
            String extendedMessage = onFailMessage + ": " + e.toString();
            Heritrix.addAlert(
                new Alert(
                    e.getClass().getName() + " on crawl: "
                    + settingsHandler.getSettingsObject(null).getName(),
                    extendedMessage,e,Level.CONFIG));
            throw new InitializationException(extendedMessage, e);
        }

        // force creation of DNS Cache now -- avoids CacheCleaner in toe-threads group
        dns.getRecords("localhost", Type.A, DClass.IN);
        
        setupToePool();
        setThresholds();
        
        reserveMemory = new LinkedList();
        for(int i = 1; i < RESERVE_BLOCKS; i++) {
            reserveMemory.add(new char[RESERVE_BLOCK_SIZE]);
        }
    }
    
    /**
     * Does setup of checkpoint recover.
     * Copies bdb log files into state dir, sets the ARCWriter serial number,
     * etc.
     * @throws IOException
     */
    protected void setupCheckpointRecover()
    throws IOException {
        long started = -1;
        if (LOGGER.isLoggable(Level.INFO)) {
            started = System.currentTimeMillis();
            LOGGER.info("Starting checkpoint recover named "
                    + this.checkpointRecover.getDisplayName());
        }
        this.progressStats.info("CHECKPOINT RECOVER " +
            this.checkpointRecover.getDisplayName());
        // Copy the bdb log files to the state dir so we don't damage
        // old checkpoint.
        File bdbSubDir = CheckpointContext.
            getBdbSubDirectory(this.checkpointRecover.getDirectory());
        FileUtils.copyFiles(bdbSubDir, CheckpointContext.getJeLogsFilter(),
            getStateDisk(), true);
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.info("Finished checkpoint recover named "
                    + this.checkpointRecover.getDisplayName() + " in "
                    + (System.currentTimeMillis() - started) + "ms.");
        }
    }
        
    /**
     * @return Deserialized CheckpointContext read out of the pointed to recover
     * checkpoint directory.
     * @throws IOException
     * @throws ClassNotFoundException
     */
    protected CheckpointContext restoreCheckpointContext()
    throws IOException, ClassNotFoundException {
        CheckpointContext cpc = (CheckpointContext)CheckpointContext.
            readObjectFromFile(CheckpointContext.class,
                this.checkpointRecover.getDirectory());
        cpc.postRecoverFixup(this.checkpointsDisk);
        return cpc;
    }
    
    private void setupBdb()
    throws FatalConfigurationException, AttributeNotFoundException {
        EnvironmentConfig envConfig = new EnvironmentConfig();
        envConfig.setAllowCreate(true);
        int bdbCachePercent = ((Integer)this.order.
            getAttribute(null, CrawlOrder.ATTR_BDB_CACHE_PERCENT)).intValue();
        if(bdbCachePercent > 0) {
            // Operator has expressed a preference; override BDB default or 
            // je.properties value
            envConfig.setCachePercent(bdbCachePercent);
        }
        envConfig.setLockTimeout(5000000); // 5 seconds
        // Uncomment this section to see the bdbje Evictor logging.
        /*
        envConfig.setConfigParam("java.util.logging.level", "SEVERE");
        envConfig.setConfigParam("java.util.logging.level.evictor", "SEVERE");
        envConfig.setConfigParam("java.util.logging.ConsoleHandler.on",
            "true");
        */
        try {
            this.bdbEnvironment = new Environment(getStateDisk(), envConfig);
            if (LOGGER.isLoggable(Level.INFO)) {
                // Write out the bdb configuration.
                envConfig = bdbEnvironment.getConfig();
                LOGGER.info("BdbConfiguration: Cache percentage " +
                    envConfig.getCachePercent() +
                    ", cache size " + envConfig.getCacheSize());
            }
            // Open the class catalog database. Create it if it does not
            // already exist. 
            DatabaseConfig dbConfig = new DatabaseConfig();
            dbConfig.setAllowCreate(true);
            this.classCatalogDB = this.bdbEnvironment.
                openDatabase(null, "classes", dbConfig);
            this.classCatalog = new StoredClassCatalog(classCatalogDB);
            // Register the current bdbje env w/ the JMX agent, if there is
            // one.
            this.jeRegisteredMBeanInstance = Heritrix.
                registerMBean(new JEApplicationMBean(this.bdbEnvironment),
                    "BdbJe");
        } catch (DatabaseException e) {
            e.printStackTrace();
            throw new FatalConfigurationException(e.getMessage());
        }
    }
    
    public Environment getBdbEnvironment() {
        return this.bdbEnvironment;
    }
    
    public StoredClassCatalog getClassCatalog() {
        return this.classCatalog;
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
            registeredCrawlURIDispositionListeners = new ArrayList(1);
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
        if (scope == null) {
            scope = (CrawlScope) order.getAttribute(CrawlScope.ATTR_NAME);
        	scope.initialize(this);
        }
        try {
            this.serverCache = new ServerCache(getSettingsHandler());
        } catch (Exception e) {
            throw new FatalConfigurationException("Unable to" +
               " initialize frontier (Failed setup of ServerCache) " + e);
        }
        
        if (frontier == null) {
            Object o = order.getAttribute(Frontier.ATTR_NAME);
            frontier = (Frontier)o;

            // Try to initialize frontier from the config file
            try {
                frontier.initialize(this);
                frontier.pause(); // pause until begun
                // Run recovery if recoverPath points to a file.
                // TODO: make recover path relative to job root dir.
                if (!isCheckpointRecover()) {
                    runFrontierRecover((String)order.
                        getAttribute(CrawlOrder.ATTR_RECOVER_PATH));
                }
            } catch (IOException e) {
                throw new FatalConfigurationException(
                    "unable to initialize frontier: " + e);
            }
        }

        // Setup processors
        if (processorChains == null) {
            processorChains = new ProcessorChainList(order);
        }
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
            // Its a directory if we're supposed to be doing a checkpoint
            // recover.
            return;
        }
        boolean retainFailures = ((Boolean)order.
          getAttribute(CrawlOrder.ATTR_RECOVER_RETAIN_FAILURES)).booleanValue();
        try {
            frontier.importRecoverLog(recoverPath, retainFailures);
        } catch (IOException e) {
            e.printStackTrace();
            throw (FatalConfigurationException) new FatalConfigurationException(
                "Recover.log " + recoverPath + " problem: " + e).initCause(e);
        }
    }

    private void setupDisk() throws AttributeNotFoundException {
        String diskPath
            = (String) order.getAttribute(null, CrawlOrder.ATTR_DISK_PATH);
        this.disk = getSettingsHandler().
            getPathRelativeToWorkingDirectory(diskPath);
        this.disk.mkdirs();
        this.logsDisk = getSettingsDir(CrawlOrder.ATTR_LOGS_PATH);
        this.checkpointsDisk = getSettingsDir(CrawlOrder.ATTR_CHECKPOINTS_PATH);
        this.stateDisk = getSettingsDir(CrawlOrder.ATTR_STATE_PATH);
        this.scratchDisk = getSettingsDir(CrawlOrder.ATTR_SCRATCH_PATH);
    }
    
    /**
     * @return The logging directory or null if problem reading the settings.
     */
    public File getLogsDir() {
        File f = null;
        try {
            f = getSettingsDir(CrawlOrder.ATTR_LOGS_PATH);
        } catch (AttributeNotFoundException e) {
            LOGGER.severe("Failed get of logs directory: " + e.getMessage());
        }
        return f;
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
    public File getSettingsDir(String key)
    throws AttributeNotFoundException {
        String path = (String)order.getAttribute(null, key);
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
    private void setupStatTracking()
    throws InvalidAttributeValueException, FatalConfigurationException {
        MapType loggers = order.getLoggers();
        final String cstName = "crawl-statistics";
        if (loggers.isEmpty(null)) {
            if (statistics == null) {
                statistics = new StatisticsTracker(cstName);
            }
            loggers.addElement(null, (StatisticsTracker)statistics);
        }
        // If checkpoint recover, restore old StatisticsTracker and add it in
        // place of the fresh StatisticsTracker instance.
        if (isCheckpointRecover()) {
            restoreStatisticsTracker(loggers, cstName);
        }
        for (Iterator it = loggers.iterator(null); it.hasNext();) {
            StatisticsTracking tracker = (StatisticsTracking)it.next();
            tracker.initialize(this);
            if (this.statistics == null) {
                this.statistics = tracker;
            }
        }
    }
    
    protected void restoreStatisticsTracker(MapType loggers, String replace)
    throws FatalConfigurationException {
        try {
            StatisticsTracker rst = (StatisticsTracker)CheckpointContext.
                readObjectFromFile(StatisticsTracker.class,
                    this.checkpointRecover.getDirectory());
            if (rst != null) {
                loggers.removeElement(loggers.globalSettings(), replace);
                loggers.addElement(loggers.globalSettings(), rst);
            }
        } catch (InvalidAttributeValueException e) {
            throw convertToFatalConfigurationException(e);
        } catch (AttributeNotFoundException e) {
            throw convertToFatalConfigurationException(e);
        } catch (IOException e) {
            throw convertToFatalConfigurationException(e);
        } catch (ClassNotFoundException e) {
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

        this.fileHandlers = new HashMap();

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
    
    /**
     * Rotate off all logs
     * Rotated get logs get 14 digit date suffix.
     * @throws IOException
     * @deprecated Let log rotation happen as part of checkpointing instead.
     */
    public void rotateLogFiles() throws IOException {
        rotateLogFiles(CURRENT_LOG_SUFFIX + "." +
            ArchiveUtils.get14DigitDate());
    }
    
    public void rotateLogFiles(String generationSuffix)
    throws IOException {
        if (this.state != PAUSED && this.state != CHECKPOINTING) {
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
        try {
            maxBytes =
                ((Long) order.getAttribute(CrawlOrder.ATTR_MAX_BYTES_DOWNLOAD))
                    .longValue();
        } catch (Exception e) {
            maxBytes = 0;
        }
        try {
            maxDocument =
                ((Long) order
                    .getAttribute(CrawlOrder.ATTR_MAX_DOCUMENT_DOWNLOAD))
                    .longValue();
        } catch (Exception e) {
            maxDocument = 0;
        }
        try {
            maxTime =
                ((Long) order.getAttribute(CrawlOrder.ATTR_MAX_TIME_SEC))
                    .longValue();
        } catch (Exception e) {
            maxTime = 0;
        }
    }

    /**
     * @return Object this controller is using to track crawl statistics
     */
    public StatisticsTracking getStatistics() {
        return statistics==null?new StatisticsTracker("crawl-statistics"):statistics;
    }
    
    /**
     * Send crawl change event to all listeners.
     * @param newState State change we're to tell listeners' about.
     * @param message Message on state change.
     * @see #sendCheckpointEvent(File) for special case telling
     * listeners to checkpoint.
     */
    protected void sendCrawlStateChangeEvent(Object newState, String message) {
        synchronized (this.registeredCrawlStatusListeners) {
            this.state = newState;
            for (Iterator i = this.registeredCrawlStatusListeners.iterator();
                    i.hasNext();) {
                CrawlStatusListener l = (CrawlStatusListener)i.next();
                if (newState.equals(PAUSED)) {
                   l.crawlPaused(message);
                } else if (newState.equals(RUNNING)) {
                    l.crawlResuming(message);
                } else if (newState.equals(PAUSING)) {
                   l.crawlPausing(message);
                } else if (newState.equals(STARTED)) {
                    l.crawlStarted(message);
                } else if (newState.equals(STOPPING)) {
                    l.crawlEnding(message);
                } else if (newState.equals(FINISHED)) {
                    l.crawlEnded(message);
                } else if (newState.equals(PREPARING)) {
                    l.crawlResuming(message);
                } else {
                    throw new RuntimeException("Unknown state: " + newState);
                }
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.fine("Sent " + newState + " to " + l);
                }
            }
            LOGGER.info("Sent " + newState);
        }
    }
    
    protected void sendCheckpointEvent(File checkpointDir) throws Exception {
        synchronized (this.registeredCrawlStatusListeners) {
            for (Iterator i = this.registeredCrawlStatusListeners.iterator();
                    i.hasNext();) {
                CrawlStatusListener l = (CrawlStatusListener)i.next();
                l.crawlCheckpoint(checkpointDir);
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.fine("Sent " + CHECKPOINTING + " to " + l);
                }
            }
            LOGGER.info("Sent " + CHECKPOINTING);
        }
    }

    /**
     * Operator requested crawl begin
     */
    public void requestCrawlStart() {
        runProcessorInitialTasks();

        sendCrawlStateChangeEvent(STARTED, CrawlJob.STATUS_PENDING);
        String jobState;
        if(beginPaused) {
            state = PAUSED;
            jobState = CrawlJob.STATUS_PAUSED;
        } else {
            state = RUNNING;
            jobState = CrawlJob.STATUS_RUNNING;
        }
        sendCrawlStateChangeEvent(this.state, jobState);

        // A proper exit will change this value.
        this.sExit = CrawlJob.STATUS_FINISHED_ABNORMAL;
        
        Thread statLogger = new Thread(statistics);
        statLogger.setName("StatLogger");
        statLogger.start();
        
        frontier.unpause();
    }

    private void completeStop() {
        LOGGER.info("Entered complete stop.");
        // Run processors' final tasks
        runProcessorFinalTasks();
        // Ok, now we are ready to exit.
        sendCrawlStateChangeEvent(FINISHED, this.sExit);
        synchronized (this.registeredCrawlStatusListeners) {
            // Remove all listeners now we're done with them.
            this.registeredCrawlStatusListeners.
                removeAll(this.registeredCrawlStatusListeners);
        }
        
        closeLogFiles();
        
        // Release reference to logger file handler instances.
        this.fileHandlers = null;
        this.uriErrors = null;
        this.uriProcessing = null;
        this.localErrors = null;
        this.runtimeErrors = null;
        this.progressStats = null;

        // Do cleanup.
        this.statistics = null;
        this.frontier = null;
        this.disk = null;
        this.scratchDisk = null;
        this.toePool = null;
        this.order = null;
        this.scope = null;
        if (this.serverCache != null) {
            this.serverCache.cleanup();
            this.serverCache = null;
        }
        this.cpContext = null;
        if (this.classCatalogDB != null) {
            try {
                this.classCatalogDB.close();
            } catch (DatabaseException e) {
                e.printStackTrace();
            }
            this.classCatalogDB = null;
        }
        if (this.bdbEnvironment != null) {
            try {
            	// can't hurt, might make bdb droppings post-crawl
            	// more useful 
                this.bdbEnvironment.sync();
                if (this.jeRegisteredMBeanInstance != null) {
                    Heritrix.unregisterMBean(this.jeRegisteredMBeanInstance);
                }
                this.bdbEnvironment.close();
            } catch (DatabaseException e) {
                e.printStackTrace();
            }
            this.bdbEnvironment = null;
        }

        LOGGER.info("Finished crawl.");
    }

    private void completePause() {
        sendCrawlStateChangeEvent(PAUSED, CrawlJob.STATUS_PAUSED);
    }

    private boolean shouldContinueCrawling() {
        if (frontier.isEmpty()) {
            this.sExit = CrawlJob.STATUS_FINISHED;
            return false;
        }

        if (maxBytes > 0 && frontier.totalBytesWritten() >= maxBytes) {
            // Hit the max byte download limit!
            sExit = CrawlJob.STATUS_FINISHED_DATA_LIMIT;
            return false;
        } else if (maxDocument > 0
                && frontier.succeededFetchCount() >= maxDocument) {
            // Hit the max document download limit!
            this.sExit = CrawlJob.STATUS_FINISHED_DOCUMENT_LIMIT;
            return false;
        } else if (maxTime > 0 &&
                statistics.crawlDuration() >= maxTime * 1000) {
            // Hit the max byte download limit!
            this.sExit = CrawlJob.STATUS_FINISHED_TIME_LIMIT;
            return false;
        }
        return state == RUNNING;
    }

    /**
     * Request a checkpoint.
     * @throws IllegalStateException Thrown if crawl is not in paused state
     * (Crawl must be first paused before checkpointing).
     */
    public synchronized void requestCrawlCheckpoint()
    throws IllegalStateException {
        if (state != PAUSED) {
            throw new IllegalStateException("Pause crawl before requesting " +
                "checkpoint.");
        }
        this.state = CHECKPOINTING;
        new Thread(new CheckpointTask()).start();
    }
    
    /**
     * @author gojomo
     */
    public class CheckpointTask implements Runnable {
        public void run() {
            CrawlController.this.writeCheckpoint();
        }
    }

    /**
     * Write a checkpoint to disk.
     */
    protected void writeCheckpoint() {
        cpContext.begin();
        try {
            this.checkpointTo(cpContext);
        } catch (Exception e) {
            cpContext.checkpointFailed(e);
        } finally {
            cpContext.end();
            this.state = PAUSED;
        }
    }

    /**
     * Run checkpointing.
     * <p>TODO: Serialize settings.  Or, I suppose these are already serialized.
     * Copy settings to the checkpoint dir.
     * <p>TODO: Turn recover log off by default when working?
     * @param context Context to use checkpointing.
     * @throws Exception
     */
    public void checkpointTo(CheckpointContext context)
    throws Exception {
        long started = System.currentTimeMillis();
        sendCheckpointEvent(context.getCheckpointInProgressDirectory());
        
        // Sync the BigMap contents to disk.
        LOGGER.info("BigMaps.");
        BigMapFactory.checkpoint();
        
        // Checkpoint bdb environment.
        LOGGER.info("Bdb environment.");
        checkpointBdb(context.getCheckpointInProgressDirectory());
        
        // Rotate off logs.
        LOGGER.info("Rotating log files.");
        rotateLogFiles(CURRENT_LOG_SUFFIX + "."
            + this.cpContext.getNextCheckpointName());
        // TODO: Copy logs to checkpoint dir.
        
        // Serialize the checkpoint context.
        CheckpointContext.writeObjectToFile(this.cpContext,
            context.getCheckpointInProgressDirectory());

        LOGGER.info("Finished: " +
            (context.isCheckpointFailed()? "Failed": "Succeeded") + ", Took " +
            (System.currentTimeMillis() - started) + "ms.");
        
        // Set crawler back into paused mode.
        completePause();
    }
    
    /**
     * Checkpoint bdb.
     * @param checkpointDir Directory to write checkpoint to.
     * @throws DatabaseException 
     * @throws IOException 
     */
    protected void checkpointBdb(File checkpointDir)
    throws DatabaseException, IOException {
        // Do a full bdb sync of all in cache to disk. Then checkpoint.
        // From 'Chapter 8. Backing up and Restoring Berkeley DB Java
        // Edition Applications'  This will clean up logs.
        this.bdbEnvironment.sync();
        // Below log cleaning looped suggested in je-2.0 javadoc.
        int totalCleaned = 0;
        for (int cleaned = 0; (cleaned = this.bdbEnvironment.cleanLog()) != 0;
                totalCleaned += cleaned) {
            LOGGER.fine("Cleaned " + cleaned + " log files.");
        }
        LOGGER.info("Cleaned out " + totalCleaned + " log files total.");
        // Pass null. Uses default values.
        this.bdbEnvironment.checkpoint(null);
        // Copy off the bdb log files. Copy them in order.
        FilenameFilter filter = CheckpointContext.getJeLogsFilter();
        FileUtils.copyFiles(getStateDisk(), filter,
            CheckpointContext.getBdbSubDirectory(checkpointDir), true);
        // Go again in case new bdb logs were added since above
        // bulk copy.  Keep cycling till two dirs match.
        List outstanding = null;
        do {
            List src = Arrays.asList(getStateDisk().list(filter));
            List tgt = Arrays.asList(CheckpointContext.
                getBdbSubDirectory(checkpointDir).list(filter));
            outstanding =  new ArrayList();
            for (Iterator i = src.iterator(); i.hasNext();) {
                Object obj = i.next();
                if (!tgt.contains(obj)) {
                    outstanding.add(obj);
                }
            }
            if (outstanding.size() > 0) {
                LOGGER.fine("Copying " + outstanding.size() +
                    " new bdb log files.");
                FileUtils.copyFiles(
                    CheckpointContext.getBdbSubDirectory(checkpointDir),
                    outstanding, getStateDisk());
            }
        } while (outstanding.size() > 0);
    }
    
    /**
     * Get recover checkpoint.
     * Returns null if we're NOT in recover mode.
     * Looks at ATTR_RECOVER_PATH and if its a directory, assumes checkpoint
     * recover. If checkpoint mode, returns deserialized Checkpoint instance if
     * checkpoint was VALID.
     * @return Deserialized Checkpoint instance if we're in recover checkpoint
     * mode and the pointed-to checkpoint was valid.
     */
    public synchronized Checkpoint getCheckpointRecover() {
        if (this.checkpointRecover != null) {
            return this.checkpointRecover;
        }
        String path = (String)order.getUncheckedAttribute(null,
            CrawlOrder.ATTR_RECOVER_PATH);
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
    
    /**
     * @return True if this CrawlJob was made of a checkpoint recover.
     */
    public boolean isCheckpointRecover() {
        return this.checkpointRecover != null;
    }

    /**
     * Operator requested for crawl to stop.
     */
    public synchronized void requestCrawlStop() {
        requestCrawlStop(CrawlJob.STATUS_ABORTED);
    }
    
    /**
     * Operator requested for crawl to stop.
     * @param message 
     */
    public synchronized void requestCrawlStop(String message) {
        if (state == STOPPING || state == FINISHED) {
            return;
        }
        if (message == null) {
            throw new IllegalArgumentException("Message cannot be" +
                " null.");
        }
        this.sExit = message;
        beginCrawlStop();
    }

    /**
     * Start the process of stopping the crawl. 
     */
    public void beginCrawlStop() {
        LOGGER.info("Starting beginCrawlStop()...");
        sendCrawlStateChangeEvent(STOPPING, this.sExit);
        frontier.terminate();
        frontier.unpause();
        LOGGER.info("Finished beginCrawlStop()."); 
    }
    
    /**
     * Stop the crawl temporarly.
     */
    public synchronized void requestCrawlPause() {
        if (state == PAUSING || state == PAUSED) {
            // Already about to pause
            return;
        }
        sExit = CrawlJob.STATUS_WAITING_FOR_PAUSE;
        frontier.pause();
        sendCrawlStateChangeEvent(PAUSING, this.sExit);
    }

    /**
     * Tell if the controller is paused
     * @return true if paused
     */
    public boolean isPaused() {
        return state == PAUSED;
    }

    /**
     * Resume crawl from paused state
     */
    public synchronized void requestCrawlResume() {
        if (state != PAUSING && state != PAUSED) {
            // Can't resume if not been told to pause
            return;
        }
        multiThreadMode();
        frontier.unpause();
        LOGGER.info("Crawl resumed.");
        sendCrawlStateChangeEvent(RUNNING, CrawlJob.STATUS_RUNNING);
    }

    /**
     * @return Active toe thread count.
     */
    public int getActiveToeCount() {
        if (toePool==null) {
            return 0;
        }
        return toePool.getActiveToeCount();
    }

    private void setupToePool() {
        toePool = new ToePool(this);
        // TODO: make # of toes self-optimizing
        toePool.setSize(order.getMaxToes());
    }

    /**
     * @return The order file instance.
     */
    public CrawlOrder getOrder() {
        return order;
    }

    /**
     * @return The server cache instance.
     */
    public ServerCache getServerCache() {
        return serverCache;
    }

    /**
     * @param o
     */
    public void setOrder(CrawlOrder o) {
        order = o;
    }


    /**
     * @return The frontier.
     */
    public Frontier getFrontier() {
        return frontier;
    }

    /**
     * @return This crawl scope.
     */
    public CrawlScope getScope() {
        return scope;
    }

    /** Get the list of processor chains.
     *
     * @return the list of processor chains.
     */
    public ProcessorChainList getProcessorChainList() {
        return processorChains;
    }

    /** Get the first processor chain.
     *
     * @return the first processor chain.
     */
    public ProcessorChain getFirstProcessorChain() {
        return processorChains.getFirstChain();
    }

    /** Get the postprocessor chain.
     *
     * @return the postprocessor chain.
     */
    public ProcessorChain getPostprocessorChain() {
        return processorChains.getLastChain();
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
        return toePool.getToeCount();
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
        toePool.setSize(order.getMaxToes());
        
        this.scope.kickUpdate();
        this.frontier.kickUpdate();
        
        // TODO: continue to generalize this, so that any major 
        // component can get a kick when it may need to refresh its data

        setThresholds();
    }

	/**
     * @return The settings handler.
     */
    public SettingsHandler getSettingsHandler() {
        return settingsHandler;
    }

    /**
     * This method iterates through processor chains to run processors' initial
     * tasks.
     *
     */
    private void runProcessorInitialTasks(){
        for (Iterator ic = processorChains.iterator(); ic.hasNext(); ) {
            for (Iterator ip = ((ProcessorChain) ic.next()).iterator();
                    ip.hasNext(); ) {
                ((Processor) ip.next()).initialTasks();
            }
        }
    }

    /**
     * This method iterates through processor chains to run processors' final
     * tasks.
     *
     */
    private void runProcessorFinalTasks(){
        for (Iterator ic = processorChains.iterator(); ic.hasNext(); ) {
            for (Iterator ip = ((ProcessorChain) ic.next()).iterator();
                    ip.hasNext(); ) {
                ((Processor) ip.next()).finalTasks();
            }
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
        return state == RUNNING && !shouldContinueCrawling();
    }

    // custom serialization
    private void writeObject(ObjectOutputStream stream) throws IOException {
        stream.defaultWriteObject();
        ObjectPlusFilesOutputStream opfos = (ObjectPlusFilesOutputStream)stream;
        // TODO: snapshot logs
        opfos.pushAuxiliaryDirectory("logs");
        List allLogs = getAllLogFilenames();
        opfos.writeInt(allLogs.size());
        Iterator iter = allLogs.iterator();
        while (iter.hasNext()) {
            opfos.snapshotAppendOnlyFile(new File((String) iter.next()));
        }
        opfos.popAuxiliaryDirectory();
        // TODO someday: snapshot on-disk settings/overrides/refinements
    }

    private List getAllLogFilenames() {
        LinkedList names = new LinkedList();
        Iterator iter = fileHandlers.keySet().iterator();
        while (iter.hasNext()) {
            Logger l = (Logger) iter.next();
            GenerationFileHandler gfh = (GenerationFileHandler) fileHandlers
                    .get(l);
            Iterator logFiles = gfh.getFilenameSeries().iterator();
            while (logFiles.hasNext()) {
                names.add(logFiles.next());
            }
        }
        return names;
    }

    private void readObject(ObjectInputStream stream)
    throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        ObjectPlusFilesInputStream opfis = (ObjectPlusFilesInputStream)stream;
        // restore logs
        int totalLogs = opfis.readInt();
        opfis.pushAuxiliaryDirectory("logs");
        for(int i = 1; i <= totalLogs; i++) {
            opfis.restoreFileTo(logsDisk);
        }
        opfis.popAuxiliaryDirectory();
        // ensure disk CrawlerSettings data is loaded
        // settingsHandler.initialize();

        // setup status listeners
        this.registeredCrawlStatusListeners =
            Collections.synchronizedList(new ArrayList());
    }

    /**
     * Go to single thread mode, where only one ToeThread may
     * proceed at a time. Also acquires the single lock, so 
     * no further threads will proceed past an 
     * acquireContinuePermission. Caller mush be sure to release
     * lock to allow other threads to proceed one at a time. 
     */
    public void singleThreadMode() {
        try {
            SINGLE_THREAD_LOCK.acquire();
            singleThreadMode = true; 
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Go to back to regular multi thread mode, where all
     * ToeThreads may proceed at once
     */
    public void multiThreadMode() {
        try {
            SINGLE_THREAD_LOCK.acquire();
            singleThreadMode = false; 
            SINGLE_THREAD_LOCK.release(SINGLE_THREAD_LOCK.holds());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
    /**
     * Proceed only if allowed, giving CrawlController a chance
     * to enforce single-thread mode.
     *  
     * @throws InterruptedException
     */
    public void acquireContinuePermission() throws InterruptedException {
        if(singleThreadMode) {
            SINGLE_THREAD_LOCK.acquire();
            if(!singleThreadMode) {
                // if changed while waiting, ignore
                SINGLE_THREAD_LOCK.release(SINGLE_THREAD_LOCK.holds());
            }
        } // else, permission is automatic
    }

    /**
     * Relinquish continue permission at end of processing (allowing
     * another thread to proceed if in single-thread mode). 
     */
    public void releaseContinuePermission() {
        if(singleThreadMode) {
            if(SINGLE_THREAD_LOCK.holds()>0) {
                SINGLE_THREAD_LOCK.release(SINGLE_THREAD_LOCK.holds()); // release all
            }
        } // else do nothing; 
    }
    
    /**
     * 
     */
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
        if (state ==  PAUSING && toePool.getActiveToeCount()==0) {
            completePause();
        }
    }
    
    /**
     * Note that a ToeThread ended, possibly completing the crawl-stop. 
     */
    public synchronized void toeEnded() {
        if (state == STOPPING && toePool.getActiveToeCount() == 0) {
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
        for (Iterator it = getSettingsHandler().getListOfAllFiles().iterator();
                it.hasNext();) {
            addToManifest((String)it.next(),
                CrawlController.MANIFEST_CONFIG_FILE, true);
        }
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
        if(e.getReasonCode()==UURIFactory.IGNORED_SCHEME) {
            // don't log those that are intentionally ignored
            return; 
        }
        Object[] array = {u, l};
        uriErrors.log(Level.INFO, e.getMessage(),array);
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

    /* (non-Javadoc)
     * @see org.archive.util.Reporter#singleLineReport()
     */
    public String singleLineReport() {
        return ArchiveUtils.singleLineReport(this);
    }

    /* (non-Javadoc)
     * @see org.archive.util.Reporter#reportTo(java.lang.String, java.io.Writer)
     */
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
     * @param writer
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
        writer.print("  Job being crawled:    " + getOrder().getCrawlOrderName()
                + "\n");

        writer.print("  Number of Processors: " +
            processorChains.processorCount() + "\n");
        writer.print("  NOTE: Some processors may not return a report!\n\n");

        for (Iterator ic = processorChains.iterator(); ic.hasNext(); ) {
            for (Iterator ip = ((ProcessorChain) ic.next()).iterator();
                    ip.hasNext(); ) {
                writer.print(((Processor) ip.next()).report());
            }
        }
    }

    public void singleLineReportTo(PrintWriter writer) {
        writer.write("[Crawl Controller]\n");
    }
}
