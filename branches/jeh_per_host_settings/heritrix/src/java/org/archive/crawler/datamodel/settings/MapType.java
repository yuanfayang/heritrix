/* MapType
 * 
 * $Id$
 * 
 * Created on Jan 8, 2004
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

import java.util.Iterator;

import javax.management.AttributeNotFoundException;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanAttributeInfo;

/**
 * 
 * @author John Erik Halse
 *
 */
public class MapType extends ComplexType {

    /**
     * @param name
     * @param description
     */
    public MapType(String name, String description) {
        super(name, description);
    }
    
    /* (non-Javadoc)
     * @see org.archive.crawler.datamodel.settings.ComplexType#addElement(org.archive.crawler.datamodel.settings.CrawlerSettings, org.archive.crawler.datamodel.settings.Type)
     */
    public Type addElement(CrawlerSettings settings, Type type) throws InvalidAttributeValueException {
        if(!(type instanceof MapType)) {
            return super.addElement(settings, type);
        } else {
            throw new IllegalArgumentException("Nested maps are not allowed.");
        }
    }
    
    class It implements Iterator {
        CrawlerSettings settings;
        Iterator atts;
                        
        public It(CrawlerSettings settings) {
            this.settings = settings;
            this.atts = settings.getData(getAbsoluteName()).attributeInfoIterator();
        }

        public boolean hasNext() {
            return atts.hasNext();
        }

        public Object next() {
            try {
                return getAttribute(settings, ((MBeanAttributeInfo) atts.next()).getName());
            } catch (AttributeNotFoundException e) {
                // This should never happen
                e.printStackTrace();
                return null;
            }
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    };

    public Iterator iterator(CrawlerSettings settings) {
        settings = settings == null ? globalSettings() : settings;
        return new It(settings);
    }
    
    public boolean isEmpty(CrawlerSettings settings) {
        settings = settings == null ? globalSettings() : settings;
        return !settings.getData(getAbsoluteName()).hasAttributes();
    }
}
