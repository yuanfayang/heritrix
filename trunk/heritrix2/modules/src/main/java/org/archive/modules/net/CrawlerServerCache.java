package org.archive.modules.net;

import org.archive.modules.fetcher.DefaultServerCache;
import org.archive.settings.file.BdbModule;
import org.archive.state.Immutable;
import org.archive.state.Initializable;
import org.archive.state.Key;
import org.archive.state.KeyManager;
import org.archive.state.StateProvider;

import com.sleepycat.je.DatabaseException;


public class CrawlerServerCache extends DefaultServerCache 
implements Initializable {

    private static final long serialVersionUID = 1L;


    @Immutable
    final public static Key<BdbModule> BDB =
        Key.makeAuto(BdbModule.class);
    
    
    static {
        KeyManager.addKeys(CrawlerServerCache.class);
    }
    
    
    public CrawlerServerCache() {
    }
    

    public void initialTasks(StateProvider provider) {
        BdbModule bdb = provider.get(this, BDB);
        try {
            this.servers = bdb.getBigMap("servers", false, String.class, CrawlServer.class);
            this.hosts = bdb.getBigMap("hosts", false, String.class, CrawlHost.class);
        } catch (DatabaseException e) {
            throw new IllegalStateException(e);
        }
    }
    
    
    
}
