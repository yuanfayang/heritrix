/*
 * CrawlerBehavior.java
 * Created on May 21, 2003
 *
 * $Header$
 */
package org.archive.crawler.datamodel;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URISyntaxException;
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
	String caseFlattenedUserAgent = null;

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
	public int getMaxLinkDepth(){
		return getIntAt("//limits/max-link-depth/@value");
	}
	/**
	 * @return
	 */
	public String getUserAgent() {
		if (caseFlattenedUserAgent==null) {
			caseFlattenedUserAgent =  getStringAt("//http-headers/User-Agent").toLowerCase();
		}
		return caseFlattenedUserAgent;
	}

	/**
	 * @return
	 */
	public String getFrom() {
		return getStringAt("//http-headers/From");
	}

	public List getSeeds() throws IOException {
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
					try {
						seeds.add( UURI.createUURI(read) );
					} catch (URISyntaxException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return seeds;
   	
	}


	/**
	 * @param list
	 */
	public void clearSeeds() {
		seeds = new ArrayList();
	}

	public void addSeed(String url){
		try {
			seeds.add( UURI.createUURI(url) );
		} catch (URISyntaxException e1) {
			e1.printStackTrace();
		}
	}
	
	public BufferedReader  nodeValueOrSrcReader(String node) throws IOException {
		return super.nodeValueOrSrcReader(node, this.getDefaultFileLocation());
	}
}
