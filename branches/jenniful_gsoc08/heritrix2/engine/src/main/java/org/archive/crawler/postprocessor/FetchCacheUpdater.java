package org.archive.crawler.postprocessor;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.logging.Logger;

import static org.archive.crawler.datamodel.SchedulingConstants.*;
import static org.archive.modules.fetcher.FetchStatusCodes.*;

import org.apache.commons.httpclient.URIException;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.framework.CrawlerLoggerModule;
//import org.archive.crawler.framework.Frontier;
import org.archive.modules.PostProcessor;
import org.archive.modules.Processor;
import org.archive.modules.ProcessorURI;
import org.archive.modules.extractor.LinkContext;
import org.archive.modules.fetchcache.FetchCache;
import org.archive.net.UURI;
import org.archive.net.UURIFactory;
import org.archive.state.Immutable;
import org.archive.state.Initializable;
import org.archive.state.Key;
import org.archive.state.KeyManager;
import org.archive.state.StateProvider;

/**
 * Update fetch cache after a document has been successfully fetched.
 * @author Ping Wang
 *
 */
public class FetchCacheUpdater extends Processor
implements Initializable, PostProcessor{
	
    private static final long serialVersionUID = 1L;

    private static final Logger logger =
        Logger.getLogger(FetchCacheUpdater.class.getName());


    //@Immutable
    //final public static Key<Frontier> FRONTIER = Key.makeAuto(Frontier.class);

    @Immutable
    final public static Key<FetchCache> FETCH_CACHE = 
        Key.makeAuto(FetchCache.class);
    
    @Immutable
    final public static Key<CrawlerLoggerModule> LOGGER_MODULE = 
        Key.makeAuto(CrawlerLoggerModule.class);
    
    //private Frontier frontier;
    private FetchCache fetchCache;
    
    protected CrawlerLoggerModule loggerModule;

    public void initialTasks(StateProvider global) {
        this.fetchCache = global.get(this, FETCH_CACHE);
        //this.frontier = global.get(this, FRONTIER);
        this.loggerModule = global.get(this, LOGGER_MODULE);
    }
    
    protected boolean shouldProcess(ProcessorURI puri) {
        if (! (puri instanceof CrawlURI)) {
            return false;
        }
        
        if (! isProperURI(puri)) {
            return false;
        }
        
        if (puri.forceFetch()) {
            return false;
        }
        
        return true;
    }
    
    protected void innerProcess(ProcessorURI puri) {
        
        CrawlURI curi = (CrawlURI) puri;
        if (puri.getFetchStatus() < 200 || puri.getFetchStatus() >= 400) {
        	if (isScriptDocument(puri)) {
                handleFailedURI(puri);
            }
        } else {
	        updateStatus(curi);
        }
        scheduleAnalysisURI(curi);
    }

    /**
     * If the fetch for a resource failed, then remove its information 
     * in fetch cache.  
     * @param puri
     */
    protected void handleFailedURI(ProcessorURI puri) {
    	int fetchStatus = puri.getFetchStatus();
    	
    	switch (fetchStatus) {
	    	case S_BLOCKED_BY_USER:
	    	case S_OUT_OF_SCOPE:
	    	case S_ROBOTS_PRECLUDED:
	    	case S_ROBOTS_PREREQUISITE_FAILURE:
	    	case S_UNFETCHABLE_URI:
	    	case S_DOMAIN_PREREQUISITE_FAILURE:
	    		updateFailedURIStatus(puri);
	        	String uriStr = puri.getUURI().toString().toLowerCase();
	        	fetchCache.removeResource(uriStr);
	    		logger.warning(puri.getUURI().toString() + "is " + 
	    		        CrawlURI.fetchStatusCodesToString(fetchStatus));
	    		break;
	    	default:
	    		break;
    	}
    }
    
    protected void updateFailedURIStatus(ProcessorURI puri) {
    	String uriStr = puri.getUURI().toString().toLowerCase();
    	fetchCache.removeResource(uriStr);
    }
    
    protected void updateStatus(CrawlURI curi) {
        String uriStr = curi.getUURI().toString().toLowerCase();
        
        if (isHTMLDocument(curi)) {
            updateHTMLStatus(curi);
        } else if (isScriptDocument(curi)) {
            updateResourceStatus(curi);
        }
    }
    
    /**
     * Update fetch cache when an HTML document is available.
     * @param curi
     */
    protected void updateHTMLStatus(CrawlURI curi) {
        
        Collection<String> resURIs = new HashSet<String>();
        for (CrawlURI link : curi.getOutCandidates()) {
            String uri = link.getUURI().toString().toLowerCase();
            if (uri.endsWith(".js")) {
                resURIs.add(uri);
            }
        }
        
        fetchCache.updateDependentStatus(curi, resURIs);
        
        Collection<CrawlURI> outScopeURIs = 
        	(Collection<CrawlURI>) curi.getData().get("out-of-scope-uris");
        
        if (outScopeURIs != null && outScopeURIs.size() > 0) {
        	for (CrawlURI link : outScopeURIs) {
        		String uri = link.getUURI().toString().toLowerCase();
        		if (uri.endsWith(".js")) {
        			logger.info("Required resource " + 
        					uri + " is out of scope.");
        		}
        	}
        }
    }
    
    /**
     * Update fetch cache when a resource is available.
     * @param curi
     */
    protected void updateResourceStatus(CrawlURI curi) {
        fetchCache.updateResourceStatus(curi);
    }
    
    /**
     * If there are HTML documents whose resources are all available, then
     * create special CrawlURI (scheme: x-jseval), and add them to
     * outCandidates - waiting to be scheduled.
     * @param curi
     */
    protected void scheduleAnalysisURI(CrawlURI curi) {
        Map<String, Collection<String>> uris = fetchCache.getNClearReadyURIs();
        
        if (uris == null || uris.size() == 0) {
            return;
        }
        
        Collection<String> uriKeys = uris.keySet();
        for (String urikey : uriKeys) {
            Collection<String> resList = uris.get(urikey);
            CrawlURI caURI = createAnalysisURI(curi, urikey, resList);
            caURI.setForceFetch(true);
            curi.getOutCandidates().add(caURI);
            //frontier.schedule(caURI);
        }
    }
    
    /**
     * Create a special CrawlURI instance, whose scheme is "x-jseval".
     * @param curi current normal uri (HTML document).
     * @param uristr the string representation of current uri.
     * @param resources (script document) the list of uris 
     * (string representation) on which the HTML document depends.
     * @return 
     */
    protected CrawlURI createAnalysisURI(CrawlURI curi, String uristr, 
            Collection<String> resources) {
        CrawlURI caURI = null;
        UURI uuri = null;
        try {
            uuri = UURIFactory.getInstance("x-jseval:" + uristr);
            caURI = new CrawlURI(uuri, curi.getPathFromSeed() + "L", 
                    curi.getUURI(), null);
            caURI.setSchedulingDirective(HIGH);
            caURI.setSeed(false);
            caURI.getData().put("js-required-resources", resources);
        } catch (URIException e) {
            loggerModule.logUriError(e, uuri, uristr);
        }
        return caURI;
    }
    
    /**
     * This processor only handle HTML document and script document 
     * (for JavaScript execution purpose).
     * @param puri
     * @return
     */
    protected boolean isProperURI(ProcessorURI puri) {
    	return (isHTMLDocument(puri) || isScriptDocument(puri));
    }
    
    /**
     * Check if a document is an HTML document.
     * @param puri
     * @return
     */
    public static boolean isHTMLDocument(ProcessorURI puri) {
        String mime = puri.getContentType().toLowerCase();
        if (mime.startsWith("text/html")) {
            return true;
        }
        if (mime.startsWith("application/xhtml")) {
            return true;
        }
        
    	return false;
    }
    
    /**
     * Check if a document is a script document
     * @param puri
     * @return
     */
    public static boolean isScriptDocument(ProcessorURI puri) {
        String contentType = puri.getContentType();
        // If the content-type indicates js, we should process it.
        if (contentType.indexOf("javascript") >= 0) {
            return true;
        }
        if (contentType.indexOf("jscript") >= 0) {
            return true;
        }
        if (contentType.indexOf("ecmascript") >= 0) {
            return true;
        }
        
        // If the filename indicates js, we should process it.
        if (puri.toString().toLowerCase().endsWith(".js")) {
            return true;
        }
        
        // If the viaContext indicates a script, we should process it.
        LinkContext context = puri.getViaContext();
        if (context == null) {
            return false;
        }
        String s = context.toString().toLowerCase();
        return s.startsWith("script");
    }

    static {
        KeyManager.addKeys(FetchCacheUpdater.class);
    }
}
