/* Type
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

import javax.management.Attribute;

/** Interface implemented by all element types.
 * 
 * @author John Erik Halse
 */
public abstract class Type extends Attribute {
    /**
     * @param name
     * @param value
     */
    public Type(String name, Object value) {
        super(name, value);
    }

    /** Get the description of this type
     * 
     * The description should be suitable for showing in a user interface.
     * 
     * @return this type's description
     */
    abstract String getDescription();

    /** The default value for this type
     *  
     * @return this type's default value
     */
    abstract Object getDefaultValue();

    /** Get the legal values for this type.
     * 
     * @return the legal values for this type or null if there are no
     *         restrictions.
     */
    abstract Object[] getLegalValues();

}
