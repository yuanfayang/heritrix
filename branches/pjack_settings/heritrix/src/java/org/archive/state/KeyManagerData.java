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
 * KeyManagerData.java
 * Created on January 17, 2007
 *
 * $Header$
 */
package org.archive.state;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Map;

class KeyManagerData {

    
    final Map<String,Key<Object>> keys;
    final Constructor depCons;
    final List<Key<Object>> depConsParams;
    
    
    public KeyManagerData(Map<String,Key<Object>> keys, Constructor depCons, 
            List<Key<Object>> depConsParams) {
        this.keys = keys;
        this.depCons = depCons;
        this.depConsParams = depConsParams;
    }
    
    
}
