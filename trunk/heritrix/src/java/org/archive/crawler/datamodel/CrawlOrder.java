/* 
 * CrawlOrder
 *
 * $Header$ 
 * 
 * Created on May 15, 2003
 *
 * Copyright (C) 2003 Internet Archive.
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
 *
 */

package org.archive.crawler.datamodel;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import org.archive.crawler.framework.XMLConfig;
import org.archive.crawler.framework.exceptions.*;
import org.archive.util.ArchiveUtils;
import org.w3c.dom.Document;

/** Read and manipulate configuration (order) file.
 */
public class CrawlOrder extends XMLConfig {
	private static final String XP_CRAWL_ORDER_NAME = "//crawl-order/@name";
	private static final String XP_HTTP_USER_AGENT =
		"//http-headers/@User-Agent";
	private static final String XP_HTTP_FROM = "//http-headers/@From";
	private static final String XP_MAX_TOE_THREADS =
		"//behavior/@max-toe-threads";
	private static final String XP_ROBOTS_HONORING_POLICY_NAME =
		"//behavior/robots-honoring-policy/@name";
	private static final String XP_ROBOTS_HONORING_POLICY_MASQUERADE =
		"//behavior/robots-honoring-policy/@masquerade";
	private static final String XP_ROBOTS_HONORING_POLICY_CUSTOM_ROBOTS =
		"//behavior/robots-honoring-policy/custom-robots";
	private static final String XP_ROBOTS_HONORING_POLICY_USER_AGENTS =
		"//behavior/robots-honoring-policy/user-agents/agent";
	private static final String XP_DISK_PATH = "//behavior/@disk-path";

	String caseFlattenedUserAgent;
	String name;
	String outputLocation;
	String crawlOrderFilename;
	RobotsHonoringPolicy robotsHonoringPolicy = null;

	/**
	 * Create a CrawlOrder instance from the given file.
	 * 
	 * @param crawlOrderFile
	 * @return the created CrawlOrder
	 * @throws InitializationException
	 */
	public static CrawlOrder readFromFile(String crawlOrderFile)
		throws InitializationException {

		Document doc = null;
		try {
			doc = readDocumentFromFile(crawlOrderFile);
		} catch (IOException e) {
			throw new InitializationException(
				"Can't read from '" + crawlOrderFile + "': " + e.toString(),
				e);
		}

		CrawlOrder co = new CrawlOrder(doc, crawlOrderFile);
		return co;
	}

	/** 
	 * Construct a CrawlOrder instance given a Document.
	 * 
	 * @param doc
	 * @throws InitializationException
	 */
	public CrawlOrder(Document doc) throws InitializationException {
		xNode = doc;
		initialize();
	}
	
	/**
	 * Construct a CrawlOrder instance given a Document and a order file name.
	 * 
	 * @param doc
	 * @param crawlOrderFile
	 * @throws InitializationException
	 */
	public CrawlOrder(Document doc, String crawlOrderFile)
		throws InitializationException {
			
		xNode = doc;
		setCrawlOrderFilename(crawlOrderFile);
		initialize();
	}

	//	/** 
	//	 * Construct a CrawlOrder instance given a Document and a path to that
	//	 * document.  The path is necessary in the event that the config file 
	//	 * inherits from another config file (so we can locate that on the file
	//	 * system).
	//	 * 
	//	 * @param doc
	//	 * @param pathToDoc
	//	 * @throws InitializationException
	//	 */
	//	public CrawlOrder(Document doc, String pathToDoc)
	//		throws InitializationException {
	//		xNode = doc;
	//
	//		if (pathToDoc != null) {
	//			defaultFilePath = pathToDoc;
	//		} else {
	//			defaultFilePath = ".";
	//		}
	//
	//		//loadParents(pathToDoc);
	//		initialize();
	//
	//	}

	/** 
	 * Load common configuration variables from config file and set 'disk' 
	 * path. Should only be called on the primary (non-inherited from) crawl
	 * order.
	 */
	public void initialize() {
		name = getStringAt(XP_CRAWL_ORDER_NAME);
	}

	protected void loadParents(String pathToDoc)
		throws InitializationException {

		// try to read any configuration files which this extends
		// if this fails let the Initialization exception bubble up
		String parentFileName = getStringAt("//crawl-order/@extends");
		//TODO check for infinite loop (parent == self)
		if (parentFileName != null && parentFileName.length() != 0) {
			try {
				if (isAbsoluteFilePath(parentFileName)) {
					parentConfigurationFile = readFromFile(parentFileName);
				} else {
					parentConfigurationFile =
						readFromFile(
							pathToDoc + File.separator + parentFileName);
				}
				// can't read parent, proceed as best we can.
			} catch (InitializationException e) {
				System.out.println(
					"Unable to read config file '"
						+ pathToDoc
						+ File.separator
						+ parentFileName
						+ "' (non fatal), continuing...");
			}
		}
	}

	public String getName() {
		// if this node doesn't have it but we have a parent conf file check that
		if (name == null && parentConfigurationFile != null) {
			return ((CrawlOrder) parentConfigurationFile).getName();
		}
		return name;
	}

	public String getOutputLocation() {
		if (outputLocation == null && parentConfigurationFile != null) {
			return ((CrawlOrder) parentConfigurationFile).getOutputLocation();
		}
		return outputLocation;
	}

	public String getUserAgent() {
		if (caseFlattenedUserAgent == null) {
			caseFlattenedUserAgent =
				getStringAt(XP_HTTP_USER_AGENT).toLowerCase();
		}
		return caseFlattenedUserAgent;
	}

	public String getFrom() {
		return getStringAt(XP_HTTP_FROM);
	}

	public int getMaxToes() {
		return getIntAt(XP_MAX_TOE_THREADS);
	}

	public void setCrawlOrderFilename(String aOrderFilename) {
		crawlOrderFilename = aOrderFilename;
	}

	public String getCrawlOrderFilename() {
		return crawlOrderFilename;
	}

	/**
	 * This method constructs a new RobotsHonoringPolicy object from the orders 
	 * file.
	 * 
	 * If this method is called repeatedly it will return the same instance 
	 * each time.
	 * 
	 * @return the new RobotsHonoringPolicy
	 */
	public RobotsHonoringPolicy getRobotsHonoringPolicy() {
		if (robotsHonoringPolicy == null) {
			robotsHonoringPolicy =
				new RobotsHonoringPolicy(
					getStringAt(XP_ROBOTS_HONORING_POLICY_NAME));
			robotsHonoringPolicy.setMasquerade(
				getStringAt(XP_ROBOTS_HONORING_POLICY_MASQUERADE));

			// if the policy type is custom, we should look up the admins 
			// robots.txt file
			if (robotsHonoringPolicy.isType(RobotsHonoringPolicy.CUSTOM)) {
				robotsHonoringPolicy.setCustomRobots(
					getStringAt(XP_ROBOTS_HONORING_POLICY_CUSTOM_ROBOTS));
			}
			if (robotsHonoringPolicy
				.isType(RobotsHonoringPolicy.MOST_FAVORED_SET)) {
				Iterator iter =
					getTextNodesAt(
						xNode,
						XP_ROBOTS_HONORING_POLICY_USER_AGENTS)
						.iterator();
				while (iter.hasNext()) {
					robotsHonoringPolicy.addUserAgent((String) iter.next());
				}
			}
		}
		return robotsHonoringPolicy;
	}

	/**
	 * Returns path to a file relative to a order file.
	 * 
	 * If a file does not have absolute path, this method creates a new path 
	 * relative to the path of the order file. Otherwise returns absolute path 
	 * to the file.
	 * If aFileName is a directory and does not end with trailing '/' it is 
	 * treated as a file.
	 * 
	 * @param aFileName
	 * @return String path relative to the order file
	 */
	public String getPathRelativeToOrderFile(String aFileName) {
		String path;
		if (aFileName.endsWith("/")){
			path = aFileName; 
		}else{
			path = ArchiveUtils.getFilePath(aFileName);
		}
		if (!ArchiveUtils.isFilePathAbsolute(aFileName)) {
			return ArchiveUtils.getFilePath(getCrawlOrderFilename()) + path;
		} else {
			return path;
		}
	}

}
