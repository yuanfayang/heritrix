/* XMLSettingsHandlerTest
 *
 * $Id$
 *
 * Created on Jan 28, 2004
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
import java.io.IOException;

import javax.management.Attribute;
import javax.management.AttributeNotFoundException;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanException;
import javax.management.ReflectionException;

import org.archive.crawler.datamodel.CrawlOrder;
import org.archive.crawler.framework.CrawlScope;

/**
 * Tests the handling of settings files.
 * 
 * @author John Erik Halse
 *  
 */
public class XMLSettingsHandlerTest extends SettingsFrameworkTestCase {

    /**
     * Constructor for XMLSettingsHandlerTest.
     * 
     * @param arg0
     */
    public XMLSettingsHandlerTest(String arg0) {
        super(arg0);
    }

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /*
     * Test for void writeSettingsObject(CrawlerSettings)
     */
    public void testWriteSettingsObjectCrawlerSettings()
            throws AttributeNotFoundException, InvalidAttributeValueException,
            MBeanException, ReflectionException, IOException {

        // Write a crawl order file
        CrawlerSettings settings = getGlobalSettings();
        XMLSettingsHandler handler = getSettingsHandler();
        handler.writeSettingsObject(settings);
        assertTrue("Order file was not written", getOrderFile().exists());

        // Get a module to alter a setting on
        ComplexType scope = settings.getModule(CrawlScope.ATTR_NAME);
        assertNotNull("Could not get module scope", scope);

        // Alter two settings in a per host file
        CrawlerSettings perHost = getPerHostSettings();
        String newSeeds = "newseed.txt";
        String newFrom = "newfrom";
        scope.setAttribute(perHost, new Attribute(CrawlScope.ATTR_SEEDS,
                newSeeds));
        CrawlOrder order = handler.getOrder();
        ComplexType httpHeaders = (ComplexType) order
                .getAttribute(CrawlOrder.ATTR_HTTP_HEADERS);
        httpHeaders.setAttribute(perHost, new Attribute(CrawlOrder.ATTR_FROM,
                newFrom));

        // Write the per host file
        handler.writeSettingsObject(perHost);
        assertTrue("Per host file was not written", handler.scopeToFile(
                perHost.getScope()).exists());

        // Create a new handler for testing that changes was written to disk
        XMLSettingsHandler newHandler = new XMLSettingsHandler(getOrderFile());
        newHandler.initialize();

        // Read perHost
        CrawlerSettings newPerHost = newHandler.getSettingsObject(perHost
                .getScope());
        assertNotNull("Per host scope could not be read", newPerHost);

        ComplexType newScope = newHandler.getModule(CrawlScope.ATTR_NAME);
        assertNotNull(newScope);
        String result = (String) newScope.getAttribute(newPerHost,
                CrawlScope.ATTR_SEEDS);
        assertEquals(result, newSeeds);

        ComplexType newHttpHeaders = (ComplexType) newHandler.getOrder()
                .getAttribute(newPerHost, CrawlOrder.ATTR_HTTP_HEADERS);
        assertNotNull(newHttpHeaders);

        result = (String) newHttpHeaders.getAttribute(newPerHost,
                CrawlOrder.ATTR_FROM);
        assertEquals(result, newFrom);
    }

    /**
     * Test the copying of the entire settings directory.
     * 
     * @throws IOException
     */
    public void testCopySettings() throws IOException {
        //String testScope = "www.archive.org";

        // Write the files
        XMLSettingsHandler handler = getSettingsHandler();
        handler.writeSettingsObject(getGlobalSettings());
        handler.writeSettingsObject(getPerHostSettings());

        // Copy to new location
        File newOrderFile = new File(getTmpDir(), "SETTINGS_new_order.xml");
        String newSettingsDir = "SETTINGS_new_per_host_settings";
        handler.copySettings(newOrderFile, newSettingsDir);

        // Check if new files where created.
        assertTrue("Order file was not written", newOrderFile.exists());

        assertTrue("New settings dir not set", handler.scopeToFile(
                getPerHostSettings().getScope()).getAbsolutePath().matches(
                ".*" + newSettingsDir + ".*"));
        assertTrue("Per host file was not written", handler.scopeToFile(
                getPerHostSettings().getScope()).exists());
    }

    public void testGetSettings() {
        XMLSettingsHandler handler = getSettingsHandler();
        CrawlerSettings order = handler.getSettingsObject(null);
        CrawlerSettings perHost = handler.getSettings("localhost.localdomain");
        assertNotNull("Didn't get any file", perHost);
        assertSame("Did not get same file", order, perHost);
    }

    public void testGetSettingsObject() {
        String testScope = "audio.archive.org";

        XMLSettingsHandler handler = getSettingsHandler();
        assertNotNull("Couldn't get orderfile", handler.getSettingsObject(null));
        assertNull("Got nonexisting per host file", handler
                .getSettingsObject(testScope));
        assertNotNull("Couldn't create per host file", handler
                .getOrCreateSettingsObject(testScope));
        assertNotNull("Couldn't get per host file", handler
                .getSettingsObject(testScope));
    }

    public void testDeleteSettingsObject() {
        XMLSettingsHandler handler = getSettingsHandler();
        File file = handler.scopeToFile(getPerHostSettings().getScope());
        handler.writeSettingsObject(getPerHostSettings());
        assertTrue("Per host file was not written", file.exists());
        handler.deleteSettingsObject(getPerHostSettings());
        assertFalse("Per host file was not deleted", file.exists());
    }
}
