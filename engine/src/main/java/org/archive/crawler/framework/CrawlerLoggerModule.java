/* 
 * Copyright (C) 2007 Internet Archive.
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
 * CrawlerLoggerModule.java
 *
 * Created on Mar 16, 2007
 *
 * $Id:$
 */

package org.archive.crawler.framework;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.apache.commons.httpclient.URIException;
import org.archive.crawler.io.NonFatalErrorFormatter;
import org.archive.crawler.io.RuntimeErrorFormatter;
import org.archive.crawler.io.StatisticsLogFormatter;
import org.archive.crawler.io.UriErrorFormatter;
import org.archive.crawler.io.UriProcessingFormatter;
import org.archive.crawler.util.Logs;
import org.archive.io.GenerationFileHandler;
import org.archive.modules.extractor.UriErrorLoggerModule;
import org.archive.net.UURI;
import org.archive.net.UURIFactory;
import org.archive.openmbeans.annotations.Bean;
import org.archive.settings.RecoverAction;
import org.archive.settings.file.Checkpointable;
import org.archive.spring.ConfigPath;
import org.archive.util.ArchiveUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.Lifecycle;

/**
 * @author pjack
 *
 */
public class CrawlerLoggerModule 
    extends Bean  
    implements 
        UriErrorLoggerModule, AlertTracker, Lifecycle, InitializingBean,
        Checkpointable {
    private static final long serialVersionUID = 1L;

    protected ConfigPath path = new ConfigPath(EngineImpl.LOGS_DIR_NAME,"logs"); 
    public ConfigPath getPath() {
        return path;
    }
    public void setPath(ConfigPath path) {
        this.path = path.merge(this.path);
    }
    
    // manifest support
    /** abbrieviation label for config files in manifest */
    public static final char MANIFEST_CONFIG_FILE = 'C';
    /** abbrieviation label for report files in manifest */
    public static final char MANIFEST_REPORT_FILE = 'R';
    /** abbrieviation label for log files in manifest */
    public static final char MANIFEST_LOG_FILE = 'L';

    
    // key log names
    private static final String LOGNAME_CRAWL = "crawl";
    private static final String LOGNAME_ALERTS = "alerts";
    private static final String LOGNAME_PROGRESS_STATISTICS =
        "progress-statistics";
    private static final String LOGNAME_URI_ERRORS = "uri-errors";
    private static final String LOGNAME_RUNTIME_ERRORS = "runtime-errors";
    private static final String LOGNAME_NONFATAL_ERRORS = "nonfatal-errors";


    protected ConfigPath crawlLogPath = 
        new ConfigPath(Logs.CRAWL.getFilename(),Logs.CRAWL.getFilename()); 
    public ConfigPath getCrawlLogPath() {
        return crawlLogPath;
    }
    public void setCrawlLogPath(ConfigPath cp) {
        this.crawlLogPath = cp.merge(this.crawlLogPath);
    }
    
    protected ConfigPath alertsLogPath = 
        new ConfigPath(Logs.ALERTS.getFilename(),Logs.ALERTS.getFilename()); 
    public ConfigPath getAlertsLogPath() {
        return alertsLogPath;
    }
    public void setAlertsLogPath(ConfigPath cp) {
        this.alertsLogPath = cp.merge(this.alertsLogPath);
    }
    
    protected ConfigPath progressLogPath = 
        new ConfigPath(Logs.PROGRESS_STATISTICS.getFilename(),Logs.PROGRESS_STATISTICS.getFilename()); 
    public ConfigPath getProgressLogPath() {
        return progressLogPath;
    }
    public void setProgressLogPath(ConfigPath cp) {
        this.progressLogPath = cp.merge(this.progressLogPath);
    }
    
    protected ConfigPath uriErrorsLogPath = 
        new ConfigPath(Logs.URI_ERRORS.getFilename(),Logs.URI_ERRORS.getFilename()); 
    public ConfigPath getUriErrorsLogPath() {
        return uriErrorsLogPath;
    }
    public void setUriErrorsLogPath(ConfigPath cp) {
        this.uriErrorsLogPath = cp.merge(this.uriErrorsLogPath);
    }
    
    protected ConfigPath runtimeErrorsLogPath = 
        new ConfigPath(Logs.RUNTIME_ERRORS.getFilename(),Logs.RUNTIME_ERRORS.getFilename()); 
    public ConfigPath getRuntimeErrorsLogPath() {
        return runtimeErrorsLogPath;
    }
    public void setRuntimeErrorsLogPath(ConfigPath cp) {
        this.runtimeErrorsLogPath = cp.merge(this.runtimeErrorsLogPath);
    }
    
    protected ConfigPath nonfatalErrorsLogPath = 
        new ConfigPath(Logs.NONFATAL_ERRORS.getFilename(),Logs.NONFATAL_ERRORS.getFilename()); 
    public ConfigPath getNonfatalErrorsLogPath() {
        return nonfatalErrorsLogPath;
    }
    public void setNonfatalErrorsLogPath(ConfigPath cp) {
        this.nonfatalErrorsLogPath = cp.merge(this.nonfatalErrorsLogPath);
    }
    
    /** suffix to use on active logs */
//    public static final String CURRENT_LOG_SUFFIX = ".log";
    
    /**
     * Crawl progress logger.
     *
     * No exceptions.  Logs summary result of each url processing.
     */
    private transient Logger uriProcessing;

    /**
     * This logger contains unexpected runtime errors.
     *
     * Would contain errors trying to set up a job or failures inside
     * processors that they are not prepared to recover from.
     */
    private transient Logger runtimeErrors;

    /**
     * This logger is for job-scoped logging, specifically recoverable 
     * errors which happen and are handled within a particular processor.
     *
     * Examples would be socket timeouts, exceptions thrown by 
     * extractors, etc.
     */
    private transient Logger nonfatalErrors;

    /**
     * Special log for URI format problems, wherever they may occur.
     */
    private transient Logger uriErrors;

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
    private transient Logger reports;

    /**
     * Record of fileHandlers established for loggers,
     * assisting file rotation.
     */
    transient private Map<Logger,FileHandler> fileHandlers;

    transient private Map<String,Logger> loggers = new HashMap<String,Logger>();

    private StringBuffer manifest = new StringBuffer();
    
    private transient AlertThreadGroup atg;

    public CrawlerLoggerModule() {
        super(AlertTracker.class);
    }

    
    public void start() {
        if(isRunning) {
            return; 
        }
        getPath().getFile().mkdirs();
        this.atg = AlertThreadGroup.current();
        try {
            setupLogs();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        isRunning = true; 
    }
    
    boolean isRunning = false; 
    public boolean isRunning() {
        return this.isRunning; 
    }
    
    public void stop() {
        closeLogFiles();
        isRunning = false; 
    }
    
    private void setupLogs() throws IOException {
        String logsPath = getPath().getFile().getAbsolutePath() + File.separatorChar;
        uriProcessing = Logger.getLogger(LOGNAME_CRAWL + "." + logsPath);
        runtimeErrors = Logger.getLogger(LOGNAME_RUNTIME_ERRORS + "." +
            logsPath);
        nonfatalErrors = Logger.getLogger(LOGNAME_NONFATAL_ERRORS + "." + logsPath);
        uriErrors = Logger.getLogger(LOGNAME_URI_ERRORS + "." + logsPath);
        progressStats = Logger.getLogger(LOGNAME_PROGRESS_STATISTICS + "." +
            logsPath);

        this.fileHandlers = new HashMap<Logger,FileHandler>();
        setupLogFile(uriProcessing,
            getCrawlLogPath().getFile().getAbsolutePath(),
            new UriProcessingFormatter(), true);

        setupLogFile(runtimeErrors,
            getRuntimeErrorsLogPath().getFile().getAbsolutePath(),
            new RuntimeErrorFormatter(), true);

        setupLogFile(nonfatalErrors,
            getNonfatalErrorsLogPath().getFile().getAbsolutePath(),
            new NonFatalErrorFormatter(), true);

        setupLogFile(uriErrors,
            getUriErrorsLogPath().getFile().getAbsolutePath(),
            new UriErrorFormatter(), true);

        setupLogFile(progressStats,
            getProgressLogPath().getFile().getAbsolutePath(),
            new StatisticsLogFormatter(), true);

        setupAlertLog(logsPath);
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
        this.loggers.put(logger.getName(), logger);
    }
    
    
    private void setupAlertLog(String logsPath) throws IOException {
        Logger logger = Logger.getLogger(LOGNAME_ALERTS + "." + logsPath);
        String filename = getAlertsLogPath().getFile().getAbsolutePath();
        GenerationFileHandler fh = new GenerationFileHandler(filename, true, 
                true);
        fh.setFormatter(new SimpleFormatter());
        AlertThreadGroup.setCurrentHandler(fh);
        AlertHandler ah = new AlertHandler();
        ah.setLevel(Level.WARNING);
        ah.setFormatter(new SimpleFormatter());
        logger.addHandler(ah);
        addToManifest(filename, MANIFEST_LOG_FILE, true);
        logger.setUseParentHandlers(false);
        this.fileHandlers.put(logger, fh);
        this.loggers.put(logger.getName(), logger);
        
    }

    
    public void rotateLogFiles() throws IOException {
        rotateLogFiles("." + ArchiveUtils.get14DigitDate());
    }
    
    protected void rotateLogFiles(String generationSuffix)
    throws IOException {
        for (Logger l: fileHandlers.keySet()) {
            GenerationFileHandler gfh =
                (GenerationFileHandler)fileHandlers.get(l);
            GenerationFileHandler newGfh =
                gfh.rotate(generationSuffix, "");
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
       for (Logger l: fileHandlers.keySet()) {
            GenerationFileHandler gfh =
                (GenerationFileHandler)fileHandlers.get(l);
            gfh.close();
            l.removeHandler(gfh);
        }
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
     * Run checkpointing.
     * 
     * <p>Default access only to be called by Checkpointer.
     * @throws Exception
     */
    public void checkpoint(File checkpointDir, List<RecoverAction> actions) 
    throws IOException {
        // Rotate off crawler logs.
        rotateLogFiles("." + checkpointDir.getName());
//            this.checkpointer.getNextCheckpointName());
    }


    
    
    public Logger getLogger(String name) {
        return loggers.get(name);
    }


    public Logger getNonfatalErrors() {
        return nonfatalErrors;
    }


    public Logger getProgressStats() {
        return progressStats;
    }


    public Logger getReports() {
        return reports;
    }


    public Logger getRuntimeErrors() {
        return runtimeErrors;
    }


    public Logger getUriErrors() {
        return uriErrors;
    }


    public Logger getUriProcessing() {
        return uriProcessing;
    }

    
    public int getAlertCount() {
        if (atg != null) {
            return atg.getAlertCount();
        } else {
            return -1;
        }
    }
    
    
    public void resetAlertCount() {
        if (atg != null) {
            atg.resetAlertCount();
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
        if (e.getReasonCode() == UURIFactory.IGNORED_SCHEME) {  
            // don't log those that are intentionally ignored
            return;
        }
        Object[] array = {u, l};
        uriErrors.log(Level.INFO, e.getMessage(), array);
    }
    
    
    private void readObject(ObjectInputStream in) 
    throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        loggers = new HashMap<String,Logger>();
        getPath().getFile().mkdirs();
        this.atg = AlertThreadGroup.current();
        this.setupLogs();
    }
    
    
    public void afterPropertiesSet() throws Exception {
        ConfigPath[] paths = { 
                crawlLogPath, alertsLogPath, progressLogPath, 
                uriErrorsLogPath, runtimeErrorsLogPath, nonfatalErrorsLogPath };
        for(ConfigPath cp : paths) {
            if(cp.getBase()==null) {
                cp.setBase(getPath());
            }
        }
    }
}
