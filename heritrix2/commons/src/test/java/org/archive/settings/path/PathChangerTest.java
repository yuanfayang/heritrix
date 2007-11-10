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
 * PathChangerTest.java
 * Created on October 24, 2006
 *
 * $Header: /cvsroot/archive-crawler/ArchiveOpenCrawler/src/java/org/archive/settings/path/Attic/PathChangerTest.java,v 1.1.2.4 2007/01/17 01:48:00 paul_jack Exp $
 */
package org.archive.settings.path;


import org.archive.settings.MemorySheetManager;
import org.archive.settings.SheetManager;
import org.archive.settings.SingleSheet;


/**
 * Unit test for {@link PathChanger}.  The test uses PathLister and PathChanger
 * to clone the sheets set up by {@link PathTestBase}. 
 * 
 * @author pjack
 */
public class PathChangerTest extends PathTestBase {


    /**
     * Run the test.
     */
    public void testPathChanger() throws Exception {
        SheetManager live = new MemorySheetManager();        
        live.addSingleSheet("o1");
        testSheet(live, "global");
        testSheet(live, "o1");
        
        SheetManager stub = new MemorySheetManager(false);
        stub.addSingleSheet("o1");
        testSheet(stub, "global");
        testSheet(stub, "o1");
    }
    

    /**
     * List the contents of the given sheet using the {@link stub_manager},
     * piping the results to a PathChanger that modifies the sheet with 
     * the same name in the given mgr.
     * 
     * @param mgr          the destination sheet manager for the cloned sheets
     * @param sheetName    the name of the sheet to clone
     */
    private void testSheet(SheetManager mgr, final String sheetName) 
    throws Exception {
        final PathChanger pc = new PathChanger();
        final SingleSheet src = (SingleSheet)stub_manager.getSheet(sheetName);
        final SingleSheet dest = (SingleSheet)mgr.getSheet(sheetName);
        PathListConsumer consumer = new StringPathListConsumer() {

            @Override
            protected void consume(String path, String[] sheets, String value,
                    String type) {
                System.out.println(sheetName + "|" + path + "=" + type + ", " + value);
                PathChange pce = new PathChange(path, type, value);
                pc.change(dest, pce);
            }
        };
        PathLister.getAll(src, consumer, true);
        pc.finish(dest);
        
        PathListerTest.run(mgr, sheetName, true);
    }

}
