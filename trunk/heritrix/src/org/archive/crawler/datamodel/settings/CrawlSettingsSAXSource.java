/*
 * CrawlConfigurationToSax.java
 * Created on Dec 5, 2003
 *
 * $Header$
 */
package org.archive.crawler.datamodel.settings;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import javax.management.AttributeNotFoundException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.ReflectionException;
import javax.xml.transform.sax.SAXSource;

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

/**
 * @author John Erik Halse
 *
 */
public class CrawlSettingsSAXSource extends SAXSource implements XMLReader {
	private CrawlerSettings settings;
	private ContentHandler handler;
	private boolean orderFile = false;

	/**
	 * 
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
	private static final Attributes nullAtts = new AttributesImpl();
	private static final String schema = "heritrix_settings.xsd";
	private static final String orderElement = "crawl-order";
	private static final String hostSettingsElement = "crawl-settings";
	private static final String settingElement = "setting";
	private static final String objectElement = "object";
	private static final String newObjectElement = "newObject";
	private static final String nameAttribute = "name";
	private static final String typeAttribute = "type";
	private static final String classAttribute = "class";
	private static final String commentAttribute = "comment";

	private static final char[] indentArray =
		"\n                          ".toCharArray();
	// for readability!
	private static final int indentAmount = 2;

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
			schema);
		String rootElement;
		if (orderFile) {
			rootElement = orderElement;
		} else {
			rootElement = settingElement;
		}
		handler.startElement(nsu, rootElement, rootElement, atts);
		handler.ignorableWhitespace(indentArray, 0, 1);
		handler.startElement(nsu, "meta", "meta", nullAtts);
		handler.ignorableWhitespace(indentArray, 0, 1+indentAmount);
		handler.startElement(nsu, "name", "name", nullAtts);
		handler.characters(settings.getName().toCharArray(), 0, settings.getName().length());
		handler.endElement(nsu, "name", "name");
		handler.ignorableWhitespace(indentArray, 0, 1+indentAmount);
		handler.startElement(nsu, "description", "description", nullAtts);
		handler.characters(settings.getDescription().toCharArray(), 0, settings.getDescription().length());
		handler.endElement(nsu, "description", "description");
		handler.ignorableWhitespace(indentArray, 0, 1);
		handler.endElement(nsu, "meta", "meta");

		Iterator modules = settings.modules();
		while (modules.hasNext()) {
			ComplexType complexType = (ComplexType) modules.next();
			boolean newObject = false;
			parseComplexType(complexType.getName(), complexType, 3);
		}
		handler.ignorableWhitespace(indentArray, 0, 1);
		handler.endElement(nsu, rootElement, rootElement);
		handler.ignorableWhitespace(indentArray, 0, 1);
		handler.endDocument();
	}

	private void parseComplexType(String name, ComplexType complexType, int indent)	throws SAXException {
		DataContainer data = settings.getData(complexType.getAbsoluteName());
		MBeanInfo mbeanInfo = complexType.getMBeanInfo();
		String objectElement = CrawlSettingsSAXSource.objectElement;
		AttributesImpl atts = new AttributesImpl();
		atts.addAttribute(nsu, nameAttribute, nameAttribute, nsu, name);
		atts.addAttribute(nsu, classAttribute, classAttribute, nsu, mbeanInfo.getClassName());
		if (name.equals("controller")) {
			objectElement = "controller";
			atts = (AttributesImpl) nullAtts;
		} else if(complexType instanceof CrawlerModule && complexType.getSettingsHandler().getModuleFromRegistry(name) != null) {
			objectElement = newObjectElement;
		} else {
			objectElement = AbstractSettingsHandler.getTypeName(complexType.getClass().getName());
		}
		handler.ignorableWhitespace(indentArray, 0, indent);
		handler.startElement(nsu, objectElement, objectElement, atts);

		MBeanAttributeInfo attsInfo[] = mbeanInfo.getAttributes();
		for (int i = 0; i < attsInfo.length; i++) {
			ModuleAttributeInfo attribute = (ModuleAttributeInfo) attsInfo[i];
			if (attribute.getComplexType() != null) {
				parseComplexType(attribute.getName(), attribute.getComplexType(), indent+indentAmount);
			} else {
				String elementName =
					AbstractSettingsHandler.getTypeName(attribute.getType());
				atts.clear();
				atts.addAttribute(
					nsu,
					nameAttribute,
					nameAttribute,
					nsu,
					attribute.getName());
				handler.ignorableWhitespace(
					indentArray,
					0,
					indent + indentAmount);
				handler.startElement(nsu, elementName, elementName, atts);
				if(data != null) { // TODO: DEBUG
					Object d = data.get(attribute.getName());
					if(d==null) {
						try {
							d = complexType.getAttribute(attribute.getName());
						} catch (AttributeNotFoundException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (MBeanException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (ReflectionException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					if(d instanceof List) {
						d = "LISTE: " + d;
					}
					char value[] = d.toString().toCharArray();
					handler.characters(value, 0, value.length);
				}
				handler.endElement(nsu, elementName, elementName);
			}
		}

		handler.ignorableWhitespace(indentArray, 0, indent);
		handler.endElement(nsu, objectElement, objectElement);

		/*			
					Iterator it = settings.fieldNames(module);
					List fields = new ArrayList();
					while(it.hasNext()) {
						String name = (String) it.next();
						Object value = settings.getLocal(name);
						if(value != null) {
							fields.add(new String[] {name, value.toString()});
						}
					}
					
					if (fields.size() > 0) {
						atts.clear();
						handler.ignorableWhitespace(indent1, 0, indent1.length);
						atts.addAttribute(
							nsu,
							classAttribute,
							classAttribute,
							nsu,
							module);
						handler.startElement(nsu, moduleElement, moduleElement, atts);
		
						for (int i = 0; i < fields.size(); i++) {
							String name = ((String[]) fields.get(i))[0];
							String value = ((String[]) fields.get(i))[1];
							atts.clear();
							handler.ignorableWhitespace(indent2, 0, indent2.length);
							String field = name.substring(name.indexOf('#') + 1);
							atts.addAttribute(
								nsu,
								nameAttribute,
								nameAttribute,
								nsu,
								field);
							atts.addAttribute(
								nsu,
								typeAttribute,
								typeAttribute,
								nsu,
								settings.getFieldTypeString(name));
							handler.startElement(
								nsu,
								settingElement,
								settingElement,
								atts);
							handler.characters(value.toCharArray(), 0, value.length());
							handler.endElement(nsu, settingElement, settingElement);
						}
		
						handler.ignorableWhitespace(indent1, 0, indent1.length);
						handler.endElement(nsu, moduleElement, moduleElement);
					}
				}
		*/
	}

	/* (non-Javadoc)
	 * @see org.xml.sax.XMLReader#parse(java.lang.String)
	 */
	public void parse(String systemId) throws IOException, SAXException {
		System.out.println("1");
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
