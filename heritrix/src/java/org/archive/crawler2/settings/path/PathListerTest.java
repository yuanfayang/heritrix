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
 * PathTestBase.java
 * Created on October 24, 2006
 *
 * $Header$
 */
package org.archive.crawler2.settings.path;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import org.archive.crawler2.settings.Sheet;
import org.archive.crawler2.settings.SingleSheet;

public class PathListerTest extends PathTestBase {

    public static class Consumer implements PathListConsumer {
        
        public StringBuilder sb = new StringBuilder();
        
        public void consume(String path, List<Sheet> sheets, Object value) {
            for (Sheet s: sheets) {
                sb.append(s.getName()).append(" -> ");
            }
            Class c = value.getClass();
            if (isSimple(c)) {
                sb.append(path).append('=').append(value);
            } else {
                sb.append(path).append("._impl=").append(c.getName()); 
            }
            sb.append('\n');
        }
    }
    
    public void testResolveAll() throws Exception {
        runResolve("default");
        runResolve("override1");
        runResolve("override2");
        runResolve("bundle");
    }
    
    
    public void testGetAll() throws Exception {
        runGet("default");
        runGet("override1");
        runGet("override2");
    }

    
    private void runResolve(String sheetName) throws IOException {
        String expected = load(sheetName + ".resolved.txt");
        Consumer consumer = new Consumer();
        PathLister.resolveAll(manager.getSheet(sheetName), consumer);
        String result = consumer.sb.toString();
        assertEquals(expected, result);
    }

    
    private void runGet(String sheetName) throws IOException {
        String expected = load(sheetName + ".get.txt");
        Consumer consumer = new Consumer();
        SingleSheet ss = (SingleSheet)manager.getSheet(sheetName);
        PathLister.getAll(ss, consumer);
        String result = consumer.sb.toString();
        assertEquals(expected, result);
    }

    
    static boolean isSimple(Class c) {
        if (c.isPrimitive()) {
            return true;
        }
        if (c == Boolean.class) {
            return true;
        }
        if (c == String.class) {
            return true;
        }
        // FIXME: BigDecimal, BigInteger, Date, Pattern and so on...
        return false;
    }

    private static String load(String name) throws IOException {
        InputStream inp = PathListerTest.class.getResourceAsStream(name);
        BufferedReader br = new BufferedReader(new InputStreamReader(inp));
        StringBuilder sb = new StringBuilder();
        for (String s = br.readLine(); s != null; s = br.readLine()) {
            sb.append(s).append('\n');
        }
        return sb.toString();
    }
}
