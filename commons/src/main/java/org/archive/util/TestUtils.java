/* TestUtils
*
* $Id$
*
* Created on Dec 28, 2004
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
package org.archive.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.ref.SoftReference;
import java.util.Arrays;
import java.util.LinkedList;

import junit.framework.TestCase;
import junit.framework.TestSuite;


/**
 * Utility methods useful in testing situations.
 * 
 * @author gojomo
 */
public class TestUtils {

    /**
     * Temporarily exhaust memory, forcing weak/soft references to
     * be broken. 
     */
    public static void forceScarceMemory() {
        // force soft references to be broken
        LinkedList<SoftReference> hog = new LinkedList<SoftReference>();
        long blocks = Runtime.getRuntime().maxMemory() / 1000000;
        for(long l = 0; l <= blocks; l++) {
            try {
                hog.add(new SoftReference<byte[]>(new byte[1000000]));
            } catch (OutOfMemoryError e) {
                hog = null;
                break;
            }
        }
    }


    public static void testSerialization(Object proc) throws Exception {
        byte[] first = serialize(proc);
        ByteArrayInputStream binp = new ByteArrayInputStream(first);
        ObjectInputStream oinp = new ObjectInputStream(binp);
        Object o = oinp.readObject();
        oinp.close();
        TestCase.assertEquals(proc.getClass(), o.getClass());
        byte[] second = serialize(o);
        TestCase.assertTrue(Arrays.equals(first, second));
    }


    private static byte[] serialize(Object o) throws Exception {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ObjectOutputStream oout = new ObjectOutputStream(bout);    
        oout.writeObject(o);
        oout.close();
        return bout.toByteArray();        
    }

    
    public static TestSuite makePackageSuite(Class c) 
    throws ClassNotFoundException {
        String cname = c.getName();
        int p = cname.lastIndexOf('.');
        String dir = cname.substring(0, p).replace('.', File.separatorChar);
        String root = "heritrix/src/test/java/".replace('/', File.separatorChar);
        File src = new File(root);
        return makeSuite(src, new File(root + dir));
    }
    

    public static TestSuite makeSuite(File srcRoot, File dir) 
    throws ClassNotFoundException {
        TestSuite result = new TestSuite("All Tests");
        if (!dir.exists()) {
            throw new IllegalArgumentException(dir + " does not exist.");
        }
        scanSuite(result, srcRoot, dir);
        return result;
    }
    
    
    private static void scanSuite(TestSuite suite, File start, File dir) 
    throws ClassNotFoundException {
        for (File f: dir.listFiles()) {
            if (f.isDirectory() && !f.getName().startsWith(".")) {
                String prefix = start.getAbsolutePath();
                String full = f.getAbsolutePath();
                TestSuite sub = new TestSuite(full.substring(prefix.length()));
                scanSuite(sub, start, f);
                if (sub.testCount() > 0) {
                    suite.addTest(sub);
                }
            } else {
                if (f.getName().endsWith("Test.java")) {
                    String full = f.getAbsolutePath();
                    String prefix = start.getAbsolutePath();
                    String cname = full.substring(prefix.length());
                    if (cname.startsWith(File.separator)) {
                        cname = cname.substring(1);
                    }
                    cname = cname.replace(File.separatorChar, '.');
                    cname = cname.substring(0, cname.length() - 5);
                    suite.addTestSuite(Class.forName(cname));
                }
            }
        }
    }

}
