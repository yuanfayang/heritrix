/*
 * CrawlHost.java
 * Created on Apr 17, 2003
 *
 * $Header$
 */
package org.archive.crawler.framework;

import java.net.InetAddress;

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
