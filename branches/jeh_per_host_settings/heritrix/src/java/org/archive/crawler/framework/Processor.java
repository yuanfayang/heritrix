/* Copyright (C) 2003 Internet Archive.
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
 * Processor.java
 * Created on Apr 16, 2003
 *
 * $Header$
 */
package org.archive.crawler.framework;

import java.lang.reflect.Constructor;
import java.util.Iterator;
import java.util.logging.Logger;

import javax.management.AttributeNotFoundException;

import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.settings.CrawlerModule;
import org.archive.crawler.datamodel.settings.MapType;
import org.archive.crawler.datamodel.settings.SimpleType;

/**
 * 
 * @author Gordon Mohr
 */
public class Processor extends CrawlerModule {
    /**
	 * XPath to any specified filters
	 */
	//private static String XP_FILTERS = "filter";
	private final static String ATTR_FILTERS = "filters";
    private final static String ATTR_ENABLED = "enabled";
    private final static String ATTR_NEXT = "next";
    private final static String ATTR_POSTPROCESSOR = "postprocessor";
    private MapType filters;
    
	private static Logger logger = Logger.getLogger("org.archive.crawler.framework.Processor");

    /**
     * @param name
     * @param description
     */
    public Processor(String name, String description) {
        super(name, description);
        addElementToDefinition(new SimpleType(ATTR_ENABLED, "Is processor enabled", new Boolean(true)));
        addElementToDefinition(new SimpleType(ATTR_NEXT, "Next processor in chain", ""));
        addElementToDefinition(new SimpleType(ATTR_POSTPROCESSOR, "Postprocessor", new Boolean(false)));
        filters = (MapType) addElementToDefinition(new MapType(ATTR_FILTERS, "Filters"));
    }

	protected CrawlController controller;
	
	public final void process(CrawlURI curi) {
		// by default, simply forward curi along
		curi.setNextProcessor(getDefaultNext(curi));
		if(filtersAccept(curi)) {
			innerProcess(curi);
		} else {
			innerRejectProcess(curi);
		}
	}
	
	/**
	 * @param curi
	 */
	protected void innerRejectProcess(CrawlURI curi) {
		// by default do nothing
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
	 * @return True if all filters accept this CrawlURI.
	 */
	protected boolean filtersAccept(CrawlURI curi) {
		if (filters.isEmpty(curi)) {
			return true;
		}
		Iterator iter = filters.iterator(curi);
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
	 * @return Name of processor.
	protected String getName() {
		return name;
	}
     */

	/**
	 * 
	 */
	private Processor getDefaultNext(CrawlURI curi) {
        String nextProcessorName;
        Processor nextProcessor = null;
        try {
            nextProcessorName = (String) getAttribute(ATTR_NEXT, curi);
            if (nextProcessorName == null || nextProcessorName.equals("")) {
                nextProcessor = null;
            } else {
                nextProcessor = (Processor) getParent().getAttribute(nextProcessorName, curi);
            }
        } catch (AttributeNotFoundException e) {
            logger.severe(e.getMessage());
        }
        return nextProcessor;
	}
	
    private Processor getPostprocessor(CrawlURI curi) {
        String processorName;
        Processor processor = null;
        try {
            processorName = (String) getAttribute(ATTR_POSTPROCESSOR, curi);
            processor = (Processor) getParent().getAttribute(processorName, curi);
        } catch (AttributeNotFoundException e) {
            logger.severe(e.getMessage());
        }
        return processor;
    }
    
	public void initialize(CrawlController c) {
		controller = c;
		//Node n;
		//if ((n=xNode.getAttributes().getNamedItem("next"))!=null) {
		//	defaultNext = (Processor)c.getProcessors().get(n.getNodeValue());
		//}
		try {
            if (((Boolean) getAttribute(null, ATTR_POSTPROCESSOR)).booleanValue()) {
            	// I am the distinguished postprocessor earlier stage can skip to
            	controller.setPostprocessor(this);
            }
        } catch (AttributeNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        Iterator iter = filters.iterator(null);
        while (iter.hasNext()) {
            ((Filter) iter.next()).initialize(c);
        }
	}
	
	/**
	 * @param string
	private void setName(String string) {
		name=string;
	}
     */

	public Processor spawn(int serialNum) {
		Processor newInstance = null;
		try {
            Constructor co =
                getClass().getConstructor(new Class[] { String.class });
            newInstance =
                (Processor) co.newInstance(new Object[] {
                    getName() + serialNum
                    });
            getParent().setAttribute(newInstance);
            newInstance.setTransient(true);
			newInstance.initialize(controller);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
        }
		return newInstance;
	}
    
    /**
     * Compiles and returns a report (in human readable form) about the status
     * of the processor.  The processor's name (of implementing class) should
     * always be included.
     * <p>
     * Examples of stats declared would include:<br>
     * * Number of CrawlURIs handled.<br>
     * * Number of links extracted (for link extractors)<br>
     * etc.
     * 
     * @return A human readable report on the processor's state.
     */
    public String report(){
        return ""; // Default behavior.
    }
}
