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
package org.archive.configuration.registry;

import java.util.logging.Logger;

import javax.management.Attribute;
import javax.management.AttributeNotFoundException;
import javax.management.openmbean.CompositeData;

import junit.framework.TestCase;

import org.archive.configuration.Configurable;
import org.archive.configuration.Configuration;
import org.archive.configuration.ConfigurationException;
import org.archive.configuration.Pointer;
import org.archive.configuration.Registry;

/**
 * @author stack
 * @version $Date$ $Revision$
 */
public class JmxRegistryTest extends TestCase {
    private final Logger LOGGER =
        Logger.getLogger(this.getClass().getName());
    
    static final String BASE_DOMAIN = "org.archive";
    static final String SUB_DOMAIN = "org.archive.com.archive.web";
    private static final String BASE_NAME = "base";
    private Registry registry = null;
    private Object baseInstanceReference = null;
    private Object domainInstanceReference = null;
    private static final TestProcessor TP = new TestProcessor("test");


    public static void main(String[] args) {
        junit.textui.TestRunner.run(JmxRegistryTest.class);
    }

    protected void setUp() throws Exception {
        super.setUp();
        this.registry = new JmxRegistry(BASE_DOMAIN);
        // Populate the registry.
        if (!this.registry.isRegistered(BASE_NAME, TP.getClass())) {
            this.baseInstanceReference =
                this.registry.register(BASE_NAME, TP.getClass(),
                    BASE_DOMAIN, TP.getConfiguration());
        }
        if (!this.registry.isRegistered(BASE_NAME, TP.getClass(),
                SUB_DOMAIN)) {
            Configuration c = TP.getConfiguration();
            Boolean b = (Boolean)c.
                getAttribute(TestProcessor.BOOLEAN_ATTRIBUTE_NAME);
            c.setAttribute(new Attribute(TestProcessor.BOOLEAN_ATTRIBUTE_NAME,
               (b.equals(Boolean.TRUE)? Boolean.FALSE: Boolean.TRUE)));
            this.domainInstanceReference =
                this.registry.register(BASE_NAME, TP.getClass(),
                    SUB_DOMAIN, c);
        }
    }
    
    protected void tearDown() throws Exception {
        super.tearDown();
        if (this.registry != null) {
            this.registry.deregister(this.domainInstanceReference);
            this.registry.deregister(this.baseInstanceReference);
        }
    }
    
    public void testSimpleProcessor()
    throws AttributeNotFoundException, ConfigurationException {
        Object value = this.registry.get(TestProcessor.BOOLEAN_ATTRIBUTE_NAME,
            BASE_NAME, TestProcessor.class, BASE_DOMAIN);
        Configuration c = TP.getConfiguration();
        Object defaultValue =
            c.getAttribute(TestProcessor.BOOLEAN_ATTRIBUTE_NAME);
        assertEquals(defaultValue, value);
        value = this.registry.get(TestProcessor.BOOLEAN_ATTRIBUTE_NAME,
            BASE_NAME, TestProcessor.class, SUB_DOMAIN);
        assertNotSame(defaultValue, value);
    }
    
    public void testProcessorPtrs()
    throws AttributeNotFoundException, ConfigurationException {
        Object [] ptrs = (Object []) 
            this.registry.get(TestProcessor.PROCESSOR_PTR_ATTRIBUTE_NAME,
                BASE_NAME, TestProcessor.class, BASE_DOMAIN);
        for (int i = 0; i < ptrs.length; i++) {
            CompositeData cd = (CompositeData)ptrs[i];
            Pointer p = new Pointer(cd);
            Configurable c = p.getConfiguredInstance(this.registry);
        }
    }
}
