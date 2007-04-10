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
package org.archive.settings.path;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;

import org.archive.settings.SingleSheet;
import org.archive.settings.file.FilePathListConsumer;


public class PathListerTest extends PathTestBase {


/*
    public void ntestQ() throws Exception {
//        String expected = load(sheetName + ".resolved.txt");
        StringWriter sw = new StringWriter();
        FilePathListConsumer consumer = new FilePathListConsumer(sw);
        consumer.setIncludeSheets(true);
        PathLister.getAll((SingleSheet)
                offlineManager.getSheet("override2"), consumer);
        String result = sw.toString();
        System.out.println(result);
        
        
  //      System.out.println("SHEET: " + sheetName);
  //      System.out.println(expected + "\n\n\n");
  //      System.out.println(result);
  //      assertEquals(expected, result);
        
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
        StringWriter sw = new StringWriter();
        FilePathListConsumer consumer = new FilePathListConsumer(sw);
        consumer.setIncludeSheets(true);
        
        PathLister.resolveAll(offlineManager.getSheet(sheetName), consumer);
        String result = sw.toString();
        System.out.println("OFFLINE SHEET: " + sheetName);
        System.out.println(expected + "\n\n\n");
        System.out.println(result);
        assertEquals(expected, result);

        sw = new StringWriter();
        consumer = new FilePathListConsumer(sw);
        consumer.setIncludeSheets(true);
        
        PathLister.resolveAll(manager.getSheet(sheetName), consumer);
        result = sw.toString();
        System.out.println("ONLINE SHEET: " + sheetName);
        System.out.println(expected + "\n\n\n");
        System.out.println(result);
        assertEquals(expected, result);

    }

    
    private void runGet(String sheetName) throws IOException {
        String expected = load(sheetName + ".get.txt");
        StringWriter sw = new StringWriter();
        FilePathListConsumer consumer = new FilePathListConsumer(sw);
        consumer.setIncludeSheets(true);
        SingleSheet ss = (SingleSheet)manager.getSheet(sheetName);
        PathLister.getAll(ss, consumer);
        String result = sw.toString();
        assertEquals(expected, result);
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
*/

}
