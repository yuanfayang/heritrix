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
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.AttributeNotFoundException;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanException;
import javax.management.ReflectionException;

import org.archive.crawler.Heritrix;
import org.archive.crawler.admin.Alert;
import org.archive.crawler.admin.CrawlJob;
import org.archive.crawler.admin.StatisticsTracker;
import org.archive.crawler.basic.Frontier;
import org.archive.crawler.datamodel.CandidateURI;
import org.archive.crawler.datamodel.CrawlOrder;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.ServerCache;
import org.archive.crawler.datamodel.UURI;
import org.archive.crawler.datamodel.settings.MapType;
import org.archive.crawler.datamodel.settings.SettingsHandler;
import org.archive.crawler.event.CrawlStatusListener;
import org.archive.crawler.event.CrawlURIDispositionListener;
import org.archive.crawler.framework.exceptions.FatalConfigurationException;
import org.archive.crawler.framework.exceptions.InitializationException;
import org.archive.crawler.io.LocalErrorFormatter;
import org.archive.crawler.io.PassthroughFormatter;
import org.archive.crawler.io.RuntimeErrorFormatter;
import org.archive.crawler.io.StatisticsLogFormatter;
import org.archive.crawler.io.UriErrorFormatter;
import org.archive.crawler.io.UriProcessingFormatter;
import org.archive.util.ArchiveUtils;

/**
 * CrawlController collects all the classes which cooperate to
 * perform a crawl, provides a high-level interface to the
 * running crawl, and executes the "master thread" which doles
 * out URIs from the Frontier to the ToeThreads.
 *
 * As the "global context" for a crawl, subcomponents will
 * usually reach each other through the CrawlController.
 *
 * @author Gordon Mohr
 */
public class CrawlController extends Thread {
    private static final String LOGNAME_PROGRESS_STATISTICS =
        "progress-statistics";
    private static final String LOGNAME_URI_ERRORS = "uri-errors";
    private static final String LOGNAME_RUNTIME_ERRORS = "runtime-errors";
    private static final String LOGNAME_LOCAL_ERRORS = "local-errors";
    private static final String LOGNAME_CRAWL = "crawl";
    private static final String LOGNAME_RECOVER = "recover";
    private static final String LOGNAME_REPORTS = "reports";

    private SettingsHandler settingsHandler;

    protected String sExit;

    /**
     * Comment for <code>DEFAULT_MASTER_THREAD_PRIORITY</code>
     */
    public static final int DEFAULT_MASTER_THREAD_PRIORITY =
        Thread.NORM_PRIORITY + 1;

    private int timeout = 1000;
    // to wait for CrawlURI from frontier before spinning
    private Thread controlThread;
    private ToePool toePool;
    private URIFrontier frontier;
    private boolean shouldCrawl;
    private boolean shouldPause;

    private File disk;
    private File scratchDisk;

    /**
     * Messges from the crawlcontroller.
     *
     * They appear on console.
     */
    private static Logger logger =
        Logger.getLogger("org.archive.crawler.framework.CrawlController");

    /**
     * Crawl progress logger.
     *
     * No exceptions.  Logs summary result of each url processing.
     */
    public Logger uriProcessing;

    /**
     * This logger contains unexpected runtime errors.
     *
     * Would contain errors trying to set up a job or failures inside
     * processors that they are not prepared to recover from.
     */
    public Logger runtimeErrors;

    /**
     * This logger is for job-scoped logging, specifically errors which
     * happen and are handled within a particular processor.
     *
     * Examples would be socket timeouts, exceptions thrown by extractors, etc.
     */
    public Logger localErrors;

    /**
     * Special log for URI format problems, wherever they may occur.
     */
    public Logger uriErrors;

    /**
     * Statistics tracker writes here at regular intervals.
     */
    public Logger progressStats;

    /**
     * Crawl replay logger.
     *
     * Currently captures Frontier/URI transitions but recovery is unimplemented.
     */
    public Logger recover;

    /**
     * Logger to hold job summary report.
     *
     * Large state reports made at infrequent intervals (e.g. job ending) go
     * here.
     */
    public Logger reports;

    // create a statistic tracking object and have it write to the log every
    protected StatisticsTracking statistics = null;

    protected ArrayList registeredCrawlStatusListeners;
    // Since there is a high probability that there will only ever by one
    // CrawlURIDispositionListner we will use this while there is only one:
    CrawlURIDispositionListener registeredCrawlURIDispositionListener;
    // And then switch to the array once there is more then one.
    protected ArrayList registeredCrawlURIDispositionListeners;

    CrawlOrder order;
    CrawlScope scope;

    private ProcessorChainList processorChains;

    int nextToeSerialNumber = 0;

    ServerCache serverCache;

    private boolean paused = false;
    private boolean finished = false;

    /**
     * 
     */
    public CrawlController() {
    }

    /**
     * Starting from nothing, set up CrawlController and associated
     * classes to be ready for crawling.
     *
     * @param settingsHandler
     * @throws InitializationException
     */
    public void initialize(SettingsHandler settingsHandler)
        throws InitializationException {
        this.settingsHandler = settingsHandler;
        order = settingsHandler.getOrder();
        order.setController(this);
        sExit = "";

        if (checkUserAgentAndFrom(order) == false) {
            String message = "You must set the User-Agent and From HTTP" +
            " header values to acceptable strings. \n" +
            " User-Agent: [software-name](+[info-url])[misc]\n" +
            " From: [email-address]";
            Heritrix.addAlert(
                new Alert(
                    "FatalConfigurationException on crawl: " 
                    + settingsHandler.getSettingsObject(null).getName(),
                    message,Level.CONFIG));
            throw new FatalConfigurationException(message);
        }

        try {
            setupDisk();
        } catch (FatalConfigurationException e) {
            Heritrix.addAlert(
                new Alert(
                    "FatalConfigurationException on crawl: " 
                    + settingsHandler.getSettingsObject(null).getName(),
                    "Unable to setup disk: \n" + e.toString(),e,Level.CONFIG));
            throw new InitializationException(
                "Unable to setup disk\n", e);
        } catch (AttributeNotFoundException e) {
            Heritrix.addAlert(
                new Alert(
                    "AttributeNotFoundException on crawl: " 
                    + settingsHandler.getSettingsObject(null).getName(),
                    "Unable to setup disk\n",e,Level.CONFIG));
            throw new InitializationException(
                "Unable to setup disk\n", e);
        }

        try {
            setupLogs();
        } catch (IOException e) {
            Heritrix.addAlert(
                new Alert(
                    "IOException on crawl: " 
                    + settingsHandler.getSettingsObject(null).getName(),
                     "Unable to create log file(s)\n",e,Level.CONFIG));
            throw new InitializationException(
                "Unable to create log file(s): " + e.toString(),
                e);
        }

        try {
            setupStatTracking();
        } catch (InvalidAttributeValueException e) {
            Heritrix.addAlert(
                new Alert(
                    "InvalidAttributeValueException on crawl: " 
                    + settingsHandler.getSettingsObject(null).getName(),
                     "Unable to setup statistics \n",e,Level.CONFIG));
            throw new InitializationException(
                "Unable to setup statistics: " + e.toString(), e);
        }


        try {
            setupCrawlModules();
        } catch (FatalConfigurationException e) {
            Heritrix.addAlert(
                new Alert(
                    "FatalConfigurationException on crawl: " 
                    + settingsHandler.getSettingsObject(null).getName(),
                     "Unable to setup crawl modules \n", e,Level.CONFIG));
            throw new InitializationException(
                "Unable to setup crawl modules: " + e.toString(), e);
        } catch (AttributeNotFoundException e) {
            Heritrix.addAlert(
                new Alert(
                    "AttributeNotFoundException on crawl: " 
                    + settingsHandler.getSettingsObject(null).getName(),
                     "Unable to setup crawl modules \n", e,Level.CONFIG));
            throw new InitializationException(
                "Unable to setup crawl modules: " + e.toString(), e);
        } catch (InvalidAttributeValueException e) {
            Heritrix.addAlert(
                new Alert(
                    "InvalidAttributeValueException on crawl: " 
                    + settingsHandler.getSettingsObject(null).getName(),
                     "Unable to setup crawl modules \n",e,Level.CONFIG));
            throw new InitializationException(
                "Unable to setup crawl modules: " + e.toString(), e);
        } catch (MBeanException e) {
            Heritrix.addAlert(
                new Alert(
                    "MBeanException on crawl: " 
                    + settingsHandler.getSettingsObject(null).getName(),
                     "Unable to setup crawl modules \n", e,Level.CONFIG));
            throw new InitializationException(
                "Unable to setup crawl modules: " + e.toString(), e);
        } catch (ReflectionException e) {
            Heritrix.addAlert(
                new Alert(
                    "ReflectionException on crawl: " 
                    + settingsHandler.getSettingsObject(null).getName(),
                     "Unable to setup crawl modules \n", e,Level.CONFIG));
            throw new InitializationException(
                "Unable to setup crawl modules: " + e.toString(), e);
        }
        
        setupToePool();
    }

    /**
     * Register for CrawlStatus events.
     *
     * @param cl a class implementing the CrawlStatusListener interface
     *
     * @see CrawlStatusListener
     */
    public void addCrawlStatusListener(CrawlStatusListener cl) {
        if (registeredCrawlStatusListeners == null) {
            registeredCrawlStatusListeners = new ArrayList();
        }
        registeredCrawlStatusListeners.add(cl);
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
    public void throwCrawledURISuccessfulEvent(CrawlURI curi) {
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
    public void throwCrawledURINeedRetryEvent(CrawlURI curi) {
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
    public void throwCrawledURIDisregardEvent(CrawlURI curi) {
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
    public void throwCrawledURIFailureEvent(CrawlURI curi) {
        if (registeredCrawlURIDispositionListener != null) {
            // Then we'll just use that.
            registeredCrawlURIDispositionListener.crawledURIFailure(curi);
        } else {
            // Go through the list.
            if (registeredCrawlURIDispositionListeners != null
                && registeredCrawlURIDispositionListeners.size() > 0) {
                Iterator it = registeredCrawlURIDispositionListeners.iterator();
                while (it.hasNext()) {
                    (
                        (CrawlURIDispositionListener) it
                            .next())
                            .crawledURIFailure(
                        curi);
                }
            }
        }
    }

    private void setupCrawlModules() throws FatalConfigurationException,
             AttributeNotFoundException, InvalidAttributeValueException,
             MBeanException, ReflectionException {
        scope = (CrawlScope) order.getAttribute(CrawlScope.ATTR_NAME);
        Object o = order.getAttribute(URIFrontier.ATTR_NAME);
        if (o instanceof URIFrontier) {
            frontier = (URIFrontier) o;
        } else {
            frontier = new Frontier(URIFrontier.ATTR_NAME);
            order.setAttribute((Frontier) frontier);
        }

        // try to initialize each scope and frontier from the config file
        //scope.initialize(this);
        try {
            frontier.initialize(this);
            
            String recoverPath = (String) order.getAttribute(CrawlOrder.ATTR_RECOVER_PATH);
            if(recoverPath.length()>0) {
                try {
                    frontier.importRecoverLog(recoverPath);
                } catch (IOException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                    throw new FatalConfigurationException("Recover.log problem: "+e1);
                }
            }
        } catch (IOException e) {
            throw new FatalConfigurationException(
                "unable to initialize frontier: " + e);
        }

        serverCache = new ServerCache(getSettingsHandler());

        // Setup processors
        processorChains = new ProcessorChainList(order);
    }

    private void setupDisk() throws FatalConfigurationException,
                                    AttributeNotFoundException {
        String diskPath
            = (String) order.getAttribute(null, CrawlOrder.ATTR_DISK_PATH);
        disk = getSettingsHandler().getPathRelativeToWorkingDirectory(diskPath);
        disk.mkdirs();

        String scratchDiskPath
            = (String) order.getAttribute(null, CrawlOrder.ATTR_SCRATCH_PATH);
        scratchDisk = new File(scratchDiskPath);
        if (!scratchDisk.isAbsolute()) {
            scratchDisk = new File(disk.getPath(), scratchDiskPath);
        }
        scratchDisk.mkdirs();
    }

    private void setupStatTracking() throws InvalidAttributeValueException {
        // the statistics object must be created before modules that use it if those
        // modules retrieve the object from the controller during initialization
        // (which some do).  So here we go with that.
        MapType loggers = order.getLoggers();
        if (loggers.isEmpty(null)) {
            // set up a default tracker
            loggers.addElement(null, new StatisticsTracker("crawl-statistics"));
        }
        Iterator it = loggers.iterator(null);
        while (it.hasNext()) {
            StatisticsTracking tracker = (StatisticsTracking) it.next();
            tracker.initalize(this);
            if (statistics == null && tracker instanceof StatisticsTracker) {
                statistics = tracker;
            }
        }
    }

    private void setupLogs() throws IOException {
        String diskPath = disk.getAbsolutePath() + File.separatorChar;

        uriProcessing = Logger.getLogger(LOGNAME_CRAWL+"."+diskPath);
        runtimeErrors = Logger.getLogger(LOGNAME_RUNTIME_ERRORS+"."+diskPath);
        localErrors = Logger.getLogger(LOGNAME_LOCAL_ERRORS+"."+diskPath);
        uriErrors = Logger.getLogger(LOGNAME_URI_ERRORS+"."+diskPath);
        progressStats = Logger.getLogger(LOGNAME_PROGRESS_STATISTICS+"."+diskPath);
        recover = Logger.getLogger(LOGNAME_RECOVER+"."+diskPath);
        reports = Logger.getLogger(LOGNAME_REPORTS+"."+diskPath);

        FileHandler up = new FileHandler(diskPath + LOGNAME_CRAWL + ".log");
        up.setFormatter(new UriProcessingFormatter());
        uriProcessing.addHandler(up);
        uriProcessing.setUseParentHandlers(false);

        FileHandler cerr =
            new FileHandler(diskPath + LOGNAME_RUNTIME_ERRORS + ".log");
        cerr.setFormatter(new RuntimeErrorFormatter());
        runtimeErrors.addHandler(cerr);
        runtimeErrors.setUseParentHandlers(false);

        FileHandler lerr =
            new FileHandler(diskPath + LOGNAME_LOCAL_ERRORS + ".log");
        lerr.setFormatter(new LocalErrorFormatter());
        localErrors.addHandler(lerr);
        localErrors.setUseParentHandlers(false);

        FileHandler uerr =
            new FileHandler(diskPath + LOGNAME_URI_ERRORS + ".log");
        uerr.setFormatter(new UriErrorFormatter());
        uriErrors.addHandler(uerr);
        uriErrors.setUseParentHandlers(false);

        FileHandler stat =
            new FileHandler(diskPath + LOGNAME_PROGRESS_STATISTICS + ".log");
        stat.setFormatter(new StatisticsLogFormatter());
        progressStats.addHandler(stat);
        progressStats.setUseParentHandlers(false);

        FileHandler reco = new FileHandler(diskPath + LOGNAME_RECOVER + ".log");
        reco.setFormatter(new PassthroughFormatter());
        recover.addHandler(reco);
        recover.setUseParentHandlers(false);

        FileHandler rep = new FileHandler(diskPath + LOGNAME_REPORTS + ".log");
        rep.setFormatter(new PassthroughFormatter());
        reports.addHandler(rep);
        reports.setUseParentHandlers(false);
        reports.setLevel(Level.INFO);
    }

    // must include a bot name and info URL
    private static String ACCEPTABLE_USER_AGENT =
        "\\S+.*\\(\\+http://\\S*\\).*";
    // must include a contact email address
    private static String ACCEPTABLE_FROM = "\\S+@\\S+\\.\\S+";

    /**
     * Checks if the User Agent and From field are set 'correctly' in
     * the specified Crawl Order.
     *
     * @param order The Crawl Order to check
     * @return true if it passes, false otherwise.
     */
    public static boolean checkUserAgentAndFrom(CrawlOrder order) {
        // don't start the crawl if they're using the default user-agent
        String userAgent = order.getUserAgent(null);
        String from = order.getFrom(null);
        return userAgent.matches(ACCEPTABLE_USER_AGENT)
            && from.matches(ACCEPTABLE_FROM);
    }

    /**
     * @param thread
     */
//    public void toeFinished(ToeThread thread) {
//        // for now do nothing
//    }

    /**
     * @return Object this controller is using to track crawl statistics
     */
    public StatisticsTracking getStatistics() {
        return statistics;
    }

    /**
     *
     */
    public void startCrawl() {
        // assume Frontier state already loaded
        shouldCrawl = true;
        shouldPause = false;
        logger.info("Should start Crawl");

        this.start();
    }

    /** (non-Javadoc)
     * @see java.lang.Thread#run()
     */
    public void run() {
        logger.fine(getName() + " started for CrawlController");
        sExit = CrawlJob.STATUS_FINISHED_ABNORMAL;
        // A proper exit will change this value.
        assert controlThread == null : "non-null control thread";
        controlThread = Thread.currentThread();
        controlThread.setName("crawlControl");
        controlThread.setPriority(DEFAULT_MASTER_THREAD_PRIORITY);

        // start periodic background logging of crawl statistics
        Thread statLogger = new Thread(statistics);
        statLogger.setName("StatLogger");
        statLogger.start();

        toePool.setShouldPause(false);
//        frontier.start();
        while (shouldCrawl()) {
            if (shouldPause) {
                pauseCrawl();
            }
            synchronized(this) {
                try {
                    wait(1000); // Wake in 1 sec to check if crawl is finished.
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        // Tell everyone that this crawl is ending (threads will take this to mean that they are to exit.
        Iterator iterator = registeredCrawlStatusListeners.iterator();
        while (iterator.hasNext()) {
            ((CrawlStatusListener) iterator.next()).crawlEnding(sExit);
        }

        // Wait for all ToeThreads to exit.
        while (getActiveToeCount() > 0 ) {
            synchronized(this){
                try {
                    wait(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        // Ok, now we are ready to exit.
        while (registeredCrawlStatusListeners.size() > 0) {
            // Let the listeners know that the crawler is finished.
            ((CrawlStatusListener)
                registeredCrawlStatusListeners.get(0)).crawlEnded(sExit);
            registeredCrawlStatusListeners.remove(0);
        }

        // Save processors report to file
        reports.info(reportProcessors());

        // Run processors' final tasks
        runProcessorFinalTasks();

        logger.info("exiting run");

        //Do cleanup to facilitate GC.
        controlThread = null;
        frontier = null;
        disk = null;
        scratchDisk = null;
        toePool = null;
        registeredCrawlStatusListeners = null;
        order = null;
        scope = null;
        serverCache = null;

        logger.fine(getName() + " finished for order CrawlController");
    }

    private synchronized void pauseCrawl() {
        Iterator iterator = registeredCrawlStatusListeners.iterator();

        // Wait until all ToeThreads are finished with their work
        // The crawlPausing() event should have told them to pause.
        while (getActiveToeCount() > 0 && shouldPause) {
            try {
                wait(200);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        
        if(shouldPause){
            paused = true;
            // Tell everyone that we have paused
            logger.info("Crawl job paused");
            iterator = registeredCrawlStatusListeners.iterator();
            while (iterator.hasNext()) {
                ((CrawlStatusListener) iterator.next()).crawlPaused(
                        CrawlJob.STATUS_PAUSED);
            }
            
            try {
                wait(); // resumeCrawl() will wake us.
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        
        // Been given an order to resume while waiting
        // to pause
        paused = false;
        toePool.setShouldPause(false);

        logger.info("Crawl job resumed");
        
        // Tell everyone that we have resumed from pause
        iterator = registeredCrawlStatusListeners.iterator();
        while (iterator.hasNext()) {
            ((CrawlStatusListener) iterator.next()).crawlResuming(
                    CrawlJob.STATUS_RUNNING);
        }
    }

    private boolean shouldCrawl() {
        boolean frontierEmpty = frontier.isEmpty();
        if (frontierEmpty) {
            sExit = CrawlJob.STATUS_FINISHED;
        }
        //if(order.getLongAt(XP_MAX_BYTES_DOWNLOAD,0) > 0 && frontier.totalBytesWritten()>= order.getLongAt(XP_MAX_BYTES_DOWNLOAD,0)) {
        long maxBytes = 0;
        try {
            maxBytes =
                ((Long) order.getAttribute(CrawlOrder.ATTR_MAX_BYTES_DOWNLOAD))
                    .longValue();
        } catch (Exception e) {
        }
        long maxDocument = 0;
        try {
            maxDocument =
                ((Long) order
                    .getAttribute(CrawlOrder.ATTR_MAX_DOCUMENT_DOWNLOAD))
                    .longValue();
        } catch (Exception e) {
        }
        long maxTime = 0;
        try {
            maxTime =
                ((Long) order.getAttribute(CrawlOrder.ATTR_MAX_TIME_SEC))
                    .longValue();
        } catch (Exception e) {
        }

        if (maxBytes > 0 && frontier.totalBytesWritten() >= maxBytes) {
            // Hit the max byte download limit!
            sExit = CrawlJob.STATUS_FINISHED_DATA_LIMIT;
            shouldCrawl = false;
        } else if (
            maxDocument > 0
                && frontier.successfullyFetchedCount() >= maxDocument) {
            // Hit the max document download limit!
            sExit = CrawlJob.STATUS_FINISHED_DOCUMENT_LIMIT;
            shouldCrawl = false;
        } else if (
            maxTime > 0 && statistics.crawlDuration() >= maxTime * 1000) {
            // Hit the max byte download limit!
            sExit = CrawlJob.STATUS_FINISHED_TIME_LIMIT;
            shouldCrawl = false;
        }
        return shouldCrawl && (frontier.isEmpty()==false);
    }

    /**
     * 
     */
    public synchronized void stopCrawl() {
        sExit = CrawlJob.STATUS_ABORTED;
        shouldCrawl = false;
        // If crawl is paused it should be resumed first so it
        // can be stopped properly
        resumeCrawl();
        notifyAll();
    }

    /**
     * Stop the crawl temporarly.
     */
    public synchronized void requestCrawlPause() {
        if (shouldPause) {
            // Already about to pause
            return;
        }
        sExit = CrawlJob.STATUS_WAITING_FOR_PAUSE;
        shouldPause = true;
        notifyAll();
        logger.info("Pausing crawl job ...");

        // Notify listeners that we are going to pause
        Iterator it = registeredCrawlStatusListeners.iterator();
        while (it.hasNext()) {
            ((CrawlStatusListener) it.next()).crawlPausing(sExit);
        }
    }

    /**
     * Tell if the controller is paused
     * @return true if paused
     */
    public boolean isPaused() {
        return paused;
    }

    /**
     * Resume crawl from paused state
     */
    public synchronized void resumeCrawl() {
        if (shouldPause==false) {
            // Can't resume if not been told to pause
            return;
        }

        shouldPause = false;
        notify();
    }

    /**
     * @return
     */
    public int getActiveToeCount() {
        return toePool.getActiveToeCount();
    }

    private void setupToePool() {
        toePool = new ToePool(this, order.getMaxToes());
    }

    /**
     * @return
     */
    public CrawlOrder getOrder() {
        return order;
    }

    /**
     * @return
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
    public URIFrontier getFrontier() {
        return frontier;
    }

    /**
     * @return
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
     * @return
     */
    public File getDisk() {
        return disk;
    }

    /**
     * @return
     */
    public File getScratchDisk() {
        return scratchDisk;
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
        getScope().refreshSeedsIteratorCache();
        Iterator iter = getScope().getSeedsIterator();
        while (iter.hasNext()) {
            UURI u = (UURI) iter.next();
            CandidateURI caUri = new CandidateURI(u);
            caUri.setSeed();
            caUri.setSchedulingDirective(CandidateURI.HIGH);
            frontier.schedule(caUri);
        }
    }

    /**
     * @return The settings handler.
     */
    public SettingsHandler getSettingsHandler() {
        return settingsHandler;
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
}