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

import java.util.List;
import java.util.Map;

import org.archive.settings.MemorySheetManager;
import org.archive.settings.Stub;
import org.archive.settings.SettingsList;
import org.archive.settings.SettingsMap;
import org.archive.settings.SheetManager;
import org.archive.settings.SingleSheet;

import junit.framework.TestCase;


/**
 * Base class for path testing code.  This class sets up two 
 * {@link MemorySheetManager} instances, one in stub mode and one in live 
 * mode.  {@link Foo} and {@link Bar} modules are then added to each 
 * manager.  Both managers get the same configuration.  The stub-mode and
 * live-mode managers form the test data set used by subclasses to perform
 * tests on path-related operations.
 * 
 * Things not yet tested:
 * 
 * 1. Maps/Lists when two sheets with same override are bundled
 * 2. Bundled sheet does what we expect
 * 3. Modifying a checked-out sheet doesn't modify original.
 * 4. It's impossible to modify a sheet that isn't checked out.
 * 5. Impossible to modify an IMMUTABLE setting in a non-stub manager.
 * 6. Impossible to modify a GLOBAL setting in a non-global sheet.
 * 7. Primary change in an override sheet?
 * 
 * @author pjack
 */
public abstract class PathTestBase extends TestCase {

    SheetManager manager;
    
    // Objects in the global sheet for normal-mode tests
    Foo first;                // root:first
    Foo primary;              // root:primary
    Foo second;               // root:second
    Bar bar;                  // root:bar
    Foo bar_foo;              // root:bar:foo
    List<Foo> bar_list;       // root:bar:list
    Foo bar_list_0;           // root:bar:list:0
    Foo bar_list_1;           // root:bar:list:1
    Foo bar_list_2;           // root:bar:list:2
    Map<String,Foo> bar_map;  // root:bar:map
    Foo bar_map_a;            // root:bar:map:a
    Foo bar_map_b;            // root:bar:map:b
    Foo bar_map_b_five;       // root:bar:map:b:five (override default)
    Foo bar_map_c;            // root:bar:map:c
    List<String> bar_slist;   // root:bar:slist
    String bar_slist_0;       // root:bar:slist:0
    String bar_slist_1;       // root:bar:slist:1
    String bar_slist_2;       // root:bar:slist:2
    Map<String,String> bar_smap;
    String bar_smap_a;
    String bar_smap_b;
    String bar_smap_c;
    
    
    // Objects in the first override sheet.
    String o1_first_ten;        // root:first:ten (override)
    Foo o1_second;              // root:second (override)
    List<Foo> o1_bar_list;      // root:bar:list (override/append)
    Foo o1_bar_list_3;          // root:bar:list:3 (new element)
    Foo o1_bar_list_4;          // root:bar:list:4 (new element)
    Map<String,Foo> o1_bar_map; // root:bar:map (override/merge)
    Baz o1_bar_map_b;           // root:bar:map:b  (replace element)
    Foo o1_bar_map_d;           // root:bar:map:d  (new element)
    List<String> o1_bar_slist;
    String o1_bar_slist_3;
    String o1_bar_slist_4;
    Map<String,String> o1_bar_smap;
    String o1_bar_smap_b;
    String o1_bar_smap_d;
    
    
    SheetManager stub_manager;

    // Objects in global sheet for stub-mode tests
    Stub<Foo> stub_first;
    Stub<Foo> stub_primary;
    Stub<Foo> stub_second;
    Stub<Bar> stub_bar;
    Stub<Foo> stub_bar_foo;
    List<Object> stub_bar_list;
    Stub<Foo> stub_bar_list_0;
    Stub<Foo> stub_bar_list_1;
    Stub<Foo> stub_bar_list_2;
    Map<String,Object> stub_bar_map;
    Stub<Foo> stub_bar_map_a;
    Stub<Foo> stub_bar_map_b;
    Stub<Foo> stub_bar_map_c;
    List<String> stub_bar_slist;   
    String stub_bar_slist_0;
    String stub_bar_slist_1;
    String stub_bar_slist_2;
    Map<String,String> stub_bar_smap;
    String stub_bar_smap_a;
    String stub_bar_smap_b;
    String stub_bar_smap_c;

    // Objects in the first override sheet.
    String stub_o1_first_ten;        // root:first:ten (override)
    Stub<Foo> stub_o1_second;              // root:second (override)
    List<Foo> stub_o1_bar_list;      // root:bar:list (override/append)
    Stub<Foo> stub_o1_bar_list_3;          // root:bar:list:3 (new element)
    Stub<Foo> stub_o1_bar_list_4;          // root:bar:list:4 (new element)
    Map<String,Foo> stub_o1_bar_map; // root:bar:map (override/merge)
    Stub<Baz> stub_o1_bar_map_b;           // root:bar:map:b  (replace element)
    Stub<Foo> stub_o1_bar_map_d;           // root:bar:map:d  (new element)
    List<String> stub_o1_bar_slist;  // root:bar:slist  (override/append)
    String stub_o1_bar_slist_3;         
    String stub_o1_bar_slist_4;
    Map<String,String> stub_o1_bar_smap; 
    String stub_o1_bar_smap_b;
    String stub_o1_bar_smap_d;
    
    public void setUp() {
        setUpNormal();
        setUpStub();
    }
    
    
    @SuppressWarnings("unchecked")
    private void setUpStub() {        
        // ===== Global sheet. =====
        this.stub_manager = new MemorySheetManager(false);
        SingleSheet global = (SingleSheet)stub_manager.checkout("global");
        Map<String,Object> root = global.resolveEditableMap(
                stub_manager.getManagerModule(), 
                SheetManager.ROOT);
        
        this.stub_first = Stub.make(Foo.class);
        root.put("first", stub_first);
        this.stub_primary = Stub.make(Foo.class);
        root.put("primary", stub_primary);
        global.addPrimary(stub_primary);
        this.stub_second = Stub.make(Foo.class);
        root.put("second", stub_second);
        
        this.stub_bar = Stub.make(Bar.class);
        this.stub_bar_foo = Stub.make(Foo.class);
        global.setStub(stub_bar, Bar.FOO, stub_bar_foo);
        
        this.stub_bar_list = new SettingsList(global, Foo.class);
        global.setStub(stub_bar, Bar.LIST, stub_bar_list);
        this.stub_bar_list_0 = Stub.make(Foo.class);
        stub_bar_list.add(stub_bar_list_0);
        this.stub_bar_list_1 = Stub.make(Foo.class);
        stub_bar_list.add(stub_bar_list_1);
        this.stub_bar_list_2 = Stub.make(Foo.class);
        stub_bar_list.add(stub_bar_list_2);
        
        this.stub_bar_map = new SettingsMap(global, Foo.class);
        global.setStub(stub_bar, Bar.MAP, stub_bar_map);
        this.stub_bar_map_a = Stub.make(Foo.class);
        stub_bar_map.put("a", stub_bar_map_a);
        this.stub_bar_map_b = Stub.make(Foo.class);
        global.set(stub_bar_map_b, Foo.FIVE, 50000);
        stub_bar_map.put("b", stub_bar_map_b);        
        this.stub_bar_map_c = Stub.make(Foo.class);
        stub_bar_map.put("c", stub_bar_map_c);

        this.stub_bar_slist = new SettingsList<String>(global, String.class);
        global.setStub(stub_bar, Bar.SLIST, stub_bar_slist);
        this.stub_bar_slist_0 = "zero";        
        stub_bar_slist.add(stub_bar_slist_0);
        this.stub_bar_slist_1 = "one";
        stub_bar_slist.add(stub_bar_slist_1);
        this.stub_bar_slist_2 = "two";
        stub_bar_slist.add(stub_bar_slist_2);

        this.stub_bar_smap = new SettingsMap<String>(global, String.class);
        global.setStub(stub_bar, Bar.SMAP, stub_bar_smap);
        this.stub_bar_smap_a = "65";
        stub_bar_smap.put("a", stub_bar_smap_a);
        this.stub_bar_smap_b = "66";
        stub_bar_smap.put("b", stub_bar_smap_b);        
        this.stub_bar_smap_c = "67";
        stub_bar_smap.put("c", stub_bar_smap_c);
        
        root.put("bar", stub_bar);
        stub_manager.commit(global);
        
        // ===== o1 sheet =====
        stub_manager.addSingleSheet("o1");
        SingleSheet o1 = (SingleSheet)stub_manager.checkout("o1");
        Map<String,Object> o1_root = new SettingsMap<Object>(o1, Object.class);
        o1.set(stub_manager.getManagerModule(), SheetManager.ROOT, o1_root);
        
        this.stub_o1_first_ten = "three plus seven";
        o1.set(this.stub_first, Foo.TEN, this.stub_o1_first_ten);        
        this.stub_o1_second = Stub.make(Foo.class);
        o1_root.put("second", stub_o1_second);
        
        List l1 = new SettingsList(o1, Bar.class);
        this.stub_o1_bar_list = l1;
        o1.set(this.stub_bar, Bar.LIST, this.stub_o1_bar_list);
        this.stub_o1_bar_list_3 = Stub.make(Foo.class);
        l1.add(stub_o1_bar_list_3);
        this.stub_o1_bar_list_4 = Stub.make(Foo.class);
        l1.add(stub_o1_bar_list_4);
        
        Map m1 = new SettingsMap(o1, Foo.class);
        this.stub_o1_bar_map = m1;
        o1.set(this.stub_bar, Bar.MAP, this.stub_o1_bar_map);
        this.stub_o1_bar_map_b = Stub.make(Baz.class);
        m1.put("b", stub_o1_bar_map_b);
        this.stub_o1_bar_map_d = Stub.make(Foo.class);
        m1.put("d", stub_o1_bar_map_d);

        this.stub_o1_bar_slist = new SettingsList<String>(o1, String.class);
        o1.set(this.stub_bar, Bar.SLIST, this.stub_o1_bar_slist);
        this.stub_o1_bar_slist_3 = "three";
        stub_o1_bar_slist.add(stub_o1_bar_slist_3);
        this.stub_o1_bar_slist_4 = "four";
        stub_o1_bar_slist.add(stub_o1_bar_slist_4);
        
        stub_manager.commit(o1);        
    }
    
    
    private void setUpNormal() {
        // ===== Global sheet. =====
        this.manager = new MemorySheetManager();
        SingleSheet global = (SingleSheet)manager.checkout("global");
        Map<String,Object> root = global.resolveEditableMap(manager, 
                SheetManager.ROOT);
        
        this.first = new Foo("first");
        root.put("first", first);
        this.primary = new Foo("primary");
        root.put("primary", primary);
        global.addPrimary(primary);
        this.second = new Foo("second");
        root.put("second", second);
        
        this.bar = new Bar();
        this.bar_foo = new Foo("bar_foo");
        global.set(bar, Bar.FOO, bar_foo);
        Foo test = global.get(bar, Bar.FOO);
        assertTrue("WTF?", test == bar_foo);
        this.bar_list = new SettingsList<Foo>(global, Foo.class);
        global.set(bar, Bar.LIST, bar_list);
        this.bar_list_0 = new Foo("bar_list_0");
        bar_list.add(bar_list_0);
        this.bar_list_1 = new Foo("bar_list_1");
        bar_list.add(bar_list_1);
        this.bar_list_2 = new Foo("bar_list_2");
        bar_list.add(bar_list_2);
        
        this.bar_map = new SettingsMap<Foo>(global, Foo.class);
        global.set(bar, Bar.MAP, bar_map);
        this.bar_map_a = new Foo("bar_map_a");
        bar_map.put("a", bar_map_a);
        this.bar_map_b = new Foo("bar_map_b");
        global.set(bar_map_b, Foo.FIVE, 50000);
        bar_map.put("b", bar_map_b);
        this.bar_map_c = new Foo("bar_map_c");
        bar_map.put("c", bar_map_c);
        
        this.bar_slist = new SettingsList<String>(global, String.class);
        global.set(bar, Bar.SLIST, bar_slist);
        this.bar_slist_0 = "zero";
        bar_slist.add(bar_slist_0);
        this.bar_slist_1 = "one";
        bar_slist.add(bar_slist_1);
        this.bar_slist_2 = "two";
        bar_slist.add(bar_slist_2);

        this.bar_smap = new SettingsMap<String>(global, String.class);
        global.set(bar, Bar.SMAP, bar_smap);
        this.bar_smap_a = "65";
        bar_smap.put("a", bar_smap_a);
        this.bar_smap_b = "66";
        bar_smap.put("b", bar_smap_b);
        this.bar_smap_c = "67";
        bar_smap.put("c", bar_smap_c);
        
        root.put("bar", bar);
        manager.commit(global);
        
        // ===== o1 sheet =====
        manager.addSingleSheet("o1");
        SingleSheet o1 = (SingleSheet)manager.checkout("o1");
        Map<String,Object> o1_root = new SettingsMap<Object>(o1, Object.class);
        o1.set(manager, SheetManager.ROOT, o1_root);
        
        this.o1_first_ten = "three plus seven";
        o1.set(this.first, Foo.TEN, this.o1_first_ten);        
        this.o1_second = new Foo("o1_second");
        o1_root.put("second", o1_second);
        
        this.o1_bar_list = new SettingsList<Foo>(o1, Foo.class);
        o1.set(this.bar, Bar.LIST, this.o1_bar_list);
        this.o1_bar_list_3 = new Foo("o1_bar_list_3");
        o1_bar_list.add(o1_bar_list_3);
        this.o1_bar_list_4 = new Foo("o1_bar_list_4");
        o1_bar_list.add(o1_bar_list_4);
        
        this.o1_bar_map = new SettingsMap<Foo>(o1, Foo.class);
        o1.set(this.bar, Bar.MAP, this.o1_bar_map);
        this.o1_bar_map_b = new Baz("o1_bar_map_b");
        o1_bar_map.put("b", o1_bar_map_b);
        this.o1_bar_map_d = new Foo("o1_bar_map_d");
        o1_bar_map.put("d", o1_bar_map_d);

        this.o1_bar_slist = new SettingsList<String>(o1, String.class);
        o1.set(this.bar, Bar.SLIST, this.o1_bar_slist);
        this.o1_bar_slist_3 = "three";
        o1_bar_slist.add(o1_bar_slist_3);
        this.o1_bar_slist_4 = "four";
        o1_bar_slist.add(o1_bar_slist_4);
        
        manager.commit(o1);        
    }
    

}
