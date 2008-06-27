/* PersistOnlineProcessor.java
 * 
 * Created on Feb 18, 2005
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
package org.archive.modules.recrawl;

import java.util.Map;

import org.archive.settings.file.BdbModule;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

import com.sleepycat.bind.serial.SerialBinding;
import com.sleepycat.bind.serial.StoredClassCatalog;
import com.sleepycat.bind.tuple.StringBinding;
import com.sleepycat.collections.StoredSortedMap;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseException;

/**
 * Common superclass for persisting Processors which directly store/load
 * to persistence (as opposed to logging for batch load later). 
 * @author gojomo
 */
public abstract class PersistOnlineProcessor extends PersistProcessor 
implements InitializingBean, DisposableBean {
    
    private static final long serialVersionUID = -666479480942267268L;
    
    protected BdbModule bdb;
    @Autowired
    public void setBdbModule(BdbModule bdb) {
        this.bdb = bdb;
    }
    
    String historyDbName = "uri_history";
    public String getHistoryDbName() {
        return this.historyDbName;
    }
    public void setHistoryDbName(String name) {
        this.historyDbName = name; 
    }

    protected StoredSortedMap store;
    protected Database historyDb;

    public PersistOnlineProcessor() {
    }


    public void afterPropertiesSet() {
        // TODO: share single store instance between Load and Store processors
        // (shared context? EnhancedEnvironment?)

        StoredSortedMap historyMap;
        try {
            StoredClassCatalog classCatalog = bdb.getClassCatalog();
            BdbModule.BdbConfig dbConfig = historyDatabaseConfig();

            historyDb = bdb.openDatabase(getHistoryDbName(), dbConfig, true);
            historyMap = new StoredSortedMap(historyDb,
                    new StringBinding(), new SerialBinding(classCatalog,
                            Map.class), true);
        } catch (DatabaseException e) {
        	throw new RuntimeException(e);
        }
        store =  historyMap;
    }


    public void destroy() {
    	// TODO leave this cleanup to BdbModule?
        try {
            historyDb.sync();
            historyDb.close();
        } catch (DatabaseException e) {
            // TODO Auto-generated catch block
            throw new RuntimeException(e);
        }
    }

}