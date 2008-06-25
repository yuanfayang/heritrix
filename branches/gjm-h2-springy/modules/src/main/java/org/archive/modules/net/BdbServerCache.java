package org.archive.modules.net;

import org.archive.modules.fetcher.DefaultServerCache;
import org.archive.settings.file.BdbModule;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

import com.sleepycat.je.DatabaseException;


public class BdbServerCache extends DefaultServerCache 
implements InitializingBean {

    private static final long serialVersionUID = 1L;

    protected BdbModule bdb;
    @Autowired
    public void setBdbModule(BdbModule bdb) {
        this.bdb = bdb;
    }
    
    public BdbServerCache() {
    }
    

    public void afterPropertiesSet() {
        try {
            this.servers = bdb.getBigMap("servers", false, String.class, CrawlServer.class);
            this.hosts = bdb.getBigMap("hosts", false, String.class, CrawlHost.class);
        } catch (DatabaseException e) {
            throw new IllegalStateException(e);
        }
    }
    
    
    
}
