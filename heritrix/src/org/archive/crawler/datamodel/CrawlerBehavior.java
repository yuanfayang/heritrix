/*
 * CrawlerBehavior.java
 * Created on May 21, 2003
 *
 * $Header$
 */
package org.archive.crawler.datamodel;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.archive.crawler.framework.XMLConfig;
import org.w3c.dom.Node;

/**
 * @author gojomo
 *
 */
public class CrawlerBehavior extends XMLConfig {
	List seeds = null;


	/**
	 * @param node
	 */
	public CrawlerBehavior(Node node) {
		xNode = nodeOrSrc(node);
	}

	/**
	 * @return
	 */
	public int getMaxToes() {
		return getIntAt("//limits/max-toe-threads/@value");
	}

	/**
	 * @return
	 */
	public String getUserAgent() {
		return getStringAt("//http-headers/User-Agent");
	}

	/**
	 * @return
	 */
	public String getFrom() {
		return getStringAt("//http-headers/From");
	}

	public List getSeeds() {
		if (seeds != null) {
			return seeds;
		}
		seeds = new ArrayList();
		BufferedReader reader = nodeValueOrSrcReader("//seeds");
		String read;
		try {
			while (reader != null) {
				do {read = reader.readLine();}
				while (
				  (read != null) 
				  && ( (read=read.trim()).startsWith("#") 
					   || read.length() == 0) );
			
				if (read == null) {
					reader.close();
					reader = null;
				} else {
					seeds.add( UURI.createUURI(read) );
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return seeds;
   	
	}


}
