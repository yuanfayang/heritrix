/* 
 * Processor.java
 * Created on Apr 16, 2003
 *
 * $Header$
 */
package org.archive.crawler.framework;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Logger;

import org.archive.crawler.datamodel.CrawlURI;
import org.w3c.dom.Node;

/**
 * 
 * @author Gordon Mohr
 */
public class Processor extends XMLConfig {
	/**
	 * XPath to any specified filters
	 */
	private static String XP_FILTERS = "filter";
	
	private static Logger logger = Logger.getLogger("org.archive.crawler.framework.Processor");

	protected CrawlController controller;
	Processor defaultNext;
	String name;
	ArrayList filters = new ArrayList();
	
	public final void process(CrawlURI curi) {
		// by default, simply forward curi along
		curi.setNextProcessor(getDefaultNext());
		if(filtersAccept(curi)) {
			innerProcess(curi);
		} // TODO: else perhaps send to different next? 
	}
	
	/**
	 * @param curi
	 */
	protected void innerProcess(CrawlURI curi) {
		// by default do nothing
	}

	/**
	 * Do all specified filters (if any) accept this CrawlURI? 
	 *  
	 * @param curi
	 * @return
	 */
	protected boolean filtersAccept(CrawlURI curi) {
		if (filters.isEmpty()) {
			return true;
		}
		Iterator iter = filters.iterator();
		while(iter.hasNext()) {
			Filter f = (Filter)iter.next();
			if( !f.accepts(curi) ) {
				logger.info(f+" rejected "+curi+" in Processor "+getName());
				return false; 
			}
		}
		return true;
	}

	/**
	 * @return
	 */
	protected String getName() {
		return name;
	}

	/**
	 * 
	 */
	private Processor getDefaultNext() {
		return defaultNext;
		
	}
	
	public void initialize(CrawlController c) {
		setName(getStringAt("@name"));
		controller = c;
		Node n;
		if ((n=xNode.getAttributes().getNamedItem("next"))!=null) {
			defaultNext = (Processor)c.getProcessors().get(n.getNodeValue());
		}
		instantiateAllInto(XP_FILTERS,filters);
		Iterator iter = filters.iterator();
		while(iter.hasNext()) {
			Object o = iter.next();
			Filter f = (Filter)o;
			f.initialize(controller);
		}
	}
	
	/**
	 * @param string
	 */
	private void setName(String string) {
		name=string;
	}

	public Processor spawn() {
		Processor newInstance = null;
		try {
			newInstance = (Processor) this.getClass().newInstance();
			newInstance.setNode(xNode);
			newInstance.initialize(controller);
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return newInstance;
	}
}
