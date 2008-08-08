package org.archive.modules.spamdetection;

import java.io.StringReader;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Map;
import java.util.logging.Logger;

import org.archive.modules.Processor;
import org.archive.modules.ProcessorURI;
import org.archive.modules.extractor.jsexecutor.ExecuteJS;
import org.archive.modules.extractor.jsexecutor.HTMLParser;
import org.archive.modules.fetchcache.FetchCache;
import org.archive.modules.fetchcache.FetchCacheUtil;
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

public class DetectCloaking extends Processor implements Initializable, Finishable{
    private static final long serialVersionUID = 1L;
    
    private static Logger logger =
        Logger.getLogger(DetectCloaking.class.getName());
    
    @Immutable
    final public static Key<FetchCache> FETCH_CACHE = 
        Key.makeAuto(FetchCache.class);

    static {
        KeyManager.addKeys(DetectCloaking.class);
    }
    
    private FetchCache fetchCache;

    public void initialTasks(StateProvider global) {
        System.out.println("Detect Cloaking");
        this.fetchCache = global.get(this, FETCH_CACHE);
    }

    public void finalTasks(StateProvider global) {
        
    }

    final protected boolean shouldProcess(ProcessorURI uri) {
        String scheme = uri.getUURI().getScheme(); 
        if (! scheme.equals("x-jseval")) {
            return false;
        }
        return true;
    }
    
    protected void innerProcess(ProcessorURI uri) {
        boolean hasClientCloaking = clientCloakingDetection(uri);
        boolean hasServerCloaking = serverCloakingDetection(uri);
    }
    
    protected boolean clientCloakingDetection(ProcessorURI uri) {
        String referrer = "";
        Document document = parse(uri, referrer);        
        String woReferrer = DOMUtil.transfomToString(document); 
        
        document = parse(uri, "");
        String wReferrer = DOMUtil.transfomToString(document);
        System.out.println(woReferrer);
        System.out.println(wReferrer);
        
        if (wReferrer.equals(woReferrer)) {
            return false;
        }
        
        return true;
    }
    
    protected boolean serverCloakingDetection(ProcessorURI uri) {
        return false;
    }

    protected Document parse(ProcessorURI uri, String referrer) {
        HTMLDocumentImpl document = null;
        Map<String, Object> resourceLocation = new Hashtable<String, Object>();
        
        Collection<String> resources = 
            (Collection<String>) uri.getData().get("js-required-resources");

        for (String resURIStr : resources) {
            Object location = 
                FetchCacheUtil.getContentLocation(this.fetchCache, resURIStr);
            if (location != null) {
                resourceLocation.put(resURIStr, location);
            }
        }
        
        try {
            String uriStr = uri.getUURI().getPath();
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
                String text = "document.referrer = 'www.google.com'";
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
