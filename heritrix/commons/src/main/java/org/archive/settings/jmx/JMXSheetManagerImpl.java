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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.OpenDataException;

import org.archive.util.Transform;
import org.archive.util.Transformer;
import org.archive.openmbeans.annotations.Bean;
import org.archive.settings.Association;
import org.archive.settings.SettingsList;
import org.archive.settings.SettingsMap;
import org.archive.settings.Sheet;
import org.archive.settings.SheetBundle;
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

    final private Map<String,Sheet> checkedOut 
        = new HashMap<String,Sheet>();

//    final private Map<String,List<PathChangeException>> problems;
    
    final private Map<String,Map<String,PathChangeException>> problems;
    
    private ObjectName oname;
    
    public JMXSheetManagerImpl(String job, String domain, SheetManager manager) {
        super(JMXSheetManager.class);
        this.manager = manager;
        this.problems = new HashMap<String,Map<String,PathChangeException>>();
        for (String s: manager.getProblemSingleSheetNames()) {
            Map<String,PathChangeException> sheetProblems = 
                new HashMap<String,PathChangeException>();
            problems.put(s, sheetProblems);
            for (PathChangeException e: manager.getSingleSheetProblems(s)) {
                sheetProblems.put(e.getPathChange().getPath(), e);
            }
        }
        this.oname = JMXModuleListener.nameOf(domain, job, this);
    }

    public ObjectName getObjectName(){
        return oname;
    }
    
    private Sheet getSheet(String sheetName) {
        Sheet result = checkedOut.get(sheetName);
        if (result != null) {
            return result;
        }
        
        return manager.getSheet(sheetName);
    }
    
    

    public synchronized Set<String> getSheetNames() {
        return manager.getSheetNames();
    }



    public synchronized void removeSheet(String sheetName) 
    throws IllegalArgumentException {
        manager.removeSheet(sheetName);
    }


    public synchronized void renameSheet(String oldName, String newName) {
        manager.renameSheet(oldName, newName);
    }


    public synchronized void makeSingleSheet(String name) {
        manager.addSingleSheet(name);
    }
    
    
    public synchronized void makeSheetBundle(String name) {
        Collection<Sheet> empty = Collections.emptyList();
        manager.addSheetBundle(name, empty);
    }

    
    public synchronized CompositeData[] getAll(String name) {
        SingleSheet sheet = (SingleSheet)getSheet(name);
        JMXPathListConsumer c = new JMXPathListConsumer();
        PathLister.getAll(sheet, c, true);
        return c.getData();
    }


    public synchronized CompositeData[] resolveAll(String name) {
        Sheet sheet = getSheet(name);
        JMXPathListConsumer c = new JMXPathListConsumer();
        PathLister.resolveAll(sheet, c, false);
        return c.getData();
    }


    public synchronized void setMany(String sheetName, CompositeData[] setData) {
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
        PathChanger pc = new PathChanger();

        Map<String,PathChangeException> sheetProblems = 
            problems.get(sheetName);
        if (sheetProblems == null) {
            sheetProblems = new HashMap<String,PathChangeException>();
        }

        for (PathChange change: changes) {
            sheetProblems.remove(change.getPath());
            try {
                pc.change(sheet, change);
            } catch (PathChangeException e) {
                sheetProblems.put(change.getPath(), e);
            }
        }

        for (PathChangeException e: pc.getProblems()) {
            sheetProblems.put(e.getPathChange().getPath(), e);
        }
        
        if (sheetProblems.isEmpty()) {
            problems.remove(sheetName);
        } else {
            problems.put(sheetName, sheetProblems);
        }

    }

    
    private void moveElement(String sheetName, String path, boolean up) {
        SingleSheet ss = (SingleSheet)checkedOut.get(sheetName);
        int p = path.lastIndexOf(PathValidator.DELIMITER);
        if (p < 0) {
            throw new IllegalArgumentException(
                    "Path does not point to a container: " + path);
        }
        String parentPath = path.substring(0, p);
        String key = path.substring(p + 1);
        Object o = PathValidator.check(ss, parentPath);

        if (o instanceof SettingsList) {
            int index = Integer.parseInt(key);
            int index2 = up ? index-- : index++;
            Collections.swap((SettingsList)o, index, index2);
        } else if (o instanceof SettingsMap) {
            ((SettingsMap)o).moveElement(key, up);
        } else {
            throw new IllegalArgumentException(parentPath + 
                    " is not a container.");
        }        
    }

    public synchronized void moveElementUp(String sheetName, String path) {
        moveElement(sheetName, path, true);
    }


    public synchronized void moveElementDown(String sheetName, String path) {
        moveElement(sheetName, path, false);
   }


    private SingleSheet getSingleSheet(String name) {
        Sheet s = getSheet(name);
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
    
    
    public synchronized String[] getSheets() {
        Set<String> names = getSheetNames();
        return names.toArray(new String[names.size()]);
    }


    public synchronized void save() {
        manager.save();
    }
    
    
    public synchronized void reload() {
        manager.reload();
    }
    
    
    public void set(
            String sheet, 
            String path, 
            String type,
            String value) {
        CompositeData[] cd = new CompositeData[1];
        try {
            cd[0] = new CompositeDataSupport(
                    Types.SET_DATA,
                    new String[] { "path", "type", "value" },
                    new Object[] { path, type, value });
        } catch (OpenDataException e) {
            
        } 
        setMany(sheet, cd);
    }

    
    public synchronized String resolve(
            String sheetName,
            String path
            ) {
        Sheet sheet = getSheet(sheetName);
        Object v = PathValidator.validate(sheet, path);
        if (v == null) {
            return null;
        }
        if (KeyTypes.isSimple(v.getClass())) {
            return v.toString();
        }
        return v.getClass().getName();
    }
    
    
    public synchronized void associate(
            String sheetName, 
            String surt) {
        Sheet sheet = manager.getSheet(sheetName);
        manager.associate(sheet, Collections.singleton(surt));
    }


    public synchronized void disassociate(
            String sheetName, 
            String surt) {
        Sheet sheet = manager.getSheet(sheetName);
        manager.disassociate(sheet, Collections.singleton(surt));
    }



    public synchronized String resolveAllAsString(
            String sheetName) {
        Sheet ss = getSheet(sheetName);
        StringWriter sw = new StringWriter();
        
        FilePathListConsumer c = new FilePathListConsumer(sw);
        PathLister.resolveAll(ss, c, false);
        return sw.toString();
    }

    
    public synchronized String getAllAsString(
            String sheetName) {
        SingleSheet ss = getSingleSheet(sheetName);
        StringWriter sw = new StringWriter();
        FilePathListConsumer c = new FilePathListConsumer(sw);
        PathLister.getAll(ss, c, true);
        return sw.toString();
    }


    public synchronized void checkout(String sheetName) {
        Sheet sheet = manager.checkout(sheetName);
        checkedOut.put(sheetName, sheet);
    }

    
    public synchronized void commit(String sheetName) {
        Sheet sheet = checkedOut.remove(sheetName);
        manager.commit(sheet);
    }
    
    
    public synchronized void cancel(String sheetName) {
        checkedOut.remove(sheetName);
    }

    
    public synchronized String[] getCheckedOutSheets() {
        return checkedOut.keySet().toArray(new String[0]);
    }


    public synchronized String[] getProblemSingleSheetNames() {
        Set<String> problems = this.problems.keySet();
        return problems.toArray(new String[problems.size()]);
    }


    public synchronized CompositeData[] getSingleSheetProblems(String sheet) {
        List<CompositeData> result = new ArrayList<CompositeData>();
        Map<String,PathChangeException> sheetProblems = problems.get(sheet);
        if (sheetProblems == null) {
            return new CompositeData[0];
        }
        for (PathChangeException e: sheetProblems.values()) {
            PathChange pc = e.getPathChange();
            result.add(Types.makeSetResult(
                    pc.getType(),
                    pc.getPath(),
                    pc.getValue(),
                    e.getMessage()));
        }
        return result.toArray(new CompositeData[result.size()]);
    }


    public CompositeData[] findConfig(String uri) {
        Sheet sheet = manager.findConfig(uri);
        JMXPathListConsumer c = new JMXPathListConsumer();
        PathLister.resolveAll(sheet, c, false);
        return c.getData();        
    }
    
    
    public String[] findConfigNames(String uri) {
        List<Association> surtToSheet = manager.findConfigNames(uri);
        String[] result = new String[surtToSheet.size() * 2];
        int i = 0;
        for (Association assoc: surtToSheet) {
            result[i] = assoc.getContext();
            result[i + 1] = assoc.getSheetName();
            i += 2;
        }
        return result;
    }


    public synchronized boolean isSingleSheet(String sheetName) {
        Sheet sheet = getSheet(sheetName);
        if (sheet == null) {
            throw new IllegalArgumentException("No such sheet: " + sheetName);
        }
        
        return sheet instanceof SingleSheet;
    }
    
    
    public synchronized String[] getBundledSheets(String bundleName) {
        Sheet sheet = getSheet(bundleName);
        if (!(sheet instanceof SheetBundle)) {
            throw new IllegalArgumentException(bundleName + " is not a bundle.");
        }
        
        SheetBundle bundle = (SheetBundle)sheet;
        List<Sheet> sheets = bundle.getSheets();
        String[] result = new String[sheets.size()];
        for (int i = 0; i < sheets.size(); i++) {
            result[i] = sheets.get(i).getName();
        }
        return result;
    }

    
    public synchronized void moveBundledSheet(
            String bundleName, 
            String move, 
            int index) {
        Sheet sheet = checkedOut.get(bundleName);
        if (sheet == null) {
            throw new IllegalArgumentException(bundleName + 
                    " is not checked out.");
        }
        if (!(sheet instanceof SheetBundle)) {
            throw new IllegalArgumentException(bundleName + " is not a bundle.");
        }
        
        SheetBundle bundle = (SheetBundle)sheet;
        List<Sheet> sheets = bundle.getSheets();
        int moveIndex = indexOf(sheets, move);
        
        if (moveIndex < 0) {
            Sheet s = manager.getSheet(move);
            if (s == null) {
                throw new IllegalArgumentException("No such sheet: " + move);
            }
            moveIndex = sheets.size();
            sheets.add(s);
        }
        
        if (index == moveIndex) {
            return;
        }
        
        Sheet s = sheets.remove(moveIndex);
        sheets.add(index, s);
    }
    
    
    private static int indexOf(List<Sheet> sheets, String name) {
        int i = 0;
        for (Sheet sheet: sheets) {
            if (sheet.getName().equals(name)) {
                return i;
            }
            i++;
        }
        return -1;
    }
    
    
    public boolean isOnline() {
        return manager.isOnline();
    }

    
    public String[] listContexts(String sheetName, int start) {
        Collection<String> c = manager.listContexts(sheetName, start, 100);
        return c.toArray(new String[c.size()]);
    }


    public synchronized void offlineCleanup() {
        manager.offlineCleanup();
    }

    
    public void remove(String sheetName, String path) {
        SingleSheet sheet = getSingleSheet(sheetName);
        PathChanger.remove(sheet, path);
    }
}
