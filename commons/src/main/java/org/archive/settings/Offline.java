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
 * Offline.java
 * Created on January 17, 2007
 *
 * $Header: /cvsroot/archive-crawler/ArchiveOpenCrawler/src/java/org/archive/settings/Attic/Offline.java,v 1.1.2.1 2007/01/17 01:48:01 paul_jack Exp $
 */
package org.archive.settings;

import org.archive.util.TypeSubstitution;

final public class Offline<T> implements TypeSubstitution {

    
    private Class<T> type;
    
    
    public Offline(Class<T> type) {
        this.type = type;
    }
    
    
    public Class<T> getType() {
        return type;
    }
    
    
    public Class getActualClass() {
        return type;
    }
    

    public static <T> Offline<T> make(Class<T> cls) {
        return new Offline<T>(cls);
    }
    
    
    public String toString() {
        String tname = type.getName();
        int p = tname.lastIndexOf('.');
        if (p >= 0) {
            tname = tname.substring(p + 1);
        }
        return "Offline<" + tname + ">";
    }


    public static Class getType(Object object) {
        if (object instanceof TypeSubstitution) {
            return ((TypeSubstitution)object).getActualClass();
        } else {
            return object.getClass();
        }
    }

}
