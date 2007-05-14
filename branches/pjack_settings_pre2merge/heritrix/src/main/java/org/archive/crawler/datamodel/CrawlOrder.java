/*
 * CrawlOrder
 *
 * $Header$
 *
 * Created on May 15, 2003
 *
 * Copyright (C) 2003 Internet Archive.
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
 */

package org.archive.crawler.datamodel;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.archive.processors.util.RobotsHonoringPolicy;
import org.archive.state.Expert;
import org.archive.state.Immutable;
import org.archive.state.Key;
import org.archive.state.KeyMaker;
import org.archive.state.KeyManager;
import org.archive.state.Module;


/**
 * Represents the 'root' of the settings hierarchy. Contains those settings that
 * do not belong to any specific module, but rather relate to the crawl as a
 * whole (much of this is used by the CrawlController directly or indirectly).
 *
 * @see org.archive.crawler.settings.ModuleType
 */
public class CrawlOrder implements Module, Serializable {


    private static final long serialVersionUID = 3L;


    /**
     * Directory where logs, arcs and other run time files will
     * be kept. If this path is a relative path, it will be
     * relative to the crawl order.
     */
    @Expert @Immutable
    final public static Key<String> DISK_PATH = Key.make("");


    /**
     * Directory where crawler checkpoint files will be kept. If this 
     * path is a relative path, it will be relative to the {@link #DISK_PATH}.
     */
    @Expert @Immutable
    final public static Key<String> CHECKPOINTS_PATH = Key.make("checkpoints");


    /**
     * Directory where crawler-state files will be kept. If this path 
     * is a relative path, it will be relative to the {@link #DISK_PATH}.
     */
    @Expert @Immutable
    final public static Key<String> STATE_PATH = Key.make("state");


    /**
     * Directory where discardable temporary files will be kept. If 
     * this path is a relative path, it will be relative to the {@link #DISK_PATH}.
     */
    @Expert @Immutable
    final public static Key<String> SCRATCH_PATH = Key.make("scratch");


    /**
     * Maximum number of bytes to download. Once this number is exceeded 
     * the crawler will stop. A value of zero means no upper limit.
     */
    @Immutable
    final public static Key<Long> MAX_BYTES_DOWNLOAD = Key.make(0L);


    /**
     * Maximum number of documents to download. Once this number is exceeded the 
     * crawler will stop. A value of zero means no upper limit.
     */
    @Immutable
    final public static Key<Long> MAX_DOCUMENT_DOWNLOAD = Key.make(0L);


    /**
     * Maximum amount of time to crawl (in seconds). Once this much time has 
     * elapsed the crawler will stop. A value of zero means no upper limit.
     */
    @Immutable
    final public static Key<Long> MAX_TIME_SEC = Key.make(0L);


    /**
     * Maximum number of threads processing URIs at the same time.
     */
    @Immutable
    final public static Key<Integer> MAX_TOE_THREADS = Key.make(0);


    /**
     * Size in bytes of in-memory buffer to record outbound traffic. One such 
     * buffer is reserved for every ToeThread. 
     */
    @Expert @Immutable
    final public static Key<Integer> RECORDER_OUT_BUFFER_BYTES = Key.make(4096);


    /**
     * Size in bytes of in-memory buffer to record inbound traffic. One such 
     * buffer is reserved for every ToeThread.
     */
    @Expert @Immutable
    final public static Key<Integer> RECORDER_IN_BUFFER_BYTES = 
        Key.make(65536);

            
    /**
     * HTTP headers. Information that will be used when constructing the HTTP 
     * headers of the crawler's HTTP requests.
     */
    @Immutable
    final public static Key<Map<String,String>> HTTP_HEADERS
     = makeHttpHeaders();


    final public static Key<RobotsHonoringPolicy> ROBOTS_HONORING_POLICY =
        Key.makeNull(RobotsHonoringPolicy.class);




    /**
     * Optional. Points at recover log (or recover.gz log) OR the checkpoint 
     * directory to use recovering a crawl.
     */
    @Expert @Immutable
    final public static Key<String> RECOVER_PATH = Key.make("");


    /**
     * When recovering via the recover.log, should failures in the log be
     * retained in the recovered crawl, preventing the corresponding URIs from
     * being retried. Default is false, meaning failures are forgotten, and the
     * corresponding URIs will be retried in the recovered crawl.
     */
    @Expert @Immutable
    final public static Key<Boolean> RECOVER_RETAIN_FAILURES = 
        Key.make(false);


    
    static {
        KeyManager.addKeys(CrawlOrder.class);
    }
    

    /** Construct a CrawlOrder.
     */
    public CrawlOrder() {
    }


    private static Key<Map<String,String>> makeHttpHeaders() {
        Map<String,String> hh = new HashMap<String,String>();
        hh.put("user-agent", 
         "Mozilla/5.0 (compatible; heritrix/@VERSION@ +PROJECT_URL_HERE)");
        hh.put("from", "CONTACT_EMAIL_ADDRESS_HERE");
        hh = Collections.unmodifiableMap(hh);
        
        KeyMaker<Map<String,String>> km = KeyMaker.makeMap(String.class);
        km.def = hh;
        
        // FIXME: Add header constraints to enforce valid email etc
        
        return new Key<Map<String,String>>(km);
    }


}
