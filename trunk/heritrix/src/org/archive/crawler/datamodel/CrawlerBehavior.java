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

import org.archive.crawler.basic.CrawlerConfigurationContants;
import org.archive.crawler.framework.XMLConfig;
import org.w3c.dom.Node;

/**
 * @author gojomo
 *
 */
public class CrawlerBehavior extends XMLConfig implements CrawlerConfigurationContants {
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
		int maxToes = getIntAt("//limits/max-toe-threads/@value");
		
		if(maxToes < 0 && parentConfigurationFile != null){
			return ((CrawlOrder)parentConfigurationFile).getBehavior().getMaxToes();
		}
		return maxToes;
	}
	/**
	 * @return
	 */	
	public int getMaxLinkDepth(){
		int maxLinkD =  getIntAt("//limits/max-link-depth/@value");
		
		if(maxLinkD < 0 && parentConfigurationFile != null){
			return ((CrawlOrder)parentConfigurationFile).getBehavior().getMaxLinkDepth();
		}
		return maxLinkD;
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

	public List getSeeds() throws FatalConfigurationException {
		if (seeds != null) {
			return seeds;
		}
		seeds = new ArrayList();
		try {
			BufferedReader reader = nodeValueOrSrcReader("//seeds");
			String read;
			while (reader != null) {
				do {
					read = reader.readLine();
				} while (
					(read != null)
						&& ((read = read.trim()).startsWith("#")
							|| read.length() == 0));

				if (read == null) {
					reader.close();
					reader = null;
				} else {
					try {
						seeds.add(UURI.createUURI(read));
					} catch (URISyntaxException e1) {
						e1.printStackTrace();
					}
				}
			}
		} catch (IOException e) {
			throw new FatalConfigurationException(
				"Unable to locate seeds file: " + e.toString());
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
