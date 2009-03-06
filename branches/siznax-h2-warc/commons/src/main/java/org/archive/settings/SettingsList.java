/* 
 * Copyright (C) 2007 Internet Archive.
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
 * SettingsList.java
 *
 * Created on Mar 9, 2007
 *
 * $Id:$
 */

package org.archive.settings;

import java.io.Serializable;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * A list whose elements are configured by a SheetManager.  Modifications
 * to this list trigger ModuleListener events.
 * 
 * @author pjack
 *
 */
public class SettingsList<T> extends AbstractList<T> 
implements TypedList<T>, Serializable {

    /** For serialization.*/
    private static final long serialVersionUID = 1L;


    /**
     * The delegate list (an ArrayList).  Note the element type of this list
     * may or may not be T.  It will be T if the element type is simple;
     * otherwise it will be a list of {@link ModuleInfo}.
     */
    final private List<Object> delegate;
    
    /**
     * The sheet that created this list.  Will be notified when elements 
     * change.
     */
    final private SingleSheet sheet;
    
    /**
     * Collection view of this.sheet -- this list will always only contain
     * 1 element, which will be this.sheet.
     */
    final private List<Sheet> sheets;
    
    /**
     * The sheet manager; equivalent to sheet.getSheetManager().
     */
    final private SheetManager manager;
    
    /**
     * The element type of the list.
     */
    final private Class<T> elementType;
    
    
    /**
     * Constructs a new SettingsList backed by an empty ArrayList.
     * 
     * @param sheet  The sheet creating this list
     * @param c      The element type of this list
     */
    public SettingsList(SingleSheet sheet, Class<T> c) {
        this(sheet, new ArrayList<T>(), c);
    }
    

    /**
     * Constructs a new SettingsList backed by the given list.
     * 
     * @param sheet  The sheet that created this list
     * @param list   The backing list
     * @param c      The element type of this list
     */
    @SuppressWarnings("unchecked")
    public SettingsList(SingleSheet sheet, List<T> list, Class<T> c) {
        List l = list;
        this.delegate = new ArrayList<Object>(l);
        this.sheet = sheet;
        this.sheets = Collections.unmodifiableList(
                Collections.singletonList((Sheet)sheet));
        this.manager = sheet.getSheetManager();
        this.elementType = c;
    }


    /**
     * Duplicates this SettingsList.  The result is a deep copy of this list 
     * -- any contained SettingsLists or SettingsMaps will also be 
     * duplicated.  
     * 
     * <p>This method is used when a sheet is checked out for editing.
     * 
     * @param d   The Duplicator (makes this easier)
     * @return    A deep copy of this list
     */
    @SuppressWarnings("unchecked")
    Object duplicate(Duplicator d) {        
        List newElements = new ArrayList<T>();
        for (Object e: delegate) {
            newElements.add(d.duplicate(e));
        }
        return new SettingsList(d.getNewSheet(), newElements, elementType);
    }

    
    /**
     * Returns the sheets that defined the element at the given index.
     * Since this list was necessarily created by a single sheet, the return
     * value of this method is always the same thing: A singleton list 
     * containing that single sheet.
     * 
     * @return  the sheet used to construct this list
     */
    public List<Sheet> getSheets(int index) {
        return sheets;
    }
     

    public Class<T> getElementType() {
        return elementType;
    }


    /**
     * Validates elements.  This is a hack to prevent special characters in 
     * strings.  Right now just ensures that no strings contain a newline 
     * character, which would corrupt sheet files.  FIXME, obviously.
     * 
     * <p>This method is called before any element is added/changed in the
     * list, so if you have other validations put them here.
     * 
     * <p>Also, this method increments the modCount after the validation 
     * succeeds.
     * 
     * @param element  the potential element to validate.
     */
    private void validate(T element) {
        if (element instanceof String) {
            String s = (String)element;
            if ((s.indexOf('\n') >= 0) || (s.indexOf('\r') >= 0)) {
                throw new IllegalArgumentException("List elements cannot contain a newline.");
            }
        }
        modCount++;
    }
    

    @Override
    public void add(int index, T element) {
        validate(element);
        if (!SingleSheet.isModuleType(elementType)) {
            delegate.add(index, element);
        } else {
            setModuleValue(index, element);
        }
    }


    @Override
    public boolean add(T o) {
        validate(o);
        if (!SingleSheet.isModuleType(elementType)) {
            delegate.add(o);
        } else {
            setModuleValue(size(), o);
        }
        return true;
    }


    @Override
    public void clear() {
        for (Object t: delegate) {
            manager.fireModuleChanged(t, null);
        }
        delegate.clear();
    }


    
    /**
     * If the given element is a ModuleInfo, returns the module; otherwise
     * just returns the element.  Needed when retrieving elements from the
     * list.
     * 
     * @param in   the element
     * @return     the element, or the module referred to by its ModuleInfo
     */
    private T toActualObject(Object in) {
        if (in instanceof ModuleInfo) {
            in = ((ModuleInfo)in).holder.module;
        }
        // FIXME: This warning actually indicates a serious bug.
        // In the case of a SettingsList in "stub mode", the in object
        // will actually be a Stub<T>, and not a T.  This compile-time
        // type safety doesn't catch that.
        return (T)in;
    }
    
    
    @Override
    public T get(int index) {
        return toActualObject(delegate.get(index));
    }

    
    @Override
    public T remove(int index) {
        T result = toActualObject(delegate.remove(index));
        manager.fireModuleChanged(result, null);
        return result;
    }


    @Override
    public T set(int index, T element) {
        validate(element);
        Object old;
        if (!SingleSheet.isModuleType(elementType)) {
            old = delegate.set(index, element);
        } else {
            old = setModuleValue(index, element);
        }
        return (T)old;
    }
    

    /**
     * Changes a module in this list.  Used by set and add methods.  Ensures
     * that events are fired, and that if the element was the first use of
     * the module, then all uses of that module in the configuration are 
     * changed to the new value.
     * 
     * @param i       the index of the element to change
     * @param value   the new value for that index
     * @return        the old value at that index
     */
    private Object setModuleValue(int i, Object value) {
        Container c = new ListContainer(delegate);
        Object old = sheet.setModuleValue(c, i, value);
        manager.fireModuleChanged(old, value);
        return old;
    }

    
    /**
     * Swaps two elements in this list.  This method exists for efficiency;
     * it avoids unnecessary checks for event notifications and so on.
     * 
     * @param index1   the index of the first element to swap
     * @param index2   the index of the second element to swap
     */
    public void swap(int index1, int index2) {
        Object o1 = delegate.get(index1);
        Object o2 = delegate.set(index2, o1);
        delegate.set(index1, o2);
        modCount++;
    }

    
    @Override
    public int size() {
        return delegate.size();
    }

    
}
