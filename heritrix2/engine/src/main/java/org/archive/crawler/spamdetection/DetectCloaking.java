package org.archive.crawler.spamdetection;

import static org.archive.crawler.datamodel.SchedulingConstants.HIGH;

import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.httpclient.URIException;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.postprocessor.FetchCacheUpdater;
import org.archive.modules.Processor;
import org.archive.modules.ProcessorURI;
import org.archive.modules.extractor.HTMLLinkContext;
import org.archive.modules.extractor.LinkContext;
import org.archive.modules.extractor.jsexecutor.HTMLParser;
import org.archive.modules.fetchcache.FetchCache;
import org.archive.modules.fetchcache.FetchCacheUtil;
import org.archive.net.UURI;
import org.archive.settings.Finishable;
import org.archive.state.Immutable;
import org.archive.state.Initializable;
import org.archive.state.Key;
import org.archive.state.KeyManager;
import org.archive.state.StateProvider;
import org.lobobrowser.html.UserAgentContext;
import org.lobobrowser.html.domimpl.HTMLDocumentImpl;
import org.lobobrowser.html.io.WritableLineReader;
import org.lobobrowser.html.js.Executor;
import org.lobobrowser.html.test.SimpleUserAgentContext;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.w3c.dom.Document;

/**
 * This processor is used to detect both client and server side cloaking.
 * 
 * @author Ping Wang
 *
 */
public class DetectCloaking extends Processor 
                            implements Initializable, Finishable{
    private static final long serialVersionUID = 1L;
    
    private static Logger logger =
        Logger.getLogger(DetectCloaking.class.getName());
    
    @Immutable
    final public static Key<FetchCache> FETCH_CACHE = 
        Key.makeAuto(FetchCache.class);
    
    @Immutable
    final public static Key<Cache4CloakingDetection> CLOAKING_CACHE = 
        Key.makeAuto(Cache4CloakingDetection.class);
    
    static {
        KeyManager.addKeys(DetectCloaking.class);
    }
    
    private FetchCache fetchCache;
    private Cache4CloakingDetection cloakingCache;

    public void initialTasks(StateProvider global) {
        this.fetchCache = global.get(this, FETCH_CACHE);
        this.cloakingCache = global.get(this, CLOAKING_CACHE);
    }

    public void finalTasks(StateProvider global) {
        
    }

    final protected boolean shouldProcess(ProcessorURI uri) {
        String scheme = uri.getUURI().getScheme(); 
        if (FetchCacheUpdater.isHTMLDocument(uri)) {
            if (uri.getFetchStatus() >= 200 && uri.getFetchStatus() < 400) {
                return true;
            }
        }
        
        if (scheme.equals("x-jseval")) {
            return true;
        }
        
        return false;
    }
    
    protected void innerProcess(ProcessorURI uri) {
        try {
            updatePrerequisitesCache(uri);
            cloakingDetection(uri);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Update prerequisite information in cloaking cache, when a normal 
     * document is available, or when a x-jseval uri appears.  
     * @param uri
     * @throws IOException
     */
    protected void updatePrerequisitesCache(ProcessorURI uri)
    throws IOException {
        String scheme = uri.getUURI().getScheme();
        CrawlURI caURI = null;
        
        if (scheme.equals("http") || scheme.equals("https")) {
            String uriStr = uri.getUURI().toString();
            String content = 
                uri.getRecorder().getReplayCharSequence().toString();
            String via = flattenVia(uri);
            Map<String, Object> prerequisites = 
                cloakingCache.getEntry(uriStr);
            
            if (via != null && ! via.equals("")){
                // If via field is not empty, which means the uri has a referrer, 
                // then update prerequisite: "referrer" and 
                // "content-with-referrer".
                // And if "content-without-referrer" is still not available, then 
                // create a duplicate uri, but set its via to null, and force it
                // to be scheduled.
                cloakingCache.addPrerequisite(uriStr, "referrer", via);
                cloakingCache.addPrerequisite(uriStr, 
                        "content-with-referrer", content);
                if (! prerequisites.containsKey("content-without-referrer")) {
                    HTMLLinkContext hc = null;
                    caURI = createDuplicateURI(uri, null, hc);
                    ((CrawlURI) uri).getOutCandidates().add(caURI);
                }
            } else {
                // If via field is empty, which means the uri has no referrer, 
                // then update prerequisite: "content-without-referrer".
                // And if "content-with-referrer" is still not available, then 
                // create a duplicate uri, but set its via to itself, 
            	// and force it to be scheduled.

            	cloakingCache.addPrerequisite(uriStr, 
                        "content-without-referrer", content);
                if (! prerequisites.containsKey("content-with-referrer")) {
                    //HTMLLinkContext hc = new HTMLLinkContext(uriStr);
                	HTMLLinkContext hc = null;
                    caURI = createDuplicateURI(uri, uri.getUURI(), hc);
                    ((CrawlURI) uri).getOutCandidates().add(caURI);
                }
            }
        } else if (scheme.equals("x-jseval")) {
        	// For x-jseval uri, update "js-required-resources" prerequisite.
            String uriStr = uri.getUURI().getPath();
            Object res = uri.getData().get("js-required-resources");
            Collection<String> resources = (Collection<String>) res;
            cloakingCache.addPrerequisite(uriStr, "resources", resources);
        }
    }
    
    protected CrawlURI createDuplicateURI(ProcessorURI uri, UURI uuri, 
    		LinkContext via) {
        CrawlURI caURI = null;
        
        caURI = new CrawlURI(uri.getUURI(), uri.getPathFromSeed(), uuri, via);
        caURI.setSchedulingDirective(HIGH);
        caURI.setSeed(false);
        caURI.setForceFetch(true);
        
        return caURI;
    }
    
    /**
     * Detect cloaking both client and server side.
     * @param uri
     * @return true if one of the cloakings happened, otherwise false.
     * @throws URIException
     */
    protected boolean cloakingDetection(ProcessorURI uri) 
    throws URIException {
        
        UURI uuri = uri.getUURI();
        String key = uuri.getScheme().equals("x-jseval") ? 
                uuri.getPath() : uuri.toString();
                
        if (! cloakingCache.isReady4Detection(key)) {
            return false;
        }

        boolean clientCloaking = false;
        boolean serverCloaking = false;
        
        Map<String, Object> prerequisites= cloakingCache.getEntry(key);
        Collection<String> resources = 
            (Collection<String>) prerequisites.get("resources");
        
        // Get contents of HTML document and required JS resources. 
        HashMap<String, String> contents = new HashMap<String, String>();
        String content;
        
        for (String resURIStr : resources) {
            content = 
                (String) FetchCacheUtil.getContentLocation(fetchCache, 
                        resURIStr);
            
            if (content != null) {
                contents.put(resURIStr, content);
            }
        }
        
        content = (String) FetchCacheUtil.getContentLocation(fetchCache, key);
        if (content != null) {
            contents.put(key, content);
        }
        
        // Client side cloaking detection: parse the HTML document with JS 
        // enabled twice, once with referrer set to "" and once set to itself.
        // Then compare the resulting DOMs.
        String referrer = "";
        Document document = parse(key, referrer, contents);
        String withoutReferrer = DOMUtil.transfomToString(document);
        
        referrer = (String) prerequisites.get("referrer");
        document = parse(key, referrer, contents);
        String withReferrer = DOMUtil.transfomToString(document);
        
        if (! withReferrer.equals(withoutReferrer)) {
            clientCloaking = true;
            logger.warning("Client side cloaking: " + key);
            System.out.println("Client side cloaking: " + key);
        }
        
        // Server side cloaking detection: compare content-without-referrer and
        // content-with-referrer, to decide if server side cloaking happened.
        withoutReferrer = 
        	(String) prerequisites.get("content-without-referrer");
        withReferrer = (String) prerequisites.get("content-with-referrer");
        if (! withReferrer.equals(withoutReferrer)) {
            serverCloaking = true;
            logger.warning("Server side cloaking: " + key);
            System.out.println("Server side cloaking: " + key);
        }
        
        cloakingCache.removeEntry(key);
        
        return (clientCloaking | serverCloaking);
    }
    
    /**
     * Parse a HTML document, similar to parse method in class ExecuteJS, the 
     * difference is here document.referrer is considered.
     * @param uristr current uri
     * @param referrer value of document.referrer.
     * @param contents contents of document and JS required resources.
     * @return
     */
    protected Document parse(String uristr, String referrer, 
            HashMap<String, String> contents) {
        HTMLDocumentImpl document = null;
        String uriStr = uristr;

        String content = null;
        try {
            content = contents.get(uriStr);

            UserAgentContext uContext = new SimpleUserAgentContext();
            WritableLineReader wis = 
                new WritableLineReader(new StringReader(content));

            document = new HTMLDocumentImpl(uContext, null, wis, uriStr);
            
            String systemId = uriStr;
            String publicId = systemId;
            HTMLParser parser = new HTMLParser(uContext,document, null, 
                    publicId, systemId, contents);
            
            if (! referrer.equals("")) {
            	
                Context ctx = 
                    Executor.createContext(document.getDocumentURL(), 
                    		uContext);
                
                Scriptable scope = 
                    (Scriptable) document.getUserData(Executor.SCOPE_KEY);
                
                String scriptURI = document.getBaseURI();
                int baseLineNumber = 1;
                String text = "document.referrer = '" + referrer + "'";
                ctx.evaluateString(scope, text, scriptURI, 
                		baseLineNumber, null);
                Context.exit();
            }
            
            parser.parse(wis);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return document;
    }
}