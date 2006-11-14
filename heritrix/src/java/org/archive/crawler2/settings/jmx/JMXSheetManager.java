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
package org.archive.crawler2.settings.jmx;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.openmbean.CompositeData;

import org.archive.crawler.util.Transform;
import org.archive.crawler.util.Transformer;
import org.archive.crawler2.settings.NamedObject;
import org.archive.crawler2.settings.Sheet;
import org.archive.crawler2.settings.SheetBundle;
import org.archive.crawler2.settings.SheetManager;
import org.archive.crawler2.settings.SingleSheet;
import org.archive.crawler2.settings.file.FilePathListConsumer;
import org.archive.crawler2.settings.path.PathChange;
import org.archive.crawler2.settings.path.PathChanger;
import org.archive.crawler2.settings.path.PathLister;
import org.archive.crawler2.settings.path.PathValidator;
import org.archive.crawler2.settings.path.Paths;
import org.archive.openmbeans.annotations.Bean;
import org.archive.openmbeans.annotations.Operation;
import org.archive.openmbeans.annotations.Parameter;


public class JMXSheetManager extends SheetManager implements DynamicMBean {

    
    final private SheetManager manager;
    final private Bean support;


    public JMXSheetManager(MBeanServer server, SheetManager manager) 
    throws MBeanRegistrationException {
        this.manager = manager;
        this.support = new Bean(this);
        ObjectName name;
        try {
            // FIXME: Think of something sensible here.
            name = new ObjectName("archive.org", "id", 
                    Integer.toString(System.identityHashCode(this)));
        } catch (MalformedObjectNameException e) {
            // This never should have been a checked exception.
            throw new AssertionError();
        }
        try {
            server.registerMBean(this, name);
        } catch (NotCompliantMBeanException e) {
            // Framework ensures compliance.
            throw new AssertionError();
        } catch (InstanceAlreadyExistsException e) {
            // Object name guaranteed to be unique
            throw new AssertionError();
        }
    }


    public Object getAttribute(String attribute) 
    throws AttributeNotFoundException, ReflectionException {
        return support.getAttribute(attribute);
    }


    public AttributeList getAttributes(String[] attributes) {
        return support.getAttributes(attributes);
    }


    public MBeanInfo getMBeanInfo() {
        return support.getMBeanInfo();
    }


    public Object invoke(String actionName, Object[] params, String[] sig) 
    throws MBeanException, ReflectionException {
        return support.invoke(actionName, params, sig);
    }


    public void setAttribute(Attribute attribute) 
    throws AttributeNotFoundException, ReflectionException {
        support.setAttribute(attribute);
    }


    public AttributeList setAttributes(AttributeList attributes) {
        return support.setAttributes(attributes);
    }




    
    @Override
    public SingleSheet addSingleSheet(String name) {
        return manager.addSingleSheet(name);
    }

    
    @Override
    public SheetBundle addSheetBundle(String name, Collection<Sheet> c) {
        SheetBundle r = manager.addSheetBundle(name, c);
        return r;
    }


    @Override
    public void associate(Sheet sheet, Iterable<String> strings) {
        manager.associate(sheet, strings);
    }


    @Override
    public void disassociate(Sheet sheet, Iterable<String> strings) {
        manager.disassociate(sheet, strings);
    }


    @Override
    public Sheet getAssociation(String context) {
        return manager.getAssociation(context);
    }


    @Override
    public SingleSheet getDefault() {
        return manager.getDefault();
    }


    @Override
    public List<NamedObject> getRoots() {
        return manager.getRoots();
    }


    @Override
    public Sheet getSheet(String sheetName) throws IllegalArgumentException {
        return manager.getSheet(sheetName);
    }


    @Override
    public Set<String> getSheetNames() {
        return manager.getSheetNames();
    }


    @Operation(desc="Removes the sheet with the given name.")
    @Override
    public void removeSheet(
            @Parameter(name="sheetName", desc="The name of the sheet to remove.")
            String sheetName) throws IllegalArgumentException {
        manager.removeSheet(sheetName);
    }


    @Operation(desc="Renames a sheet.")
    @Override
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
        addSingleSheet(name);
    }
    
  /*  
    @Operation(desc="Creates a new sheet bundle.", impact=Bean.ACTION)
    public void makeSheetBundle(
            @Parameter(name="name", desc="The name for the new sheet")
            String name, 
            
            @Parameter(name="sheets", desc="The names of the sheets " 
                + "to include in the bundle, in order of priority.")
            String[] sheets) {
        List<Sheet> list = new ArrayList<Sheet>();
        for (String s: sheets) {
            list.add(getSheet(s));
        }
        addSheetBundle(name, list);
    }

*/
    
    @Operation(desc="Creates a new sheet bundle.", impact=Bean.ACTION)
    public void makeSheetBundle(
            @Parameter(name="name", desc="The name for the new sheet")
            String name, 
            
            @Parameter(name="sheets", desc="The names of the sheets " 
                + "to include in the bundle, in order of priority.")
            String sheets) {
        List<Sheet> list = new ArrayList<Sheet>();
        for (String s: sheets.split(",")) {
            list.add(getSheet(s));
        }
        addSheetBundle(name, list);
    }

    
    public void addRoot(String name, Object root) {
        manager.addRoot(name, root);
    }
    

    @Operation(desc="Instantiates and adds a new object to the list of root objects.")
    public void addRoot(
            @Parameter(name="name", desc="The name for the new root name.")
            String name,
            
            @Parameter(name="className", desc="The fully-qualified Java "
             + "class name of the new root name.")
            String className) 
    throws ClassNotFoundException, InstantiationException, IllegalAccessException
    {
        Object o = Class.forName(className).newInstance();
        manager.addRoot(name, o);
    }


    @Override
    @Operation(desc="Moves an object in the root object list up one position.")
    public void moveRootUp(
            @Parameter(name="name", 
                    desc="The name of the root object to move up.")
            String name) 
    {
        manager.moveRootUp(name);
    }


    @Override
    @Operation(desc="Moves an object in the root object list down one position.")
    public void moveRootDown(
            @Parameter(name="name", 
                    desc="The name of the root object to move down.")
            String name) 
    {
        manager.moveRootDown(name);
    }
    
 
    @Override
    @Operation(desc="Removes a root object.  Any sheets that configure the " +
            "removed root will have that root's configuration removed.")
    public void removeRoot(
            @Parameter(name="rootName", desc="The name of the root to remove.")
            String rootName) {
        manager.removeRoot(rootName);
    }

    
    @Override
    public void swapRoot(String name, Object newValue) {
        manager.swapRoot(name, newValue);
    }
    
    
    @Operation(desc="Returns the settings overriden by the given single sheet.",
            type="org.archive.crawler2.settings.jmx.Types.GET_DATA_ARRAY")
    public CompositeData[] getAll(
            @Parameter(name="name", desc=
            "The name of the single sheet whose overrides to return.")
            String name
    ) {
        SingleSheet sheet = (SingleSheet)getSheet(name);
        JMXPathListConsumer c = new JMXPathListConsumer();
        PathLister.getAll(sheet, c);
        return c.getData();
    }


    @Operation(desc="Resolves all settings defined by the given sheet.", 
            type="org.archive.crawler2.settings.jmx.Types.GET_DATA_ARRAY")
    public CompositeData[] resolveAll(
            @Parameter(name="name", desc=
            "The name of the single sheet whose overrides to return.")
            String name
    ) {
        Sheet sheet = getSheet(name);
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
             type="org.archive.crawler2.settings.jmx.Types.SET_DATA_ARRAY")
            CompositeData[] setData) 
    {
        SingleSheet sheet = (SingleSheet)getSheet(sheetName);
        Transformer<CompositeData,PathChange> transformer
         = new Transformer<CompositeData,PathChange>() {
            public PathChange transform(CompositeData cd) {
                String path = (String)cd.get("path");
                String value = (String)cd.get("value");
                return new PathChange(path, value);
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
        Sheet s = getSheet(name);
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
        Sheet sheet = getSheet(sheetName);
        manager.associate(sheet, Arrays.asList(contexts));
    }
    
    
    @org.archive.openmbeans.annotations.Attribute(desc="The names of the sheets being managed.", def="")
    public String[] getSheets() {
        Set<String> names = getSheetNames();
        return names.toArray(new String[names.size()]);
    }
    
    
    @org.archive.openmbeans.annotations.Attribute(desc="The names of the root processors.", def="")
    public String[] getRootNames() {
        List<NamedObject> list = getRoots();
        String[] r = new String[list.size()];
        for (int i = 0; i < r.length; i++) {
            r[i] = list.get(i).getName();
        }
        return r;
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
            
            @Parameter(name="value", desc="The new value for the setting at that path.")
            String value) {
        SingleSheet ss = getSingleSheet(sheet);
        PathChange change = new PathChange(path, value);
        new PathChanger().change(ss, Collections.singleton(change));
    }

    
    @Operation(desc="Resolves the value of a setting.")
    public String resolve(
            
            @Parameter(name="sheetName", desc="The name of the sheet whose setting to resolve.")
            String sheetName,
            
            @Parameter(name="path", desc="The path to the setting whose value to resolve.")
            String path
            ) {
        Sheet sheet = getSheet(sheetName);
        Object v = PathValidator.validate(sheet, path);
        if (v == null) {
            return null;
        }
        if (Paths.isSimple(v.getClass())) {
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
        Sheet sheet = getSheet(sheetName);
        manager.associate(sheet, Collections.singleton(surt));
    }

    
    @Operation(desc="Disassociates a SURT from a sheet.")
    public void disassociate(
        
            @Parameter(name="sheetName", desc="The name of the sheet to disassociate from the SURT.")
            String sheetName, 
            
            @Parameter(name="surt", desc="The SURT to disassociate from that sheet.")
            String surt) {
        Sheet sheet = getSheet(sheetName);
        manager.disassociate(sheet, Collections.singleton(surt));
    }


    @Operation(desc="Returns the sheet associated with the given SURT prefix, if any.")
    public String getSheetFor(
            
            @Parameter(name="surt", desc="The SURT whose sheet to return.")
            String surt) {
        Sheet s = getAssociation(surt);
        if (s == null) {
            return null;
        }
        return s.getName();
    }


    @Operation(desc="Resolves all settings in the given sheet.")
    public String resolveAllAsString(
            
            @Parameter(name="sheetName", desc="The name of the sheet whose settings to resolve.")
            String sheetName) {
        Sheet ss = getSheet(sheetName);
        StringWriter sw = new StringWriter();
        FilePathListConsumer c = new FilePathListConsumer(ss, sw);
        PathLister.resolveAll(ss, c);
        return sw.toString();
    }

    
    @Operation(desc="Returns only those settings that are overriden by the given sheet.")
    public String getAllAsString(
            
            @Parameter(name="sheetName", desc="The name of the sheet whose settings to resolve.")
            String sheetName) {
        SingleSheet ss = getSingleSheet(sheetName);
        StringWriter sw = new StringWriter();
        FilePathListConsumer c = new FilePathListConsumer(ss, sw);
        PathLister.getAll(ss, c);
        return sw.toString();
    }


}
