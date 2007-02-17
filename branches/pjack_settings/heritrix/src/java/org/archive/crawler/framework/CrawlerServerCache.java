package org.archive.crawler.framework;

import org.archive.processors.fetcher.DefaultServerCache;
import org.archive.processors.util.CrawlHost;
import org.archive.processors.util.CrawlServer;
import org.archive.state.Dependency;
import org.archive.state.Key;

public class CrawlerServerCache extends DefaultServerCache {
   
    
    @Dependency
    final public static Key<CrawlController> CONTROLLER =
        Key.make(CrawlController.class, null);
    
    
    public CrawlerServerCache(CrawlController c) throws Exception {
        super(c.getBigMap("servers", String.class, CrawlServer.class),
                c.getBigMap("hosts", String.class, CrawlHost.class));
    }
    
    
    
    
    
}
