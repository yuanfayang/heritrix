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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.archive.crawler2.settings.NamedObject;
import org.archive.crawler2.settings.Sheet;
import org.archive.crawler2.settings.SheetBundle;
import org.archive.crawler2.settings.SheetManager;
import org.archive.crawler2.settings.SingleSheet;
import org.archive.openmbeans.annotations.Bean;
import org.archive.openmbeans.annotations.Operation;
import org.archive.openmbeans.annotations.Parameter;


public class JMXSheetManager extends SheetManager implements DynamicMBean {

    
    final private SheetManager manager;
    final private MBeanServer server;
    final private Bean support;


    public JMXSheetManager(MBeanServer server, SheetManager manager) {
        this.manager = manager;
        this.server = server;
        this.support = new Bean(this);
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
    protected void addSheet(Sheet sheet, String name) {
        // No-op...
    }


    
    @Override
    public SingleSheet createSingleSheet(String name) {
        SingleSheet r = manager.createSingleSheet(name);
        SingleSheetProxy proxy = new SingleSheetProxy(r);
        registerSheet(name, proxy);
        return r;
    }

    
    @Override
    public SheetBundle createSheetBundle(String name, Collection<Sheet> c) {
        SheetBundle r = manager.createSheetBundle(name, c);
        SheetBundleProxy proxy = new SheetBundleProxy(r);
        registerSheet(name, proxy);
        registerSheet(name, r);
        return r;
    }
    
    
    private ObjectName toObjectName(String sheetName) {
        String domain = Sheet.class.getName();
        try {
            return new ObjectName(domain, "name", sheetName); 
        } catch (MalformedObjectNameException e) {
            throw new IllegalArgumentException(e);
        }
    }


    private void registerSheet(String name, Object proxy) {
        try {
            server.registerMBean(proxy, toObjectName(name));
        } catch (InstanceAlreadyExistsException e) {
            throw new IllegalStateException(e);
        } catch (MBeanRegistrationException e) {
            throw new IllegalStateException(e);            
        } catch (NotCompliantMBeanException e) {
            throw new IllegalStateException(e);            
        }        
    }
    
    
    private void unregisterSheet(String sheetName) {
        ObjectName oname = toObjectName(sheetName);
        try {
            server.unregisterMBean(oname);
        } catch (InstanceNotFoundException e) {
            throw new IllegalStateException(e);
        } catch (MBeanRegistrationException e) {
            throw new IllegalStateException(e);
        }        
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


    @Override
    public void removeSheet(String sheetName) throws IllegalArgumentException {
        manager.removeSheet(sheetName);
    }


    @Override
    public void renameSheet(String oldName, String newName) {
        unregisterSheet(oldName);
        Sheet sheet = manager.getSheet(oldName);
        manager.renameSheet(oldName, newName);
        Object proxy;
        if (sheet instanceof SingleSheet) {
            SingleSheet ss = (SingleSheet)sheet;
            proxy = new SingleSheetProxy(ss);
        } else {
            SheetBundle sb = (SheetBundle)sheet;
            proxy = new SheetBundleProxy(sb);
        }
        registerSheet(newName, proxy);
    }


    @Operation(desc="Creates a new single sheet.", impact=Bean.ACTION)
    public void makeSingleSheet(
            @Parameter(name="name", desc="The name for the new sheet")
            String name) {
        createSingleSheet(name);
    }
    
    
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
        createSheetBundle(name, list);
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

}
