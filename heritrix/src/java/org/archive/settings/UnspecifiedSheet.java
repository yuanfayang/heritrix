/* Copyright (C) 2007 Internet Archive.
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
 * UnspecifiedSheet.java
 * Created on January 17, 2007
 *
 * $Header$
 */
package org.archive.settings;

import org.archive.state.Key;

public class UnspecifiedSheet extends Sheet {

    
    
    
    public UnspecifiedSheet(SheetManager manager, String name) {
        super(manager, name);
    }

    @Override
    public <T> T check(Object module, Key<T> key) {
        validateModuleType(module.getClass(), key);
        return key.getDefaultValue();
    }

    @Override
    public <T> Offline checkOffline(Offline module, Key<T> key) {
        validateModuleType(module.getType(), key);
        return Offline.make(key.getDefaultValue().getClass());
    }

    @Override
    public <T> Resolved<T> resolve(Object module, Key<T> key) {
        // TODO Auto-generated method stub
        return null;
    }

    
    
    
}
