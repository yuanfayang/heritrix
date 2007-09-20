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

import java.io.File;
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
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.OpenDataException;

import org.archive.util.Transform;
import org.archive.util.Transformer;
import org.archive.openmbeans.annotations.Bean;
import org.archive.settings.Association;
import org.archive.settings.Offline;
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
import org.archive.state.Path;


public class JMXSheetManagerImpl extends Bean implements Serializable, JMXSheetManager {

    // IMPORTANT: a JMXSheetManager in stub mode will automatically close itself
    // after a 10 minute timeout.  We do this to prevent crawl operators from
    // having to remember to manually close profiles.  If you add a method to
    // the public API, it should invoke stamp() before doing anything, to 
    // reset the "lastUse" timestamp.
    
    final private static Timer TIMER = new Timer(true);

    final private static long TIMEOUT = 10 * 60 * 1000;
    
    final public static String DOMAIN = "org.archive";
    
    final private static Logger LOGGER = 
        Logger.getLogger(JMXSheetManagerImpl.class.getName());
    
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
    private long lastUse;
    final private TimerTask reapTask;
    
    
    public JMXSheetManagerImpl(MBeanServer server, 
            String job, String domain, SheetManager manager) {
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
        LoggingDynamicMBean.register(server, this, oname);
        this.lastUse = System.currentTimeMillis();
        if (manager.isOnline()) {
            this.reapTask = null;
        } else {
            this.reapTask = new ReapTask(server);
            TIMER.schedule(reapTask, TIMEOUT, TIMEOUT);
        }
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
    
    
    private void stamp() {
        this.lastUse = System.currentTimeMillis();
    }
    

    public synchronized Set<String> getSheetNames() {
        stamp();
        return manager.getSheetNames();
    }



    public synchronized void removeSheet(String sheetName) 
    throws IllegalArgumentException {
        stamp();
        manager.removeSheet(sheetName);
    }


    public synchronized void renameSheet(String oldName, String newName) {
        stamp();
        manager.renameSheet(oldName, newName);
    }


    public synchronized void makeSingleSheet(String name) {
        stamp();
        manager.addSingleSheet(name);
    }
    
    
    public synchronized void makeSheetBundle(String name) {
        stamp();
        Collection<Sheet> empty = Collections.emptyList();
        manager.addSheetBundle(name, empty);
    }

    
    public synchronized CompositeData[] getAll(String name) {
        stamp();
        SingleSheet sheet = (SingleSheet)getSheet(name);
        JMXPathListConsumer c = new JMXPathListConsumer();
        PathLister.getAll(sheet, c, true);
        return c.getData();
    }


    public synchronized CompositeData[] resolveAll(String name) {
        stamp();
        Sheet sheet = getSheet(name);
        JMXPathListConsumer c = new JMXPathListConsumer();
        PathLister.resolveAll(sheet, c, false);
        return c.getData();
    }


    public synchronized void setMany(String sheetName, CompositeData[] setData) {
        stamp();
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
            Collections.swap((SettingsList<?>)o, index, index2);
        } else if (o instanceof SettingsMap) {
            ((SettingsMap<?>)o).moveElement(key, up);
        } else {
            throw new IllegalArgumentException(parentPath + 
                    " is not a container.");
        }        
    }

    public synchronized void moveElementUp(String sheetName, String path) {
        stamp();
        moveElement(sheetName, path, true);
    }


    public synchronized void moveElementDown(String sheetName, String path) {
        stamp();
        moveElement(sheetName, path, false);
    }


    private SingleSheet getSingleSheet(String name) {
        Sheet s = getSheet(name);
        if (!(s instanceof SingleSheet)) {
            throw new IllegalArgumentException(name + " is not a SingleSheet.");
        }
        return (SingleSheet)s;
    }
    
    
    public synchronized void associate(
            String sheetName, 
            String[] contexts) {
        stamp();
        Sheet sheet = manager.getSheet(sheetName);
        manager.associate(sheet, Arrays.asList(contexts));
    }
    
    
    public synchronized String[] getSheets() {
        stamp();
        Set<String> names = getSheetNames();
        return names.toArray(new String[names.size()]);
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
        stamp();
        Sheet sheet = getSheet(sheetName);
        Object v = PathValidator.validate(sheet, path);
        if (v == null) {
            return null;
        }
        if (KeyTypes.isSimple(v.getClass())) {
            return v.toString();
        }
        return Offline.getType(v).getName();
    }
    
    
    public synchronized void associate(
            String sheetName, 
            String surt) {
        stamp();
        Sheet sheet = manager.getSheet(sheetName);
        manager.associate(sheet, Collections.singleton(surt));
    }


    public synchronized void disassociate(
            String sheetName, 
            String surt) {
        stamp();
        Sheet sheet = manager.getSheet(sheetName);
        manager.disassociate(sheet, Collections.singleton(surt));
    }



    public synchronized String resolveAllAsString(
            String sheetName) {
        stamp();
        Sheet ss = getSheet(sheetName);
        StringWriter sw = new StringWriter();
        
        FilePathListConsumer c = new FilePathListConsumer(sw);
        PathLister.resolveAll(ss, c, false);
        return sw.toString();
    }

    
    public synchronized String getAllAsString(
            String sheetName) {
        stamp();
        SingleSheet ss = getSingleSheet(sheetName);
        StringWriter sw = new StringWriter();
        FilePathListConsumer c = new FilePathListConsumer(sw);
        PathLister.getAll(ss, c, true);
        return sw.toString();
    }


    public synchronized void checkout(String sheetName) {
        stamp();
        Sheet sheet = manager.checkout(sheetName);
        checkedOut.put(sheetName, sheet);
    }

    
    public synchronized void commit(String sheetName) {
        stamp();
        Sheet sheet = checkedOut.remove(sheetName);
        manager.commit(sheet);
    }
    
    
    public synchronized void cancel(String sheetName) {
        stamp();
        checkedOut.remove(sheetName);
    }

    
    public synchronized String[] getCheckedOutSheets() {
        stamp();
        return checkedOut.keySet().toArray(new String[0]);
    }


    public synchronized String[] getProblemSingleSheetNames() {
        stamp();
        Set<String> problems = this.problems.keySet();
        return problems.toArray(new String[problems.size()]);
    }


    public synchronized CompositeData[] getSingleSheetProblems(String sheet) {
        stamp();
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


    public synchronized CompositeData[] findConfig(String uri) {
        stamp();
        Sheet sheet = manager.findConfig(uri);
        JMXPathListConsumer c = new JMXPathListConsumer();
        PathLister.resolveAll(sheet, c, false);
        return c.getData();        
    }
    
    
    public synchronized String[] findConfigNames(String uri) {
        stamp();
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
        stamp();
        Sheet sheet = getSheet(sheetName);
        if (sheet == null) {
            throw new IllegalArgumentException("No such sheet: " + sheetName);
        }
        
        return sheet instanceof SingleSheet;
    }
    
    
    public synchronized String[] getBundledSheets(String bundleName) {
        stamp();
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
        stamp();
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
    
    
    public synchronized boolean isOnline() {
        stamp();
        return manager.isOnline();
    }

    
    public synchronized String[] listContexts(String sheetName, int start) {
        stamp();
        Collection<String> c = manager.listContexts(sheetName, start, 100);
        return c.toArray(new String[c.size()]);
    }


    public synchronized void offlineCleanup() {
        if (reapTask != null) {
            reapTask.cancel();
        }
        manager.offlineCleanup();
    }

    
    public synchronized void remove(String sheetName, String path) {
        stamp();
        SingleSheet sheet = getSingleSheet(sheetName);
        PathChanger.remove(sheet, path);
    }

    
    public synchronized String getFilePath(String settingPath) {
        Sheet global = manager.getGlobalSheet();
        Object o = PathValidator.validate(global, settingPath);
        if (o == null) {
            return null;
        }
        Path path = (Path)o;
        return path.toFile().getAbsolutePath();
    }
    
    

    private class ReapTask extends TimerTask {
        
        private MBeanServer server;
        
        public ReapTask(MBeanServer server) {
            this.server = server;
        }
        
        public void run() {
            long now = System.currentTimeMillis();
            synchronized (JMXSheetManagerImpl.this) {
                if (!checkedOut.isEmpty()) {
                    return;
                }
                if (now  - lastUse > TIMEOUT) {
                    try {
                        offlineCleanup();
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, e.getMessage(), e);
                    }
                    try {
                        server.unregisterMBean(oname);
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, e.getMessage(), e);
                    }
                }
            }
        }
    }

}