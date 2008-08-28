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

import org.archive.settings.Sheet;
import org.archive.settings.SheetManager;
import org.archive.settings.SingleSheet;
import org.archive.settings.file.FilePathListConsumer;


/**
 * Unit test for {@link PathLister}.  The test loads text representations of the 
 * sheets defined by {@link PathTestBase}, then generates a new text 
 * representation for the sheets using PathLister and a 
 * {@link FilePathListConsumer}.  The generated text is compared against the
 * loaded text.
 * 
 * <p>Each sheet has two resource files used by this test.  One has a 
 * <code>.resolved.txt</code> suffix, and contains the expected result of 
 * {@link PathLister#resolveAll(Sheet, PathListConsumer, boolean)} for the
 * sheet.  The second resource has a <code>.get.txt</code> suffix, and contains
 * the expected result of 
 * {@link PathLister#getAll(SingleSheet, PathListConsumer, boolean)}.
 * 
 * <p>Each sheet is tested four times:
 * 
 * <ol>
 * <li>Using the live-mode sheet manager and the resolveAll operation;
 * <li>Using the live-mode sheet manager and the getAll operation;
 * <li>Using the stub-mode sheet manager and the resolveAll operation;
 * <li>Using the stub-mode sheet manager and the getAll operation.
 * </ol>
 * 
 * @author pjack
 */
public class PathListerTest extends PathTestBase {


    /**
     * Rename to <code>testPrint</code> if you'd like to generate expected
     * output.  Just be sure to visually inspect the results before pasting
     * it into a resource file.
     */
    public void xtestPrint() {
        SingleSheet ss = (SingleSheet)manager.getSheet("o1");
        StringWriter sw = new StringWriter();
        FilePathListConsumer consumer = new FilePathListConsumer(sw);
        consumer.setSheetsDelim('|');
        consumer.setIncludeSheets(true);
        PathLister.resolveAll(ss, consumer, false);
        System.out.println(sw.toString());
    }

    public void testDummy() {
        // suppress 'no tests found' warning
    }

    /**
     * Runs the test.
     */
    public void xestPathLister() throws Exception {
        runSheet("global");
        runSheet("o1");
        run(manager, bundle, true);
    }


    /**
     * Runs four tests on one sheet.
     *
     * <ol>
     * <li>Using the live-mode sheet manager and the resolveAll operation;
     * <li>Using the live-mode sheet manager and the getAll operation;
     * <li>Using the stub-mode sheet manager and the resolveAll operation;
     * <li>Using the stub-mode sheet manager and the getAll operation.
     * </ol>
     * 
     * @param sheetName  the name of the sheet to test
     */
    private void runSheet(String sheetName) throws Exception {
        run(manager, sheetName, true);
        run(manager, sheetName, false);
        run(stub_manager, sheetName, true);
        run(stub_manager, sheetName, false);
    }

    
    static void run(SheetManager manager, String sheetName, boolean resolve) 
    throws Exception {
        SingleSheet ss = (SingleSheet)manager.getSheet(sheetName);
        run(manager, ss, resolve);
    }
    

    /**
     * Load the resource containing expected output for the given sheet,
     * then run the specified PathLister operation and compare the results
     * against the expected output.
     * 
     * <p>This is package-protected since PathChangerTest also uses it.
     * 
     * @param manager     the sheet manager containing the sheet
     * @param sheetName   the name of the sheet to test
     * @param resolve     true to test the resolveAll operation, false to test
     *                     the getAll operation.
     */
    static void run(SheetManager manager, Sheet sheet, boolean resolve) 
    throws Exception {
        String sheetName = sheet.getName();
        String suffix = resolve ? ".resolved.txt" : ".get.txt";
        String expected = load(sheetName + suffix);
        StringWriter sw = new StringWriter();
        FilePathListConsumer consumer = new FilePathListConsumer(sw);
        consumer.setIncludeSheets(true);
        consumer.setSheetsDelim('|');
        if (resolve) {
            PathLister.resolveAll(sheet, consumer, true);
        } else {
            PathLister.getAll((SingleSheet)sheet, consumer, true);
        }
        String result = sw.toString();
        assertEquals(expected, result);        
    }


    /**
     * Loads the resource with the given name.
     * 
     * @param name   the name of the resource to load
     * @return      the resource as text
     */
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
