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
 * $Header: /cvsroot/archive-crawler/ArchiveOpenCrawler/src/java/org/archive/settings/path/Attic/PathChange.java,v 1.1.2.2 2007/01/17 01:48:00 paul_jack Exp $
 */
package org.archive.settings.path;



public class PathChange {
    
    
    final private String path;
    final private String value;
    final private String type;
    

    public PathChange(String path, String type, String value) {
        if (path == null) {
            throw new IllegalArgumentException("path must not be null");
        }
        if (type == null) {
            throw new IllegalArgumentException("type must not be null");
        }
        if (type == null) {
            throw new IllegalArgumentException("value must not be null");
        }
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
    
    
    public boolean equals(Object other) {
        if (!(other instanceof PathChange)) {
            return false;
        }
        
        PathChange pc = (PathChange)other;
        return pc.type.equals(type) 
            && pc.value.equals(value)
            && pc.path.equals(path);
    }
    
    
    public int hashCode() {
        return type.hashCode() 
            ^ value.hashCode() 
            ^ path.hashCode();
    }
    
    
    public String toString() {
        return "PathChange{path=" + path 
            + ", type=" + type 
            + ", value=" + value + "}";
    }

}
