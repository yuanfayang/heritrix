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
 * $Header: /cvsroot/archive-crawler/ArchiveOpenCrawler/src/java/org/archive/settings/file/Attic/FileSheetManager.java,v 1.1.2.2 2006/12/06 23:33:16 paul_jack Exp $
 */
package org.archive.settings.file;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.archive.settings.ModuleListener;
import org.archive.settings.Sheet;
import org.archive.settings.SheetBundle;
import org.archive.settings.SheetManager;
import org.archive.settings.SingleSheet;
import org.archive.settings.path.PathLister;
import org.archive.state.ExampleStateProvider;
import org.archive.state.Immutable;
import org.archive.state.Key;
import org.archive.state.KeyManager;
import org.archive.util.FileUtils;
import org.archive.util.IoUtils;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.sleepycat.je.DatabaseException;

/**
 * Simple sheet manager that stores settings in a directory hierarchy.
 * 
 * <p>
 * A "main" directory is specified in the constructor. Root objects, sheets and
 * associations are all stored under that main directory.
 * 
 * <p>
 * Root objects are listed in a file named <code>roots.txt</code> in the main
 * directory itself. The roots.txt file contains name/value pairs; the named
 * specifies the name of the root object, and the value is the fully-qualified
 * class name for the root object. Root objects must define a public no-argument
 * constructor; this is used to construct the roots during initialization.
 * 
 * <p>
 * Sheets are stored in a subdirectory named <code>sheets</code> of the main
 * directory. Each sheet is stored in its own file. For SingleSheets, the
 * filename is the name of the sheet plus a <code>.single</code> extension.
 * Similarly, SheetBundles are stored in a file with a <code>.bundle</code>
 * extension.
 * 
 * <p>
 * A <code>.single</code> file is a list of key/value pairs, where the key is
 * a path and the value is the stringified value for that path. See the
 * {@link org.archive.settings.path} package for more information.
 * 
 * <p>
 * A <code>.bundle</code> file is simply a list of sheet names contained in
 * the bundle.
 * 
 * <p>
 * And finally, associations are stored in a BDB database stored in the
 * <code>assoc</code> subdirectory of the main directory.
 * 
 * <p>
 * A sample directory hierarchy used by this class:
 * 
 * <pre>
 *  main/roots.txt    
 *  main/sheets/X.single
 *  main/sheets/Y.bundle
 *  main/assoc
 * </pre>
 * 
 * <p>
 * A sample <code>roots.txt</code> file:
 * 
 * <pre>
 *  html=org.archive.crawler.extractor.ExtractorHTML
 *  js=org.archive.crawler.extractor.ExtractorJS
 *  css=org.archive.crawler.extractor.ExtractorCSS
 * </pre>
 * 
 * <p>
 * A sample <code>.single</code> file:
 * 
 * <pre>
 *  html.ENABLED=true
 *  html.DECIDE_RULES.RULES._impl=java.util.ArrayList
 *  html.DECIDE_RULES.RULES.0._impl=org.archive.crawler.deciderules.AcceptDecideRule
 * </pre>
 * 
 * <p>
 * A sample <code>.bundle</code> file:
 * 
 * <pre>
 *  X
 *  Y
 *  Z
 * </pre>
 * 
 * @author pjack
 */
public class FileSheetManager extends SheetManager implements Checkpointable {

    /**
     * First version.
     */
    private static final long serialVersionUID = 1L;

    
    
    
    
    /** The BDB environment. */
    @Immutable
    final public static Key<BdbModule> BDB = 
        Key.make(BdbModule.class, null);


    static {
        KeyManager.addKeys(FileSheetManager.class);
    }

    final private static String ATTR_SHEETS = "sheets-dir";
    
    final private static String ATTR_BDB = "bdb-dir";
    
    final private static String ATTR_BDB_PERCENT = "bdb-cache-percent";
    
    /** The default name of the subdirectory that stores sheet files. */
    final private static String DEFAULT_SHEETS = "sheets";
    
    final private static String DEFAULT_BDB_PERCENT = 
        String.valueOf(BdbConfig.BDB_CACHE_PERCENT.getDefaultValue());
    
    /** The default name of the subdirectory that stores the bdb database. */
    final private static String DEFAULT_BDB = 
        BdbConfig.DIR.getDefaultValue();

    /** The extension for files containing SingleSheet information. */
    final private static String SINGLE_EXT = ".single";

    /** The extension for files containing SingleBundle information. */
    final private static String BUNDLE_EXT = ".bundle";

    /** Logger. */
    final private static Logger LOGGER = Logger
            .getLogger(FileSheetManager.class.getName());

    /** The sheets subdirectory. */
    final private File sheetsDir;

    /** The main configuration file. */
    final private File mainConfig;
    
    /** Sheets that are currently in memory. */
    final private Map<String, Sheet> sheets;

    /** The database of associations. Maps string context to sheet name. */
    final private Map<String, String> associations;

    /** The default sheet. */
    private SingleSheet defaultSheet;

    private boolean online;

    final private SAXParserFactory saxParserFactory;
    

    final private BdbModule bdb;
    
    final private String bdbDir;
    
    final private int bdbCachePercent;

    
    public FileSheetManager() 
    throws IOException, DatabaseException {
        this(new File("config.txt"), true);
    }

    
    public FileSheetManager(File main, boolean online) 
    throws IOException, DatabaseException {
        this(main, online, emptyList());
    }


    /**
     * Constructor.
     * 
     * @param main
     *            the main directory
     * @param maxSheets
     *            the maximum number of sheets to keep in memory
     */
    public FileSheetManager(File main, boolean online, 
            Collection<ModuleListener> listeners) 
    throws IOException, DatabaseException {
        super(listeners);
        this.mainConfig = main;
        this.saxParserFactory = SAXParserFactory.newInstance();
        
        this.online = online;
        Properties p = load(main);
        
        String path = p.getProperty(ATTR_BDB, DEFAULT_BDB);
        File f = new File(path);
        if (!f.isAbsolute()) {
            f = new File(mainConfig.getParent(), path);
        }
        
        this.bdbDir = f.getAbsolutePath();
        this.bdbCachePercent = Integer.parseInt(p.getProperty(ATTR_BDB_PERCENT, 
                DEFAULT_BDB_PERCENT));
        
        
        ExampleStateProvider dsp = new ExampleStateProvider();
        BdbConfig config = new BdbConfig();
        dsp.set(config, BdbConfig.DIR, this.bdbDir);
        dsp.set(config, BdbConfig.BDB_CACHE_PERCENT, bdbCachePercent);
        
        this.bdb = new BdbModule(dsp, config);

        String prop = p.getProperty(ATTR_SHEETS);
        this.sheetsDir = getRelative(main, prop, DEFAULT_SHEETS);
        validateDir(sheetsDir);
        sheets = new HashMap<String, Sheet>();
        this.associations = 
            bdb.getBigMap("associations", String.class, String.class);
        reload();
    }

    
    private static List<ModuleListener> emptyList() {
        return Collections.emptyList();
    }

    
    private static Properties load(File file) throws IOException {
        FileInputStream finp = new FileInputStream(file);
        try {
            Properties p = new Properties();
            p.load(finp);
            return p;
        } finally {
            IoUtils.close(finp);
        }
    }
    
    
    private static File getRelative(File main, String prop, String def) {
        if (prop == null) {
            return new File(main.getParentFile(), def);
        }
        
        File r = new File(prop);
        if (r.isAbsolute()) {
            return r;
        }
        r = new File(main.getParentFile(), prop);
        return r;
    }
    

    /**
     * Validates that the given file exists, is a directory and is both readable
     * and writeable. Raises IllegalArgument otherwise.
     * 
     * @param f
     *            the file to test
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
        
        // Load default sheet first, since every other sheet relies on it.
        this.defaultSheet = loadSingleSheet(DEFAULT_SHEET_NAME);
        if (defaultSheet == null) {
            throw new IllegalStateException("Could not load default sheet.");
        }
        sheets.put("default", defaultSheet);
        
        // Load single sheets next, since bundles rely on them
        for (File f: sheetsDir.listFiles()) {
            String name = f.getName();
            if (!name.equals(DEFAULT_SHEET_NAME + SINGLE_EXT) 
                    && name.endsWith(SINGLE_EXT)) {
                int p = name.lastIndexOf('.');
                String sname = name.substring(0, p);
                loadSingleSheet(sname);
            }
        }
        
        // Load sheet bundles last.
        for (File f: sheetsDir.listFiles()) {
            String name = f.getName();
            if (name.endsWith(BUNDLE_EXT)) {
                int p = name.lastIndexOf('.');
                String sname = name.substring(0, p);
                loadSheetBundle(sname);
            }
        }
        // FIXME: Load or at least clear associations...
    }

    public void save() {
        for (Sheet s : sheets.values()) {
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
     * Adds a new sheet. The given sheet is saved to disk and placed in the
     * in-memory cache.
     * 
     * @param sheet
     *            the sheet to add
     * @throws IllegalArgumentException
     *             if any existing sheet has the same name as the given sheet
     */
    private void addSheet(Sheet sheet) {
        String name = sheet.getName();
        String ext = this.getSheetExtension(name);
        if (ext != null) {
            throw new IllegalArgumentException("Sheet already exists: " + name);
        }
        if (sheet instanceof SheetBundle) {
            saveSheetBundle((SheetBundle) sheet);
        } else {
            saveSingleSheet((SingleSheet) sheet);
        }
        this.sheets.put(name, sheet);
    }

    @Override
    public void associate(Sheet sheet, Iterable<String> strings) {
        for (String s : strings) {
            associations.put(s, sheet.getName());
        }
    }

    @Override
    public void disassociate(Sheet sheet, Iterable<String> strings) {
        for (String s : strings) {
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
    public Sheet getSheet(String sheetName) throws IllegalArgumentException {
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
        for (String s : files) {
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
     * Loads the sheet with the given name from disk. This method determines
     * whether the sheet with the given name is a single sheet or bundle, and
     * loads the appropriate file.
     * 
     * Returns null if no such sheet exists.
     * 
     * @param name
     *            the name of the sheet
     * @return the sheet with that name, or null if no such sheet exists
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
     * Loads the single sheet with the given name. If the sheet cannot be loaded
     * for any reason, the reason is logged and this method returns null.
     * 
     * @param name
     *            the name of the single sheet to load
     * @return the loaded single sheet, or null
     */
    private SingleSheet loadSingleSheet(String name) {
        File f = new File(sheetsDir, name + SINGLE_EXT);
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(f));
            SAXParser parser = saxParserFactory.newSAXParser();
            InputSource source = new InputSource(br);
            SingleSheet r = createSingleSheet(name);
            if (name.equals(DEFAULT_SHEET_NAME)) {
                setManagerDefaults(r);
            }
            sheets.put(name, r);
            DefaultPathChangeListener dpcl = new DefaultPathChangeListener(r);
            SettingsSAX sax = new SettingsSAX(dpcl);
            parser.parse(source, sax);
            return r;
        } catch (SAXException e) {
            LOGGER.log(Level.SEVERE, "Could not load sheet.", e);
            return null;
        } catch (ParserConfigurationException e) {
            LOGGER.log(Level.SEVERE, "Could not load sheet.", e);
            return null;
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Could not load sheet.", e);
            return null;
        } finally {
            IoUtils.close(br);
        }
    }

    /**
     * Loads the sheet bundle with the given name. If the sheet cannot be loaded
     * for any reaso, the reason is logged and this method returns null.
     * 
     * @param name
     *            the name of the sheet bundle to load
     * @return the loaded sheet bundle, or null
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
     * @param s
     *            the sheet to save
     */
    private void saveSheet(Sheet s) {
        if (s instanceof SingleSheet) {
            saveSingleSheet((SingleSheet) s);
        } else {
            saveSheetBundle((SheetBundle) s);
        }
    }

    /**
     * Saves a single sheet to disk. This method first saves the sheet to a
     * temporary file, then renames the temporary file to the
     * <code>.single</code> file for the sheet. If a problem occurs, the
     * problem is logged but no exception is raised.
     * 
     * @param ss
     *            the single sheet to save
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
     * Saves a sheet bundle to disk. First saves the sheet to a temporary file,
     * then renames that file to the <code>.bundle</code> for that sheet. If a
     * problem occurs, the problem is logged but no exception is raised.
     * 
     * @param sb
     *            the sheet bundle to save
     */
    private void saveSheetBundle(SheetBundle sb) {
        File temp = new File(sheetsDir, sb.getName() + BUNDLE_EXT + ".temp");
        FileWriter fw = null;
        try {
            fw = new FileWriter(temp);
            for (Sheet s : sb.getSheets()) {
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
     * @param sheetName
     *            the name of the sheet whose extension to return
     * @return the extension for that sheet, or null if the sheet does not exist
     *         in the filesystem
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

    @Override
    public boolean isOnline() {
        return online;
    }



    
    private void setManagerDefaults(SingleSheet ss) {
        ss.set(this, MANAGER, this);
        ss.set(this, BDB, bdb);
        ss.set(bdb, BdbModule.CONFIG, bdb.getBdbConfig());
        ss.set(bdb, BdbModule.PROVIDER, this);
        ss.set(bdb.getBdbConfig(), BdbConfig.DIR, bdbDir);
        ss.set(bdb.getBdbConfig(), BdbConfig.BDB_CACHE_PERCENT, bdbCachePercent);
    }
    
    
    @Override
    public File getWorkingDirectory() {
        return mainConfig.getParentFile();
    }

    
    public void checkpoint(File dir) throws IOException {
        File config = new File(dir, "config.txt");
        FileUtils.copyFile(mainConfig, config);
        
        File sheets = new File(dir, "sheets");
        FileUtils.copyFiles(sheetsDir, sheets);
    }
}
