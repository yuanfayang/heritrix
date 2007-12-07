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
 * KeyChangeEvent.java
 *
 * Created on Aug 14, 2007
 *
 * $Id:$
 */

package org.archive.settings;

import java.util.EventObject;

import org.archive.state.Key;
import org.archive.state.StateProvider;

/**
 * @author pjack
 *
 */
public class KeyChangeEvent extends EventObject {

    private static final long serialVersionUID = 1L;

    final private StateProvider stateProvider;
    final private Key<?> key;
    final private Object oldValue;
    final private Object newValue;
    
    public KeyChangeEvent(StateProvider provider, Object source, 
            Key<?> key, Object old, Object newV) {
        super(source);
        this.stateProvider = provider;
        this.key = key;
        this.oldValue = old;
        this.newValue = newV;
    }

    public StateProvider getStateProvider() {
        return stateProvider;
    }
    
    public Key<?> getKey() {
        return key;
    }

    public Object getNewValue() {
        return newValue;
    }

    public Object getOldValue() {
        return oldValue;
    }

    public String toString() {
        return "KeyChangeEvent{source=" + getSource() + ", key=" + key 
            + ", oldValue=" + oldValue + ", newValue=" + newValue;
    }
}
