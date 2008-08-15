package org.archive.modules.spamdetection;

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
        eventsToSim.add("onclick");
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
        if (uri.getData().containsKey("ExecuteJS")) {
            hasJSRedirection = simpleDetection(uri);
        } else {
            hasJSRedirection = complexDetection(uri);
        }
        
        if (hasJSRedirection) {
            logger.warning("JavaScript redirection: " + uriStr);
        }
    }
    
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
    
    protected boolean complexDetection(ProcessorURI uri) {
        HashMap<String, String> contents = 
            ExecuteJS.getContent(uri, fetchCache);
        
        Document document = ExecuteJS.parse(uri, contents);
        if (hasJSRedirection(uri, document)) {
            return true;
        }
        
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
    
    protected boolean simHTMLEvents(ProcessorURI uri, Document doc, 
            HashMap<String, String> contents) {
        int numOfEventsToSim = eventsToSim.size();
        
        NodeList nodeList = null;
        Document curDoc;
        int listSize;
        for (int i = 0; i < numOfEventsToSim; ++ i) {
            if (eventsToSim.get(i).equalsIgnoreCase("onclick")) {
                // Simulate common onclick event
                nodeList = 
                    ExecuteJS.getNodeList(ExecuteJS.ONCLICK_REG_EXPR, doc);
                
                if (nodeList != null && 
                        (listSize = nodeList.getLength()) != 0) {
                    for (int j = 0; j < listSize; ++ j) {
                        curDoc = ExecuteJS.parse(uri, contents);
                        
                        nodeList = 
                            ExecuteJS.getNodeList(ExecuteJS.ONCLICK_REG_EXPR, 
                                    curDoc);
                        
                        Element el = (Element)nodeList.item(j);
                        ExecuteJS.onMouseClick(el);
                        if (hasJSRedirection(uri, curDoc)) {
                            return true;
                        }
                    }
                }

                // Simulate a click on an anchor tag
                nodeList = 
                    ExecuteJS.getNodeList(ExecuteJS.HREF_REG_EXPR, doc);
                if (nodeList != null && 
                        (listSize = nodeList.getLength()) != 0) {
                    curDoc = ExecuteJS.parse(uri, contents);
                    nodeList = 
                        ExecuteJS.getNodeList(ExecuteJS.HREF_REG_EXPR, curDoc);
                    for (int j = 0; j < listSize; ++ j) {
                        Element el = (Element)nodeList.item(j);
                        ExecuteJS.onMouseClick(el);
                        if (hasJSRedirection(uri, curDoc)) {
                            return true;
                        }
                    }
                }
                
            } else if (eventsToSim.get(i).equalsIgnoreCase("onmouseover")) {
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
