/*
 * CrawlSettingsSAXHandler.java
 * Created on Dec 8, 2003
 *
 * $Header$
 */
package org.archive.crawler.datamodel.settings;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import javax.management.Attribute;

import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * @author johnh
 *
 */
public class CrawlSettingsSAXHandler extends DefaultHandler {
    private Locator locator;
    private CrawlerSettings settings;
    private Map handlers = new HashMap();
    private Stack handlerStack = new Stack();
    private StringBuffer buffer = new StringBuffer();

/*
    private int state;
    private Stack stateStack = new Stack();
    private Stack complexTypeStack = new Stack();
    private String currentElementName;
    private Attributes currentAttributes;
    private ComplexType currentComplexType;
    //private String currentSetting;

    private static final int STATE_ROOT = 0;
    private static final int STATE_DOCUMENT = 1;
    private static final int STATE_META = 2;
    private static final int STATE_COMPLEX = 3;
    private static final int STATE_SIMPLE = 4;
    private static final int STATE_LIST = 5;
*/

    /**
     * 
     */
    public CrawlSettingsSAXHandler(CrawlerSettings settings) {
        super();
        this.settings = settings;
        handlers.put(XMLSettingsHandler.XML_ROOT_ORDER, new RootHandler());
        handlers.put(XMLSettingsHandler.XML_ROOT_HOST_SETTINGS, new RootHandler());
        handlers.put(XMLSettingsHandler.XML_ELEMENT_CONTROLLER, new ModuleHandler());
        handlers.put(XMLSettingsHandler.XML_ELEMENT_META, new MetaHandler());
        handlers.put(XMLSettingsHandler.XML_ELEMENT_NAME, new NameHandler());
        handlers.put(XMLSettingsHandler.XML_ELEMENT_DESCRIPTION, new DescriptionHandler());
        handlers.put(XMLSettingsHandler.XML_ELEMENT_DATE, new DateHandler());
    }

    /* (non-Javadoc)
     * @see org.xml.sax.ContentHandler#setDocumentLocator(org.xml.sax.Locator)
     */
    public void setDocumentLocator(Locator locator) {
        super.setDocumentLocator(locator);
        this.locator = locator;
    }

    /* (non-Javadoc)
     * @see org.xml.sax.ContentHandler#startDocument()
     */
    public void startDocument() throws SAXException {
        super.startDocument();
        //state = STATE_ROOT;
    }

    /* (non-Javadoc)
     * @see org.xml.sax.ContentHandler#characters(char[], int, int)
     */
    public void characters(char[] ch, int start, int length)
        throws SAXException {
        super.characters(ch, start, length);
        buffer.append(ch, start, length);
    }

    /**
    * Start of an element. Decide what handler to use, and call it.
    */
    public void startElement(
        String uri,
        String localName,
        String qName,
        Attributes attributes)
        throws SAXException {
        ElementHandler handler = ((ElementHandler) handlers.get(qName));
        if (handler != null) {
            handler.newInstance();
            handlerStack.push(handler);
            handler.startElement(qName, attributes);
        } else {
            throw new SAXParseException("Unknown element '" + qName + "'", locator);
        }
    }

    /**
    * End of an element.
    */
    public void endElement(String uri, String localName, String qName)
        throws SAXException {
        ElementHandler handler = (ElementHandler) handlerStack.pop();
        if (handler != null) {
            handler.endElement(qName);
        }
    }

    public void illegalElementError(String name) throws SAXParseException {
        throw new SAXParseException(
            "Element '" + name + "' not allowed here",
            locator);
    }

    /* (non-Javadoc)
    * @see org.xml.sax.ContentHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
    *
    public void endElement(String uri, String localName, String qName)
        throws SAXException {
        super.endElement(uri, localName, qName);
    
        switch (state) {
            case STATE_SIMPLE :
                String value = buffer.toString().trim();
                buffer.setLength(0);
                prevState();
                if (state == STATE_META) {
                    if (qName.equals(XMLSettingsHandler.XML_ELEMENT_NAME)) {
                        settings.setName(value);
                    } else if (
                        qName.equals(
                            XMLSettingsHandler.XML_ELEMENT_DESCRIPTION)) {
                        settings.setDescription(value);
                    } else if (
                        qName.equals(XMLSettingsHandler.XML_ELEMENT_DATE)) {
                    }
                } else if (state == STATE_LIST) {
                    ((ListType) complexTypeStack.peek()).add(value);
                } else {
                    try {
                        String name =
                            currentAttributes.getValue(
                                XMLSettingsHandler.XML_ATTRIBUTE_NAME);
                        Object type =
                            (
                                (ComplexType) complexTypeStack
                                    .peek())
                                    .getAttribute(
                                settings,
                                name);
                        if (type != null) {
                            (
                                (ComplexType) complexTypeStack
                                    .peek())
                                    .setAttribute(
                                settings,
                                new Attribute(name, value));
                            System.out.println(
                                "XXX handle simple: "
                                    + qName
                                    + " - "
                                    + AbstractSettingsHandler.getClassName(
                                        qName));
                        } else {
                            System.out.println("XXX arrrrgh");
                        }
                    } catch (Exception e) {
                        throw new SAXException(e);
                    }
                }
                break;
    
            case STATE_META :
                if (qName.equals(XMLSettingsHandler.XML_ELEMENT_META)) {
                    prevState();
                }
                break;
        }
    }
    
    /* (non-Javadoc)
     * @see org.xml.sax.ContentHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
     *
    public void startElement(
        String uri,
        String localName,
        String qName,
        Attributes attributes)
        throws SAXException {
        super.startElement(uri, localName, qName, attributes);
        boolean handled = false;
    
        System.out.println("XXX START: " + qName);
    
        switch (state) {
            case STATE_ROOT :
    
            case STATE_DOCUMENT :
                // only meta, controller & object allowed here
                if (qName.equals(XMLSettingsHandler.XML_ELEMENT_META)) {
                    newState(STATE_META);
                    handled = true;
                } else if (
                    qName.equals(XMLSettingsHandler.XML_ELEMENT_CONTROLLER)) {
                    complexTypeStack.push(
                        settings.getSettingsHandler().getController());
                    newState(STATE_COMPLEX);
                    handled = true;
                }
                break;
    
            case STATE_META :
                if (qName.equals(XMLSettingsHandler.XML_ELEMENT_NAME)
                    || qName.equals(XMLSettingsHandler.XML_ELEMENT_DESCRIPTION)
                    || qName.equals(XMLSettingsHandler.XML_ELEMENT_DATE)) {
                    newState(STATE_SIMPLE);
                    handled = true;
                }
                break;
    
            case STATE_COMPLEX :
                String name =
                    currentAttributes.getValue(
                        XMLSettingsHandler.XML_ATTRIBUTE_NAME);
                Object type = null;
                try {
                    currentAttributes = attributes;
                    type =
                        ((ComplexType) complexTypeStack.peek()).getAttribute(
                            settings,
                            name);
    
                    if (qName.equals(XMLSettingsHandler.XML_ELEMENT_NEW_OBJECT)
                        || qName.equals(AbstractSettingsHandler.MAP)) {
                        if (type == null)
                            throw new Exception();
                        complexTypeStack.push(type);
                        newState(STATE_COMPLEX);
                        System.out.println("XXX handle complex: " + qName);
                        handled = true;
                    } else if (type instanceof ListType) {
                        ListType t = (ListType) type;
                        Constructor c =
                            type.getClass().getConstructor(
                                new Class[] { String.class, String.class });
                        Object o =
                            c.newInstance(
                                new Object[] {
                                    t.getName(),
                                    t.getDescription()});
                        complexTypeStack.push(o);
                        newState(STATE_LIST);
                        handled = true;
                    } else {
                        if (type != null) {
                            newState(STATE_SIMPLE);
                            handled = true;
                        }
                    }
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    System.out.println("XXX name: " + name);
                    System.out.println(
                        "XXX parent: " + complexTypeStack.peek());
                    System.out.println("XXX type: " + type);
                }
                break;
        }
    
        if (!handled) {
            throw new SAXParseException(
                "Illegal element '" + qName + "'",
                locator);
        }
        currentElementName = qName;
        currentAttributes = attributes;
        //elements.push(qName);
    }

    private void newState(int state) {
        stateStack.push(new Integer(this.state));
        this.state = state;
    }

    private int prevState() {
        this.state = ((Integer) stateStack.pop()).intValue();
        return this.state;
    }
    */

    private class ElementHandler implements Cloneable {
        String value;
        
        /**
         * Start of an element
         */
        public void startElement(String name, Attributes atts)
            throws SAXException {
        }

        /**
        * End of an element
        */
        public void endElement(String name) throws SAXException {
            value = buffer.toString().trim();
            buffer.setLength(0);
        }
        
        public ElementHandler newInstance() {
            ElementHandler res = null;
            try {
                res = (ElementHandler) this.clone();
            } catch (CloneNotSupportedException e) {
                // Should never reach this
                e.printStackTrace();
            }
            return res;
        }
    }

    private class RootHandler extends ElementHandler {
        public void startElement(String name, Attributes atts)
            throws SAXException {
            super.startElement(name, atts);
            //  Check filetype
            if ((name.equals(XMLSettingsHandler.XML_ROOT_ORDER)
                && settings.getScope() == null)
                || (name.equals(XMLSettingsHandler.XML_ROOT_HOST_SETTINGS)
                    && settings.getScope() != null)) {
            } else {
                throw new SAXParseException(
                    "Wrong document type '" + name + "'",
                    locator);
            }
        }

        public void endElement(String name) throws SAXException {
            super.endElement(name);
        }
    }

    private class ModuleHandler extends ElementHandler {
        public void startElement(String name, Attributes atts)
            throws SAXException {
            super.startElement(name, atts);
        }

        public void endElement(String name) throws SAXException {
            super.endElement(name);
        }
    }

    private class MetaHandler extends ElementHandler {
        public void startElement(String name, Attributes atts)
            throws SAXException {
            super.startElement(name, atts);
        }

        public void endElement(String name) throws SAXException {
            super.endElement(name);
        }
    }

    private class NameHandler extends ElementHandler {
        public void startElement(String name, Attributes atts)
            throws SAXException {
            super.startElement(name, atts);
        }

        public void endElement(String name) throws SAXException {
            super.endElement(name);
            if (handlerStack.peek() instanceof MetaHandler) {
                settings.setName(value);
            } else {
                illegalElementError(name);
            }
        }
    }

    private class DescriptionHandler extends ElementHandler {
        public void startElement(String name, Attributes atts)
            throws SAXException {
            super.startElement(name, atts);
        }

        public void endElement(String name) throws SAXException {
            super.endElement(name);
        }
    }

    private class DateHandler extends ElementHandler {
        public void startElement(String name, Attributes atts)
            throws SAXException {
            super.startElement(name, atts);
        }

        public void endElement(String name) throws SAXException {
            super.endElement(name);
        }
    }

    /*
    private class Handler extends ElementHandler {
        public void startElement(String name, Attributes atts)
            throws SAXException {
            super.startElement(name, atts);
        }

        public void endElement(String name) throws SAXException {
            super.endElement(name);
        }
    }
    */
}
