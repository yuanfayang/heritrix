package org.archive.modules.extractor.jsexecutor;

import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.logging.Logger;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.httpclient.URIException;
//import org.archive.crawler.datamodel.CrawlURI;
import org.archive.modules.fetchcache.FetchCache;
import org.archive.modules.fetchcache.FetchCacheUtil;
import org.archive.io.ReplayInputStream;
import org.archive.modules.Processor;
import org.archive.modules.ProcessorURI;
import org.archive.modules.extractor.ExtractorHTML;
import org.archive.modules.extractor.HTMLLinkContext;
import org.archive.modules.extractor.Hop;
import org.archive.modules.extractor.Link;
import org.archive.modules.extractor.UriErrorLoggerModule;
import org.archive.modules.fetcher.FetchStatusCodes;
import org.archive.net.UURI;
import org.archive.net.UURIFactory;
import org.archive.settings.Finishable;
import org.archive.state.Immutable;
import org.archive.state.Initializable;
import org.archive.state.Key;
import org.archive.state.KeyManager;
import org.archive.state.StateProvider;
import org.archive.util.Recorder;
import org.lobobrowser.html.UserAgentContext;
import org.lobobrowser.html.domimpl.HTMLAbstractUIElement;
import org.lobobrowser.html.domimpl.HTMLDocumentImpl;
import org.lobobrowser.html.io.WritableLineReader;
import org.lobobrowser.html.js.Window;
import org.lobobrowser.html.renderer.HtmlController;
import org.lobobrowser.html.test.SimpleUserAgentContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class ExecuteJS extends Processor implements Initializable, Finishable{
    private static final long serialVersionUID = 1L;
    
    private static Logger logger =
        Logger.getLogger(ExecuteJS.class.getName());
    
    final public static Key<UriErrorLoggerModule> URI_ERROR_LOGGER_MODULE = 
        Key.makeAuto(UriErrorLoggerModule.class);
    
    @Immutable
    final public static Key<FetchCache> FETCH_CACHE = 
        Key.makeAuto(FetchCache.class);

    static {
        KeyManager.addKeys(ExecuteJS.class);
    }
    
    protected UriErrorLoggerModule uriErrors;
    private FetchCache fetchCache; 
    
    public void initialTasks(StateProvider global) {
        this.uriErrors = global.get(this, URI_ERROR_LOGGER_MODULE); 
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
        System.out.println(uri.getUURI().toString());
        
        /*Collection<String> resources = 
            (Collection<String>) uri.getData().get("js-required-resources");
        for (String resURIStr : resources) {
            System.out.println(resURIStr);
        }*/
        
        HashSet<String> handledUris = new HashSet<String>();
        
        Document document = parse(uri);
        if (document != null) {
            discoverNewLinks(uri, document, handledUris);
            
            if (shouldSimHTMLEvents()) {
                handleHTMLEvents1(uri, document, handledUris);
            }
            
            for (Link wref: uri.getOutLinks()) {
                System.out.println(wref.getDestination());
            }
        	//uri.setFetchStatus(200);
        }
    }

    // TODO: based on user's configurateion to decide
    protected boolean shouldSimHTMLEvents() {
        return true;
    }

    /**
     * Parse HTML document, and generate DOM tree, 
     * inlined JavaScript code is executed. 
     * @param uri
     * @return
     */
    protected Document parse(ProcessorURI uri) {
        HTMLDocumentImpl document = null;
        Map<String, Object> resourceLocation = new Hashtable<String, Object>();
        
        Collection<String> resources = 
            (Collection<String>) uri.getData().get("js-required-resources");

        for (String resURIStr : resources) {
            //Object location = getContentLocation(this.fetchCache, resURIStr);
            Object location = 
                FetchCacheUtil.getContentLocation(this.fetchCache, resURIStr);
            if (location != null) {
                resourceLocation.put(resURIStr, location);
            }
        }
        
        try {
            String uriStr = uri.getUURI().getPath();
            //Object docLocation = getContentLocation(this.fetchCache, uriStr);
            Object docLocation = 
                FetchCacheUtil.getContentLocation(this.fetchCache, uriStr);
            //Recorder recorder = (Recorder) docLocation;
            //String chacacterEncoding = recorder.getCharacterEncoding();            
            //ReplayInputStream replayIn = recorder.getReplayInputStream();
            //replayIn.skip(replayIn.getHeaderSize());
            
            String content = (String) docLocation;

            UserAgentContext uContext = new SimpleUserAgentContext();
            //WritableLineReader wis = new WritableLineReader(
            //        new InputStreamReader(replayIn, chacacterEncoding));
            WritableLineReader wis = 
            	new WritableLineReader(new StringReader(content));

            document = new HTMLDocumentImpl(uContext, null, wis, uriStr);
            
            String systemId = uriStr;
            String publicId = systemId;
            HTMLParser parser = new HTMLParser(uContext,document, null, 
                    publicId, systemId,resourceLocation);
            
            parser.parse(wis);

            // For testing
            saveHtml(document, "1.html");
            
        } catch (Exception e) {
            e.printStackTrace();
        }

        return document;
    }
    
    protected Object getContentLocation(FetchCache fetchCache, String uriStr) {
        return fetchCache.getContentLocation(uriStr);
    }
    
    /**
     * Simulate HTML events in an accumulative fashion, 
     * and discover new link after simulation 
     * @param uri
     * @param document
     */
    protected void handleHTMLEvents(ProcessorURI uri, Document document, 
            HashSet<String> handledUris) {
        ArrayList<String> eventsToSim = null;
        if ((eventsToSim = getEventsToSim()) == null)
            return;
        
        int numOfEventsToSim = eventsToSim.size();
        HTMLEventHandler eventHandler = new HTMLEventHandler(document);
        
        for (int i = 0; i < numOfEventsToSim; ++ i) {
            simHTMLEvent(eventsToSim.get(i), eventHandler);
            discoverNewLinks(uri, document, handledUris);
        }
    }
    
    /**
     * Simulate each type of HTML event
     * @param event type of HTML event
     * @param eventHandler
     */
    protected void simHTMLEvent(String event, HTMLEventHandler eventHandler) {
        if (event.equalsIgnoreCase("onclick")) {
            eventHandler.simMouseClick();
        } else if (event.equalsIgnoreCase("onmouseover")) {
            eventHandler.simMouseOver();
        } else if (event.equalsIgnoreCase("onmouseout")) {
            eventHandler.simMouseOut();
        }
    }

    private static String HREF_REG_EXPR = "html//a";
    private static String ONCLICK_REG_EXPR = "//*[@" + "onclick" + "]";
    private static String ONMOUSEOVER_REG_EXPR = "//*[@" + "onmouseover" + "]";
    private static String ONMOUSEOUT_REG_EXPR = "//*[@" + "onmouseout" + "]";

    /**
     * Simulate each HTML event from a fresh start, 
     * i.e. a DOM newly generated by HTML parser.
     * @param uri
     * @param document
     * @param handledUris
     */
    protected void handleHTMLEvents1(ProcessorURI uri, Document document, 
            HashSet<String> handledUris) {
        ArrayList<String> eventsToSim = null;
        if ((eventsToSim = getEventsToSim()) == null)
            return;
        
        int numOfEventsToSim = eventsToSim.size();
        
        NodeList nodeList = null;
        Document curDocument;
        int listSize;
        for (int i = 0; i < numOfEventsToSim; ++ i) {
            if (eventsToSim.get(i).equalsIgnoreCase("onclick")) {
                // Simulate common onclick event
                nodeList = getNodeList(ONCLICK_REG_EXPR, document);
                if (nodeList != null && 
                        (listSize = nodeList.getLength()) != 0) {
                    for (int j = 0; j < listSize; ++ j) {
                        curDocument = getDocument(uri);
                        nodeList = 
                            getNodeList(ONCLICK_REG_EXPR, curDocument);
                        Element el = (Element)nodeList.item(j);
                        onMouseClick(el);
                        discoverNewLinks(uri, curDocument, handledUris);
                    }
                }

                // Simulate a click on an anchor tag
                nodeList = getNodeList(HREF_REG_EXPR, document);
                if (nodeList != null && 
                        (listSize = nodeList.getLength()) != 0) {
                	curDocument = getDocument(uri);
                    for (int j = 0; j < listSize; ++ j) {
                        Element el = (Element)nodeList.item(j);
                        onMouseClick(el);
                        discoverNewLinks(uri, curDocument, handledUris);
                    }
                }
                
            } else if (eventsToSim.get(i).equalsIgnoreCase("onmouseover")) {
                // Simulate onmouseover event
                nodeList = getNodeList(ONMOUSEOVER_REG_EXPR, document);
                if (nodeList != null && 
                        (listSize = nodeList.getLength()) != 0) {
                    for (int j = 0; j < listSize; ++ j) {
                        curDocument = getDocument(uri);
                        nodeList = 
                            getNodeList(ONMOUSEOVER_REG_EXPR, curDocument);
                        Element el = (Element)nodeList.item(j);
                        onMouseOver(el);
                        discoverNewLinks(uri, curDocument, handledUris);
                    }
                }
                
            } else if (eventsToSim.get(i).equalsIgnoreCase("onmouseout")) {
                // Simulate onmouseout event
                nodeList = getNodeList(ONMOUSEOUT_REG_EXPR, document);
                if (nodeList != null && 
                        (listSize = nodeList.getLength()) != 0) {
                    for (int j = 0; j < listSize; ++ j) {
                        curDocument = getDocument(uri);
                        nodeList = 
                            getNodeList(ONMOUSEOUT_REG_EXPR, curDocument);
                        Element el = (Element)nodeList.item(j);
                        onMouseOut(el);
                        discoverNewLinks(uri, curDocument, handledUris);
                    }
                }
            }
        }
    }
    
    Document getDocument(ProcessorURI uri) {
        return parse(uri);
    }
    
    private void onMouseClick(Element el) {
        if (el instanceof HTMLAbstractUIElement) {
            HTMLAbstractUIElement uiElement = 
                (HTMLAbstractUIElement) el;
            HtmlController.getInstance().onMouseClick(uiElement, 
                    null, 0, 0);
        }
    }
    
    public void onMouseOver(Element el) {
        if (el instanceof HTMLAbstractUIElement) {
            HTMLAbstractUIElement uiElement = 
                (HTMLAbstractUIElement) el;
            HtmlController.getInstance().onMouseOver(uiElement, 
                    null, 0, 0, null);
        }
    }

    public void onMouseOut(Element el) {
        if (el instanceof HTMLAbstractUIElement) {
            HTMLAbstractUIElement uiElement = 
                (HTMLAbstractUIElement) el;
            HtmlController.getInstance().onMouseOut(uiElement, 
                    null, 0, 0, null);
        }
    }

    /**
     * Discover new links, including anchor and img tags.
     * @param uri current uri
     * @param document DOM generated
     */
    protected void discoverNewLinks(ProcessorURI uri, Document document, 
            HashSet<String> handledUris) {
        
        handleRedirection(uri, document, handledUris);
        extractLinksFromDOM(uri, document, handledUris);
    }
    
    /**
     * Check if JS redirection happened, if so, add a new link and mark it as
     * navigation link not a referrer link
     * @param curi
     * @param document
     */
    protected void handleRedirection(ProcessorURI curi, Document document, 
            HashSet<String> handledUris) {
        String newUri = null;
        if ((newUri = getJSRedirection((HTMLDocumentImpl)document)) != null) {
            if (handledUris.add(newUri)) {
                addLinkFromString(curi, newUri, "JSRedirection", Hop.NAVLINK);
            }
        }
    }
    
    /**
     * Get JavaScript redirection
     * @param document
     * @return uri of JS redirection, return null if no redirection
     */
    protected String getJSRedirection(HTMLDocumentImpl document) {
        
        Window window = (Window) document.getDefaultView();
        String locationHref = window.getLocation().getLocationHref();
        if (locationHref != null && !locationHref.equals(document.getURL())) {
            //System.out.println("JS redirection happened");
            return locationHref;
        }
        
        return null;
    }

    /**
     * Find links from a DOM
     * @param uri
     * @param document
     */
    protected void extractLinksFromDOM(ProcessorURI uri, Document document, 
            HashSet<String> handledUris) {
        
        extractImgUris(uri, document, handledUris);
        extractAnchorUris(uri, document, handledUris);
    }
    
    /**
     * Find image source links
     * @param uri
     * @param document
     */
    private void extractImgUris(ProcessorURI uri, Document document, 
            HashSet<String> handledUris) {
        NodeList nodeList = getNodeList("html//img", document);
        
        if (nodeList != null) {
            int size = nodeList.getLength();
            for (int i = 0; i < size; ++ i) {
                Element element = (Element) nodeList.item(i);
                String value = element.getAttribute("src");
                if (value != null && handledUris.add(value)) {
                    CharSequence context = 
                        ExtractorHTML.elementContext("img", "src");
                    addLinkFromString(uri, value, context, Hop.EMBED);
                }
            }
        }
    }
    
    /**
     * Find anchor href links
     * @param uri
     * @param document
     */
    private void extractAnchorUris(ProcessorURI uri, Document document, 
            HashSet<String> handledUris) {
        NodeList nodeList = getNodeList("html//a", document);
        
        if (nodeList != null) {
            int size = nodeList.getLength();
            for (int i = 0; i < size; ++ i) {
                Element element = (Element) nodeList.item(i);
                String value = element.getAttribute("href");
                if (value != null && handledUris.add(value)) {
                    CharSequence context = 
                        ExtractorHTML.elementContext("a", "href");
                    addLinkFromString(uri, value, context, Hop.NAVLINK);
                }
            }
        }
    }
    
    /**
     * Copied from class ExtractorHTML, made a little modification
     * @param uri
     * @param value
     * @param context
     * @param hop
     */
    private void addLinkFromString(ProcessorURI uri, String value,
            CharSequence context, Hop hop) {
        try {
            // We do a 'toString' on context because its a sequence from
            // the underlying ReplayCharSequence and the link its about
            // to become a part of is expected to outlive the current
            // ReplayCharSequence.
            HTMLLinkContext hc = new HTMLLinkContext(context.toString());
            int max = uriErrors.getMaxOutlinks(uri);
            
            String realUriStr = uri.getUURI().getPath();
            UURI baseUURI = UURIFactory.getInstance(realUriStr);
            UURI uuri = UURIFactory.getInstance(baseUURI, value);
            Link.add(uri, max, uuri.toString(), hc, hop);
        } catch (URIException e) {
            logUriError(e, uri, value);
        }
    }
    
    // TODO: Get user's configuration
    private ArrayList<String> getEventsToSim() {
        ArrayList<String> eventsToSim = new ArrayList<String>();
        eventsToSim.add("onmouseover");
        eventsToSim.add("onmouseout");
        eventsToSim.add("onclick");
        return eventsToSim;
    }
    
    /**
     * Copied from class ExtractorHTML
     * @param e
     * @param uri
     * @param l
     */
    protected void logUriError(URIException e, ProcessorURI uri, 
            CharSequence l) {
        if (e.getReasonCode() == UURIFactory.IGNORED_SCHEME) {
            // don't log those that are intentionally ignored
            return; 
        }
        uriErrors.logUriError(e, uri.getUURI(), l);
    }
    
    /**
     * Given an XPath expression, find nodes in a DOM 
     * @param evalExp an XPath expression
     * @param document 
     * @return a list of nodes if found
     */
    public static NodeList getNodeList(String evalExp, Object document) {
        
        NodeList nodeList = null;
        XPath xpath = XPathFactory.newInstance().newXPath();
        try {
            nodeList = (NodeList) xpath.evaluate(evalExp, document, 
                    XPathConstants.NODESET);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return nodeList;
    }
    
    public static void saveHtml(Document document, String name) {
        try {
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.METHOD, "html");
            //transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, "-//W3C//DTD HTML 4.0 Transitional//EN");
            DOMSource dSource = new DOMSource(document);
            FileOutputStream fos = new FileOutputStream(name);
            Result out = new StreamResult(fos);
            transformer.transform(dSource, out);
            //System.out.println(fos.getFD().toString());
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
