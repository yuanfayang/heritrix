/* PersistProcessor.java
 * 
 * Created on Feb 17, 2005
 *
 * Copyright (C) 2007 Internet Archive.
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
 */
package org.archive.crawler.processor.recrawl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map.Entry;

import org.apache.commons.codec.binary.Base64;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.framework.Processor;
import org.archive.util.IoUtils;
import org.archive.util.SURT;
import org.archive.util.bdbje.EnhancedEnvironment;
import org.archive.util.iterator.LineReadingIterator;

import st.ata.util.AList;

import com.sleepycat.bind.serial.SerialBinding;
import com.sleepycat.bind.serial.StoredClassCatalog;
import com.sleepycat.bind.tuple.StringBinding;
import com.sleepycat.collections.StoredIterator;
import com.sleepycat.collections.StoredSortedMap;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.EnvironmentConfig;



/**
 * Superclass for Processors which utilize BDB-JE for URI state
 * (including most notably history) persistence.
 * 
 * @author gojomo
 */
public abstract class PersistProcessor extends Processor {
    /** name of history Database */
    public static final String URI_HISTORY_DBNAME = "uri_history";
    
    /**
     * @return DatabaseConfig for history Database
     */
    protected static DatabaseConfig historyDatabaseConfig() {
        DatabaseConfig dbConfig = new DatabaseConfig();
        dbConfig.setTransactional(false);
        dbConfig.setAllowCreate(true);
        dbConfig.setDeferredWrite(true);
        return dbConfig;
    }

    /**
     * Usual constructor
     * 
     * @param name
     * @param string
     */
    public PersistProcessor(String name, String string) {
        super(name,string);
    }

    /**
     * Return a preferred String key for persisting the given CrawlURI's
     * AList state. 
     * 
     * @param curi CrawlURI
     * @return String key
     */
    public String persistKeyFor(CrawlURI curi) {
        // use a case-sensitive SURT for uniqueness and sorting benefits
        return SURT.fromURI(curi.getUURI().toString(),true);
    }

    /**
     * Whether the current CrawlURI's state should be persisted (to log or
     * direct to database)
     * 
     * @param curi CrawlURI
     * @return true if state should be stored; false to skip persistence
     */
    protected boolean shouldStore(CrawlURI curi) {
        // TODO: don't store some codes, such as 304 unchanged?
        return curi.isSuccess();
    }

    /**
     * Whether the current CrawlURI's state should be loaded
     * 
     * @param curi CrawlURI
     * @return true if state should be loaded; false to skip loading
     */
    protected boolean shouldLoad(CrawlURI curi) {
        // TODO: don't load some (prereqs?)
        return true;
    }

    /**
     * Utility main for importing a log into a BDB-JE environment or moving a
     * database between environments. 
     * 
     * @param args command-line arguments
     * @throws DatabaseException
     * @throws IOException
     */
    public static void main(String[] args) throws DatabaseException, IOException {
        if(args.length!=3) {
            System.out.println("Requires 2 arguments: ");
            System.out.println("    source target");
            System.out.println(
                "...where source is either a txtser log file or BDB env dir");
            System.out.println(
                "and target is a BDB env dir. ");
            return;
        }
        File source = new File(args[0]);
        File env = new File(args[1]);
        if(!env.exists()) {
            env.mkdirs();
        }
        
        // setup target environment
        EnhancedEnvironment targetEnv = setupEnvironment(env);
        StoredClassCatalog classCatalog = targetEnv.getClassCatalog();
        Database historyDB = targetEnv.openDatabase(
                null,URI_HISTORY_DBNAME,historyDatabaseConfig());
        StoredSortedMap historyMap = new StoredSortedMap(historyDB,
                new StringBinding(), new SerialBinding(classCatalog,
                        AList.class), true);
        
        int count = 0;
        
        if(source.isFile()) {
            // scan log, writing to database
            BufferedReader br = new BufferedReader(new FileReader(source));
            Iterator iter = new LineReadingIterator(br);
            while(iter.hasNext()) {
                String line = (String) iter.next(); 
                String[] splits = line.split(" ");
                historyMap.put(
                        splits[0], 
                        IoUtils.deserializeFromByteArray(
                                Base64.decodeBase64(splits[1].getBytes("UTF8"))));
                count++;
            }
            br.close();
        } else {
            // open the source env history DB, copying entries to target env
            EnhancedEnvironment sourceEnv = setupEnvironment(source);
            StoredClassCatalog sourceClassCatalog = targetEnv.getClassCatalog();
            Database sourceHistoryDB = targetEnv.openDatabase(
                    null,URI_HISTORY_DBNAME,historyDatabaseConfig());
            StoredSortedMap sourceHistoryMap = new StoredSortedMap(historyDB,
                    new StringBinding(), new SerialBinding(sourceClassCatalog,
                            AList.class), true);
            Iterator iter = sourceHistoryMap.entrySet().iterator();
            while(iter.hasNext()) {
                Entry item = (Entry) iter.next(); 
                historyMap.put(item.getKey(), item.getValue());
                count++;
            }
            StoredIterator.close(iter);
            sourceHistoryDB.close();
            sourceEnv.close();
        }
        
        // cleanup
        historyDB.sync();
        historyDB.close();
        targetEnv.close();
        System.out.println(count+" records imported from "+source+" to BDB env "+env);
    }

    private static EnhancedEnvironment setupEnvironment(File env) throws DatabaseException {
        EnvironmentConfig envConfig = new EnvironmentConfig();
        envConfig.setAllowCreate(true);
        return new EnhancedEnvironment(env, envConfig);
    }
}