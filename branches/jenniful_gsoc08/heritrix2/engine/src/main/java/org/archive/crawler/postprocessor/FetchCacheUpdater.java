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
import org.archive.crawler.framework.Frontier;
import org.archive.modules.PostProcessor;
import org.archive.modules.Processor;
import org.archive.modules.ProcessorURI;
import org.archive.modules.extractor.Link;
import org.archive.modules.fetchcache.FetchCache;
import org.archive.net.UURI;
import org.archive.net.UURIFactory;
import org.archive.state.Immutable;
import org.archive.state.Initializable;
import org.archive.state.Key;
import org.archive.state.KeyManager;
import org.archive.state.StateProvider;

public class FetchCacheUpdater extends Processor implements 
Initializable, PostProcessor{
    private static final long serialVersionUID = 1L;

    private static final Logger logger =
        Logger.getLogger(FetchCacheUpdater.class.getName());


    @Immutable
    final public static Key<Frontier> FRONTIER = Key.makeAuto(Frontier.class);

    @Immutable
    final public static Key<FetchCache> FETCH_CACHE = 
        Key.makeAuto(FetchCache.class);
    
    @Immutable
    final public static Key<CrawlerLoggerModule> LOGGER_MODULE = 
        Key.makeAuto(CrawlerLoggerModule.class);
    
    private Frontier frontier;
    private FetchCache fetchCache;
    
    protected CrawlerLoggerModule loggerModule;

    public void initialTasks(StateProvider global) {
        this.fetchCache = global.get(this, FETCH_CACHE);
        this.frontier = global.get(this, FRONTIER);
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
        //fetchCache.test();
        
        CrawlURI curi = (CrawlURI) puri;
        if (puri.getFetchStatus() < 200 || puri.getFetchStatus() >= 400) {
            if (puri.getUURI().toString().toLowerCase().endsWith(".js")) {
                handleFailedURI(puri);
            }
        } else {
	        updateStatus(curi);
        }
        scheduleAnalysisURI(curi);
    }
    
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
        
        if (uriStr.endsWith(".html") || uriStr.endsWith(".htm")) {
            updateHTMLStatus(curi);
        } else if (uriStr.endsWith(".js")) {
            updateResourceStatus(curi);
        }
    }
    
    /*protected void updateHTMLStatus(CrawlURI curi) {
        
        Collection<String> resURIs = new HashSet<String>();
        for (Link link : curi.getOutLinks()) {
            String uri = link.getDestination().toString().toLowerCase();
            if (uri.endsWith(".js")) {
                resURIs.add(uri);
            }
        }
        
        fetchCache.updateDependentStatus(curi, resURIs);
    }*/
    
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
    
    /*protected void updateHTMLStatus(CrawlURI curi) {
    
    Collection<String> resURIs = new HashSet<String>();
    for (CrawlURI link : curi.getOutCandidates()) {
        String uri = link.getUURI().toString().toLowerCase();
        if (uri.endsWith(".js")) {
            resURIs.add(uri);
        }
    }
    
    fetchCache.updateDependentStatus(curi, resURIs);
	}*/

    
    protected void updateResourceStatus(CrawlURI curi) {
        fetchCache.updateResourceStatus(curi);
    }
    
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
            System.out.print(urikey + ": ");
            for (String tmpStr : resList) {
                System.out.print(tmpStr + " ");
            }
            System.out.println();
        }
    }
    
/*    protected AnalysisURI createAnalysisURI(String uristr, 
            Collection<String> resources) {
        AnalysisURI aURI = null;
        UURI uuri = null;
        try {
            uuri = UURIFactory.getInstance(uristr);
            aURI = new AnalysisURI(uuri);
            aURI.setResourceUris(resources);
        } catch (URIException e) {
            loggerModule.logUriError(e, uuri, uristr);
        }
        return aURI;
    }*/
    
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
    
    
    protected boolean isProperURI(ProcessorURI puri) {
        String uriStr = puri.getUURI().toString().toLowerCase();
        
        if (uriStr.endsWith(".html") || uriStr.endsWith(".htm") 
                || uriStr.endsWith(".js")) {
            return true;
        }
        return false;
    }

    static {
        KeyManager.addKeys(FetchCacheUpdater.class);
    }
}
