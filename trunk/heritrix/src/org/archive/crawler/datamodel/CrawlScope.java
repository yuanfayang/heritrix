/*
 * CrawlScope.java
 * Created on May 21, 2003
 *
 * $Header$
 */
package org.archive.crawler.datamodel;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.archive.crawler.framework.*;
import org.w3c.dom.Node;

/**
 * @author gojomo
 *
 */
public class CrawlScope extends XMLConfig {
	List seeds = null;
	
	/**
	 * @param node
	 */
	public CrawlScope(Node node) {
		xNode = nodeOrSrc(node);
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
