/* CrawlerSettingsTest
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


/** Test the CrawlerSettings object
 *
 * @author John Erik Halse
 */
public class CrawlerSettingsTest extends SettingsFrameworkTestCase {

    /**
     * Constructor for CrawlerSettingsTest.
     * @param arg0
     */
    public CrawlerSettingsTest(String arg0) {
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

    final public void testAddComplexType() {
        ModuleType mod = new ModuleType("name");
        DataContainer data = getGlobalSettings().addComplexType(mod);
        assertNotNull(data);
    }

    final public void testGetModule() {
        ModuleType mod = new ModuleType("name");
        DataContainer data = getGlobalSettings().addComplexType(mod);
        assertSame(mod, getGlobalSettings().getModule("name"));
    }

}
