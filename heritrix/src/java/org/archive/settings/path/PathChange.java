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
 * PathChange.java
 * Created on October 24, 2006
 *
 * $Header$
 */
package org.archive.settings.path;

public class PathChange {
    
    
    final private String path;
    final private String value;
    final private String type;
    

    public PathChange(String path, String type, String value) {
        this.path = path;
        this.type = type;
        this.value = value;
    }
    
    
    public String getPath() {
        return path;
    }
    
    
    public String getValue() {
        return value;
    }
    
    
    public String getType() {
        return type;
    }
}
