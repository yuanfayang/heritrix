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


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.archive.settings.MemorySheetManager;
import org.archive.settings.Stub;
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
    public void xestPathChanger() throws Exception {
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
                if (path.equals("root:bar:foo")) {
                    System.out.println("break");
                }
                PathChange pce = new PathChange(path, type, value);
                System.out.println(pce);
                pc.change(dest, pce);
                if (!pc.getProblems().isEmpty()) {
                    for (Exception e: pc.getProblems()) {
                        e.printStackTrace();
                    }
                    fail("Problem(s) with " + pce);
                }
            }
        };
        PathLister.getAll(src, consumer, true);
        pc.finish(dest);
        PathListerTest.run(mgr, sheetName, true);
        
        changeExistingSheet(mgr, sheetName);
    }

    
    private void changeExistingSheet(SheetManager mgr, final String sheetName) 
    throws Exception {
        SingleSheet dest = (SingleSheet)mgr.getSheet(sheetName);
        // Simulate a user pressing "Submit Changes" in the sheet editor in
        // the web ui.
        final List<PathChange> list = new ArrayList<PathChange>();
        PathListConsumer plc = new StringPathListConsumer() {
            @Override
            protected void consume(String path, String[] sheets, String value,
                    String type) {
                PathChange pce = new PathChange(path, type, value);
                list.add(pce);
            }
        };
        PathLister.getAll(dest, plc, true);
        PathChanger pc = new PathChanger();
        pc.change(dest, list);
        PathListerTest.run(mgr, sheetName, true);
    }
    

    public void testPrimaryChange() throws Exception {
        Object root1 = stub_manager.getRoot();
        SingleSheet global = (SingleSheet)stub_manager.checkout("global");
        PathChange pce = new PathChange(
                "root:primary", 
                "primary", 
                "org.archive.settings.path.Baz");
        PathChanger pc = new PathChanger();
        pc.change(global, pce);
        pc.finish(global);
        stub_manager.commit(global);
        Object root2 = stub_manager.getRoot();
        assertFalse("Root map didn't change after commit", root1 == root2);
        global = stub_manager.getGlobalSheet();
        Object o = stub_manager.getRoot().get("primary");
        assertFalse("root:primary didn't change.", o == stub_primary);
        assertTrue("root:primary didn't change to a Baz.", Stub.getType(o) == Baz.class);

        Object o2 = global.findPrimary(Foo.class);
        assertTrue("Global sheet returned wrong primary", o2 == o);
        
        Object o3 = global.resolve(stub_bar, Bar.FOO_AUTO).getStubValue();
        assertTrue("Prior auto references didn't update to new primary", o3 == o);
    }


    @SuppressWarnings("unchecked")
    public void testGlobalListRemove() throws Exception {
        SingleSheet global = (SingleSheet)manager.checkout("global");
        PathChanger.remove(global, "root:bar:list:1");
        List list = (List)PathValidator.validate(global, "root:bar:list");
        assertSame(list, 
                bar_list_0,
                bar_list_2);
        
        PathChanger.remove(global, "root:bar:slist:1");
        list = (List)PathValidator.validate(global, "root:bar:slist");
        assertSame(list,
                bar_slist_0,
                bar_slist_2);

    }
    
    
    
    @SuppressWarnings("unchecked")
    public void testOverrideListRemove() throws Exception {
        SingleSheet o1 = (SingleSheet)manager.checkout("o1");
        PathChanger.remove(o1, "root:bar:list:3");
        List list = (List)PathValidator.validate(o1, "root:bar:list");
        assertSame(list, 
                bar_list_0, 
                bar_list_1, 
                bar_list_2, 
                o1_bar_list_4);
        
        PathChanger.remove(o1, "root:bar:slist:3");
        list = (List)PathValidator.validate(o1, "root:bar:slist");
        assertSame(list, 
                bar_slist_0, 
                bar_slist_1, 
                bar_slist_2, 
                o1_bar_slist_4);

        o1 = (SingleSheet)stub_manager.checkout("o1");
        PathChanger.remove(o1, "root:bar:list:3");
        list = (List)PathValidator.validate(o1, "root:bar:list");
        assertSame(list, 
                stub_bar_list_0, 
                stub_bar_list_1, 
                stub_bar_list_2, 
                stub_o1_bar_list_4);

        PathChanger.remove(o1, "root:bar:slist:3");
        list = (List)PathValidator.validate(o1, "root:bar:slist");
        assertSame(list, 
                stub_bar_slist_0, 
                stub_bar_slist_1,
                stub_bar_slist_2,
                stub_o1_bar_slist_4);
    }
    
    
    @SuppressWarnings("unchecked")
    public void testGlobalMapRemove() throws Exception {
        SingleSheet global = (SingleSheet)manager.checkout("global");
        PathChanger.remove(global, "root:bar:map:b");
        Map map = (Map)PathValidator.validate(global, "root:bar:map");
        assertSame(map.values(), bar_map_a, bar_map_c);

        PathChanger.remove(global, "root:bar:smap:b");
        map = (Map)PathValidator.validate(global, "root:bar:smap");
        assertSame(map.values(), bar_smap_a, bar_smap_c);
    }

    
    @SuppressWarnings("unchecked")
    public void testOverrideMapRemove() throws Exception {
        SingleSheet o1 = (SingleSheet)manager.checkout("o1");
        PathChanger.remove(o1, "root:bar:map:b");
        Map map = (Map)PathValidator.validate(o1, "root:bar:map");
        assertSame(map.values(),
                bar_map_a,
                bar_map_b,
                bar_map_c,
                o1_bar_map_d);
        
        PathChanger.remove(o1, "root:bar:map:d");
        map = (Map)PathValidator.validate(o1, "root:bar:map");
        assertSame(map.values(),
                bar_map_a,
                bar_map_b,
                bar_map_c);
    }
    
    
    private static void assertSame(Collection<Object> actual, 
            Object... expected) {
        ArrayList<Object> list = new ArrayList<Object>(actual);
        if (list.equals(Arrays.asList(expected))) {
            return;
        }
        
        fail("Expected " + Arrays.asList(expected) + " but got " + list);
    }
}
