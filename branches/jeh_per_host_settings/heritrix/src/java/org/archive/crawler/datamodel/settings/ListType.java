/* ListType
 * 
 * $Id$
 * 
 * Created on Jan 7, 2004
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Super type for all lists.
 * 
 * @author John Erik Halse
 */
public abstract class ListType implements Type {
    private final List listData = new ArrayList();
    private final String name;
    private final String description;

    /** Constructs a new ListType.
     * 
     * @param name the name of the list
     * @param description the description of the list
     */
    public ListType(String name, String description) {
        this.name = name;
        this.description = description;
    }

    /** Appends the specified element to the end of this list.
     * 
     * @param element element to be appended to this list.
     * @throws ClassCastException is thrown if the element was of wrong type
     *         and could not be converted.
     */
    public void add(Object element) {
        element = checkType(element);
        listData.add(element);
    }

    /** Inserts the specified element at the specified position in this list.
     *  
     * Shifts the element currently at that position (if any) and any
     * subsequent elements to the right (adds one to their indices).
     * 
     * @param index index at which the specified element is to be inserted.
     * @param element element to be inserted.
     * @throws ClassCastException is thrown if the element was of wrong type
     *         and could not be converted.
     */
    public void add(int index, Object element) {
        element = checkType(element);
        listData.add(index, element);
    }

    /** Appends all of the elements in the specified list to the end of this
     * list, in the order that they are returned by the specified lists's
     * iterator.
     * 
     * The behavior of this operation is unspecified if the specified
     * collection is modified while the operation is in progress.
     *
     * This method is a helper method for subclasses. 
     *
     * @param l list whose elements are to be added to this list.
     */
    protected void addAll(ListType l) {
        listData.addAll(l.listData);
    }

    /** Replaces the element at the specified position in this list with the
     *  specified element.
     * 
     * @param index index at which the specified element is to be inserted.
     * @param element element to be inserted.
     * @return the element previously at the specified position.
     * @throws ClassCastException is thrown if the element was of wrong type
     *         and could not be converted.
     */
    public Object set(int index, Object element) {
        element = checkType(element);
        return listData.set(index, element);
    }

    /** Returns an iterator over the elements in this list in proper sequence.
     * 
     * @return an iterator over the elements in this list.
     */
    public Iterator iterator() {
        return listData.iterator();
    }

    /** Get the number of elements in this list.
     * 
     * @return number of elements.
     */
    public int size() {
        return listData.size();
    }

    /** Returns true if this list contains no elements.
     * 
     * @return true if this list contains no elements.
     */
    public boolean isEmpty() {
        return listData.isEmpty();
    }

    /** Check if element is of right type for this list.
     * 
     * If this method gets a String, it should try to convert it to
     * the right element type before throwing an exception.
     * 
     * @param element element to check.
     * @return element of the right type.
     * @throws ClassCastException is thrown if the element was of wrong type
     *         and could not be converted.
     */
    public abstract Object checkType(Object element) throws ClassCastException;

    /* (non-Javadoc)
     * @see org.archive.crawler.datamodel.settings.Type#getDefaultValue()
     */
    public Object getDefaultValue() {
        return this;
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.datamodel.settings.Type#getDescription()
     */
    public String getDescription() {
        return description;
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.datamodel.settings.Type#getName()
     */
    public String getName() {
        return name;
    }

    /** Removes all elements from this list.
     */
    public void clear() {
        listData.clear();
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.datamodel.settings.Type#getLegalValues()
     */
    public Object[] getLegalValues() {
        return null;
    }
}
