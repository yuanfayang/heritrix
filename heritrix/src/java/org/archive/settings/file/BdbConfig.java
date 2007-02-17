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
 * BdbConfig.java
 *
 * Created on Feb 15, 2007
 *
 * $Id:$
 */
package org.archive.settings.file;

import java.io.Serializable;

import org.archive.state.Immutable;
import org.archive.state.Key;
import org.archive.state.KeyManager;
import org.archive.state.Module;

/**
 * @author pjack
 *
 */
public class BdbConfig implements Module, Serializable {

    
    
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    @Immutable
    final public static Key<String> DIR = Key.make("state");
    
    @Immutable
    final public static Key<Integer> BDB_CACHE_PERCENT = Key.make(60); 
    
    static {
        KeyManager.addKeys(BdbConfig.class);
    }
    
    public BdbConfig() {
    }
    
    
}
