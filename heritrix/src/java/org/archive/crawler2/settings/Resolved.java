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
 * Resolved.java
 * Created on October 24, 2006
 *
 * $Header$
 */
package org.archive.crawler2.settings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.archive.state.Key;

public class Resolved<T> {

    private Key<T> key;
    private Object processor;
    private T value;
    private List<Sheet> sheets;
    
    
    public Resolved(SingleSheet sheet, Object processor, Key<T> key, T value) {        
        this.key = key;
        this.processor = processor;
        this.value = value;
        Sheet s = sheet;
        this.sheets = Collections.singletonList(s);
    }

    
    
    public Resolved(List<Sheet> sheets, Object processor, Key<T> key, T value) {
        this.key = key;
        this.processor = processor;
        this.value = value;
        this.sheets = new ArrayList<Sheet>(sheets);
    }
    

    public Key<T> getKey() {
        return key;
    }


    public Object getProcessor() {
        return processor;
    }


    public List<Sheet> getSheets() {
        return sheets;
    }

    
    public SingleSheet getSingleSheet() {
        return (SingleSheet)sheets.get(sheets.size() - 1);
    }

    public T getValue() {
        return value;
    }
}
