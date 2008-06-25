/* Copyright (C) 2006 Internet Archive.
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
 * Extractor.java
 * Created on October 5, 2006
 *
 * $Header$
 */
package org.archive.modules.extractor;


import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.httpclient.URIException;
import org.archive.crawler.framework.CrawlerLoggerModule;
import org.archive.modules.Processor;
import org.archive.modules.ProcessorURI;
import org.archive.net.UURI;
import org.archive.net.UURIFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;


/**
 * Extracts links from fetched URIs.  This class provides error handling
 * for some common issues that occur when parsing document content.  You
 * almost certainly want to subclass {@link ContentExtractor} instead of
 * this class.
 * 
 * @author pjack
 */
public abstract class Extractor extends Processor implements InitializingBean {


    /** Logger. */
    private static final Logger logger = 
        Logger.getLogger(Extractor.class.getName());

    protected CrawlerLoggerModule loggerModule;
    public CrawlerLoggerModule getLoggerModule() {
        return this.loggerModule;
    }
    @Autowired
    public void setLoggerModule(CrawlerLoggerModule loggerModule) {
        this.loggerModule = loggerModule;
    }
    
    public void afterPropertiesSet() {
//        this.uriErrors = global.get(this, URI_ERROR_LOGGER_MODULE);    
    }
    
    ExtractorHelper extractorHelper = new ExtractorHelper();
    public ExtractorHelper getExtractorHelper() {
        return extractorHelper;
    }
    @Autowired
    public void setExtractorHelper(ExtractorHelper helper) {
        this.extractorHelper = helper; 
    }
    
    /**
     * Processes the given URI.  This method just delegates to 
     * {@link #extract(ExtractorURI)}, catching runtime exceptions and
     * errors that are usually non-fatal, to highlight them in the 
     * relevant log(s). 
     * 
     * <p>Notably, StackOverflowError is caught here, as that seems to 
     * happen a lot when dealing with document parsing APIs.
     * 
     * @param uri  the URI to extract links from
     */
    final protected void innerProcess(ProcessorURI uri)
    throws InterruptedException {
        try {
            extract(uri);
        } catch (NullPointerException npe) {
            handleException(uri, npe);
        } catch (StackOverflowError soe) {
            handleException(uri, soe);
        } catch (java.nio.charset.CoderMalfunctionError cme) {
            // See http://sourceforge.net/tracker/index.php?func=detail&aid=1540222&group_id=73833&atid=539099
            handleException(uri, cme);
        }
    }
    
    
    private void handleException(ProcessorURI uri, Throwable t) {
        // both annotate (to highlight in crawl log) & add as local-error
        uri.getAnnotations().add("err=" + t.getClass().getName());
        uri.getNonFatalFailures().add(t);
        // also log as INFO
        // TODO: remove as redundant, given nonfatal logging?
        logger.log(Level.INFO, "Exception", t);        
    }


    /**
     * Extracts links from the given URI.  Subclasses should use 
     * {@link ExtractorURI#getInputStream()} or 
     * {@link ExtractorURI#getCharSequence()} to process the content of the
     * URI.  Any links that are discovered should be added to the
     * {@link ExtractorURI#getOutLinks()} set.
     * 
     * @param uri  the uri to extract links from
     */
    protected abstract void extract(ProcessorURI uri);

    
    public void logUriError(URIException e, UURI uuri, 
            CharSequence l) {
        if (e.getReasonCode() == UURIFactory.IGNORED_SCHEME) {
            // don't log those that are intentionally ignored
            return; 
        }
        loggerModule.logUriError(e, uuri, l);
    }

}
