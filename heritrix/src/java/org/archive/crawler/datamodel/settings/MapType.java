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

import java.util.EmptyStackException;
import java.util.Iterator;
import java.util.Stack;

import javax.management.AttributeNotFoundException;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanAttributeInfo;

/** This class represents a container of settings.
 * 
 * This class is usually used to make it possible to have a dynamic number
 * of CrawlerModules like for instance a list of filters of different type.
 * 
 * @author John Erik Halse
 */
public class MapType extends ComplexType {
    private final Class contentType;

    /** Construct a new MapType object.
     * 
     * @param name the name of this element.
     * @param description the description of the attribute.
     */
    public MapType(String name, String description) {
        this(name, description, Type.class);
    }

    /** Construct a new MapType object.
     * 
     * @param name the name of this element.
     * @param description the description of the attribute.
     * @param type the type allowed for this map
     */
    public MapType(String name, String description, Class type) {
        super(name, description);
        this.contentType = type;
    }

    /** Add a new element to this map.
     * 
     * @param settings the settings object for this method to have effect.
     * @param element the element to be added.
     * @return Element added.
     * @throws InvalidAttributeValueException
     */
    public Type addElement(CrawlerSettings settings, Type element)
        throws InvalidAttributeValueException {
        settings = settings == null ? globalSettings() : settings;
        if (!(element instanceof MapType) && (contentType.isInstance(element))) {
            return super.addElement(settings, element);
        } else {
            throw new IllegalArgumentException("Nested maps are not allowed.");
        }
    }
    
    /** Remove an attribute from the map.
     * 
     * @param settings the settings object for which this method has effect.
     * @param name name of the attribute to remove.
     * @return the element that was removed.
     * @throws AttributeNotFoundException is thrown if there is no attribute
     *         with the submitted name.
     */
    public Type removeElement(CrawlerSettings settings, String name)
      throws AttributeNotFoundException {
        settings = settings == null ? globalSettings() : settings;
        return settings.getData(this).removeElement(name);
    }
    
    /** Move an attribute up one place in the list. 
     * 
     * @param settings the settings object for which this method has effect.
     * @param name name of attribute to move.
     * @return true if attribute was moved, false if attribute was already
     *              at the top.
     * @throws AttributeNotFoundException is thrown if there is no attribute
     *         with the submitted name.
     */
    public boolean moveElementUp(CrawlerSettings settings, String name)
      throws AttributeNotFoundException {
        settings = settings == null ? globalSettings() : settings;
        return settings.getData(this).moveElementUp(name);
    }

    /** Move an attribute down one place in the list. 
     * 
     * @param settings the settings object for which this method has effect.
     * @param name name of attribute to move.
     * @return true if attribute was moved, false if attribute was already
     *              at bottom.
     * @throws AttributeNotFoundException is thrown if there is no attribute
     *         with the submitted name.
     */
    public boolean moveElementDown(CrawlerSettings settings, String name)
      throws AttributeNotFoundException {
        settings = settings == null ? globalSettings() : settings;
        return settings.getData(this).moveElementDown(name);
    }

    private class It implements Iterator {
        CrawlerSettings settings;
        Stack attributeStack = new Stack();
        Iterator currentIterator;

        public It(CrawlerSettings settings) {
            this.settings = settings;

            DataContainer data = getDataContainerRecursive(settings);
            while (data != null) {
                attributeStack.push(data.getLocalAttributeInfoList().iterator());
                data = getDataContainerRecursive(data.getSettings().getParent());
            }
            
            currentIterator = (Iterator) attributeStack.pop();
        }

        public boolean hasNext() {
            if (currentIterator.hasNext()) {
                return true;
            } else {
                try {
                    currentIterator = (Iterator) attributeStack.pop();
                } catch (EmptyStackException e) {
                    return false;
                }
            }
            return currentIterator.hasNext();
        }

        public Object next() {
            hasNext();
            try {
                return getAttribute(
                    settings,
                    ((MBeanAttributeInfo) currentIterator.next()).getName());
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
    public Iterator iterator(CrawlerSettings settings) {
        settings = settings == null ? globalSettings() : settings;
        return new It(settings);
    }

    /** Returns true if this map is empty.
     * 
     * @param uri the URI for which this set of elements are valid.
     * @return true if this map is empty.
     */
    public boolean isEmpty(CrawlerSettings settings) {
        settings = settings == null ? globalSettings() : settings;

        return !getDataContainerRecursive(settings).hasAttributes();
    }
    
    /** Get the number of elements in this map.
     * 
     * @param uri the URI for which this set of elements are valid.
     * @return the number of elements in this map.
     */
    public int size(CrawlerSettings settings) {
        settings = settings == null ? globalSettings() : settings;

        int size = 0;
        DataContainer data = getDataContainerRecursive(settings);
        while (data != null) {
            size += data.size();
            data = getDataContainerRecursive(data.getSettings().getParent());
        }
        return size;
    }
    
    /** Get the content type allowed for this map.
     * 
     * @return the content type allowed for this map.
     */
    public Class getContentType() {
        return contentType;
    }

}
