/* MapTypeTest
 *
 * $Id$
 *
 * Created on Jan 29, 2004
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

import java.util.Iterator;

import javax.management.AttributeNotFoundException;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.ReflectionException;

import org.archive.crawler.datamodel.CrawlOrder;
import org.archive.crawler.framework.Filter;

/** JUnit tests for MapType
 *
 * @author John Erik Halse
 *
 */
public class MapTypeTest extends SettingsFrameworkTestCase {

    /**
     * Constructor for MapTypeTest.
     * @param arg0
     */
    public MapTypeTest(String arg0) {
        super(arg0);
    }

    /*
     * @see TmpDirTestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
    }

    /*
     * @see TmpDirTestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /** Test different aspects of manipulating a MapType for the global
     * settings.
     *
     * @throws InvalidAttributeValueException
     * @throws AttributeNotFoundException
     */
    public void testAddRemoveSizeGlobal()
           throws InvalidAttributeValueException, AttributeNotFoundException, MBeanException, ReflectionException {

        MapType map = (MapType) getSettingsHandler().getOrder().getAttribute(CrawlOrder.ATTR_PRE_FETCH_PROCESSORS);

        assertTrue("Map should be empty", map.isEmpty(null));
        assertEquals("Map should be empty", map.size(null), 0);

        CrawlerModule module = new CrawlerModule("testModule");
        assertSame("Did not return added element", map.addElement(null, module), module);
        assertFalse("Map should contain a element", map.isEmpty(null));
        assertEquals("Map should contain a element", map.size(null), 1);

        assertSame("Did not return removed element", map.removeElement(null, "testModule"), module);
        assertTrue("Map should be empty", map.isEmpty(null));
        assertEquals("Map should be empty", map.size(null), 0);
    }

    /** Test different aspects of manipulating a MapType for the per domain
     * settings.
     *
     * @throws InvalidAttributeValueException
     * @throws AttributeNotFoundException
     * @throws MBeanException
     * @throws ReflectionException
     */
    public void testAddRemoveSizeHost()
           throws InvalidAttributeValueException, AttributeNotFoundException,
                  MBeanException, ReflectionException {

        MapType map = (MapType) getSettingsHandler().getOrder().getAttribute(CrawlOrder.ATTR_PRE_FETCH_PROCESSORS);

        assertTrue("Map should be empty", map.isEmpty(getPerHostSettings()));
        assertEquals("Map should be empty", 0, map.size(getPerHostSettings()));

        CrawlerModule module1 = new Filter("testModule1", "Desc1");
        CrawlerModule module2 = new Filter("testModule2", "Desc2");
        CrawlerModule module3 = new Filter("testModule3", "Desc3");

        assertSame("Did not return added element",
            map.addElement(getGlobalSettings(), module1), module1);

        assertSame("Did not return added element",
            map.addElement(getPerHostSettings(), module2), module2);

        assertSame("Did not return added element",
            map.addElement(getPerHostSettings(), module3), module3);

        assertFalse("Map should contain elements",
            map.isEmpty(getPerHostSettings()));
        assertEquals("Wrong number of elements", 3,
            map.size(getPerHostSettings()));
        assertEquals("Wrong number of elements", 1,
            map.size(getGlobalSettings()));

        module1.setAttribute(getPerHostSettings(), new SimpleType("enabled", "desc", new Boolean(false)));
        checkOrder(getGlobalSettings(), new Type[] {module1}, map);
        checkOrder(getPerHostSettings(), new Type[] {module1, module2, module3}, map);

        assertSame("Did not return removed element",
            map.removeElement(getGlobalSettings(), "testModule1"), module1);

        assertSame("Did not return removed element",
            map.removeElement(getPerHostSettings(), "testModule2"), module2);

        assertSame("Did not return removed element",
            map.removeElement(getPerHostSettings(), "testModule3"), module3);

        assertTrue("Map should be empty", map.isEmpty(getPerHostSettings()));
        assertEquals("Map should be empty", 0, map.size(getPerHostSettings()));
    }

    public void testMoveElementUp() throws AttributeNotFoundException, MBeanException, ReflectionException, InvalidAttributeValueException {
        MapType map = (MapType) getSettingsHandler().getOrder().getAttribute(CrawlOrder.ATTR_PRE_FETCH_PROCESSORS);

        CrawlerModule module1 = new CrawlerModule("testModule1");
        CrawlerModule module2 = new CrawlerModule("testModule2");
        CrawlerModule module3 = new CrawlerModule("testModule3");
        map.addElement(null, module1);
        map.addElement(null, module2);
        map.addElement(null, module3);

        Type modules[] = new Type[] {module1, module2, module3};
        checkOrder(null, modules, map);

        assertTrue(map.moveElementUp(null, "testModule2"));

        modules = new Type[] {module2, module1, module3};
        checkOrder(null, modules, map);

        assertFalse(map.moveElementUp(null, "testModule2"));

        modules = new Type[] {module2, module1, module3};
        checkOrder(null, modules, map);
    }

    public void testMoveElementDown() throws InvalidAttributeValueException, AttributeNotFoundException, MBeanException, ReflectionException {
        MapType map = (MapType) getSettingsHandler().getOrder().getAttribute(CrawlOrder.ATTR_PRE_FETCH_PROCESSORS);

        CrawlerModule module1 = new CrawlerModule("testModule1");
        CrawlerModule module2 = new CrawlerModule("testModule2");
        CrawlerModule module3 = new CrawlerModule("testModule3");
        map.addElement(null, module1);
        map.addElement(null, module2);
        map.addElement(null, module3);

        Type modules[] = new Type[] {module1, module2, module3};
        checkOrder(null, modules, map);

        assertTrue(map.moveElementDown(null, "testModule2"));

        modules = new Type[] {module1, module3, module2};
        checkOrder(null, modules, map);

        assertFalse(map.moveElementDown(null, "testModule2"));

        modules = new Type[] {module1, module3, module2};
        checkOrder(null, modules, map);
    }

    /** Helper method for checking that elements are in a certain order after
     * maipulating them.
     *
     * @param settings
     * @param modules
     * @param map
     * @throws AttributeNotFoundException
     * @throws MBeanException
     * @throws ReflectionException
     */
    public void checkOrder(CrawlerSettings settings, Type[] modules, MapType map)
           throws AttributeNotFoundException, MBeanException, ReflectionException {

        settings = settings == null ? map.globalSettings() : settings;

        MBeanAttributeInfo atts[] = map.getMBeanInfo(settings).getAttributes();
        assertEquals("AttributeInfo wrong length", modules.length, atts.length);
        for(int i=0; i<atts.length; i++) {
            assertSame("AttributeInfo in wrong order", modules[i],
                map.getAttribute(settings, atts[i].getName()));
        }

        Iterator it = map.iterator(settings);
        int i = 0;
        while(it.hasNext()) {
            assertSame("Iterator in wrong order", it.next(), modules[i]);
            i++;
        }
        assertEquals("Iterator wrong length", modules.length, i);
    }

    public void testGetDefaultValue() throws AttributeNotFoundException, MBeanException, ReflectionException {
        MapType map = (MapType) getSettingsHandler().getOrder().getAttribute(CrawlOrder.ATTR_PRE_FETCH_PROCESSORS);

        assertSame(map.getDefaultValue(), map);
    }

    public void testGetLegalValues() throws AttributeNotFoundException, MBeanException, ReflectionException {
        MapType map = (MapType) getSettingsHandler().getOrder().getAttribute(CrawlOrder.ATTR_PRE_FETCH_PROCESSORS);

        assertNull(map.getLegalValues());
    }

    /*
     * Test for Object getValue()
     */
    public void testGetValue() throws AttributeNotFoundException, MBeanException, ReflectionException {
        MapType map = (MapType) getSettingsHandler().getOrder().getAttribute(CrawlOrder.ATTR_PRE_FETCH_PROCESSORS);

        assertSame(map.getValue(), map);
    }

}
