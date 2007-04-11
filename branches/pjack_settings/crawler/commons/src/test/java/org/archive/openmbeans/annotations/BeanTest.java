/* BeanTest
 *
 * Created on October 18, 2006
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
package org.archive.openmbeans.annotations;

import java.util.Iterator;
import java.util.List;
import java.util.Random;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;


import junit.framework.TestCase;

public class BeanTest extends TestCase {

    
    private ExampleBean bean;
    private Random random;
    
    public void setUp() {
        bean = new ExampleBean();
        random = new Random();
    }
    
    public void testExampleAnd() throws Exception {
        runAndTest(true, true);
        runAndTest(true, false);
        runAndTest(false, true);
        runAndTest(false, false);
    }
    
    
    private void runAndTest(boolean b1, boolean b2) throws Exception {
        String[] sig = new String[] { "java.lang.Boolean", "java.lang.Boolean" };
        Object[] params = new Object[] { b1, b2 };
        Object o = bean.invoke("and", params, sig);
        boolean r = (Boolean)o;
        assertEquals(b1 && b2, r);
    }
    
    
    public void testExampleSumByte() throws Exception {
        String[] sig = new String[] { "java.lang.Byte", "java.lang.Byte" };
        for (int i = 0; i < 1000; i++) {
            byte a = (byte)random.nextInt();
            byte b = (byte)random.nextInt();
            runSumTest("sumByte", sig, a, b, (byte)(a + b));
        }
    }

    
    public void testExampleSumChar() throws Exception {
        String[] sig = new String[] { "java.lang.Character", "java.lang.Character" };
        for (int i = 0; i < 1000; i++) {
            char a = (char)random.nextInt();
            char b = (char)random.nextInt();
            runSumTest("sumChar", sig, a, b, (char)(a + b));
        }
    }

    
    public void testExampleSumDouble() throws Exception {
        String[] sig = new String[] { "java.lang.Double", "java.lang.Double" };
        for (int i = 0; i < 1000; i++) {
            double a = random.nextDouble();
            double b = random.nextDouble();
            runSumTest("sumDouble", sig, a, b, a + b);
        }
    }

    
    public void testExampleSumFloat() throws Exception {
        String[] sig = new String[] { "java.lang.Float", "java.lang.Float" };
        for (int i = 0; i < 1000; i++) {
            float a = random.nextFloat();
            float b = random.nextFloat();
            runSumTest("sumFloat", sig, a, b, a + b);
        }
    }

    
    public void testExampleSumInt() throws Exception {
        String[] sig = new String[] { "java.lang.Integer", "java.lang.Integer" };
        for (int i = 0; i < 1000; i++) {
            int a = random.nextInt();
            int b = random.nextInt();
            runSumTest("sumInt", sig, a, b, a + b);
        }
    }

    
    public void testExampleSumLong() throws Exception {
        String[] sig = new String[] { "java.lang.Long", "java.lang.Long" };
        for (int i = 0; i < 1000; i++) {
            long a = random.nextLong();
            long b = random.nextLong();
            runSumTest("sumLong", sig, a, b, a + b);
        }
    }

    
    public void testExampleSumShort() throws Exception {
        String[] sig = new String[] { "java.lang.Short", "java.lang.Short" };
        for (int i = 0; i < 1000; i++) {
            short a = (short)random.nextInt();
            short b = (short)random.nextInt();
            runSumTest("sumShort", sig, a, b, (short)(a + b));
        }
    }

    
    private void runSumTest(String op, String[] sig, 
            Object a, Object b, Object expected) throws Exception {
        Object[] params = new Object[] { a, b };
        Object result = bean.invoke(op, params, sig);
        assertEquals(expected, result);
    }

    public void testAgentRegister()
    throws MalformedObjectNameException, NullPointerException,
    		InstanceAlreadyExistsException, MBeanRegistrationException,
    		NotCompliantMBeanException, InstanceNotFoundException {
    	// Won't run unless 1.5 SUN JVM and following system properties are set:
    	// -Dcom.sun.management.jmxremote.authenticate=false \
    	// -Dcom.sun.management.jmxremote.port=8849
    	String jmxport =
    		System.getProperty("com.sun.management.jmxremote.port");
    	if (jmxport == null || jmxport.length() <= 0) {
    		return;
    	}
        List servers = MBeanServerFactory.findMBeanServer(null);
        assertNotNull(servers);
        MBeanServer server = null;
        for (Iterator i = servers.iterator(); i.hasNext();) {
            server = (MBeanServer)i.next();
            if (server == null) {
                continue;
            }
            break;
        }
        ObjectName on = new ObjectName("org.archive", "a", "b");
        server.registerMBean(this.bean, on);
        // Could pause here in debugger to inspect the bean with jconsole, etc.
        server.unregisterMBean(on);
    }
}
