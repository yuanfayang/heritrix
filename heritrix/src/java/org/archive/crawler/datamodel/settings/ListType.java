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

/**
 * 
 * @author John Erik Halse
 *
 */
public abstract class ListType implements Type {
    private final List listData = new ArrayList();
    private final String name;
    private final String description;

    /**
     * 
     */
    public ListType(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public void add(Object element) {
        element = checkType(element);
        listData.add(element);
    }
    
    public void add(int index, Object element) {
        element = checkType(element);
        listData.add(index, element);
    }
    
    protected void addAll(ListType l) {
        listData.addAll(l.listData);
    }
    
    public Object set(int index, Object element) {
        element = checkType(element);
        return listData.set(index, element);
    }
    
    public Iterator iterator() {
        return listData.iterator();
    }

    public int size() {
        return listData.size();    
    }
    
    public boolean isEmpty() {
        return listData.isEmpty();
    }
    
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
}
