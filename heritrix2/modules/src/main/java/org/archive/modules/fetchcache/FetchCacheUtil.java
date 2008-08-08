package org.archive.modules.fetchcache;

import java.util.logging.Logger;

public class FetchCacheUtil {
    private static Logger logger =
        Logger.getLogger(FetchCacheUtil.class.getName());
    
    private FetchCacheUtil() {
    }
    
    public static Object getContentLocation(FetchCache fetchCache, 
            String uriStr) {
        return fetchCache.getContentLocation(uriStr);
    }

}
