/* SettingsFrameworkTestCase
 * 
 * $Id$
 * 
 * Created on Feb 2, 2004
 *
 * Copyright (C) 2004 Internet Archive.
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
 */
package org.archive.crawler.datamodel.settings;

import java.io.File;

import javax.management.Attribute;

import org.archive.crawler.datamodel.CrawlOrder;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.ServerCache;
import org.archive.crawler.datamodel.UURI;
import org.archive.util.TmpDirTestCase;

/** Set up a couple of settings to test different functions of the settings
 * framework.
 * 
 * @author John Erik Halse
 */
public class SettingsFrameworkTestCase extends TmpDirTestCase {
    private File orderFile;
    private File settingsDir;
    private CrawlerSettings globalSettings;
    private CrawlerSettings perDomainSettings;
    private CrawlerSettings perHostSettings;
    private XMLSettingsHandler settingsHandler;
    private CrawlURI unMatchedURI;
    private CrawlURI matchDomainURI;
    private CrawlURI matchHostURI;

    /**
     * Constructor for SettingsFrameworkTestCase.
     * @param arg0
     */
    public SettingsFrameworkTestCase(String arg0) {
        super(arg0);
    }

    /*
     * @see TmpDirTestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        orderFile = new File(getTmpDir(), "SETTINGS_order.xml");
        String settingsDirName = "SETTINGS_per_host_settings";
        settingsDir = new File(orderFile, settingsDirName);
        settingsHandler = new XMLSettingsHandler(orderFile);
        settingsHandler.initialize();
        settingsHandler.getOrder().setAttribute(
          new Attribute(CrawlOrder.ATTR_SETTINGS_DIRECTORY, settingsDirName));

        globalSettings = settingsHandler.getSettingsObject(null);
        perDomainSettings = settingsHandler.getOrCreateSettingsObject("archive.org");
        perHostSettings = settingsHandler.getOrCreateSettingsObject("www.archive.org");

        ServerCache serverCache = new ServerCache(getSettingsHandler());
        
        unMatchedURI = new CrawlURI(UURI.createUURI("http://localhost.com/index.html"));
        unMatchedURI.setServer(serverCache.getServerFor(unMatchedURI));
        
        matchDomainURI = new CrawlURI(UURI.createUURI("http://audio.archive.org/index.html"));
        matchDomainURI.setServer(serverCache.getServerFor(matchDomainURI));
        
        matchHostURI = new CrawlURI(UURI.createUURI("http://www.archive.org/index.html"));
        matchHostURI.setServer(serverCache.getServerFor(matchHostURI));
    }

    /*
     * @see TmpDirTestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
        cleanUpOldFiles("SETTINGS");
    }

    /**
     * @return
     */
    public CrawlerSettings getGlobalSettings() {
        return globalSettings;
    }

    /**
     * @return
     */
    public CrawlerSettings getPerDomainSettings() {
        return perDomainSettings;
    }

    /**
     * @return
     */
    public CrawlerSettings getPerHostSettings() {
        return perHostSettings;
    }

    /**
     * @return
     */
    public XMLSettingsHandler getSettingsHandler() {
        return settingsHandler;
    }

    /**
     * @return
     */
    public File getOrderFile() {
        return orderFile;
    }

    /**
     * @return
     */
    public File getSettingsDir() {
        return settingsDir;
    }

    /**
     * @return
     */
    public CrawlURI getMatchDomainURI() {
        return matchDomainURI;
    }

    /**
     * @return
     */
    public CrawlURI getMatchHostURI() {
        return matchHostURI;
    }

    /**
     * @return
     */
    public CrawlURI getUnMatchedURI() {
        return unMatchedURI;
    }

}
