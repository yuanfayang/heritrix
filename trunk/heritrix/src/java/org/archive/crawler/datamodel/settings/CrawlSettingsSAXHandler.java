/* CrawlSettingsSAXHandler
 * 
 * $Id$
 * 
 * Created on Dec 8, 2003
 *
 * Copyright (C) 2004 Internet Archive.
 *
 * This file is part of the Heritrix web crawler (crawler.archive.org).
 *
 * Heritrix is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * any later version.
 *
 * Heritrix is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser Public License for more details.
 *
 * You should have received a copy of the GNU Lesser Public License
 * along with Heritrix; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.archive.crawler.datamodel.settings;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.logging.Logger;

import javax.management.Attribute;
import javax.management.AttributeNotFoundException;
import javax.management.InvalidAttributeValueException;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/** An SAX element handler that updates a CrawlerSettings object.
 * 
 * This is a helper class for the XMLSettingsHandler.
 * 
 * @author John Erik Halse
 */
public class CrawlSettingsSAXHandler extends DefaultHandler {
    private static Logger logger =
        Logger.getLogger(
            "org.archive.crawler.datamodel.settings.XMLSettingsHandler");

    private Locator locator;
    private CrawlerSettings settings;
    private SettingsHandler settingsHandler;
    private Map handlers = new HashMap();
    private Stack handlerStack = new Stack();
    private Stack stack = new Stack();
    private StringBuffer buffer = new StringBuffer();
    private String value;

    /** Creates a new CrawlSettingsSAXHandler.
     * 
     * @param settings the settings object that should be updated from
     *        this handler.
     */
    public CrawlSettingsSAXHandler(CrawlerSettings settings) {
        super();
        this.settings = settings;
        this.settingsHandler = settings.getSettingsHandler();
        handlers.put(XMLSettingsHandler.XML_ROOT_ORDER, new RootHandler());
        handlers.put(
            XMLSettingsHandler.XML_ROOT_HOST_SETTINGS,
            new RootHandler());
        handlers.put(
            XMLSettingsHandler.XML_ELEMENT_CONTROLLER,
            new ModuleHandler());
        handlers.put(
            XMLSettingsHandler.XML_ELEMENT_OBJECT,
            new ModuleHandler());
        handlers.put(
            XMLSettingsHandler.XML_ELEMENT_NEW_OBJECT,
            new NewModuleHandler());
        handlers.put(XMLSettingsHandler.XML_ELEMENT_META, new MetaHandler());
        handlers.put(XMLSettingsHandler.XML_ELEMENT_NAME, new NameHandler());
        handlers.put(
            XMLSettingsHandler.XML_ELEMENT_DESCRIPTION,
            new DescriptionHandler());
        handlers.put(XMLSettingsHandler.XML_ELEMENT_DATE, new DateHandler());
        handlers.put(SettingsHandler.MAP, new MapHandler());
        handlers.put(SettingsHandler.INTEGER_LIST, new ListHandler());
        handlers.put(SettingsHandler.STRING_LIST, new ListHandler());
        handlers.put(SettingsHandler.DOUBLE_LIST, new ListHandler());
        handlers.put(SettingsHandler.FLOAT_LIST, new ListHandler());
        handlers.put(SettingsHandler.LONG_LIST, new ListHandler());
        handlers.put(SettingsHandler.STRING, new SimpleElementHandler());
        handlers.put(SettingsHandler.INTEGER, new SimpleElementHandler());
        handlers.put(SettingsHandler.FLOAT, new SimpleElementHandler());
        handlers.put(SettingsHandler.LONG, new SimpleElementHandler());
        handlers.put(SettingsHandler.BOOLEAN, new SimpleElementHandler());
        handlers.put(SettingsHandler.DOUBLE, new SimpleElementHandler());
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
    * @param uri
     * @param localName
     * @param qName
     * @param attributes
     * @throws SAXException
     */
    public void startElement(
        String uri,
        String localName,
        String qName,
        Attributes attributes)
        throws SAXException {
        ElementHandler handler = ((ElementHandler) handlers.get(qName));
        if (handler != null) {
            handlerStack.push(handler);
            handler.startElement(qName, attributes);
        } else {
            logger.warning("Unknown element '" + qName
               + "' in '" + locator.getSystemId()
               + "', line: " + locator.getLineNumber()
               + ", column: " + locator.getColumnNumber());
        }
    }

    /**
    * End of an element.
    * @param uri
     * @param localName
     * @param qName
     * @throws SAXException
     */
    public void endElement(String uri, String localName, String qName)
        throws SAXException {
        value = buffer.toString().trim();
        buffer.setLength(0);
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

    /** Superclass of all the elementhandlers.
     * 
     * This class should be subclassed for the different XML-elements.
     * 
     * @author John Erik Halse
     */
    private class ElementHandler {
        /**
         * Start of an element
         * @param name
         * @param atts
         * @throws SAXException
         */
        public void startElement(String name, Attributes atts)
            throws SAXException {
        }

        /**
        * End of an element
        * @param name
         * @throws SAXException
         */
        public void endElement(String name) throws SAXException {
        }
    }

    /** Handle the root element.
     * 
     * This class checks that the root element is of the right type.
     * 
     * @author John Erik Halse
     */
    private class RootHandler extends ElementHandler {
        public void startElement(String name, Attributes atts)
            throws SAXException {
            //  Check filetype
            if ((name.equals(XMLSettingsHandler.XML_ROOT_ORDER)
                && settings.getScope() != null)
                || (name.equals(XMLSettingsHandler.XML_ROOT_HOST_SETTINGS)
                    && settings.getScope() == null)) {
                throw new SAXParseException(
                    "Wrong document type '" + name + "'",
                    locator);
            }
        }
    }

    private class ModuleHandler extends ElementHandler {
        public void startElement(String name, Attributes atts)
            throws SAXException {
            CrawlerModule module;
            if (name.equals(XMLSettingsHandler.XML_ELEMENT_CONTROLLER)) {
                module = settingsHandler.getOrder();
            } else {
                module = settingsHandler.getSettingsObject(null)
                .getModule(atts.getValue(XMLSettingsHandler.XML_ATTRIBUTE_NAME));
            }
            stack.push(module);
        }

        public void endElement(String name) throws SAXException {
            stack.pop();
        }
    }

    private class NewModuleHandler extends ElementHandler {
        public void startElement(String name, Attributes atts)
            throws SAXException {
            ComplexType parentModule = (ComplexType) stack.peek();
            String moduleName =
                atts.getValue(XMLSettingsHandler.XML_ATTRIBUTE_NAME);
            String moduleClass =
                atts.getValue(XMLSettingsHandler.XML_ATTRIBUTE_CLASS);
            try {
                CrawlerModule module = SettingsHandler.instantiateCrawlerModuleFromClassName(moduleName, moduleClass);
                try {
                    parentModule.setAttribute(settings, module);
                } catch (AttributeNotFoundException e) {
                    parentModule.addElement(settings, module);
                }
                stack.push(module);
            } catch (InvocationTargetException e) {
                throw new SAXException(e);
            } catch (InvalidAttributeValueException e) {
                throw new SAXException(e);
            }
        }

        public void endElement(String name) throws SAXException {
            stack.pop();
        }
    }

    private class MapHandler extends ElementHandler {
        public void startElement(String name, Attributes atts)
            throws SAXException {
            String mapName =
                atts.getValue(XMLSettingsHandler.XML_ATTRIBUTE_NAME);
            ComplexType parentModule = (ComplexType) stack.peek();
            try {
                stack.push(parentModule.getAttribute(settings, mapName));
            } catch (AttributeNotFoundException e) {
                throw new SAXException(e);
            }
        }

        public void endElement(String name) throws SAXException {
            stack.pop();
        }
    }

    private class MetaHandler extends ElementHandler {
    }

    private class NameHandler extends ElementHandler {
        public void endElement(String name) throws SAXException {
            if (handlerStack.peek() instanceof MetaHandler) {
                settings.setName(value);
            } else {
                illegalElementError(name);
            }
        }
    }

    private class DescriptionHandler extends ElementHandler {
        public void endElement(String name) throws SAXException {
            if (handlerStack.peek() instanceof MetaHandler) {
                settings.setDescription(value);
            } else {
                illegalElementError(name);
            }
        }
    }

    private class DateHandler extends ElementHandler {
    }

    private class SimpleElementHandler extends ElementHandler {
        public void startElement(String name, Attributes atts)
            throws SAXException {
            stack.push(atts.getValue(XMLSettingsHandler.XML_ATTRIBUTE_NAME));
        }

        public void endElement(String name) throws SAXException {
            String elementName = (String) stack.pop();
            Object container = stack.peek();
            if (container instanceof ComplexType) {
                try {
                    ((ComplexType) container).setAttribute(
                        settings,
                        new Attribute(elementName, value));
                } catch (AttributeNotFoundException e) {
                    logger.warning("Unknown attribute '" + elementName
                       + "' in '" + locator.getSystemId()
                       + "', line: " + locator.getLineNumber()
                       + ", column: " + locator.getColumnNumber());
                } catch (InvalidAttributeValueException e) {
                    logger.warning("Illegal value for attribute '" + elementName
                       + "' in '" + locator.getSystemId()
                       + "', line: " + locator.getLineNumber()
                       + ", column: " + locator.getColumnNumber());
                }
            } else {
                ((ListType) container).add(value);
            }
        }
    }

    private class ListHandler extends ElementHandler {
        public void startElement(String name, Attributes atts)
            throws SAXException {
            String listName =
                atts.getValue(XMLSettingsHandler.XML_ATTRIBUTE_NAME);
            ComplexType parentModule = (ComplexType) stack.peek();
            ListType list;
            try {
                list = (ListType) parentModule.getAttribute(settings, listName);
            } catch (AttributeNotFoundException e) {
                throw new SAXException(e);
            }
            list.clear();
            stack.push(list);
        }

        public void endElement(String name) throws SAXException {
            stack.pop();
        }
    }
}
