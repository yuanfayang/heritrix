/* CrawlSettingsSAXSource
 * 
 * $Id$
 * 
 * Created on Dec 5, 2003
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

import java.io.IOException;
import java.util.Iterator;

import javax.management.AttributeNotFoundException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.xml.transform.sax.SAXSource;

import org.archive.util.ArchiveUtils;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.AttributesImpl;

/** Class that takes a CrawlerSettings object and create SAXEvents from it.
 * 
 * This is a helper class for XMLSettingsHandler.
 * 
 * @author John Erik Halse
 */
public class CrawlSettingsSAXSource extends SAXSource implements XMLReader {
    // for prettyprinting XML file
    private static final int indentAmount = 2;

    private CrawlerSettings settings;
    private ContentHandler handler;
    private boolean orderFile = false;

    /** Constructs a new CrawlSettingsSAXSource.
     * 
     * @param settings the settings object to create SAX events from.
     */
    public CrawlSettingsSAXSource(CrawlerSettings settings) {
        super();
        this.settings = settings;
        if (settings.getParent() == null) {
            orderFile = true;
        }
    }

    /* (non-Javadoc)
     * @see org.xml.sax.XMLReader#getFeature(java.lang.String)
     */
    public boolean getFeature(String name)
        throws SAXNotRecognizedException, SAXNotSupportedException {
        return false;
    }

    /* (non-Javadoc)
     * @see org.xml.sax.XMLReader#setFeature(java.lang.String, boolean)
     */
    public void setFeature(String name, boolean value)
        throws SAXNotRecognizedException, SAXNotSupportedException {

    }

    /* (non-Javadoc)
     * @see org.xml.sax.XMLReader#getProperty(java.lang.String)
     */
    public Object getProperty(String name)
        throws SAXNotRecognizedException, SAXNotSupportedException {
        return null;
    }

    /* (non-Javadoc)
     * @see org.xml.sax.XMLReader#setProperty(java.lang.String, java.lang.Object)
     */
    public void setProperty(String name, Object value)
        throws SAXNotRecognizedException, SAXNotSupportedException {

    }

    /* (non-Javadoc)
     * @see org.xml.sax.XMLReader#setEntityResolver(org.xml.sax.EntityResolver)
     */
    public void setEntityResolver(EntityResolver resolver) {

    }

    /* (non-Javadoc)
     * @see org.xml.sax.XMLReader#getEntityResolver()
     */
    public EntityResolver getEntityResolver() {
        return null;
    }

    /* (non-Javadoc)
     * @see org.xml.sax.XMLReader#setDTDHandler(org.xml.sax.DTDHandler)
     */
    public void setDTDHandler(DTDHandler handler) {
    }

    /* (non-Javadoc)
     * @see org.xml.sax.XMLReader#getDTDHandler()
     */
    public DTDHandler getDTDHandler() {
        return null;
    }

    /* (non-Javadoc)
     * @see org.xml.sax.XMLReader#setContentHandler(org.xml.sax.ContentHandler)
     */
    public void setContentHandler(ContentHandler handler) {
        this.handler = handler;
    }

    /* (non-Javadoc)
     * @see org.xml.sax.XMLReader#getContentHandler()
     */
    public ContentHandler getContentHandler() {
        return handler;
    }

    /* (non-Javadoc)
     * @see org.xml.sax.XMLReader#setErrorHandler(org.xml.sax.ErrorHandler)
     */
    public void setErrorHandler(ErrorHandler handler) {
    }

    /* (non-Javadoc)
     * @see org.xml.sax.XMLReader#getErrorHandler()
     */
    public ErrorHandler getErrorHandler() {
        return null;
    }

    // We're not doing namespaces
    private static final String nsu = ""; // NamespaceURI
    private static final char[] indentArray =
        "\n                                          ".toCharArray();

    /* (non-Javadoc)
     * @see org.xml.sax.XMLReader#parse(org.xml.sax.InputSource)
     */
    public void parse(InputSource input) throws IOException, SAXException {
        if (handler == null) {
            throw new SAXException("No content handler");
        }
        handler.startDocument();
        AttributesImpl atts = new AttributesImpl();
        atts.addAttribute(
            "http://www.w3.org/2001/XMLSchema-instance",
            "xsi",
            "xmlns:xsi",
            nsu,
            "http://www.w3.org/2001/XMLSchema-instance");
        atts.addAttribute(
            "http://www.w3.org/2001/XMLSchema-instance",
            "noNamespaceSchemaLocation",
            "xsi:noNamespaceSchemaLocation",
            nsu,
            XMLSettingsHandler.XML_SCHEMA);
        String rootElement;
        if (orderFile) {
            rootElement = XMLSettingsHandler.XML_ROOT_ORDER;
        } else {
            rootElement = XMLSettingsHandler.XML_ROOT_HOST_SETTINGS;
        }
        Attributes nullAtts = new AttributesImpl();
        handler.startElement(nsu, rootElement, rootElement, atts);

        // Write meta information
        handler.ignorableWhitespace(indentArray, 0, 1 + indentAmount);
        handler.startElement(
            nsu,
            XMLSettingsHandler.XML_ELEMENT_META,
            XMLSettingsHandler.XML_ELEMENT_META,
            nullAtts);

        // Write settings name
        writeSimpleElement(
            XMLSettingsHandler.XML_ELEMENT_NAME,
            settings.getName(),
            null,
            1 + indentAmount * 2);

        // Write settings description
        writeSimpleElement(
            XMLSettingsHandler.XML_ELEMENT_DESCRIPTION,
            settings.getDescription(),
            null,
            1 + indentAmount * 2);

        // Write file date
        String dateStamp = ArchiveUtils.get14DigitDate();
        writeSimpleElement(
            XMLSettingsHandler.XML_ELEMENT_DATE,
            dateStamp,
            null,
            1 + indentAmount * 2);

        handler.ignorableWhitespace(indentArray, 0, 1 + indentAmount);
        handler.endElement(
            nsu,
            XMLSettingsHandler.XML_ELEMENT_META,
            XMLSettingsHandler.XML_ELEMENT_META);

        Iterator modules = settings.topLevelModules();
        while (modules.hasNext()) {
            ComplexType complexType = (ComplexType) modules.next();
            parseComplexType(complexType, 1 + indentAmount);
        }
        handler.ignorableWhitespace(indentArray, 0, 1);
        handler.endElement(nsu, rootElement, rootElement);
        handler.ignorableWhitespace(indentArray, 0, 1);
        handler.endDocument();
    }

    /** Create SAX events from a {@link ComplexType}.
     * 
     * @param complexType the object to creat SAX events from.
     * @param indent the indentation amount for prettyprinting XML.
     * @throws SAXException is thrown if an error occurs.
     */
    private void parseComplexType(ComplexType complexType, int indent)
        throws SAXException {
        if (complexType.isTransient()) {
            return;
        }
        
        MBeanInfo mbeanInfo = complexType.getMBeanInfo(settings);
        String objectElement = resolveElementName(complexType);

        AttributesImpl atts = new AttributesImpl();
        atts.addAttribute(
            nsu,
            XMLSettingsHandler.XML_ATTRIBUTE_NAME,
            XMLSettingsHandler.XML_ATTRIBUTE_NAME,
            nsu,
            complexType.getName());

        if (objectElement == XMLSettingsHandler.XML_ELEMENT_NEW_OBJECT) {
            // Only 'newObject' elements have a class attribute
            atts.addAttribute(
                nsu,
                XMLSettingsHandler.XML_ATTRIBUTE_CLASS,
                XMLSettingsHandler.XML_ATTRIBUTE_CLASS,
                nsu,
                mbeanInfo.getClassName());
        }

        if (complexType.getParent() == null) {
            atts = new AttributesImpl();
        }

        handler.ignorableWhitespace(indentArray, 0, indent);
        handler.startElement(nsu, objectElement, objectElement, atts);

        MBeanAttributeInfo attsInfo[] = mbeanInfo.getAttributes();
        for (int i = 0; i < attsInfo.length; i++) {
            ModuleAttributeInfo attribute = (ModuleAttributeInfo) attsInfo[i];
            Object value;
            try {
                value = complexType.getLocalAttribute(
                    settings, attribute.getName());
            } catch (AttributeNotFoundException e) {
                throw new SAXException(e);
            }
            if (orderFile || value != null) {
                // Write only overridden values unless this is the order file
                if (attribute.isComplexType()) {
                    // Call method recursively for complex types
                    parseComplexType(
                        (ComplexType) value, //attribute.getComplexType(),
                        indent + indentAmount);
                } else {
                    // Write element
                    String elementName =
                        SettingsHandler.getTypeName(attribute.getType());
                    atts.clear();
                    atts.addAttribute(
                        nsu,
                        XMLSettingsHandler.XML_ATTRIBUTE_NAME,
                        XMLSettingsHandler.XML_ATTRIBUTE_NAME,
                        nsu,
                        attribute.getName());
                    if (value == null) {
                        try {
                            value =
                                complexType.getAttribute(attribute.getName());
                        } catch (Exception e) {
                            throw new SAXException(
                                "Internal error in settings subsystem",
                                e);
                        }
                    }
                    if (value != null) {
                        handler.ignorableWhitespace(
                            indentArray,
                            0,
                            indent + indentAmount);
                        handler.startElement(
                            nsu,
                            elementName,
                            elementName,
                            atts);
                        if (value instanceof ListType) {
                            parseListData(value, indent + indentAmount);
                            handler.ignorableWhitespace(
                                indentArray,
                                0,
                                indent + indentAmount);
                        } else {
                            char valueArray[] = value.toString().toCharArray();
                            handler.characters(
                                valueArray,
                                0,
                                valueArray.length);
                        }
                        handler.endElement(nsu, elementName, elementName);
                    }
                }
            }
        }

        handler.ignorableWhitespace(indentArray, 0, indent);
        handler.endElement(nsu, objectElement, objectElement);
    }

    /** Create SAX events for the content of a {@link ListType}.
     * 
     * @param value the ListType whose content we create SAX events for.
     * @param indent the indentation amount for prettyprinting XML.
     * @throws SAXException is thrown if an error occurs.
     */
    private void parseListData(Object value, int indent) throws SAXException {
        ListType list = (ListType) value;
        Iterator it = list.iterator();
        while (it.hasNext()) {
            Object element = it.next();
            String elementName =
                SettingsHandler.getTypeName(element.getClass().getName());
            writeSimpleElement(
                elementName,
                element.toString(),
                null,
                indent + indentAmount);
        }
    }

    /** Resolve the XML element name of a {@link ComplexType}.
     * 
     * @param complexType the object to investigate.
     * @return the name of the XML element.
     */
    private String resolveElementName(ComplexType complexType) {
        String elementName;
        if (complexType instanceof CrawlerModule) {
            if (complexType.getParent() == null) {
                // Top level controller element
                elementName = XMLSettingsHandler.XML_ELEMENT_CONTROLLER;
            } else if (
                settings.getParent() != null
                    && complexType.globalSettings().getModule(
                        complexType.getName())
                        != null) {
                // This is not the order file and we are referencing an object
                elementName = XMLSettingsHandler.XML_ELEMENT_OBJECT;
            } else {
                // The object is not referenced before
                elementName = XMLSettingsHandler.XML_ELEMENT_NEW_OBJECT;
            }
        } else {
            // It's a map
            elementName =
                SettingsHandler.getTypeName(complexType.getClass().getName());
        }
        return elementName;
    }

    /** Create SAX events for a simple element.
     * 
     * Creates all the SAX events needed for prettyprinting an XML element
     * with a simple value and possible attributes.
     * 
     * @param elementName the name of the XML element.
     * @param value the value to pu inside the XML element.
     * @param atts the attributes for the XML element.
     * @param indent the indentation amount for prettyprinting XML.
     * @throws SAXException is thrown if an error occurs.
     */
    private void writeSimpleElement(
        String elementName,
        String value,
        Attributes atts,
        int indent)
        throws SAXException {
        if (atts == null) {
            atts = new AttributesImpl();
        }
        // make sure that the value is never null
        value = value == null ? "" : value;
        handler.ignorableWhitespace(indentArray, 0, indent);
        handler.startElement(nsu, elementName, elementName, atts);
        handler.characters(value.toCharArray(), 0, value.length());
        handler.endElement(nsu, elementName, elementName);
    }

    /* (non-Javadoc)
     * @see org.xml.sax.XMLReader#parse(java.lang.String)
     */
    public void parse(String systemId) throws IOException, SAXException {
        // Do nothing. Just for conformance to the XMLReader API.
    }

    /* (non-Javadoc)
     * @see javax.xml.transform.sax.SAXSource#getXMLReader()
     */
    public XMLReader getXMLReader() {
        return this;
    }

    /* (non-Javadoc)
     * @see javax.xml.transform.sax.SAXSource#getInputSource()
     */
    public InputSource getInputSource() {
        return new InputSource();
    }
}
