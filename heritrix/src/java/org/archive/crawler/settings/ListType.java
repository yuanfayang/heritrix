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
package org.archive.crawler.settings;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/** Super type for all lists.
 *
 * @author John Erik Halse
 */
public abstract class ListType extends Type implements List {
    
    private final List listData = new ArrayList();
    private final String description;

    /** Constructs a new ListType.
     *
     * @param name the name of the list
     * @param description the description of the list
     */
    public ListType(String name, String description) {
        super(name, null);
        this.description = description;
    }

    /** Appends the specified element to the end of this list.
     *
     * @param element element to be appended to this list.
     * @throws ClassCastException is thrown if the element was of wrong type
     *         and could not be converted.
     * @return true if this collection changed as a result of the call (as
     * per the Collections.add contract).
     */
    public boolean add(Object element) {
        element = checkType(element);
        return this.listData.add(element);
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
        this.listData.add(index, element);
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
        this.listData.addAll(l.listData);
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
        return this.listData.set(index, element);
    }

    /** Returns an iterator over the elements in this list in proper sequence.
     *
     * @return an iterator over the elements in this list.
     */
    public Iterator iterator() {
        return this.listData.iterator();
    }

    /** Get the number of elements in this list.
     *
     * @return number of elements.
     */
    public int size() {
        return this.listData.size();
    }

    /** Returns true if this list contains no elements.
     *
     * @return true if this list contains no elements.
     */
    public boolean isEmpty() {
        return this.listData.isEmpty();
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
     * @see org.archive.crawler.settings.Type#getDefaultValue()
     */
    public Object getDefaultValue() {
        return this;
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.settings.Type#getDescription()
     */
    public String getDescription() {
        return this.description;
    }

    /** Removes all elements from this list.
     */
    public void clear() {
        this.listData.clear();
    }

    /**
     * Returns the object stored at the index specified
     * @param index The location of the object to get within the list.
     * @return the object stored at the index specified
     */
    public Object get(int index){
        return this.listData.get(index);
    }

    /** The getLegalValues is not applicable for list so this method will
     * always return null.
     *
     * @return null
     * @see org.archive.crawler.settings.Type#getLegalValues()
     */
    public Object[] getLegalValues() {
        return null;
    }

    /** Returns this object.
     *
     * This method is implemented to be able to treat the ListType as an
     * subclass of {@link javax.management.Attribute}.
     *
     * @return this object.
     * @see javax.management.Attribute#getValue()
     */
    public Object getValue() {
        return this;
    }

    public boolean addAll(Collection c)
    {
        return this.listData.addAll(c);
    }
    
    public boolean addAll(int index, Collection c)
    {
        return this.listData.addAll(index, c);
    }

    public boolean contains(Object o)
    {
        return this.listData.contains(o);
    }

    public boolean containsAll(Collection c)
    {
        return this.listData.containsAll(c);
    }
    
    public int indexOf(Object o)
    {
        return this.listData.indexOf(o);
    }
    
    public int lastIndexOf(Object o)
    {
        return this.listData.lastIndexOf(o);
    }

    public ListIterator listIterator()
    {
        return this.listData.listIterator();
    }
    
    public ListIterator listIterator(int index)
    {
        return this.listData.listIterator(index);
    }

    public List subList(int fromIndex, int toIndex)
    {
        return this.listData.subList(fromIndex, toIndex);
    }

    public Object[] toArray()
    {
        return this.listData.toArray();
    }

    public Object[] toArray(Object[] a)
    {
        return this.listData.toArray(a);
    }
    
    public Object remove(int index)
    {
        return this.listData.remove(index);
    }

    public boolean remove(Object o)
    {
        return this.listData.remove(o);
    }

    public boolean removeAll(Collection c)
    {
        return this.listData.removeAll(c);
    }
    
    public boolean retainAll(Collection c)
    {
        return this.listData.retainAll(c);
    }
}
