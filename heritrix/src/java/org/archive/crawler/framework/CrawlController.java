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
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;
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
import javax.management.ReflectionException;

import org.apache.commons.httpclient.URIException;
import org.archive.crawler.Heritrix;
import org.archive.crawler.admin.Alert;
import org.archive.crawler.admin.CrawlJob;
import org.archive.crawler.admin.StatisticsTracker;
import org.archive.crawler.checkpoint.Checkpoint;
import org.archive.crawler.checkpoint.CheckpointContext;
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
import org.archive.util.ArchiveUtils;
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
public class CrawlController implements Serializable {
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

    // checkpoint support
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
    transient private List registeredCrawlStatusListeners;
    // Since there is a high probability that there will only ever by one
    // CrawlURIDispositionListner we will use this while there is only one:
    transient CrawlURIDispositionListener registeredCrawlURIDispositionListener;
    // And then switch to the array once there is more then one.
    transient protected ArrayList registeredCrawlURIDispositionListeners;

    /** Distinguished filename for this component in checkpoints.
     */
    public static final String DISTINGUISHED_FILENAME = "controller.ser";
    
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


    /**
     * default constructor
     */
    public CrawlController() {
        super();
        this.registeredCrawlStatusListeners =
            Collections.synchronizedList(new ArrayList());
        // defer most setup to initialize methods
    }

    /**
     * Starting from nothing, set up CrawlController and associated
     * classes to be ready for a first crawl.
     *
     * @param sH
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

            if (cpContext == null) {
                // Create if not already loaded
                cpContext = new CheckpointContext(checkpointsDisk);
            } else {
                // Note new begin point
                cpContext.noteResumed();
            }

            onFailMessage = "Unable to create log file(s)";
            setupLogs();

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
        } else {
            // Go through the list.
            if (registeredCrawlURIDispositionListeners != null
                && registeredCrawlURIDispositionListeners.size() > 0) {
                Iterator it = registeredCrawlURIDispositionListeners.iterator();
                while (it.hasNext()) {
                    (
                        (CrawlURIDispositionListener) it
                            .next())
                            .crawledURINeedRetry(
                        curi);
                }
            }
        }
    }

    /**
     * Allows an external class to raise a CrawlURIDispostion
     * crawledURIDisregard event that will be broadcast to all listeners that
     * have registered with the CrawlController.
     *
     * @param curi - The CrawlURI that will be sent with the event notification.
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
                // TODO: make recover path relative to job root dir
                String recoverPath =
                    (String)order.getAttribute(CrawlOrder.ATTR_RECOVER_PATH);
                if(recoverPath.length() > 0) {
                	boolean retainFailures = ((Boolean) order
							.getAttribute(CrawlOrder.ATTR_RECOVER_RETAIN_FAILURES))
							.booleanValue();
                    try {
                        frontier.importRecoverLog(recoverPath,retainFailures);
                    } catch (IOException e1) {
                        e1.printStackTrace();
                        throw (FatalConfigurationException) new FatalConfigurationException(
                            "Recover.log problem: " + e1).initCause(e1);
                    }
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
        if (loggers.isEmpty(null)) {
            // set up a default tracker
            if (statistics == null) {
                statistics = new StatisticsTracker("crawl-statistics");
            }
            loggers.addElement(null, (StatisticsTracker)statistics);
        }
        Iterator it = loggers.iterator(null);
        while (it.hasNext()) {
            StatisticsTracking tracker = (StatisticsTracking)it.next();
            tracker.initialize(this);
            if (statistics == null) {
                statistics = tracker;
            }
        }
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
     */
    public void rotateLogFiles() throws IOException {
        rotateLogFiles(CURRENT_LOG_SUFFIX + "." +
            ArchiveUtils.get14DigitDate());
    }

    /**
     * Cause all active log files to be closed and moved to filenames
     * ending with the (zero-padded to 5 places) generation suffix.
     * Resume logging to new files with the same active log names.
     *
     * @param generation
     * @throws IOException
     */
    public void rotateLogFiles(int generation) throws IOException {
        // TODO: Get formatted generation from checkpoint context. Its
        // already done the formatting and you may want to have a
        // checkpoint prefix.
        rotateLogFiles("." + (new DecimalFormat("00000")).format(generation));
    }
    
    public void rotateLogFiles(String generationSuffix)
    throws IOException {
        if (this.state != PAUSED) {
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
                    throw new RuntimeException("Unknown state: "+newState);
                }
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.fine("Sent " + newState + " to " + l);
                }
            }
            LOGGER.info("Sent " + newState);
        }
    }

    /**
     * Operator requested crawl begin
     */
    public void requestCrawlStart() {
        if(cpContext.isAtBeginning()) {
            runProcessorInitialTasks();
        }

        // Assume Frontier state already loaded.
        LOGGER.info("Starting crawl.");

        sendCrawlStateChangeEvent(STARTED, CrawlJob.STATUS_PENDING);
        String jobState;
        if(beginPaused) {
            state = PAUSED;
            jobState = CrawlJob.STATUS_PAUSED;
        } else {
            state = RUNNING;
            jobState = CrawlJob.STATUS_RUNNING;
        }
        sendCrawlStateChangeEvent(state, jobState);

        // A proper exit will change this value.
        this.sExit = CrawlJob.STATUS_FINISHED_ABNORMAL;

        // Start periodic background logging of crawl statistics.
        // TODO: Convert noteStart to be handled by statistics
        // capturing the 'STARTED' event.
        statistics.noteStart();
        
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
                this.bdbEnvironment.close();
            } catch (DatabaseException e) {
                e.printStackTrace();
            }
            this.bdbEnvironment = null;
        }

        LOGGER.info("Finished crawl.");
    }

    private void completePause() {
        LOGGER.info("Crawl paused.");
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
        state = CHECKPOINTING;
        new Thread(new CheckpointTask()).start();
    }

    /**
     * Operator requested for crawl to stop.
     */
    public synchronized void requestCrawlStop() {
        requestCrawlStop(CrawlJob.STATUS_ABORTED);
    }
    
    /**
     * Operator requested for crawl to stop.
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
        LOGGER.info("Pausing crawl...");
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
		return toePool.oneLineReport();
	}
    /**
     * @return Compiles and returns a human readable report on the
     * ToeThreads in it's ToePool.
     */
    public String reportThreads() {
        return toePool.report();
    }

    /**
     * Compiles and returns a human readable report on the active processors.
     * @return human readable report on the active processors.
     *
     * @see org.archive.crawler.framework.Processor#report()
     */
    public String reportProcessors() {
        StringBuffer rep = new StringBuffer();
        rep.append(
            "Processors report - "
                + ArchiveUtils.TIMESTAMP12.format(new Date())
                + "\n");
        rep.append("  Job being crawled:    " + getOrder().getCrawlOrderName()
                + "\n");

        rep.append("  Number of Processors: " + processorChains.processorCount()
                + "\n");
        rep.append("  NOTE: Some processors may not return a report!\n\n");

        for (Iterator ic = processorChains.iterator(); ic.hasNext(); ) {
            for (Iterator ip = ((ProcessorChain) ic.next()).iterator();
                    ip.hasNext(); ) {
                rep.append(((Processor) ip.next()).report());
            }
        }

        return rep.toString();
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
        }
        cpContext.end();
        state = PAUSED;
    }

    /**
     * @param context Context to use checkpointing.
     * @throws Exception
     */
    public void checkpointTo(CheckpointContext context)
    throws Exception {
        Checkpoint checkpoint = new Checkpoint(context
                .getCheckpointInProgressDirectory());
        // TODO: Serialize settings, write to disk all structures
        // that are still in memory (list of queues, cookies? What else?).
        //
        // TODO: Turn recover log off by default?
        //
        
        // Do a full bdb sync of all in cache to disk. Then checkpoint.
        // From 'Chapter 8. Backing up and Restoring Berkeley DB Java
        // Edition Applications'
        LOGGER.info("Started bdb sync: " + checkpoint);
        this.bdbEnvironment.sync();
        // Below log cleaning looped suggested in je-2.0 javadoc.
        int totalCleaned = 0;
        for (int cleaned = 0; (cleaned = this.bdbEnvironment.cleanLog()) != 0;
                totalCleaned += cleaned) {
            LOGGER.fine("Cleaned " + cleaned + " log files.");
        }
        LOGGER.info("Cleaned out " + totalCleaned + " log files.");
        // Pass null. Uses default values.
        this.bdbEnvironment.checkpoint(null);
        LOGGER.info("Rotating log files.");
        rotateLogFiles(context.getNextCheckpoint());
        checkpoint.writeObjectPlusToFile(this, DISTINGUISHED_FILENAME);
    }

    /**
     * @param resumeFrom
     * @return A CrawlController instance.
     * @throws InitializationException
     */
    public static CrawlController readFrom(Checkpoint resumeFrom)
            throws InitializationException {
        try {
            return 
                (CrawlController)resumeFrom.readObjectFromFile(DISTINGUISHED_FILENAME);
        } catch (IOException e) {
            e.printStackTrace();
            throw new InitializationException("unable to read controller", e);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            throw new InitializationException("unable to read controller", e);
        }
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

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
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
	 * @return Returns the current state of the manifest (Used at reporting
     * time).
	 */
	public String getManifest() {
		return manifest.toString();
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
}
