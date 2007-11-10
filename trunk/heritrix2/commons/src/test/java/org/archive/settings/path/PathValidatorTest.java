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
 * PathValidatorTest.java
 * Created on October 24, 2006
 *
 * $Header$
 */
package org.archive.settings.path;


import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.archive.settings.Offline;
import org.archive.settings.Sheet;
import org.archive.settings.SingleSheet;
import org.archive.state.Key;
import org.archive.state.KeyManager;


/**
 * Unit test for PathValidator.  The test works by validating the path for 
 * every object in every sheet created by {@link PathTestBase#setUp}, 
 * ensuring that the paths resolve to the correct object references.
 * 
 * @author pjack
 */
public class PathValidatorTest extends PathTestBase {


    /**
     * Validate every path in every sheet in both stub and live mode.
     */
    public void testValidate() {
        validateGlobal();
        validateOverride1();
        validateStubGlobal();
        validateStubOverride1();
    }

    
    /**
     * Validate the given path in the given sheet, then assert that the 
     * validation results in the expected object reference.
     * 
     * @param sheet      the sheet containing the path to validate
     * @param expected   the expected result of validation
     * @param path       the path in that sheet to validate
     */
    private void ref(Sheet sheet, Object expected, String path) {
        Object result = PathValidator.validate(sheet, path);
        assertTrue(path + " returned " + result, expected == result);
    }


    /**
     * Validate the given path in the given sheet, then assert that the 
     * validation results in the expected object.  This uses an equality
     * test and not a reference test.  It's useful for comparing lists and maps,
     * since sheet resolution may or may not return the original list or map.
     * (For instance, a map setting resolved in an override sheet will return
     * a "combo" map that combines the override map elements with the global
     * map elements).
     * 
     * @param sheet       the sheet containing the path to validate
     * @param expected    the expected result of validation
     * @param path        the path in that sheet to validate
     */
    private void eq(Sheet sheet, Object expected, String path) {
        Object result = PathValidator.validate(sheet, path);
        assertEquals(path, expected, result);
    }
    

    /**
     * Validate the given path in the given sheet, then assert that the 
     * validation results in the expected object reference.  Furthermore, 
     * assert that the sheet contains the default value for every Key field
     * of the returned object.  Keys whose names are in the given exclusions
     * list are ignored.
     * 
     * @param sheet       the sheet containing the path to validate
     * @param expected    the expected result of validation
     * @param path        the path in that sheet to validate
     * @param exclusions  a list of keys to ignore
     */
    private void refCheck(Sheet sheet, Object expected, String path, 
            String... exclusions) {
        ref(sheet, expected, path);
        checkDefaults(sheet, path, exclusions);
    }
    

    /**
     * Asserts that the sheet contains the default value for every Key field
     * of the module at the given path.  Keys whose names are in the given
     * exclusions list are ignored. 
     * 
     * @param sheet       the sheet containing the path to validate
     * @param path        the path in that sheet to validate
     * @param exclusions  a list of keys to ignore
     */
    private void checkDefaults(Sheet sheet, String path, String... exclusions) {
        List<String> ex = Arrays.asList(exclusions);
        Object module = PathValidator.validate(sheet, path);
        Class<?> mclass = Offline.getType(module);
        Collection<Key<Object>> keys = KeyManager.getKeys(mclass).values();
        for (Key<Object> k: keys) {            
            if (!ex.contains(k.getFieldName())) {
                eq(sheet, k.getDefaultValue(), path + ":" + k.getFieldName());
            }
        }
    }

    
    /**
     * Validate every path in the global sheet in live mode.
     */
    private void validateGlobal() {
        SingleSheet global = manager.getGlobalSheet();
        refCheck(global, first, "root:first");
        refCheck(global, primary, "root:primary");
        refCheck(global, second, "root:second");
        ref(global, bar, "root:bar");
        refCheck(global, bar_foo, "root:bar:foo");
        refCheck(global, primary, "root:bar:foo-auto");
        eq(global, bar_list, "root:bar:list");
        refCheck(global, bar_list_0, "root:bar:list:0");
        refCheck(global, bar_list_1, "root:bar:list:1");
        refCheck(global, bar_list_2, "root:bar:list:2");
        eq(global, bar_map, "root:bar:map");
        refCheck(global, bar_map_a, "root:bar:map:a");
        refCheck(global, bar_map_b, "root:bar:map:b", "five");
        eq(global, 50000, "root:bar:map:b:five");
        refCheck(global, bar_map_c, "root:bar:map:c");
        eq(global, bar_slist, "root:bar:slist");
        ref(global, bar_slist_0, "root:bar:slist:0");
        ref(global, bar_slist_1, "root:bar:slist:1");
        ref(global, bar_slist_2, "root:bar:slist:2");
        eq(global, bar_smap, "root:bar:smap");
        ref(global, bar_smap_a, "root:bar:smap:a");
        ref(global, bar_smap_b, "root:bar:smap:b");
        ref(global, bar_smap_c, "root:bar:smap:c");
    }
    
    
    /**
     * Validate every path in the override1 sheet in live mode. 
     */
    private void validateOverride1() {
        SingleSheet o1 = (SingleSheet)manager.getSheet("o1");
        refCheck(o1, first, "root:first", "ten");
        eq(o1, o1_first_ten, "root:first:ten");
        refCheck(o1, primary, "root:primary");
        refCheck(o1, o1_second, "root:second");
        ref(o1, bar, "root:bar");
        refCheck(o1, bar_foo, "root:bar:foo");
        refCheck(o1, primary, "root:bar:foo-auto");
        // eq(o1, bar_list, "root:bar:list");
        refCheck(o1, bar_list_0, "root:bar:list:0");
        refCheck(o1, bar_list_1, "root:bar:list:1");
        refCheck(o1, bar_list_2, "root:bar:list:2");
        refCheck(o1, o1_bar_list_3, "root:bar:list:3");
        refCheck(o1, o1_bar_list_4, "root:bar:list:4");
        // eq(o1, bar_map, "root:bar:map");
        refCheck(o1, bar_map_a, "root:bar:map:a");
        refCheck(o1, o1_bar_map_b, "root:bar:map:b");
        refCheck(o1, bar_map_c, "root:bar:map:c");
        refCheck(o1, o1_bar_map_d, "root:bar:map:d");
    }


    /**
     * Validate every path in the global sheet in stub mode.
     */
    private void validateStubGlobal() {
        SingleSheet global = stub_manager.getGlobalSheet();
        
        refCheck(global, stub_first, "root:first");
        refCheck(global, stub_primary, "root:primary");
        refCheck(global, stub_second, "root:second");
        ref(global, stub_bar, "root:bar");
        refCheck(global, stub_bar_foo, "root:bar:foo");
        refCheck(global, stub_primary, "root:bar:foo-auto");
        eq(global, stub_bar_list, "root:bar:list");
        refCheck(global, stub_bar_list_0, "root:bar:list:0");
        refCheck(global, stub_bar_list_1, "root:bar:list:1");
        refCheck(global, stub_bar_list_2, "root:bar:list:2");
        eq(global, stub_bar_map, "root:bar:map");
        refCheck(global, stub_bar_map_a, "root:bar:map:a");
        refCheck(global, stub_bar_map_b, "root:bar:map:b", "five");
        eq(global, 50000, "root:bar:map:b:five");
        ref(global, stub_bar_map_c, "root:bar:map:c");
        eq(global, stub_bar_slist, "root:bar:slist");
        ref(global, stub_bar_slist_0, "root:bar:slist:0");
        ref(global, stub_bar_slist_1, "root:bar:slist:1");
        ref(global, stub_bar_slist_2, "root:bar:slist:2");
        eq(global, stub_bar_smap, "root:bar:smap");
        ref(global, stub_bar_smap_a, "root:bar:smap:a");
        ref(global, stub_bar_smap_b, "root:bar:smap:b");
        ref(global, stub_bar_smap_c, "root:bar:smap:c");
    }

    
    /**
     * Validate every path in the global sheet in stub mode.
     */
    private void validateStubOverride1() {
        SingleSheet o1 = (SingleSheet)stub_manager.getSheet("o1");
        refCheck(o1, stub_first, "root:first", "ten");
        eq(o1, stub_o1_first_ten, "root:first:ten");
        refCheck(o1, stub_primary, "root:primary");
        refCheck(o1, stub_o1_second, "root:second");
        ref(o1, stub_bar, "root:bar");
        refCheck(o1, stub_bar_foo, "root:bar:foo");
        refCheck(o1, stub_primary, "root:bar:foo-auto");
        // eq(o1, bar_list, "root:bar:list");
        refCheck(o1, stub_bar_list_0, "root:bar:list:0");
        refCheck(o1, stub_bar_list_1, "root:bar:list:1");
        refCheck(o1, stub_bar_list_2, "root:bar:list:2");
        refCheck(o1, stub_o1_bar_list_3, "root:bar:list:3");
        refCheck(o1, stub_o1_bar_list_4, "root:bar:list:4");
        // eq(o1, bar_map, "root:bar:map");
        refCheck(o1, stub_bar_map_a, "root:bar:map:a");
        refCheck(o1, stub_o1_bar_map_b, "root:bar:map:b");
        refCheck(o1, stub_bar_map_c, "root:bar:map:c");
        refCheck(o1, stub_o1_bar_map_d, "root:bar:map:d");
    }

}
