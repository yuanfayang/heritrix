/* PreloadedUriPrecedencePolicy
*
* $Id: CostAssignmentPolicy.java 4981 2007-03-12 07:06:01Z paul_jack $
*
* Created on Nov 27, 2007
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
package org.archive.crawler.frontier.precedence;

import java.util.Map;

import org.archive.crawler.datamodel.CrawlURI;
import static org.archive.crawler.datamodel.CoreAttributeConstants.*;
import org.archive.crawler.recrawl.PersistProcessor;
import org.archive.settings.file.BdbModule;
import org.archive.state.Expert;
import org.archive.state.Immutable;
import org.archive.state.Initializable;
import org.archive.state.Key;
import org.archive.state.KeyManager;
import org.archive.state.StateProvider;

import com.sleepycat.bind.serial.SerialBinding;
import com.sleepycat.bind.serial.StoredClassCatalog;
import com.sleepycat.bind.tuple.StringBinding;
import com.sleepycat.collections.StoredSortedMap;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseException;

/**
 * UriPrecedencePolicy which assigns URIs a precedence from a value that 
 * was preloaded for them into the uri-history database. 
 */
public class PreloadedUriPrecedencePolicy extends BaseUriPrecedencePolicy 
implements Initializable {
    private static final long serialVersionUID = -1474685153995064123L;
    
    /** Backup URI precedence assignment policy to use. */
    @Expert
    final public static Key<UriPrecedencePolicy> DEFAULT_URI_PRECEDENCE_POLICY = 
        Key.make(UriPrecedencePolicy.class, BaseUriPrecedencePolicy.class);

    // TODO: refactor to better share code with PersistOnlineProcessor
    @Immutable
    final public static Key<BdbModule> BDB = Key.makeAuto(BdbModule.class);

    protected BdbModule bdb;
    protected StoredSortedMap store;
    protected Database historyDb;
    
    public void initialTasks(StateProvider provider) {

        this.bdb = provider.get(this, BDB);
        String dbName = PersistProcessor.URI_HISTORY_DBNAME;
        StoredSortedMap historyMap;
        try {
            StoredClassCatalog classCatalog = bdb.getClassCatalog();
            BdbModule.BdbConfig dbConfig = PersistProcessor.historyDatabaseConfig();

            historyDb = bdb.openDatabase(dbName, dbConfig, true);
            historyMap = new StoredSortedMap(historyDb,
                    new StringBinding(), new SerialBinding(classCatalog,
                            Map.class), true);
        } catch (DatabaseException e) {
            throw new RuntimeException(e);
        }
        store =  historyMap;
    }
    
    
    /* (non-Javadoc)
     * @see org.archive.crawler.frontier.precedence.BaseUriPrecedencePolicy#uriScheduled(org.archive.crawler.datamodel.CrawlURI)
     */
    @Override
    public void uriScheduled(CrawlURI curi) {
        int precedence = calculatePrecedence(curi);
        if(precedence==0) {
            // fall back to configured default policy
            curi.get(this,DEFAULT_URI_PRECEDENCE_POLICY).uriScheduled(curi);
            return;
        }
        curi.setPrecedence(precedence);
        
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.frontier.precedence.BaseUriPrecedencePolicy#calculatePrecedence(org.archive.crawler.datamodel.CrawlURI)
     */
    @Override
    protected int calculatePrecedence(CrawlURI curi) {
        mergePrior(curi);
        Integer preloadPrecedence = (Integer) curi.getData().get(A_PRECALC_PRECEDENCE);
        if(preloadPrecedence==null) {
            return 0;
        }
        return super.calculatePrecedence(curi) + preloadPrecedence;
    }
    
    /**
     * Merge any data from the Map stored in the URI-history store into the 
     * current instance. 
     * 
     * TODO: ensure compatibility with use of PersistLoadProcessor; suppress
     * double-loading
     * @param curi CrawlURI to receive prior state data
     */
    @SuppressWarnings("unchecked")
    protected void mergePrior(CrawlURI curi) {
        Map<String, Map> prior = null;
        String key = PersistProcessor.persistKeyFor(curi);
        prior = (Map<String,Map>) store.get(key);
        if(prior!=null) {
            // merge in keys
            curi.getData().putAll(prior); 
        }
    }

    // good to keep at end of source: must run after all per-Key
    // initialization values are set.
    static {
        KeyManager.addKeys(PreloadedUriPrecedencePolicy.class);
    }
}
