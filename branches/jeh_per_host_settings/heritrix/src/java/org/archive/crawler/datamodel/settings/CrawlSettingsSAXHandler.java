/*
 * CrawlSettingsSAXHandler.java
 * Created on Dec 8, 2003
 *
 * $Header$
 */
package org.archive.crawler.datamodel.settings;

import java.util.Stack;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * @author johnh
 *
 */
public class CrawlSettingsSAXHandler extends DefaultHandler {
	private CrawlerSettings settings;
	private Stack elements = new Stack();
	private String currentElement;
	private String currentModule;
	private String currentSetting;
	private StringBuffer buffer = new StringBuffer();

	/**
	 * 
	 */
	public CrawlSettingsSAXHandler(CrawlerSettings settings) {
		super();
		this.settings = settings;
	}

	/* (non-Javadoc)
	 * @see org.xml.sax.ContentHandler#characters(char[], int, int)
	 */
	public void characters(char[] ch, int start, int length)
		throws SAXException {
		super.characters(ch, start, length);
		buffer.append(ch, start, length);
	}

	/* (non-Javadoc)
	 * @see org.xml.sax.ContentHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
	 */
	public void endElement(String uri, String localName, String qName)
		throws SAXException {
		// TODO Auto-generated method stub
		super.endElement(uri, localName, qName);
		if (qName.equals("setting")) {
			String field = currentModule + '#' + currentSetting;
			//settings.put(field, buffer.toString().trim());
			currentSetting = null;
		} else if(qName.equals("module")) {
			currentModule = null;
		} else if(qName.equals("crawl-configuration")) {
		}
		currentElement = (String) elements.pop();
		buffer.setLength(0);
	}

	/* (non-Javadoc)
	 * @see org.xml.sax.ContentHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
	 */
	public void startElement(
		String uri,
		String localName,
		String qName,
		Attributes attributes)
		throws SAXException {
		// TODO Auto-generated method stub
		super.startElement(uri, localName, qName, attributes);

		if (qName.equals("setting")) {
			currentSetting = attributes.getValue("name");
		} else if(qName.equals("module")) {
			currentModule = attributes.getValue("class");
		} else if(qName.equals("crawl-configuration")) {
			//settings.setComment(attributes.getValue("comment"));
		}

		currentElement = qName;
		elements.push(qName);
	}
}
