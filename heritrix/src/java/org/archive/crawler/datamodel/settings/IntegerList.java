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
 * IntegerList.java
 * Created on Dec 18, 2003
 *
 * $Header$
 */
package org.archive.crawler.datamodel.settings;

/**
 * 
 * @author John Erik Halse
 *
 */
public class IntegerList extends ListType {

    /**
     * 
     */
    public IntegerList(String name, String description) {
        super(name, description);
    }

    /**
     * @param c
     */
    public IntegerList(String name, String description, IntegerList l) {
        super(name, description);
        addAll(l);
    }

    public IntegerList(String name, String description, Integer[] l) {
        super(name, description);
        addAll(l);
    }

    public IntegerList(String name, String description, int[] l) {
        super(name, description);
        addAll(l);
    }

    /* (non-Javadoc)
     * @see java.util.List#add(int, java.lang.Object)
     */
    public void add(int index, Integer element) {
        super.add(index, element);
    }

    public void add(int index, int element) {
        super.add(index, new Integer(element));
    }

    /* (non-Javadoc)
     * @see java.util.Collection#add(java.lang.Object)
     */
    public void add(Integer element) {
        super.add(element);
    }

    public void add(int element) {
        super.add(new Integer(element));
    }

    /* (non-Javadoc)
     * @see java.util.Collection#addAll(java.util.Collection)
     */
    public void addAll(IntegerList l) {
        super.addAll(l);
    }

    /* (non-Javadoc)
     * @see java.util.List#addAll(int, java.util.Collection)
     */
    public void addAll(Integer[] l) {
        for (int i = 0; i < l.length; i++) {
            add(l[i]);
        }
    }

    public void addAll(int[] l) {
        for (int i = 0; i < l.length; i++) {
            add(l[i]);
        }
    }

    /* (non-Javadoc)
     * @see java.util.List#set(int, java.lang.Object)
     */
    public Integer set(int index, Integer element) {
        return (Integer) super.set(index, element);
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.datamodel.settings.ListType#checkType(java.lang.Object)
     */
    public Object checkType(Object element) throws ClassCastException {
        if (!(element instanceof Integer)) {
            element =
                SettingsHandler.StringToType(
                    (String) element,
                    SettingsHandler.INTEGER);
        }
        return element;
    }
}
