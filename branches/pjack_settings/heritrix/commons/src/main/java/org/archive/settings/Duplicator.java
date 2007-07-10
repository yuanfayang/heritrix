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
 * Duplicator.java
 *
 * Created on Jun 13, 2007
 *
 * $Id:$
 */

package org.archive.settings;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;

/**
 * @author pjack
 *
 */
class Duplicator {

    
    final private IdentityHashMap<Object,Object> changes = 
        new IdentityHashMap<Object,Object>();
    
    final private Sheet oldSheet;
    final private Sheet newSheet;


    public Duplicator(Sheet oldSheet, Sheet newSheet) {
        this.oldSheet = oldSheet;
        this.newSheet = newSheet;
    }

    
    public Sheet getOldSheet() {
        return oldSheet;
    }
    
    
    public Sheet getNewSheet() {
        return newSheet;
    }
    
    
    public List<Sheet> duplicateSheets(List<Sheet> sheets) {
        List<Sheet> newSheets = new ArrayList<Sheet>();
        for (Sheet s: sheets) {
            if (s == oldSheet) {
                newSheets.add(newSheet);
            } else {
                newSheets.add(s);
            }
        }
        return newSheets;
    }
    
    
    public Object duplicate(Object o) {
        Object result = changes.get(o);
        if (result != null) {
            return result;
        }
        if (o instanceof SettingsList) {
            result = ((SettingsList)o).duplicate(this);
            changes.put(o, result);
        } else if (o instanceof SettingsMap) {
            result = ((SettingsMap)o).duplicate(this);
            changes.put(o, result);
        } else {
            result = o;
        }
        
        return result;
    }
}
