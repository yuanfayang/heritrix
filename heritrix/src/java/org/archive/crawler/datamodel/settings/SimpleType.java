/* SimpleType
 * 
 * $Id$
 * 
 * Created on Jan 8, 2004
 *
 * Copyright (C) 2004 Internet Archive.
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
 */
package org.archive.crawler.datamodel.settings;

/**
 * 
 * @author John Erik Halse
 *
 */
public class SimpleType implements Type {
    private final String name;
    private final String description;
    private final Object defaultValue;
    
    /**
     * 
     */
    public SimpleType(String name, String description, Object defaultValue) {
        this.name = name;
        this.description = description;
        this.defaultValue = defaultValue;
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.datamodel.settings.Type#getName()
     */
    public String getName() {
        return name;
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.datamodel.settings.Type#getDescription()
     */
    public String getDescription() {
        return description;
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.datamodel.settings.Type#getDefaultValue()
     */
    public Object getDefaultValue() {
        return defaultValue;
    }

}
