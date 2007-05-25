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


import java.util.Collections;
import java.util.List;

import org.archive.state.Key;

class UnspecifiedSheet extends Sheet {

    
    /**
     * First version.
     */
    private static final long serialVersionUID = 1L;
    
    
    final private List<Sheet> thisList;
    
    
    public UnspecifiedSheet(SheetManager manager, String name) {
        super(manager, name);
        thisList = Collections.singletonList((Sheet)this);
    }
    
    @Override
    UnspecifiedSheet duplicate() {
        return this;
    }

    @Override
    public <T> T check(Object module, Key<T> key) {
        validateModuleType(Offline.getType(module), key);
        if (getSheetManager().isOnline()) {
            return key.getDefaultValue();
        } else {
            @SuppressWarnings("unchecked")
            T t = (T)key.getOfflineDefault();
            return t;
        }
    }

    @Override
    public <T> Offline checkOffline(Offline module, Key<T> key) {
        validateModuleType(module.getType(), key);
        return (Offline)key.getOfflineDefault();
    }

    @Override
    public <T> Resolved<T> resolve(Object module, Key<T> key) {
        if (getSheetManager().isOnline()) {
            return Resolved.makeOnline(module, key, key.getDefaultValue(), 
                    thisList);
        }
        
        return Resolved.make(module, key, key.getOfflineDefault(), 
                thisList, null);
    }


}
