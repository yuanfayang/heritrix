/* XMLSettingsHandler
 * 
 * $Id$
 * 
 * Created on Dec 18, 2003
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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.management.AttributeNotFoundException;
import javax.management.MBeanException;
import javax.management.ReflectionException;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;

/**
 * 
 * @author John Erik Halse
 *
 */
public class XMLSettingsHandler extends AbstractSettingsHandler {
    // XML element name constants
    protected static final String XML_SCHEMA = "heritrix_settings.xsd";
    protected static final String XML_ROOT_ORDER = "crawl-order";
    protected static final String XML_ROOT_HOST_SETTINGS = "crawl-settings";
    protected static final String XML_ELEMENT_CONTROLLER = "controller";
    protected static final String XML_ELEMENT_META = "meta";
    protected static final String XML_ELEMENT_NAME = "name";
    protected static final String XML_ELEMENT_DESCRIPTION = "description";
    protected static final String XML_ELEMENT_DATE = "date";
    protected static final String XML_ELEMENT_OBJECT = "object";
    protected static final String XML_ELEMENT_NEW_OBJECT = "newObject";
    protected static final String XML_ATTRIBUTE_NAME = "name";
    protected static final String XML_ATTRIBUTE_CLASS = "class";

	private File orderFile;
	private File settingsDirectory;
	private final static String settingsFilename = "settings.xml";

	/**
	 * 
	 */
	public XMLSettingsHandler(File orderFile) {
        this.orderFile = orderFile;
	}

	/**
	 * @param controller
	 */
	public void initialize() {
		super.initialize();
		try {
			this.settingsDirectory =
				new File(
					(String) getController().getAttribute("settings-directory"));
		} catch (AttributeNotFoundException e) {
			e.printStackTrace();
		} catch (MBeanException e) {
			e.printStackTrace();
		} catch (ReflectionException e) {
			e.printStackTrace();
		}
	}

	private File scopeToFile(String scope) {
		File file;
		if (scope == null || scope.equals("")) {
			file = settingsDirectory;
		} else {
			String elements[] = scope.split("\\.");
			StringBuffer path = new StringBuffer();
			for (int i = elements.length - 1; i > 0; i--) {
				path.append(elements[i]);
				path.append(File.separatorChar);
			}
			path.append(elements[0]);
			file = new File(settingsDirectory, path.toString());
		}
		return file;
	}

	/* (non-Javadoc)
	 * @see org.archive.crawler.datamodel.settings.AbstractSettingsHandler#writeSettingsObject(org.archive.crawler.datamodel.settings.CrawlerSettings)
	 */
	public final void writeSettingsObject(CrawlerSettings settings) {
        File filename;
        if (settings.getScope() == null) {
            filename = orderFile;
        } else {
            File dirname = scopeToFile(settings.getScope());
            filename = new File(dirname, settingsFilename);
            dirname.mkdirs();
        }
		writeSettingsObject(settings, filename);
	}

	public final void writeSettingsObject(CrawlerSettings settings, File filename) {
		try {
			StreamResult result =
				new StreamResult(
					new BufferedOutputStream(new FileOutputStream(filename)));
			Transformer transformer =
				TransformerFactory.newInstance().newTransformer();
			Source source = new CrawlSettingsSAXSource(settings);
			transformer.transform(source, result);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/* (non-Javadoc)
	 * @see org.archive.crawler.datamodel.settings.AbstractSettingsHandler#readSettingsObject(org.archive.crawler.datamodel.settings.CrawlerSettings, java.lang.String)
	 */
	protected final CrawlerSettings readSettingsObject(CrawlerSettings settings) {
        File filename;
        if (settings.getScope() == null) {
            // Read order file
            filename = orderFile;
        } else {
            // Read per host file
            File dirname = scopeToFile(settings.getScope());
            filename = new File(dirname, settingsFilename);
        }
        System.out.print("XXX  TRY TO READ: " + filename.getAbsolutePath());
        if(filename.exists()) {
            System.out.println("  XXX  FOUND");
            try {
                XMLReader parser = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
                InputStream file = new BufferedInputStream(new FileInputStream(filename));
                parser.setContentHandler(new CrawlSettingsSAXHandler(settings));             
                parser.parse(new InputSource(file));
            } catch (SAXParseException e) {
                System.out.println(e.getMessage() + " in file '" + filename.getAbsolutePath() + "', line: " + e.getLineNumber() + ", column: " + e.getColumnNumber());
                //e.printStackTrace();
            } catch (SAXException e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
            } catch (FactoryConfigurationError e) {
                e.printStackTrace();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("  XXX  NOT FOUND");
            settings = null;
        }
		return settings;
	}
}
