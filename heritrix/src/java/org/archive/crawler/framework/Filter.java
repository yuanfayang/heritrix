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
 * Filter.java
 * Created on Apr 16, 2003
 *
 * $Header$
 */
package org.archive.crawler.framework;

import java.util.logging.Logger;

import javax.management.AttributeNotFoundException;

import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.settings.CrawlerModule;
import org.archive.crawler.datamodel.settings.SimpleType;

/**
 * 
 * @author Gordon Mohr
 */
public class Filter extends CrawlerModule {
    private static Logger logger =
        Logger.getLogger("org.archive.crawler.framework.Filter");

    static final String ATTR_INVERTED = "inverted";

    // associated CrawlController
    protected CrawlController controller;

    /**
     * @param name
     * @param description
     */
    public Filter(String name, String description) {
        super(name, description);
        addElementToDefinition(
            new SimpleType(
                ATTR_INVERTED,
                "Filter functionality should be inverted",
                new Boolean(false)));
    }


    public boolean accepts(Object o) {
        CrawlURI curi = (o instanceof CrawlURI) ? (CrawlURI) o : null;
        boolean inverter = false;
        try {
            inverter = ((Boolean) getAttribute(ATTR_INVERTED, curi)).booleanValue();
        } catch (AttributeNotFoundException e) {
            logger.severe(e.getMessage());
        }
        return inverter ^ innerAccepts(o);
    }
    
    /** Do nothing default implementation.
     * 
     * @param o
     * @return If it accepts.
     */
    protected boolean innerAccepts(Object o) {
        return true;
    }

    public void initialize(CrawlController controller) {
        this.controller = controller;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return "Filter<" + getName() + ">";
    }

    /**
     * @return The crawl controller.
     */
    public CrawlController getController() {
        return controller;
    }

}
