package org.archive.modules.fetchcache;

import java.util.logging.Logger;

/**
 * This class provides methods to access fetch cache.
 * @author Ping Wang
 *
 */
public class FetchCacheUtil {
    private static Logger logger =
        Logger.getLogger(FetchCacheUtil.class.getName());
    
    private FetchCacheUtil() {
    }
    
    /**
     * Retrieve content or content location of a uri.
     * @param fetchCache
     * @param uriStr
     * @return
     */
    public static Object getContentLocation(FetchCache fetchCache, 
            String uriStr) {
        return fetchCache.getContentLocation(uriStr);
    }
}
