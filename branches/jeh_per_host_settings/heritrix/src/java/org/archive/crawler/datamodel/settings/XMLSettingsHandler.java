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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;

import javax.management.AttributeNotFoundException;
import javax.management.MBeanException;
import javax.management.ReflectionException;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;

/**
 * 
 * @author John Erik Halse
 *
 */
public class XMLSettingsHandler extends AbstractSettingsHandler {
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
		CrawlerSettings parent = settings.getParent();
		String scope = settings.getScope();
		if (parent == null) {
			// Read order file
		
		} else {
			// Read per host file
		}
		return settings;
		/*
				File filename = new File(scopeToFile(scope), configurationFilename);
				System.out.print("READING: " + filename.getAbsolutePath());
				if(filename.exists()) {
					CrawlConfiguration config = new CrawlConfiguration(parent, scope);
					try {
						XMLReader parser = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
						InputStream file = new BufferedInputStream(new FileInputStream(filename));
						parser.setContentHandler(new CrawlConfigurationSAXHandler(config));				
						parser.parse(new InputSource(file));
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					System.out.println("  OK");
					return config;
				} else {
					System.out.println("  NOT FOUND");
					return null;
				}
		*/
	}
}
