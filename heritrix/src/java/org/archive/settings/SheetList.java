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
 * SheetList.java
 * Created on December 12, 2006
 *
 * $Id: SheetList.java 4795 2006-12-12 23:42:09Z paul_jack $
 */
package org.archive.settings;

import java.util.ArrayList;

import org.archive.settings.Sheet;
import org.archive.state.Key;
import org.archive.state.StateProvider;


class SheetList extends ArrayList<Sheet> implements StateProvider {

    private static final long serialVersionUID = 1L;

    public <T> T get(Object module, Key<T> key) {
        for (Sheet sheet: this) {
            T result = sheet.get(module, key);
            if (result != null) {
                return result;
            }
        }
        return key.getDefaultValue();
    }
}
