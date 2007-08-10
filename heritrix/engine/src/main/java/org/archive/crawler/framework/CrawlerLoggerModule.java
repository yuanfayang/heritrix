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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.httpclient.URIException;
import org.archive.crawler.io.LocalErrorFormatter;
import org.archive.crawler.io.RuntimeErrorFormatter;
import org.archive.crawler.io.StatisticsLogFormatter;
import org.archive.crawler.io.UriErrorFormatter;
import org.archive.crawler.io.UriProcessingFormatter;
import org.archive.io.GenerationFileHandler;
import org.archive.modules.LoggerModule;
import org.archive.net.UURI;
import org.archive.net.UURIFactory;
import org.archive.state.FileModule;
import org.archive.settings.RecoverAction;
import org.archive.settings.file.Checkpointable;
import org.archive.state.Immutable;
import org.archive.state.Initializable;
import org.archive.state.Key;
import org.archive.state.KeyManager;
import org.archive.state.StateProvider;

/**
 * @author pjack
 *
 */
public class CrawlerLoggerModule 
implements LoggerModule, Initializable, Checkpointable {


    private static final long serialVersionUID = 1L;

    @Immutable
    public final static Key<FileModule> DIR = 
        Key.make(FileModule.class, null);

    
    static {
        KeyManager.addKeys(CrawlerLoggerModule.class);
    }
    
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

    /** suffix to use on active logs */
    public static final String CURRENT_LOG_SUFFIX = ".log";
    
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
     * This logger is for job-scoped logging, specifically errors which
     * happen and are handled within a particular processor.
     *
     * Examples would be socket timeouts, exceptions thrown by extractors, etc.
     */
    private transient Logger localErrors;

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


    private FileModule dir;

    
    private StringBuffer manifest = new StringBuffer();

    public CrawlerLoggerModule() {
    }

    
    public void initialTasks(StateProvider provider) {
        dir = provider.get(this, DIR);
        dir.getFile().mkdirs();
        try {
            setupLogs();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
    
    
    private void setupLogs() throws IOException {
        String logsPath = dir.getFile().getAbsolutePath() + File.separatorChar;
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
        this.loggers.put(logger.getName(), logger);
    }

    
    protected void rotateLogFiles(String generationSuffix)
    throws IOException {
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
        rotateLogFiles(CURRENT_LOG_SUFFIX + "." + checkpointDir.getName());
//            this.checkpointer.getNextCheckpointName());
    }


    
    
    public Logger getLogger(String name) {
        return loggers.get(name);
    }


    public File getLogsDir() {
        return dir.getFile();
    }


    public Logger getLocalErrors() {
        return localErrors;
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
        dir.getFile().mkdirs();        
        this.setupLogs();
    }
    
}
