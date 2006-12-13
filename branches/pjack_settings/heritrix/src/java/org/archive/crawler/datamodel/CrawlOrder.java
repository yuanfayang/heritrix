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

import java.io.File;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
//import java.util.logging.Logger;

import javax.management.AttributeNotFoundException;

import org.archive.crawler.framework.CrawlController;
import org.archive.crawler.framework.CrawlScope;
import org.archive.crawler.framework.Frontier;
import org.archive.crawler.framework.StatisticsTracking;
import org.archive.crawler.framework.exceptions.FatalConfigurationException;
import org.archive.crawler.url.CanonicalizationRule;
import org.archive.processors.Processor;
import org.archive.processors.credential.CredentialStore;
import org.archive.processors.util.RobotsHonoringPolicy;
import org.archive.settings.Sheet;
import org.archive.state.Key;
import org.archive.state.KeyMaker;
import org.archive.state.StateProvider;


/**
 * Represents the 'root' of the settings hierarchy. Contains those settings that
 * do not belong to any specific module, but rather relate to the crawl as a
 * whole (much of this is used by the CrawlController directly or indirectly).
 *
 * @see org.archive.crawler.settings.ModuleType
 */
public class CrawlOrder implements Serializable {

    
    final public static String ROOT_NAME = "order";
    
    
    private static final long serialVersionUID = -6715840285961511669L;

//    private static Logger logger =
//        Logger.getLogger("org.archive.crawler.datamodel.CrawlOrder");

    /**
     * Directory where override settings are kept. The settings for many modules can 
     * be overridden based on the domain or subdomain of the URI being processed. 
     * This setting specifies a file level directory to store those settings. The path
     * is relative to {@link #DISK_PATH} unless an absolute path is provided.
     */
    final public static Key<String> SETTINGS_DIRECTORY = Key.makeExpertFinal("settings");


    /**
     * Directory where logs, arcs and other run time files will
     * be kept. If this path is a relative path, it will be
     * relative to the crawl order.
     */
    final public static Key<String> DISK_PATH = Key.makeExpertFinal("");


    /**
     * Directory where crawler log files will be kept. If this path is a 
     * relative path, it will be relative to the {@link #DISK_PATH}.
     */
    final public static Key<String> LOGS_PATH = Key.makeExpertFinal("logs"); 


    /**
     * Directory where crawler checkpoint files will be kept. If this 
     * path is a relative path, it will be relative to the {@link #DISK_PATH}.
     */
    final public static Key<String> CHECKPOINTS_PATH = Key.makeExpertFinal("checkpoints");


    /**
     * Directory where crawler-state files will be kept. If this path 
     * is a relative path, it will be relative to the {@link #DISK_PATH}.
     */
    final public static Key<String> STATE_PATH = Key.makeExpertFinal("state");


    /**
     * Directory where discardable temporary files will be kept. If 
     * this path is a relative path, it will be relative to the {@link #DISK_PATH}.
     */
    final public static Key<String> SCRATCH_PATH = Key.makeExpertFinal("scratch");


    /**
     * Maximum number of bytes to download. Once this number is exceeded 
     * the crawler will stop. A value of zero means no upper limit.
     */
    final public static Key<Long> MAX_BYTES_DOWNLOAD = Key.makeFinal(0L);


    /**
     * Maximum number of documents to download. Once this number is exceeded the 
     * crawler will stop. A value of zero means no upper limit.
     */
    final public static Key<Long> MAX_DOCUMENT_DOWNLOAD = Key.makeFinal(0L);


    /**
     * Maximum amount of time to crawl (in seconds). Once this much time has 
     * elapsed the crawler will stop. A value of zero means no upper limit.
     */
    final public static Key<Long> MAX_TIME_SEC = Key.makeFinal(0L);


    /**
     * Maximum number of threads processing URIs at the same time.
     */
    final public static Key<Integer> MAX_TOE_THREADS = Key.makeFinal(0);


    /**
     * Size in bytes of in-memory buffer to record outbound traffic. One such 
     * buffer is reserved for every ToeThread. 
     */
    final public static Key<Integer> RECORDER_OUT_BUFFER = Key.makeExpertFinal(4096);


    /**
     * Size in bytes of in-memory buffer to record inbound traffic. One such 
     * buffer is reserved for every ToeThread.
     */
    final public static Key<Integer> RECORDER_IN_BUFFER = 
        Key.makeExpertFinal(65536);

            
    /**
     * Percentage of heap to allocate to BerkeleyDB JE cache. Default of zero 
     * means no preference (accept BDB's default, usually 60%, or the 
     * je.maxMemoryPercent property value).
     */
    final public static Key<Integer> BDB_CACHE_PERCENT = Key.makeExpertFinal(0);


    /**
     * HTTP headers. Information that will be used when constructing the HTTP 
     * headers of the crawler's HTTP requests.
     */
    final public static Key<Map<String,String>> HTTP_HEADERS
     = makeHttpHeaders();


    /**
     * The frontier to use for the crawl.
     */
    final public static Key<Frontier> FRONTIER = Key.makeFinal((Frontier)null);
//     FIXME

    
    final public static Key<RobotsHonoringPolicy> ROBOTS_HONORING_POLICY =
        Key.make((RobotsHonoringPolicy)null);

    /**
     * Ordered list of url canonicalization rules.  Rules are applied in the 
     * order listed from top to bottom.
     */
    final public static Key<List<CanonicalizationRule>> RULES = 
        finalList(CanonicalizationRule.class);


    /**
     * Statistics tracking modules.  Any number of specialized statistics 
     * trackers that monitor a crawl and write logs, reports and/or provide 
     * information to the user interface.
     */
    final public static Key<List<StatisticsTracking>> LOGGERS = 
        finalList(StatisticsTracking.class);


    /**
     * Optional. Points at recover log (or recover.gz log) OR the checkpoint 
     * directory to use recovering a crawl.
     */
    final public static Key<String> RECOVER_PATH = Key.makeExpertFinal("");


    /**
     * When true, on a checkpoint, we copy off the bdbje log files to the
     * checkpoint directory. To recover a checkpoint, just set the recover-path
     * to point at the checkpoint directory to recover. This is default setting.
     * But if crawl is large, copying bdbje log files can take tens of minutes
     * and even upwards of an hour (Copying bdbje log files will consume bulk of
     * time checkpointing). If this setting is false, we do NOT copy bdbje logs
     * on checkpoint AND we set bdbje to NEVER delete log files (instead we have
     * it rename files-to-delete with a '.del' extension). Assumption is that
     * when this setting is false, an external process is managing the removal
     * of bdbje log files and that come time to recover from a checkpoint, the
     * files that comprise a checkpoint are manually assembled. This is an
     * expert setting.
     */
    final public static Key<Boolean> CHECKPOINT_COPY_BDBJE_LOGS = 
        Key.makeExpertFinal(true);


    /**
     * When recovering via the recover.log, should failures in the log be
     * retained in the recovered crawl, preventing the corresponding URIs from
     * being retried. Default is false, meaning failures are forgotten, and the
     * corresponding URIs will be retried in the recovered crawl.
     */
    final public static Key<Boolean> RECOVER_RETAIN_FAILURES = 
        Key.makeExpertFinal(false);


    /**
    FIXME
     */
    final public static Key<CredentialStore> CREDENTIAL_STORE = 
        Key.makeExpertFinal((CredentialStore)null);

    
    final public static Key<CrawlScope> SCOPE = 
        Key.makeExpertFinal((CrawlScope)null); 
    // FIXME

    
    final public static Key<Map<String,Processor>> PROCESSORS =
        Key.makeMap(Processor.class);


    private transient CrawlController controller;

    /**
     * Regex for acceptable user-agent format.
     */
    private static String ACCEPTABLE_USER_AGENT =
        "\\S+.*\\(.*\\+http(s)?://\\S+\\.\\S+.*\\).*";

    /**
     * Regex for acceptable from address.
     */
    private static String ACCEPTABLE_FROM = "\\S+@\\S+\\.\\S+";
    

    /** Construct a CrawlOrder.
     */
    public CrawlOrder() {
    }

    /**
     * @param curi
     * @return user-agent header value to use
     */
    public String getUserAgent(StateProvider curi) {
        if (curi == null) {
            Sheet def = controller.getSheetManager().getDefault();
            return def.get(this, HTTP_HEADERS).get("user-agent");
        }
        return curi.get(this, HTTP_HEADERS).get("user-agent");
    }

    /**
     * @param curi
     * @return from header value to use
     */
    public String getFrom(CrawlURI curi) {
        if (curi == null) {
            Sheet def = controller.getSheetManager().getDefault();
            return def.get(this, HTTP_HEADERS).get("from");
        }
        return curi.get(this, HTTP_HEADERS).get("from");
    }

    /**
     * Returns the set number of maximum toe threads.
     * @return Number of maximum toe threads
     */
    public int getMaxToes() {
        Sheet def = controller.getSheetManager().getDefault();
        return def.get(this, MAX_TOE_THREADS);
    }

    /**
     * This method gets the RobotsHonoringPolicy object from the orders file.
     *
     * @return the new RobotsHonoringPolicy
     */
    public RobotsHonoringPolicy getRobotsHonoringPolicy() {
        Sheet def = controller.getSheetManager().getDefault();
        return def.get(this, ROBOTS_HONORING_POLICY);
    }

    /** Get the name of the order file.
     *
     * @return the name of the order file.
     */
    public String getCrawlOrderName() {
        return controller.getSheetManager().getCrawlName();
    }

    /**
     * @return The crawl controller.
     */
    public CrawlController getController() {
        return controller;
    }

    /**
     * @param controller
     */
    public void setController(CrawlController controller) {
        this.controller = controller;
    }

    /**
     * Returns the Map of the StatisticsTracking modules that are included in the
     * configuration that the current instance of this class is representing.
     * @return Map of the StatisticsTracking modules
     */
    public List<StatisticsTracking> getLoggers() {
        Sheet def = controller.getSheetManager().getDefault();
        return def.get(this, LOGGERS);        
    }

    /**
     * Checks if the User Agent and From field are set 'correctly' in
     * the specified Crawl Order.
     *
     * @throws FatalConfigurationException
     */
    public void checkUserAgentAndFrom() throws FatalConfigurationException {
        // don't start the crawl if they're using the default user-agent
        String userAgent = this.getUserAgent(null);
        String from = this.getFrom(null);
        if (!(userAgent.matches(ACCEPTABLE_USER_AGENT)
            && from.matches(ACCEPTABLE_FROM))) {
            throw new FatalConfigurationException("unacceptable user-agent " +
                    " or from (Reedit your order file).");
        }
    }

    /**
     * @return Checkpoint directory.
     */
    public File getCheckpointsDirectory() {
        Sheet def = controller.getSheetManager().getDefault();
        String p = def.get(this, CHECKPOINTS_PATH);
        return getDirectoryRelativeToDiskPath(p);
    }

    private File getDirectoryRelativeToDiskPath(String subpath) {
        Sheet def = controller.getSheetManager().getDefault();
        String p = def.get(this, DISK_PATH);
        File disk = controller.getRelative(p);
        return new File(disk, subpath);
    }
    
    /**
     * Return fullpath to the directory named by <code>key</code>
     * in settings.
     * If directory does not exist, it and all intermediary dirs
     * will be created.
     * @param key Key to use going to settings.
     * @return Full path to directory named by <code>key</code>.
     * @throws AttributeNotFoundException
     */
    public File getSettingsDir(Key<String> key) {
        Sheet def = controller.getSheetManager().getDefault();
        String path = def.get(this, key);
        File f = new File(path);
        if (!f.isAbsolute()) {
            f = getDirectoryRelativeToDiskPath(path);
        }
        if (!f.exists()) {
            f.mkdirs();
        }
        return f;
    }
    
    



    private static Key<Map<String,String>> makeHttpHeaders() {
        Map<String,String> hh = new HashMap<String,String>();
        hh.put("user-agent", 
         "Mozilla/5.0 (compatible; heritrix/@VERSION@ +PROJECT_URL_HERE)");
        hh.put("from", "CONTACT_EMAIL_ADDRESS_HERE");
        hh = Collections.unmodifiableMap(hh);
        
        KeyMaker<Map<String,String>> km = KeyMaker.makeMap(String.class);
        km.overrideable = false;
        km.def = hh;
        
        // FIXME: Add header constraints to enforce valid email etc
        
        return new Key<Map<String,String>>(km);
    }


    private static <T> Key<List<T>> finalList(Class<T> element) {
        KeyMaker<List<T>> km = KeyMaker.makeList(element);
        km.expert = true;
        km.overrideable = false;
        return new Key<List<T>>(km);
    }

}
