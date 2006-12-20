package org.archive.crawler.framework;

import org.archive.processors.fetcher.DefaultServerCache;
import org.archive.processors.util.CrawlHost;
import org.archive.processors.util.CrawlServer;

public class CrawlerServerCache extends DefaultServerCache {
   
    
    public CrawlerServerCache(CrawlController c) throws Exception {
        super(c.getBigMap("servers", String.class, CrawlServer.class),
                c.getBigMap("hosts", String.class, CrawlHost.class));
    }
    
    
    
    
    
}
