/* Copyright (C) 2006 Internet Archive.
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
 * NamedObjectArrayList.java
 * Created on October 24, 2006
 *
 * $Header$
 */
package org.archive.settings;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;


/**
 * A list of NamedObjects that ensures all elements have unique names.
 * 
 * @author pjack
 */
public class NamedObjectArrayList extends AbstractList<NamedObject> {

    /** For serialization. */
    private static final long serialVersionUID = 1L;

    /** The delegate list. */
    private ArrayList<NamedObject> delegate;

    /** The names of the elements in the delegate list. */
    private Set<String> names;
    

    /**
     * Constructor.
     */
    public NamedObjectArrayList() {
        this.delegate = new ArrayList<NamedObject>();
        this.names = new HashSet<String>();
    }
    
    
    /**
     * Constructor.
     * 
     * @param c  the elements with which to populate this list
     */
    public NamedObjectArrayList(Collection<NamedObject> c) {
        this.delegate = new ArrayList<NamedObject>(c);
    }
    
    
    @Override
    public int size() {
        return delegate.size();
    }
    
    
    @Override
    public NamedObject get(int index) {
        return delegate.get(index);
    }


    @Override
    public NamedObject set(int index, NamedObject no) {
        String name = no.getName();
        if (get(index).getName().equals(name)) {
            return delegate.set(index, no);
        } 
        
        if (!names.add(no.getName())) {
            throw new IllegalArgumentException(name + " already exists.");
        }
        NamedObject result = delegate.set(index, no);
        names.remove(result.getName());
        names.add(name);
        return result;
    }


    @Override
    public void add(int index, NamedObject no) {
        String name = no.getName();
        if (!names.add(name)) {
            throw new IllegalArgumentException(name + " already exists.");
        }
        delegate.add(index, no);
    }
    
    
    @Override
    public NamedObject remove(int index) {
        NamedObject result = delegate.remove(index);
        names.remove(result.getName());
        return result;
    }
}
