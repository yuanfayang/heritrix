/* JmxRegistryHandlerTest
 * 
 * $Id$
 * 
 * Created on Jan 2, 2006
 *
 * Copyright (C) 2006 Internet Archive.
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
package org.archive.configuration;

import java.io.IOException;
import java.util.logging.Logger;

import javax.management.Attribute;
import javax.management.AttributeNotFoundException;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanException;
import javax.management.ReflectionException;
import javax.management.openmbean.OpenDataException;

import junit.framework.TestCase;

/**
 * @author stack
 * @version $Date$ $Revision$
 */
public class JmxRegistryHandlerTest extends TestCase {
    private static final Logger LOGGER =
        Logger.getLogger(JmxRegistryHandlerTest.class.getName());

    private static final String TEST_SETTING_NAME = "test";
    private static final String DOMAIN = "a.b.c";
    
    private Object baseInstanceReference = null;
    private Object domainInstanceReference = null;
    private Handler handler = null;

    public static void main(String[] args) {
        junit.textui.TestRunner.run(JmxRegistryHandlerTest.class);
    }

    protected void setUp() throws Exception {
        super.setUp();
        this.handler = new JmxRegistryHandler();       
        this.baseInstanceReference = this.handler.register(TEST_SETTING_NAME,
            null, getSettings(new Attribute(Configuration.ENABLED_ATTRIBUTE,
                Boolean.TRUE)));
        this.domainInstanceReference = this.handler.register(TEST_SETTING_NAME,
           DOMAIN, getSettings(new Attribute(Configuration.ENABLED_ATTRIBUTE,
               Boolean.FALSE)));
    }
    
    protected Configuration getSettings(final Attribute attr)
    throws AttributeNotFoundException, InvalidAttributeValueException,
            MBeanException, ReflectionException, OpenDataException {
        Configuration s =  new Configuration() {
            // Override because Settings is abstract.
        };
        s.setAttribute(attr);
        return s;
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        if (this.handler !=  null && this.baseInstanceReference != null) {
            this.handler.unregister(this.baseInstanceReference);
        }
        if (this.handler !=  null && this.domainInstanceReference != null) {
            this.handler.unregister(this.domainInstanceReference);
        }
    }
    
    public void testGet() {
        // In setup we added a Setting to base that is TRUE and to the domain
        // one that is FALSE.  Test that they were properly registered.
        Object obj = this.handler.get(Configuration.ENABLED_ATTRIBUTE,
            TEST_SETTING_NAME);
        assertEquals(obj, Boolean.TRUE);
        
        obj = this.handler.get(Configuration.ENABLED_ATTRIBUTE, TEST_SETTING_NAME,
            DOMAIN);
        assertEquals(obj, Boolean.FALSE);
    }
}
