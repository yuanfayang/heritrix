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

import org.archive.crawler.datamodel.CrawlURI;

/** This class represents a container of settings.
 * 
 * This class is usually used to make it possible to have a dynamic number
 * of CrawlerModules like for instance a list of filters of different type.
 * 
 * @author John Erik Halse
 */
public class MapType extends ComplexType {

    /** Construct a new MapType object.
     * 
     * @param name the name of this element.
     * @param description the description of the attribute.
     */
    public MapType(String name, String description) {
        super(name, description);
    }

    /** Add a new element to this map.
     * 
     * @param settings the settings object for this method to have effect.
     * @param type the element to be added.
     */
    public Type addElement(CrawlerSettings settings, Type type)
        throws InvalidAttributeValueException {
        settings = settings == null ? globalSettings() : settings;
        if (!(type instanceof MapType)) {
            return super.addElement(settings, type);
        } else {
            throw new IllegalArgumentException("Nested maps are not allowed.");
        }
    }
    
    public Type removeElement(CrawlerSettings settings, String name) {
        settings = settings == null ? globalSettings() : settings;
        return null;
    }
    
    /** Move an attribute up one place in the list. 
     * 
     * @param name name of attribute to move.
     * @return true if attribute was moved, false if attribute was already
     *              at the top.
     */
    public boolean moveElementUp(CrawlerSettings settings, String name) {
        settings = settings == null ? globalSettings() : settings;
        return settings.getData(this).moveElementUp(name);
    }

    /** Move an attribute down one place in the list. 
     * 
     * @param name name of attribute to move.
     * @return true if attribute was moved, false if attribute was already
     *              at bottom.
     */
    public boolean moveElementDown(CrawlerSettings settings, String name) {
        settings = settings == null ? globalSettings() : settings;
        return settings.getData(this).moveElementDown(name);
    }

    private class It implements Iterator {
        CrawlerSettings settings;
        Iterator atts;

        public It(MapType map, CrawlerSettings settings) {
            this.settings = settings;
            this.atts =
                settings.getData(map).attributeInfoIterator();
        }

        public boolean hasNext() {
            return atts.hasNext();
        }

        public Object next() {
            try {
                return getAttribute(
                    settings,
                    ((MBeanAttributeInfo) atts.next()).getName());
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

    /** Get an Iterator over all the elements in this map.
     * 
     * @param uri the URI for which this set of elements are valid.
     * @return an iterator over all the elements in this map.
     */
    public Iterator iterator(CrawlURI uri) {
        CrawlerSettings settings;
        try {
            settings = uri.getServer().getSettings();
        } catch (NullPointerException e) {
            settings = globalSettings();
        }
        return new It(this, settings);
    }

    /** Returns true if this map is empty.
     * 
     * @param uri the URI for which this set of elements are valid.
     * @return true if this map is empty.
     */
    public boolean isEmpty(CrawlURI uri) {
        CrawlerSettings settings;
        try {
            settings = uri.getServer().getSettings();
        } catch (NullPointerException e) {
            settings = globalSettings();
        }
        return !settings.getData(this).hasAttributes();
    }

    /** Get the number of elements in this map.
     * 
     * @param uri the URI for which this set of elements are valid.
     * @return the number of elements in this map.
     */
    public int size(CrawlURI uri) {
        CrawlerSettings settings;
        try {
            settings = uri.getServer().getSettings();
        } catch (NullPointerException e) {
            settings = globalSettings();
        }
        return settings.getData(this).size();
    }
}
