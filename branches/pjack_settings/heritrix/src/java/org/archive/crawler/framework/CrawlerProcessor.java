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
 * CrawlProcessor.java
 * Created on December 13, 2006
 *
 * $Id$
 */
package org.archive.crawler.framework;

import org.archive.processors.Processor;
import org.archive.processors.ProcessorURI;
import org.archive.processors.fetcher.CrawlHost;
import org.archive.processors.fetcher.CrawlServer;
import org.archive.processors.fetcher.ServerCache;
import org.archive.processors.fetcher.ServerCacheUtil;

public abstract class CrawlerProcessor extends Processor {

    
    final protected CrawlController controller;
    
    public CrawlerProcessor(CrawlController controller) {
        this.controller = controller;
    }
    
    
    protected CrawlServer getServerFor(ProcessorURI curi) {
        ServerCache cache = controller.getServerCache();
        return ServerCacheUtil.getServerFor(cache, curi.getUURI());
    }
    
    
    protected CrawlHost getHostFor(ProcessorURI curi) {
        ServerCache cache = controller.getServerCache();
        return ServerCacheUtil.getHostFor(cache, curi.getUURI());
    }

    
}
