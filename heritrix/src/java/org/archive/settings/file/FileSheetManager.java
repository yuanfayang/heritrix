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
 * FileSheetManager.java
 * Created on October 24, 2006
 *
 * $Header$
 */
package org.archive.settings.file;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.archive.settings.Sheet;
import org.archive.settings.SheetBundle;
import org.archive.settings.SheetManager;
import org.archive.settings.SingleSheet;
import org.archive.settings.path.PathChanger;
import org.archive.settings.path.PathLister;
import org.archive.util.CachedBdbMap;
import org.archive.util.IoUtils;

import com.sleepycat.je.DatabaseException;



/**
 * Simple sheet manager that stores settings in a directory hierarchy.
 * 
 * <p>A "main" directory is specified in the constructor.  Root objects,
 * sheets and associations are all stored under that main directory.
 * 
 * <p>Root objects are listed in a file named <code>roots.txt</code> in the
 * main directory itself.  The roots.txt file contains name/value pairs; the
 * named specifies the name of the root object, and the value is the 
 * fully-qualified class name for the root object.  Root objects must define
 * a public no-argument constructor; this is used to construct the roots 
 * during initialization.
 * 
 * <p>Sheets are stored in a subdirectory named <code>sheets</code> of the main
 * directory.  Each sheet is stored in its own file.  For SingleSheets, the
 * filename is the name of the sheet plus a <code>.single</code> extension.
 * Similarly, SheetBundles are stored in a file with a <code>.bundle</code>
 * extension.  
 * 
 * <p>A <code>.single</code> file is a list of key/value pairs, where the key
 * is a path and the value is the stringified value for that path.  See the
 * {@link org.archive.settings.path} package for more information.
 * 
 * <p>A <code>.bundle</code> file is simply a list of sheet names contained
 * in the bundle.
 * 
 * <p>And finally, associations are stored in a BDB database stored in the
 * <code>assoc</code> subdirectory of the main directory.
 * 
 * <p>A sample directory hierarchy used by this class:
 * 
 * <pre>
 * main/roots.txt    
 * main/sheets/X.single
 * main/sheets/Y.bundle
 * main/assoc
 * </pre>
 * 
 * <p>A sample <code>roots.txt</code> file:
 * 
 * <pre>
 * html=org.archive.crawler.extractor.ExtractorHTML
 * js=org.archive.crawler.extractor.ExtractorJS
 * css=org.archive.crawler.extractor.ExtractorCSS
 * </pre>
 * 
 * <p>A sample <code>.single</code> file:
 * 
 * <pre>
 * html.ENABLED=true
 * html.DECIDE_RULES.RULES._impl=java.util.ArrayList
 * html.DECIDE_RULES.RULES.0._impl=org.archive.crawler.deciderules.AcceptDecideRule
 * </pre>
 * 
 * <p>A sample <code>.bundle</code> file:
 * 
 * <pre>
 * X
 * Y
 * Z
 * </pre>
 * 
 * @author pjack
 */
public class FileSheetManager extends SheetManager {


    /** The name of the subdirectory that stores sheet files. */
    final private static String SHEETS_DIR_NAME = "sheets";


    /** The name of the subdirectory that stores the associations database. */
    final private static String ASSOC_DIR_NAME = "assoc";
    
    
    /** The extension for files containing SingleSheet information. */
    final private static String SINGLE_EXT = ".single";
    
    
    /** The extension for files containing SingleBundle information. */
    final private static String BUNDLE_EXT = ".bundle";
    
    
    /** Logger. */
    final private static Logger LOGGER
     = Logger.getLogger(FileSheetManager.class.getName());

    
    /** The main directory. */
    final private File mainDir;
    
    
    /** The sheets subdirectory. */
    final private File sheetsDir;

    
    /** Sheets that are currently in memory. */
    final private Map<String,Sheet> sheets;
    

    /** The root objects. */
    private Object root;


    /** The database of associations.  Maps string context to sheet name. */
    final private CachedBdbMap<String,String> associations;

    
    /** The default sheet. */
    private SingleSheet defaultSheet;
    
    /**
     * Constructor.
     * 
     * @param main        the main directory
     * @param maxSheets   the maximum number of sheets to keep in memory
     */
    public FileSheetManager(File main, int maxSheets) throws IOException {
        validateDir(main);
        this.mainDir = main;
        this.sheetsDir = new File(main, SHEETS_DIR_NAME);
        validateDir(sheetsDir);
        sheets = new HashMap<String,Sheet>();
        File assocDir = new File(mainDir, ASSOC_DIR_NAME);
        validateDir(assocDir);
        try {
            this.associations = new CachedBdbMap<String,String>(assocDir, 
                    "associations", String.class, String.class);
        } catch (DatabaseException e) {
            throw new IllegalStateException(e);
        }
        reload();
    }


    /**
     * Validates that the given file exists, is a directory and is both
     * readable and writeable.  Raises IllegalArgument otherwise.
     * 
     * @param f  the file to test
     */
    private void validateDir(File f) {
        if (!f.exists()) {
            throw new IllegalArgumentException(f.getAbsolutePath() 
                    + " does not exist.");
        }
        if (!f.isDirectory()) {
            throw new IllegalArgumentException(f.getAbsolutePath() 
                    + " is not a directory.");
        }
        if (!f.canRead()) {
            throw new IllegalArgumentException(f.getAbsolutePath() 
                    + " is unreadable.");
        }
        if (!f.canWrite()) {
            throw new IllegalArgumentException(f.getAbsolutePath() 
                    + " is unwriteable.");
        }
    }

    
    public void reload() {
            sheets.clear();
            defaultSheet = loadSingleSheet("default");
            // FIXME: Load associations...
    }

    
    public void save() {
        for (Sheet s: sheets.values()) {
            saveSheet(s);
        }
    }


    @Override
    public SingleSheet addSingleSheet(String name) {
        SingleSheet r = createSingleSheet(name);
        addSheet(r);
        return r;
    }
    
    
    @Override
    public SheetBundle addSheetBundle(String name, Collection<Sheet> sheets) {
        SheetBundle r = createSheetBundle(name, sheets);
        addSheet(r);
        return r;
    }
    

    /**
     * Adds a new sheet.  The given sheet is saved to disk and placed in the
     * in-memory cache.
     * 
     * @param sheet  the sheet to add
     * @throws IllegalArgumentException  if any existing sheet has the same
     *   name as the given sheet
     */
    private void addSheet(Sheet sheet) {
        String name = sheet.getName();
        String ext = this.getSheetExtension(name);
        if (ext != null) {
            throw new IllegalArgumentException("Sheet already exists: " + name);
        }
        if (sheet instanceof SheetBundle) {
            saveSheetBundle((SheetBundle)sheet);
        } else {
            saveSingleSheet((SingleSheet)sheet);
        }
        this.sheets.put(name, sheet);
    }


    @Override
    public void associate(Sheet sheet, Iterable<String> strings) {
        for (String s: strings) {
            associations.put(s, sheet.getName());
        }
    }


    @Override
    public void disassociate(Sheet sheet, Iterable<String> strings) {
        for (String s: strings) {
            String n = associations.get(s);
            if (n.equals(sheet.getName())) {
                associations.remove(s);
            }
        }
    }


    @Override
    public Sheet getAssociation(String context) {
        String sheetName = associations.get(context);
        if (sheetName == null) {
            return null;
        }
        return getSheet(sheetName);
    }


    @Override
    public SingleSheet getDefault() {
        return (SingleSheet)getSheet("default");
    }


    @Override
    public Object getRoot() {
        return root;
    }


    @Override
    public void setRoot(Object root) {
        this.root = root;
    }
    
    
    @Override
    public Sheet getSheet(String sheetName) throws IllegalArgumentException {
        if (sheetName.equals("default")) {
            return defaultSheet;
        }
        Sheet r = sheets.get(sheetName);
        if (r == null) {
            r = loadSheet(sheetName);
            if (r == null) {
                throw new IllegalArgumentException("No such sheet: " 
                        + sheetName);
            }
            sheets.put(sheetName, r);
        }
        return r;
    }


    @Override
    public Set<String> getSheetNames() {
        String[] files = sheetsDir.list();
        HashSet<String> r = new HashSet<String>();
        for (String s: files) {
            String name = removeSuffix(s, SINGLE_EXT);
            if (name == null) {
                name = removeSuffix(s, BUNDLE_EXT);
            }
            if (name != null) {
                r.add(name);
            }
        }
        return r;
    }

    
    private String removeSuffix(String s, String suffix) {
        if (s.endsWith(suffix)) {
            return s.substring(0, s.length() - suffix.length());
        }
        return null;
    }
    

    @Override
    public void removeSheet(String sheetName) throws IllegalArgumentException {
        sheets.remove(sheetName);
        String ext = getSheetExtension(sheetName);
        if (ext == null) {
            throw new IllegalArgumentException(sheetName + " does not exist.");
        }
        if (!(new File(sheetName + ext).delete())) {
            throw new IllegalStateException();
        }
    }

    @Override
    public void renameSheet(String oldName, String newName) {
        String prior = getSheetExtension(newName);
        if (prior != null) {
            throw new IllegalArgumentException(newName + " already exists.");
        }
        
        String ext = getSheetExtension(oldName);
        if (ext == null) {
            throw new IllegalArgumentException("No such sheet: " + oldName);
        }
        
        File orig = new File(sheetsDir, oldName + ext);
        File renamed = new File(sheetsDir, newName + ext);
        if (!orig.renameTo(renamed)) {
            throw new IllegalStateException("Rename from " + oldName + " to " 
                    + newName + " failed.");
        }
        
        Sheet s = sheets.remove(oldName);
        if (s != null) {
            sheets.put(newName, s);
        }
    }


    /**
     * Loads the sheet with the given name from disk.  This method determines
     * whether the sheet with the given name is a single sheet or bundle, 
     * and loads the appropriate file.
     * 
     * Returns null if no such sheet exists.
     * 
     * @param name   the name of the sheet 
     * @return   the sheet with that name, or null if no such sheet exists
     */
    private Sheet loadSheet(String name) {
        if (new File(sheetsDir, name + SINGLE_EXT).exists()) {
            return loadSingleSheet(name);
        }
        if (new File(sheetsDir, name + BUNDLE_EXT).exists()) {
            return loadSheetBundle(name);
        }
        return null;
    }

    
    /**
     * Loads the single sheet with the given name.  If the sheet cannot be
     * loaded for any reason, the reason is logged and this method returns
     * null.
     * 
     * @param name   the name of the single sheet to load
     * @return   the loaded single sheet, or null 
     */
    private SingleSheet loadSingleSheet(String name) {
        File f = new File(sheetsDir, name + SINGLE_EXT); 
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(f));
            SheetFileReader sfr = new SheetFileReader(br);
            SingleSheet r = createSingleSheet(name);
            new PathChanger().change(r, sfr);
            sheets.put(name, r);
            return r;
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Could not load sheet.", e);
            return null;
        } finally {
            IoUtils.close(br);
        }
    }

    
    /**
     * Loads the sheet bundle with the given name.  If the sheet cannot
     * be loaded for any reaso, the reason is logged and this method returns
     * null.
     * 
     * @param name   the name of the sheet bundle to load
     * @return   the loaded sheet bundle, or null
     */
    private SheetBundle loadSheetBundle(String name) {
        File f = new File(sheetsDir, name + BUNDLE_EXT);
        Collection<Sheet> c = new LinkedList<Sheet>();
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(f));
            for (String s = br.readLine(); s != null; s = br.readLine()) {
                if (!s.startsWith("#")) {
                    c.add(getSheet(s));
                }
            }
            SheetBundle r = createSheetBundle(name, c);
            sheets.put(name, r);
            return r;
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Could not load sheet.", e);
            return null;
        } finally {
            IoUtils.close(br);
        }
    }


    /**
     * Saves the given sheet to disk.
     * 
     * @param s   the sheet to save
     */
    private void saveSheet(Sheet s) {
        if (s instanceof SingleSheet) {
            saveSingleSheet((SingleSheet)s);
        } else {
            saveSheetBundle((SheetBundle)s);
        }
    }


    /**
     * Saves a single sheet to disk.  This method first saves the sheet to a
     * temporary file, then renames the temporary file to the 
     * <code>.single</code> file for the sheet.  If a problem occurs, the 
     * problem is logged but no exception is raised.
     * 
     * @param ss   the single sheet to save
     */
    private void saveSingleSheet(SingleSheet ss) {
        File temp = new File(sheetsDir, ss.getName() + SINGLE_EXT + ".temp");
        FileWriter fw = null;
        try {
            fw = new FileWriter(temp);
            FilePathListConsumer c = new FilePathListConsumer(fw);
            PathLister.getAll(ss, c);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Can't save " + ss.getName(), e);
            return;
        } finally {
            IoUtils.close(fw);
        }
        
        File saved = new File(sheetsDir, ss.getName() + SINGLE_EXT);
        if (!temp.renameTo(saved)) {
            LOGGER.severe("Could not rename temp file for " + ss.getName());
        }
    }


    /**
     * Saves a sheet bundle to disk.  First saves the sheet to a temporary
     * file, then renames that file to the <code>.bundle</code> for that 
     * sheet.  If a problem occurs, the problem is logged but no exception
     * is raised.
     * 
     * @param sb   the sheet bundle to save
     */
    private void saveSheetBundle(SheetBundle sb) {
        File temp = new File(sheetsDir, sb.getName() + BUNDLE_EXT + ".temp");
        FileWriter fw = null;
        try {
            fw = new FileWriter(temp);
            for (Sheet s: sb.getSheets()) {
                fw.write(s.getName() + '\n');                
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Can't save " + sb.getName(), e);
            return;
        } finally {
            IoUtils.close(fw);
        }
        
        File saved = new File(sheetsDir, sb.getName() + BUNDLE_EXT);
        if (!temp.renameTo(saved)) {
            LOGGER.severe("Could not rename temp file for " + sb.getName());
        }
    }


    /**
     * Returns the extension for the given sheet.
     * 
     * @param sheetName  the name of the sheet whose extension to return
     * @return   the extension for that sheet, or null if the sheet does not
     *    exist in the filesystem
     */
    private String getSheetExtension(String sheetName) {
        if (new File(sheetsDir, sheetName + SINGLE_EXT).exists()) {
            return SINGLE_EXT;
        }
        if (new File(sheetsDir, sheetName + BUNDLE_EXT).exists()) {
            return BUNDLE_EXT;
        }
        return null;
    }
}
