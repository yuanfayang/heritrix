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
 * UnmodifiableTypedList.java
 *
 * Created on Jun 4, 2007
 *
 * $Id:$
 */

package org.archive.settings;

import java.io.Serializable;
import java.util.AbstractList;
import java.util.Collections;
import java.util.List;

/**
 * @author pjack
 *
 */
class UnmodifiableTypedList<T> extends AbstractList<T> 
implements TypedList<T>, Serializable {

    final private static long serialVersionUID = 1L;
    
    private List<T> delegate;
    private Class<T> elementType;
    private Sheet sheet;
    
    public UnmodifiableTypedList(TypedList<T> delegate) {
        this.delegate = delegate;
        this.elementType = delegate.getElementType();
    }
    
    public UnmodifiableTypedList(Sheet sheet, List<T> delegate, Class<T> c) {
        this.sheet = sheet;
        this.delegate = delegate;
        this.elementType = c;
    }
    
    
    public T get(int index) {
        return delegate.get(index);
    }
    
    
    public int size() {
        return delegate.size();
    }
    

    public Class<T> getElementType() {
        return elementType;
    }

    
    public List<Sheet> getSheets(int index) {
        if (delegate instanceof TypedList) {
            TypedList<T> tl = (TypedList<T>)delegate;
            return tl.getSheets(index);
        }
        return Collections.singletonList(sheet);
    }
}
