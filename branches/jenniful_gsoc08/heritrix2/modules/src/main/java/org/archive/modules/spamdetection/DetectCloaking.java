package org.archive.modules.spamdetection;

import static org.archive.crawler.datamodel.SchedulingConstants.HIGH;

import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.httpclient.URIException;
import org.archive.crawler.datamodel.CrawlURI;
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

public class DetectCloaking extends Processor 
                            implements Initializable, Finishable{
    private static final long serialVersionUID = 1L;
    
    private static Logger logger =
        Logger.getLogger(DetectCloaking.class.getName());
    
    @Immutable
    final public static Key<FetchCache> FETCH_CACHE = 
        Key.makeAuto(FetchCache.class);
    
    @Immutable
    final public static Key<Cache4CloakingDetction> CLOAKING_CACHE = 
        Key.makeAuto(Cache4CloakingDetction.class);
    
    static {
        KeyManager.addKeys(DetectCloaking.class);
    }
    
    private FetchCache fetchCache;
    private Cache4CloakingDetction cloakingCache;

    public void initialTasks(StateProvider global) {
        this.fetchCache = global.get(this, FETCH_CACHE);
        this.cloakingCache = global.get(this, CLOAKING_CACHE);
    }

    public void finalTasks(StateProvider global) {
        
    }

    final protected boolean shouldProcess(ProcessorURI uri) {
        String scheme = uri.getUURI().getScheme(); 
        String uriStr = uri.getUURI().toString();
        if (uriStr.endsWith(".html") || uriStr.endsWith("htm")) {
            if (uri.getContentSize() > 0) {
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
            boolean hasCloaking = cloakingDetection(uri);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
    
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
                cloakingCache.addPrerequisite(uriStr, "referrer", via);
                cloakingCache.addPrerequisite(uriStr, 
                        "wreferrer", content);
                if (! prerequisites.containsKey("woreferrer")) {
                    HTMLLinkContext hc = new HTMLLinkContext(uriStr);
                    caURI = createDuplicateURI(uri, null);
                    ((CrawlURI) uri).getOutCandidates().add(caURI);
                }
            } else {
                cloakingCache.addPrerequisite(uriStr, 
                        "woreferrer", content);
                if (! prerequisites.containsKey("wreferrer")) {
                    HTMLLinkContext hc = new HTMLLinkContext(uriStr);
                    caURI = createDuplicateURI(uri, hc);
                    ((CrawlURI) uri).getOutCandidates().add(caURI);
                }
            }
        } else if (scheme.equals("x-jseval")) {
            String uriStr = uri.getUURI().getPath();
            Object res = uri.getData().get("js-required-resources");
            Collection<String> resources = (Collection<String>) res;
            cloakingCache.addPrerequisite(uriStr, "resources", resources);
        }
    }
    
    protected CrawlURI createDuplicateURI(ProcessorURI uri, LinkContext via) {
        CrawlURI caURI = null;
        UURI uuri = null;
        
        uuri = uri.getUURI();
        caURI = new CrawlURI(uuri, uri.getPathFromSeed(), uri.getUURI(), via);
        caURI.setSchedulingDirective(HIGH);
        caURI.setSeed(false);
        caURI.setForceFetch(true);
        
        return caURI;
    }
    
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
        
        String referrer = "";
        Document document = parse(key, referrer, resources);
        String woReferrer = DOMUtil.transfomToString(document);
        
        referrer = (String) prerequisites.get("referrer");
        document = parse(key, referrer, resources);
        String wReferrer = DOMUtil.transfomToString(document);
        System.out.println(woReferrer);
        System.out.println(wReferrer);
        
        if (! wReferrer.equals(woReferrer)) {
            clientCloaking = true;
        }
        
        woReferrer = (String) prerequisites.get("woreferrer");
        wReferrer = (String) prerequisites.get("wreferrer");
        if (! wReferrer.equals(woReferrer)) {
            serverCloaking = true;
        }
        
        cloakingCache.removeEntry(key);
        
        return (clientCloaking | serverCloaking);
    }
    
    protected Document parse(String uristr, String referrer, 
            Collection<String> resources) {
        HTMLDocumentImpl document = null;
        Map<String, Object> resourceLocation = new Hashtable<String, Object>();
        String uriStr = uristr;
        
        for (String resURIStr : resources) {
            Object location = 
                FetchCacheUtil.getContentLocation(this.fetchCache, resURIStr);
            if (location != null) {
                resourceLocation.put(resURIStr, location);
            }
        }
        
        try {
            Object docLocation = 
                FetchCacheUtil.getContentLocation(this.fetchCache, uriStr);
            
            String content = (String) docLocation;

            UserAgentContext uContext = new SimpleUserAgentContext();
            WritableLineReader wis = 
                new WritableLineReader(new StringReader(content));

            document = new HTMLDocumentImpl(uContext, null, wis, uriStr);
            
            String systemId = uriStr;
            String publicId = systemId;
            HTMLParser parser = new HTMLParser(uContext,document, null, 
                    publicId, systemId,resourceLocation);
            
            if (! referrer.equals("")) {
                Context ctx = 
                    Executor.createContext(document.getDocumentURL(), uContext);
                Scriptable scope = 
                    (Scriptable) document.getUserData(Executor.SCOPE_KEY);
                String scriptURI = document.getBaseURI();
                int baseLineNumber = 1;
                String text = "document.referrer = '" + referrer + "'";
                ctx.evaluateString(scope, text, scriptURI, baseLineNumber, null);
                Context.exit();
            }
            
            parser.parse(wis);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return document;
    }
}
