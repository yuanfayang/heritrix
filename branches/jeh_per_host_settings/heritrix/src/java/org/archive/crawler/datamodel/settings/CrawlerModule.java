/* CrawlerModule
 * 
 * $Id$
 * 
 * Created on Dec 17, 2003
 *
 * Copyright (C) 2004 Internet Archive.
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
 */
package org.archive.crawler.datamodel.settings;

import javax.management.InvalidAttributeValueException;

/**
 * Superclass of all modules that should be configurable.
 * 
 * @author John Erik Halse
 */
public class CrawlerModule extends ComplexType {
	/**
	 * @param parent
	 * @param name
	 * @param description
	 */
	protected CrawlerModule(String name, String description) {
		super(name, description);
	}
    
    /** Every subclass should implement this constructor
     * 
     * @param name of the module
     */
    public CrawlerModule(String name) {
        super(name, name);
    }
    
    /* (non-Javadoc)
     * @see org.archive.crawler.datamodel.settings.ComplexType#addElement(org.archive.crawler.datamodel.settings.CrawlerSettings, org.archive.crawler.datamodel.settings.Type)
     */
    public Type addElement(CrawlerSettings settings, Type type) throws InvalidAttributeValueException {
        if(initialized()) {
            throw new IllegalStateException("Not allowed to add elements to modules");
        }
        return super.addElement(settings, type);
    }
}
