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
 * JMXSheetManager.java
 * Created on October 24, 2006
 *
 * $Header$
 */
package org.archive.settings.jmx;

import java.io.Serializable;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.management.openmbean.CompositeData;

import org.archive.util.Transform;
import org.archive.util.Transformer;
import org.archive.openmbeans.annotations.Bean;
import org.archive.openmbeans.annotations.Operation;
import org.archive.openmbeans.annotations.Parameter;
import org.archive.settings.Sheet;
import org.archive.settings.SheetManager;
import org.archive.settings.SingleSheet;
import org.archive.settings.file.FilePathListConsumer;
import org.archive.settings.path.PathChange;
import org.archive.settings.path.PathChanger;
import org.archive.settings.path.PathLister;
import org.archive.settings.path.PathValidator;
import org.archive.state.KeyTypes;


public class JMXSheetManager extends Bean implements Serializable {

    
    final public static String DOMAIN = "org.archive";
    
    /**
     * First version.
     */
    private static final long serialVersionUID = 1L;

    final private SheetManager manager;


    public JMXSheetManager(SheetManager manager) {
        this.manager = manager;
    }


    public Set<String> getSheetNames() {
        return manager.getSheetNames();
    }


    @Operation(desc="Removes the sheet with the given name.")
    public void removeSheet(
            @Parameter(name="sheetName", desc="The name of the sheet to remove.")
            String sheetName) throws IllegalArgumentException {
        manager.removeSheet(sheetName);
    }


    @Operation(desc="Renames a sheet.")
    public void renameSheet(
            
            @Parameter(name="oldName", desc="The old name of the sheet.")
            String oldName, 
            
            @Parameter(name="newName", desc="The new name for the sheet.")
            String newName) {
        manager.renameSheet(oldName, newName);
    }


    @Operation(desc="Creates a new single sheet.", impact=Bean.ACTION)
    public void makeSingleSheet(
            @Parameter(name="name", desc="The name for the new sheet")
            String name) {
        manager.addSingleSheet(name);
    }
    
    
    @Operation(desc="Creates a new sheet bundle.", impact=Bean.ACTION)
    public void makeSheetBundle(
            @Parameter(name="name", desc="The name for the new sheet")
            String name, 
            
            @Parameter(name="sheets", desc="The names of the sheets " 
                + "to include in the bundle, in order of priority.")
            String sheets) {
        List<Sheet> list = new ArrayList<Sheet>();
        for (String s: sheets.split(",")) {
            list.add(manager.getSheet(s));
        }
        manager.addSheetBundle(name, list);
    }

    
    @Operation(desc="Returns the settings overriden by the given single sheet.",
            type="org.archive.settings.jmx.Types.GET_DATA_ARRAY")
    public CompositeData[] getAll(
            @Parameter(name="name", desc=
            "The name of the single sheet whose overrides to return.")
            String name
    ) {
        SingleSheet sheet = (SingleSheet)manager.getSheet(name);
        JMXPathListConsumer c = new JMXPathListConsumer();
        PathLister.getAll(sheet, c);
        return c.getData();
    }


    @Operation(desc="Resolves all settings defined by the given sheet.", 
            type="org.archive.settings.jmx.Types.GET_DATA_ARRAY")
    public CompositeData[] resolveAll(
            @Parameter(name="name", desc=
            "The name of the single sheet whose overrides to return.")
            String name
    ) {
        Sheet sheet = manager.getSheet(name);
        JMXPathListConsumer c = new JMXPathListConsumer();
        PathLister.resolveAll(sheet, c);
        return c.getData();
    }
    
    
    @Operation(desc="Alters one or more settings in a single sheet.")
    public void setMany(
            @Parameter(name="sheetName", desc="The name of the single sheet " +
                    "whose settings to change.")
            String sheetName,
            
            @Parameter(name="setData", desc="An array of path/values to set.",
             type="org.archive.settings.jmx.Types.SET_DATA_ARRAY")
            CompositeData[] setData) 
    {
        SingleSheet sheet = (SingleSheet)manager.getSheet(sheetName);
        Transformer<CompositeData,PathChange> transformer
         = new Transformer<CompositeData,PathChange>() {
            public PathChange transform(CompositeData cd) {
                String type = (String)cd.get("type");
                String path = (String)cd.get("path");
                String value = (String)cd.get("value");
                return new PathChange(path, type, value);
            }
        };
        Collection<CompositeData> c = Arrays.asList(setData);
        Transform<CompositeData,PathChange> changes
         = new Transform<CompositeData,PathChange>(c, transformer); 
        new PathChanger().change(sheet, changes);
    }


    @Operation(desc="Moves an element in a list up one position.")
    public void moveElementUp(
            @Parameter(name="sheetName", desc=
            "The name of the sheet containing the list.")
            String sheetName,
            
            @Parameter(name="listPath", desc=
            "The path to the list.")
            String listPath, 
            
            @Parameter(name="index", desc=
            "The index of the element to move up.")
            int index) 
    {
        SingleSheet ss = getSingleSheet(sheetName);
        Object o = PathValidator.validate(ss, listPath);
        if (!(o instanceof List)) {
            throw new IllegalArgumentException(listPath + " is not a list.");
        }
        List list = (List)o;
        Collections.swap(list, index, index - 1);
    }
    
    
    @Operation(desc="Moves an element in a list down one position.")
    public void moveElementDown(
            @Parameter(name="sheetName", desc=
            "The name of the sheet containing the list.")
            String sheetName,
            
            @Parameter(name="listPath", desc=
            "The path to the list.")
            String listPath, 
            
            @Parameter(name="index", desc=
            "The index of the element to move up.")
            int index) 
    {
        SingleSheet ss = getSingleSheet(sheetName);
        Object o = PathValidator.validate(ss, listPath);
        if (!(o instanceof List)) {
            throw new IllegalArgumentException(listPath + " is not a list.");
        }
        List list = (List)o;
        Collections.swap(list, index, index + 1);        
    }


    private SingleSheet getSingleSheet(String name) {
        Sheet s = manager.getSheet(name);
        if (!(s instanceof SingleSheet)) {
            throw new IllegalArgumentException(name + " is not a SingleSheet.");
        }
        return (SingleSheet)s;
    }
    
    
    @Operation(desc="Associates one or more SURTs with a sheet.")
    public void associate(
            
            @Parameter(name="sheetName", desc="The name of the sheet to associate the SURTs with.")
            String sheetName, 
            
            @Parameter(name="surts", desc="The surts to associate with that sheet.")
            String[] contexts) {
        Sheet sheet = manager.getSheet(sheetName);
        manager.associate(sheet, Arrays.asList(contexts));
    }
    
    
    @org.archive.openmbeans.annotations.Attribute(desc="The names of the sheets being managed.", def="")
    public String[] getSheets() {
        Set<String> names = getSheetNames();
        return names.toArray(new String[names.size()]);
    }


    @Operation(desc="Saves all settings currently in memory to persistent storage.")
    public void save() {
        manager.save();
    }
    
    
    @Operation(desc="Reloads all settings from persistent storage.")
    public void reload() {
        manager.reload();
    }
    
    
    @Operation(desc="Sets one setting in a SingleSheet.")
    public void set(
            
            @Parameter(name="sheetName", desc="The name of the sheet whose setting to change.")
            String sheet, 
            
            @Parameter(name="path", desc="The path to the setting to change.")
            String path, 
            
            @Parameter(name="type", desc="The type of that setting.")
            String type,
            
            @Parameter(name="value", desc="The new value for the setting at that path.")
            String value) {
        SingleSheet ss = getSingleSheet(sheet);
        PathChange change = new PathChange(path, type, value);
        new PathChanger().change(ss, Collections.singleton(change));
    }

    
    @Operation(desc="Resolves the value of a setting.")
    public String resolve(
            
            @Parameter(name="sheetName", desc="The name of the sheet whose setting to resolve.")
            String sheetName,
            
            @Parameter(name="path", desc="The path to the setting whose value to resolve.")
            String path
            ) {
        Sheet sheet = manager.getSheet(sheetName);
        Object v = PathValidator.validate(sheet, path);
        if (v == null) {
            return null;
        }
        if (KeyTypes.isSimple(v.getClass())) {
            return v.toString();
        }
        return v.getClass().getName();
    }
    
    
    @Operation(desc="Associates a sheet with a SURT prefix.")
    public void associate(
        
            @Parameter(name="sheetName", desc="The name of the sheet to associate with the SURT.")
            String sheetName, 
            
            @Parameter(name="surt", desc="The SURT to associate with that sheet.")
            String surt) {
        Sheet sheet = manager.getSheet(sheetName);
        manager.associate(sheet, Collections.singleton(surt));
    }

    
    @Operation(desc="Disassociates a SURT from a sheet.")
    public void disassociate(
        
            @Parameter(name="sheetName", desc="The name of the sheet to disassociate from the SURT.")
            String sheetName, 
            
            @Parameter(name="surt", desc="The SURT to disassociate from that sheet.")
            String surt) {
        Sheet sheet = manager.getSheet(sheetName);
        manager.disassociate(sheet, Collections.singleton(surt));
    }


    @Operation(desc="Returns the sheet associated with the given SURT prefix, if any.")
    public String getSheetFor(
            
            @Parameter(name="surt", desc="The SURT whose sheet to return.")
            String surt) {
        Sheet s = manager.getAssociation(surt);
        if (s == null) {
            return null;
        }
        return s.getName();
    }


    @Operation(desc="Resolves all settings in the given sheet.")
    public String resolveAllAsString(
            
            @Parameter(name="sheetName", desc="The name of the sheet whose settings to resolve.")
            String sheetName) {
        Sheet ss = manager.getSheet(sheetName);
        StringWriter sw = new StringWriter();
        
        FilePathListConsumer c = new FilePathListConsumer(sw);
        PathLister.resolveAll(ss, c);
        return sw.toString();
    }

    
    @Operation(desc="Returns only those settings that are overriden by the given sheet.")
    public String getAllAsString(
            
            @Parameter(name="sheetName", desc="The name of the sheet whose settings to resolve.")
            String sheetName) {
        SingleSheet ss = getSingleSheet(sheetName);
        StringWriter sw = new StringWriter();
        FilePathListConsumer c = new FilePathListConsumer(sw);
        PathLister.getAll(ss, c);
        return sw.toString();
    }


}
