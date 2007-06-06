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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.management.openmbean.CompositeData;

import org.archive.util.Transform;
import org.archive.util.Transformer;
import org.archive.openmbeans.annotations.Bean;
import org.archive.settings.Sheet;
import org.archive.settings.SheetManager;
import org.archive.settings.SingleSheet;
import org.archive.settings.file.FilePathListConsumer;
import org.archive.settings.path.PathChange;
import org.archive.settings.path.PathChangeException;
import org.archive.settings.path.PathChanger;
import org.archive.settings.path.PathLister;
import org.archive.settings.path.PathValidator;
import org.archive.state.KeyTypes;


public class JMXSheetManagerImpl extends Bean implements Serializable, JMXSheetManager {

    
    final public static String DOMAIN = "org.archive";
    
    /**
     * First version.
     */
    private static final long serialVersionUID = 1L;

    final private SheetManager manager;

    final private ConcurrentMap<String,Sheet> checkedOut 
        = new ConcurrentHashMap<String,Sheet>();
    

    public JMXSheetManagerImpl(SheetManager manager) {
        super(JMXSheetManager.class);
        this.manager = manager;
    }


    public Set<String> getSheetNames() {
        return manager.getSheetNames();
    }



    public void removeSheet(String sheetName) throws IllegalArgumentException {
        manager.removeSheet(sheetName);
    }


    public void renameSheet(String oldName, String newName) {
        manager.renameSheet(oldName, newName);
    }


    public void makeSingleSheet(String name) {
        manager.addSingleSheet(name);
    }
    
    
    public void makeSheetBundle(String name, String sheets) {
        List<Sheet> list = new ArrayList<Sheet>();
        for (String s : sheets.split(",")) {
            list.add(manager.getSheet(s));
        }
        manager.addSheetBundle(name, list);
    }

    
    public CompositeData[] getAll(String name) {
        SingleSheet sheet = (SingleSheet) manager.getSheet(name);
        JMXPathListConsumer c = new JMXPathListConsumer();
        PathLister.getAll(sheet, c);
        return c.getData();
    }


    public CompositeData[] resolveAll(String name) {
        Sheet sheet = manager.getSheet(name);
        JMXPathListConsumer c = new JMXPathListConsumer();
        PathLister.resolveAll(sheet, c);
        return c.getData();
    }


    public void setMany(String sheetName, CompositeData[] setData) {
        Sheet sh = checkedOut.get(sheetName);
        if (sh == null) {
            throw new IllegalArgumentException(sheetName + 
                    " must be checked out before it can be edited.");
        }
        SingleSheet sheet = (SingleSheet)sh;
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


    public void moveElementUp(String sheetName, String listPath, int index) {
        SingleSheet ss = getSingleSheet(sheetName);
        Object o = PathValidator.validate(ss, listPath);
        if (!(o instanceof List)) {
            throw new IllegalArgumentException(listPath + " is not a list.");
        }
        List list = (List)o;
        Collections.swap(list, index, index - 1);
    }


    public void moveElementDown(String sheetName, String listPath, int index) {
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
    
    
    public void associate(
            String sheetName, 
            String[] contexts) {
        Sheet sheet = manager.getSheet(sheetName);
        manager.associate(sheet, Arrays.asList(contexts));
    }
    
    
    public String[] getSheets() {
        Set<String> names = getSheetNames();
        return names.toArray(new String[names.size()]);
    }


    public void save() {
        manager.save();
    }
    
    
    public void reload() {
        manager.reload();
    }
    
    
    public void set(
            String sheet, 
            String path, 
            String type,
            String value) {
        SingleSheet ss = (SingleSheet)checkedOut.get(sheet);
        if (ss == null) {
            throw new IllegalArgumentException(sheet + " is not checked out.");
        }
        PathChange change = new PathChange(path, type, value);
        new PathChanger().change(ss, Collections.singleton(change));
    }

    
    public String resolve(
            String sheetName,
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
    
    
    public void associate(
            String sheetName, 
            String surt) {
        Sheet sheet = manager.getSheet(sheetName);
        manager.associate(sheet, Collections.singleton(surt));
    }


    public void disassociate(
            String sheetName, 
            String surt) {
        Sheet sheet = manager.getSheet(sheetName);
        manager.disassociate(sheet, Collections.singleton(surt));
    }


    public String getSheetFor(            
            String surt) {
        Sheet s = manager.getAssociation(surt);
        if (s == null) {
            return null;
        }
        return s.getName();
    }


    public String resolveAllAsString(
            String sheetName) {
        Sheet ss = manager.getSheet(sheetName);
        StringWriter sw = new StringWriter();
        
        FilePathListConsumer c = new FilePathListConsumer(sw);
        PathLister.resolveAll(ss, c);
        return sw.toString();
    }

    
    public String getAllAsString(
            String sheetName) {
        SingleSheet ss = getSingleSheet(sheetName);
        StringWriter sw = new StringWriter();
        FilePathListConsumer c = new FilePathListConsumer(sw);
        PathLister.getAll(ss, c);
        return sw.toString();
    }


    public void checkout(String sheetName) {
        Sheet sheet = manager.checkout(sheetName);
        checkedOut.putIfAbsent(sheetName, sheet);
    }

    
    public void commit(String sheetName) {
        Sheet sheet = checkedOut.remove(sheetName);
        manager.commit(sheet);
    }
    
    
    public void cancel(String sheetName) {
        checkedOut.remove(sheetName);
    }

    
    public String[] getCheckedOutSheets() {
        return checkedOut.keySet().toArray(new String[0]);
    }


    public String[] getProblemSingleSheetNames() {
        Set<String> problems = manager.getProblemSingleSheetNames();
        return problems.toArray(new String[problems.size()]);
    }


    public CompositeData[] getSingleSheetProblems(String sheet) {
        List<CompositeData> result = new ArrayList<CompositeData>();
        List<PathChangeException> problems = 
            manager.getSingleSheetProblems(sheet); 
        for (PathChangeException e: problems) {
            PathChange pc = e.getPathChange();
            result.add(Types.makeSetResult(
                    pc.getType(),
                    pc.getPath(),
                    pc.getValue(),
                    e.getMessage()));
        }
        return result.toArray(new CompositeData[result.size()]);
    }


}
