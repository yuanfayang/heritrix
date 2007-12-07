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
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.archive.settings.Association;
import org.archive.settings.CheckpointRecovery;
import org.archive.settings.ModuleListener;
import org.archive.settings.Stub;
import org.archive.settings.RecoverAction;
import org.archive.settings.Sheet;
import org.archive.settings.SheetBundle;
import org.archive.settings.SheetManager;
import org.archive.settings.SingleSheet;
import org.archive.settings.path.ConstraintChecker;
import org.archive.settings.path.PathChangeException;
import org.archive.settings.path.PathChanger;
import org.archive.settings.path.PathListConsumer;
import org.archive.settings.path.PathLister;
import org.archive.state.DefaultPathContext;
import org.archive.state.ExampleStateProvider;
import org.archive.state.Immutable;
import org.archive.state.Key;
import org.archive.state.KeyManager;
import org.archive.state.Path;
import org.archive.state.PathContext;
import org.archive.util.FileUtils;
import org.archive.util.IoUtils;

import static org.archive.settings.file.BdbModule.BDB_CACHE_PERCENT;
import static org.archive.settings.file.BdbModule.CHECKPOINT_COPY_BDBJE_LOGS;

import com.sleepycat.bind.EntryBinding;
import com.sleepycat.bind.tuple.TupleBinding;
import com.sleepycat.collections.StoredSortedMap;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseException;


/**
 * Simple sheet manager that stores settings in a directory hierarchy.
 * 
 * <p>
 * A configuration file is specified at construction time. Although this
 * configuration file may be empty, its existence is still important, as
 * FileSheetManager will use that file's directory as its main working
 * directory.
 * 
 * <p>
 * By default, sheets are stored in a subdirectory named <code>sheets</code>
 * of the main directory. Each sheet is stored in its own file. For
 * SingleSheets, the filename is the name of the sheet plus a
 * <code>.sheet</code> extension. Similarly, SheetBundles are stored in a
 * file with a <code>.bundle</code> extension.
 * 
 * <p>
 * A <code>.sheet</code> file is a list of key/value pairs, where the key is a
 * path and the value is the stringified value for that path. See the
 * {@link org.archive.settings.path} package for more information.
 * 
 * <pre>
 * root:controller:
 * </pre>
 * 
 * <p>
 * A <code>.bundle</code> file is simply a list of sheet names contained in
 * the bundle.
 * 
 * <p>
 * And finally, associations are stored in a BDB database stored in the
 * <code>state</code> subdirectory of the main directory.
 * 
 * <p>
 * A sample directory hierarchy used by this class:
 * 
 * <pre>
 *  main/config.txt    
 *  main/sheets/X.sheet
 *  main/sheets/Y.bundle
 *  main/state
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

    /** The directory this sheet manager uses. */
    @Immutable
    final public static Key<Path> DIR =
        Key.make(new Path("."));

    static {
        KeyManager.addKeys(FileSheetManager.class);
    }

    final private static String ATTR_SHEETS = "sheets-dir";
    
    final private static String BDB_PREFIX = "bdb-";
    
    /** The default name of the subdirectory that stores sheet files. */
    final private static String DEFAULT_SHEETS = "sheets";

    /** The extension for files containing SingleSheet information. */
    final private static String SINGLE_EXT = ".sheet";

    /** The extension for files containing SingleBundle information. */
    final private static String BUNDLE_EXT = ".bundle";

    /** Logger. */
    final private static Logger LOGGER = Logger
            .getLogger(FileSheetManager.class.getName());

    /** The sheets subdirectory. */
    private File sheetsDir;

    /** The main configuration file. */
    private transient File mainConfig;
    
    /** Sheets that are currently in memory. */
    private Map<String, Sheet> sheets;

    /** The database of associations. Maps string context to sheet names. */
    private transient Database surtToSheetsDB;
    
    /** The database of associations. Maps string context to sheet names. */
    private transient StoredSortedMap surtToSheets;

    /** PathContext for Path settings. */
    private PathContext pathContext;
    
    /** The default sheet. */
    private SingleSheet defaultSheet;

    /** 
     * Maps a SingeSheet's name to the list of problems encountered when that
     * sheet was loaded.
     */
    final private Map<String,List<PathChangeException>> problems = 
        new HashMap<String,List<PathChangeException>>();

    final private BdbModule bdb;
    
    final private Path dir;
    
    private String bdbDir;
    
    final private int bdbCachePercent;

    private boolean copyCheckpoint;
    

    public FileSheetManager(File main, String name, boolean live) 
    throws IOException, DatabaseException {
        this(main, name, live, emptyList());
    }


    /**
     * Constructor.
     * 
     * @param main
     *            the main directory
     * @param maxSheets
     *            the maximum number of sheets to keep in memory
     */
    public FileSheetManager(File main, String name, boolean live, 
            Collection<ModuleListener> listeners) 
    throws IOException, DatabaseException {
        super(name, listeners, live);
        this.mainConfig = main;        
        this.pathContext = new FSMPathContext(mainConfig.getParentFile(), name);

        Properties p = load(main);
        
        String path = getBdbProperty(p, BdbModule.DIR);
        File f = new File(path);
        if (!f.isAbsolute()) {
            f = new File(mainConfig.getParent(), path);
        }

        
        this.bdbDir = f.getAbsolutePath();
        if (live) {
            this.bdbCachePercent = Integer.parseInt(
                    getBdbProperty(p, BDB_CACHE_PERCENT));
        } else {
            // Don't use as much memory for profiles & ready jobs.
            this.bdbCachePercent = Integer.parseInt(
                    p.getProperty(BDB_PREFIX + "stub-cache-percent", "5"));
        }
        String truthiness = getBdbProperty(p, CHECKPOINT_COPY_BDBJE_LOGS);
        if (truthiness.equals("true")) {
            this.copyCheckpoint = true;
        } else if (truthiness.equals("false")) {
            this.copyCheckpoint = false;
        } else {
            throw new IllegalStateException("Illegal boolean: " + truthiness);
        }
        
        ExampleStateProvider dsp = new ExampleStateProvider();

        this.dir = new Path(main.getParentFile().getAbsolutePath());
        
        this.bdb = new BdbModule();
        dsp.set(bdb, BdbModule.DIR, this.bdbDir);
        dsp.set(bdb, BDB_CACHE_PERCENT, bdbCachePercent);
        dsp.set(bdb, CHECKPOINT_COPY_BDBJE_LOGS, copyCheckpoint);
        bdb.initialTasks(dsp);
        
        
        String prop = p.getProperty(ATTR_SHEETS);
        this.sheetsDir = getRelative(main, prop, DEFAULT_SHEETS);
        validateDir(sheetsDir);
        sheets = new HashMap<String, Sheet>();

        initBdb();
        
        reload();
    }

    
    private void initBdb() throws DatabaseException {
        BdbModule.BdbConfig config = new BdbModule.BdbConfig();
        config.setAllowCreate(true);
        config.setSortedDuplicates(true);
        surtToSheetsDB = bdb.openDatabase("surtToSheets", config, true);
        EntryBinding stringBinding 
            = TupleBinding.getPrimitiveBinding(String.class);
        
        this.surtToSheets = new StoredSortedMap(
                surtToSheetsDB, stringBinding, stringBinding, true); 
        
    }
    
    
    private String getBdbProperty(Properties p, Key<?> key) {
        String propName = BDB_PREFIX + key.getFieldName();
        String defValue = key.getDefaultValue().toString();
        return p.getProperty(propName, defValue);
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

    private void reload() {
        sheets.clear();
        
        // Load default sheet first, since every other sheet relies on it.
        this.defaultSheet = loadSingleSheet(GLOBAL_SHEET_NAME);
        if (defaultSheet == null) {
            throw new IllegalStateException("Could not load default sheet.");
        }
        sheets.put(GLOBAL_SHEET_NAME, defaultSheet);
        
        // Load single sheets next, since bundles rely on them
        for (File f: sheetsDir.listFiles()) {
            String name = f.getName();
            if (!name.equals(GLOBAL_SHEET_NAME + SINGLE_EXT) 
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
    public void associate(Sheet sheet, Collection<String> strings) {
        for (String s : strings) {
            surtToSheets.put(s, sheet.getName());
        }
        try {
            surtToSheetsDB.sync();
        } catch (DatabaseException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override @SuppressWarnings("unchecked")
    public void disassociate(Sheet sheet, Collection<String> strings) {
        for (String s: strings) {
            surtToSheets.duplicates(s).remove(sheet.getName());
        }
        try {
            surtToSheetsDB.sync();
        } catch (DatabaseException e) {
            throw new IllegalStateException(e);                
        }
    }

    @Override @SuppressWarnings("unchecked")
    public Collection<String> getAssociations(String context) {
        return surtToSheets.duplicates(context);
    }

    
    @Override @SuppressWarnings("unchecked")
    public List<Association> findConfigNames(String uri) {
        final List<Association> result = new ArrayList<Association>();
        List<String> prefixes = new ArrayList<String>();
        PrefixFinder.find((SortedSet)surtToSheets.keySet(), uri, prefixes);
        for (String prefix: prefixes) {
            Collection<String> all = surtToSheets.duplicates(prefix);
            for (String surt: all) {
                result.add(new Association(prefix, surt));
            }
        }
        return result;
    }
    
    
    @Override @SuppressWarnings("unchecked")
    public Sheet findConfig(String context) {
        final List<Sheet> sheets = new ArrayList<Sheet>();
        List<String> prefixes = new ArrayList<String>();
        PrefixFinder.find((SortedSet)surtToSheets.keySet(), context, prefixes);
        for (String prefix: prefixes) {
            Collection<String> all = surtToSheets.duplicates(prefix);
            for (String sheetName: all) {
                try {
                    Sheet sheet = getSheet(sheetName);
                    sheets.add(sheet);
                } catch (IllegalArgumentException e) {
                    LOGGER.warning("Association had unknown sheet: " 
                            + sheetName);
                }
            }
        }
        
        if (sheets.isEmpty()) {
            return getGlobalSheet();
        }
        
        if (sheets.size() == 1) {
            return sheets.get(0);
        }

        return this.createSheetBundle("anonymous", sheets);
    }
    
    
    @Override
    public SingleSheet getGlobalSheet() {
        return (SingleSheet)getSheet(GLOBAL_SHEET_NAME);
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
        if (!(new File(sheetsDir, sheetName + ext).delete())) {
            throw new IllegalStateException("Could not delete sheet.");
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
        try {
            SingleSheet r = createSingleSheet(name);
            if (name.equals(GLOBAL_SHEET_NAME)) {
                setManagerDefaults(r);
            }
            sheets.put(name, r);
            SheetFileReader sfr = new SheetFileReader(new FileInputStream(f));
            PathChanger pc = new PathChanger();
            pc.change(r, sfr);
            
            List<PathChangeException> problems = pc.getProblems();
            
            PathListConsumer plc = new ConstraintChecker(problems, r);
            PathLister.resolveAll(r, plc, true);

            if (!problems.isEmpty()) {
                this.problems.put(name, problems);
            }

            
            return r;
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Could not load sheet.", e);
            return null;
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
//    private void saveSheet(Sheet s) {
//        if (s instanceof SingleSheet) {
//            saveSingleSheet((SingleSheet) s);
//        } else {
//            saveSheetBundle((SheetBundle) s);
//        }
//    }

    /**
     * Saves a single sheet to disk. This method first saves the sheet to a
     * temporary file, then renames the temporary file to the
     * <code>.sheet</code> file for the sheet. If a problem occurs, the
     * problem is logged but no exception is raised.
     * 
     * @param ss
     *            the single sheet to save
     */
    private void saveSingleSheet(SingleSheet ss) {
        File temp = new File(sheetsDir, ss.getName() + SINGLE_EXT + ".temp");
        FileOutputStream fout = null;
        try {
            fout = new FileOutputStream(temp);
            FilePathListConsumer c = new FilePathListConsumer(fout);
            PathLister.getAll(ss, c, true);
            fout.flush();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Can't save " + ss.getName(), e);
            return;
        } finally {
            IoUtils.close(fout);
        }

        File saved = new File(sheetsDir, ss.getName() + SINGLE_EXT);
        try {
            // Copy temp -> Saved
            org.apache.commons.io.FileUtils.copyFile(temp, saved, true);
            // Remove temp
            org.apache.commons.io.FileUtils.forceDelete(temp);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, 
                    "Could not rename temp file for " + ss.getName(), e);
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
        try {
            // Copy temp -> Saved
            org.apache.commons.io.FileUtils.copyFile(temp, saved, true);
            // Remove temp
            org.apache.commons.io.FileUtils.forceDelete(temp);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, 
                    "Could not rename temp file for " + sb.getName(), e);
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


    private void setManagerDefaults(SingleSheet ss) {
        if (isLive()) {
            ss.set(this, MANAGER, this);
            ss.set(this, BDB, bdb);
            ss.set(this, DIR, dir);
            ss.set(bdb, BdbModule.DIR, bdbDir);
            ss.set(bdb, BDB_CACHE_PERCENT, bdbCachePercent);
            ss.set(bdb, CHECKPOINT_COPY_BDBJE_LOGS, copyCheckpoint);
            ss.addPrimary(getManagerModule());
            ss.addPrimary(bdb);
        } else {
            @SuppressWarnings("unchecked")
            Stub<SheetManager> manager = (Stub)getManagerModule();
            Stub<BdbModule> bdb = Stub.make(BdbModule.class);
            ss.setStub(manager, MANAGER, manager);
            ss.setStub(manager, BDB, bdb);
            ss.setStub(manager, DIR, dir);
            ss.set(bdb, BdbModule.DIR, bdbDir);
            ss.set(bdb, BDB_CACHE_PERCENT, bdbCachePercent);
            ss.set(bdb, CHECKPOINT_COPY_BDBJE_LOGS, copyCheckpoint);
            ss.addPrimary(getManagerModule());
            ss.addPrimary(bdb);
        }
    }
    
    
    public File getDirectory() {
        return mainConfig.getParentFile();
    }

    
    public void checkpoint(File dir, List<RecoverAction> actions) 
    throws IOException {
        File config = new File(dir, "config.txt");
        FileUtils.copyFile(mainConfig, config);
        
        File sheets = new File(dir, "sheets");
        FileUtils.copyFiles(sheetsDir, sheets);
        
        actions.add(new FSMRecover(mainConfig.getAbsolutePath(), 
                sheetsDir.getAbsolutePath()));
        
    }
    


    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeObject(mainConfig);
        out.defaultWriteObject();
    }
    
    
    private void readObject(ObjectInputStream inp) 
    throws IOException, ClassNotFoundException {
        mainConfig = (File)inp.readObject();
        if (inp instanceof CheckpointRecovery) {
            CheckpointRecovery cr = (CheckpointRecovery)inp;
            mainConfig = 
                new File(cr.translatePath(mainConfig.getAbsolutePath()));
        }
        inp.defaultReadObject();
        if (inp instanceof CheckpointRecovery) {
            CheckpointRecovery cr = (CheckpointRecovery)inp;
            sheetsDir =
                new File(cr.translatePath(sheetsDir.getAbsolutePath()));
            bdbDir = 
                new File(cr.translatePath(bdbDir)).getAbsolutePath();
        }
        surtToSheetsDB = bdb.getDatabase("surtToSheets");
        EntryBinding stringBinding 
            = TupleBinding.getPrimitiveBinding(String.class);            
        this.surtToSheets = new StoredSortedMap(
                surtToSheetsDB, stringBinding, stringBinding, true); 
    }


    static class FSMRecover implements RecoverAction {

        private static final long serialVersionUID = 1L;

        private String mainConfig;
        private String sheets;
        
        
        public FSMRecover(String mainConfig, String sheets) {
            this.mainConfig = mainConfig;
            this.sheets = sheets;
        }
        
        public void recoverFrom(File checkpointDir, CheckpointRecovery recovery) 
        throws Exception {
            mainConfig = recovery.translatePath(mainConfig);
            sheets = recovery.translatePath(sheets);
            File srcCfg = new File(checkpointDir, "config.txt");
            new File(mainConfig).getParentFile().mkdirs();
            FileUtils.copyFile(srcCfg, new File(mainConfig));
            
            File srcSheets = new File(checkpointDir, "sheets");
            FileUtils.copyFiles(srcSheets, new File(sheets));
        }

    }

    
    public void commit(Sheet sheet) {
        if (sheet.getSheetManager() != this) {
            throw new IllegalArgumentException();
        }
        if (sheet.isClone() == false) {
            throw new IllegalArgumentException();
        }
        clearCloneFlag(sheet);
        this.sheets.put(sheet.getName(), sheet);
        if (sheet instanceof SingleSheet) {
            saveSingleSheet((SingleSheet)sheet);
        }
        announceChanges(sheet);
        // FIXME: Save sheet bundles too.
    }

    
    public Set<String> getProblemSingleSheetNames() {
        return new HashSet<String>(problems.keySet());
    }

    
    public List<PathChangeException> getSingleSheetProblems(String sheet) {
        List<PathChangeException> r = problems.get(sheet);
        if (r == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(r);
    }

    
    public Collection<String> listContexts(String sheetName, int ofs, int len) {
        int count = 0;
        @SuppressWarnings("unchecked")
        Set<Map.Entry<String,String>> entries = surtToSheets.entrySet();
        List<String> result = new ArrayList<String>();
        for (Map.Entry<String,String> me: entries) {
            String surt = me.getKey();
            String sheet = me.getValue();
            if (sheet.equals(sheetName)) {
                if (count >= ofs) {
                    result.add(surt);
                    if (result.size() > len) {
                        return result;
                    }
                    count++;                    
                }
            }
        }
        
        return result;
    }

    
    public void stubCleanup() {
        bdb.close();
    }

    
    public PathContext getPathContext() {
        return pathContext;
    }
}
