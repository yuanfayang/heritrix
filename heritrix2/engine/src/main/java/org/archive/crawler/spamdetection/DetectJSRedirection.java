package org.archive.crawler.spamdetection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.logging.Logger;

import org.apache.commons.httpclient.URIException;
import org.archive.modules.Processor;
import org.archive.modules.ProcessorURI;
import org.archive.modules.extractor.Link;
import org.archive.modules.extractor.jsexecutor.ExecuteJS;
import org.archive.modules.fetchcache.FetchCache;
import org.archive.settings.Finishable;
import org.archive.state.Immutable;
import org.archive.state.Initializable;
import org.archive.state.Key;
import org.archive.state.KeyManager;
import org.archive.state.StateProvider;
import org.lobobrowser.html.domimpl.HTMLDocumentImpl;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * This processor is used to detect JavaScript redirection by evaluating
 * JavaScrip code (internal or external).
 * It only accepts special CrawlURI - scheme: x-jseval.
 * @author Ping Wang
 *
 */
public class DetectJSRedirection extends Processor 
implements Initializable, Finishable{

    private static final long serialVersionUID = 1L;
    
    private static Logger logger =
        Logger.getLogger(DetectJSRedirection.class.getName());
    
    private static ArrayList<String> eventsToSim = new ArrayList<String>();

    @Immutable
    final public static Key<FetchCache> FETCH_CACHE = 
        Key.makeAuto(FetchCache.class);

    static {
        KeyManager.addKeys(DetectJSRedirection.class);
        
        eventsToSim.add("onmouseover");
        eventsToSim.add("onmouseout");
    }
    
    private FetchCache fetchCache;
    
    public void initialTasks(StateProvider global) {
        this.fetchCache = global.get(this, FETCH_CACHE);
    }

    public void finalTasks(StateProvider global) {
        
    }

    final protected boolean shouldProcess(ProcessorURI uri) {
        String scheme = uri.getUURI().getScheme();
        if (scheme.equals("x-jseval")) {
            return true;
        }
        return false;
    }
    
    protected void innerProcess(ProcessorURI uri) {
        String uriStr = null;
        try {
            uriStr = uri.getUURI().getPath();
        } catch (URIException e) {
            e.printStackTrace();
            return;
        }
        
        boolean hasJSRedirection = false;
        
        // If ExecuteJS appears before this processor, only take advantage 
        // of the result it generated, else JS code has to been evaluated in
        // order to check JS redirection.
        if (uri.getData().containsKey("ExecuteJS")) {
            hasJSRedirection = simpleDetection(uri);
        } else {
            hasJSRedirection = complexDetection(uri);
        }
        
        if (hasJSRedirection) {
            logger.warning("JavaScript redirection: " + uriStr);
            System.out.println("JavaScript redirection: " + uriStr);
        }
    }
    
    /**
     * Only need to check if there is one "JSRedirection" link in out links.
     * @param uri
     * @return
     */
    protected boolean simpleDetection(ProcessorURI uri) {
        Collection<Link> outLinks = uri.getOutLinks();
        if (outLinks != null && outLinks.size() > 0) {
            for (Link link : outLinks) {
                String context = link.getContext().toString();
                if (context.equals("JSRedirection")) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Evaluate JS code within the HTML document in order to check 
     * JS redirection.
     * @param uri
     * @return true if JS redirection happened, otherwise false.
     */
    protected boolean complexDetection(ProcessorURI uri) {
        HashMap<String, String> contents = 
            ExecuteJS.getContent(uri, fetchCache);
        
        // Parse the HTML document, and evaluate JS code 
        // triggered by onload event.
        Document document = ExecuteJS.parse(uri, contents);
        if (hasJSRedirection(uri, document)) {
            return true;
        }
        
        // Simulate some HTML events, try to detect hidden JS redirection.
        if (simHTMLEvents(uri, document, contents)) {
            return true;
        }
        return false;
    }
    
    protected boolean hasJSRedirection(ProcessorURI uri, Document document) {
        if (ExecuteJS.getJSRedirection((HTMLDocumentImpl)document) != null) {
            return true;
        }
        return false;
    }
    
    /**
     * Simulate some HTML events: onmouseover and onmouseout.
     * Trigger each HTML event from a fresh DOM, once JS redirection is 
     * detected, stop simulation.
     * @param uri current uri.
     * @param doc DOM created by HTML parser (JS code triggered by onload
     * event have been evaluated).
     * @param contents contents of current HTML document and requried 
     * resources for JS evaluation.
     * @return true if JS redirection happened, otherwise false.
     */
    protected boolean simHTMLEvents(ProcessorURI uri, Document doc, 
            HashMap<String, String> contents) {
        int numOfEventsToSim = eventsToSim.size();
        
        NodeList nodeList = null;
        Document curDoc;
        int listSize;
        for (int i = 0; i < numOfEventsToSim; ++ i) {
            if (eventsToSim.get(i).equalsIgnoreCase("onmouseover")) {
                // Simulate onmouseover event
                nodeList = 
                    ExecuteJS.getNodeList(ExecuteJS.ONMOUSEOVER_REG_EXPR, doc);
                if (nodeList != null && 
                        (listSize = nodeList.getLength()) != 0) {
                    for (int j = 0; j < listSize; ++ j) {
                        curDoc = ExecuteJS.parse(uri, contents);
                        
                        nodeList = 
                            ExecuteJS.getNodeList(ExecuteJS.ONMOUSEOVER_REG_EXPR, 
                                    curDoc);
                        
                        Element el = (Element)nodeList.item(j);
                        ExecuteJS.onMouseOver(el);
                        if (hasJSRedirection(uri, curDoc)) {
                            return true;
                        }
                    }
                }
                
            } else if (eventsToSim.get(i).equalsIgnoreCase("onmouseout")) {
                // Simulate onmouseout event
                nodeList = ExecuteJS.getNodeList(ExecuteJS.ONMOUSEOUT_REG_EXPR, 
                        doc);
                
                if (nodeList != null && 
                        (listSize = nodeList.getLength()) != 0) {
                    for (int j = 0; j < listSize; ++ j) {
                        curDoc = ExecuteJS.parse(uri, contents);
                        
                        nodeList = 
                            ExecuteJS.getNodeList(ExecuteJS.ONMOUSEOUT_REG_EXPR,
                                    curDoc);
                        
                        Element el = (Element)nodeList.item(j);
                        ExecuteJS.onMouseOut(el);
                        if (hasJSRedirection(uri, curDoc)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
}