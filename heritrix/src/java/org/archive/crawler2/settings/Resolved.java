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

import org.archive.state.Key;

public class Resolved<T> {

    private Key<T> key;
    private Object processor;
    private T value;
    private SingleSheet sheet;
    
    
    public Resolved(SingleSheet sheet, Object processor, Key<T> key, T value) {
        this.key = key;
        this.processor = processor;
        this.value = value;
        this.sheet = sheet;
    }


    public Key<T> getKey() {
        return key;
    }


    public Object getProcessor() {
        return processor;
    }


    public SingleSheet getSheet() {
        return sheet;
    }


    public T getValue() {
        return value;
    }
}
