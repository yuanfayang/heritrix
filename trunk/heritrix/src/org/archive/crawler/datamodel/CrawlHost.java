/*
 * CrawlHost.java
 * Created on Apr 17, 2003
 *
 * $Header$
 */
package org.archive.crawler.datamodel;

import java.net.InetAddress;

import org.archive.crawler.framework.RobotsExclusionPolicy;

/**
 * Represents a 
 * @author gojomo
 *
 */
public class CrawlHost {
	String hostname;
	InetAddress ip;
	long ipExpires;
	RobotsExclusionPolicy robots;
	
}
