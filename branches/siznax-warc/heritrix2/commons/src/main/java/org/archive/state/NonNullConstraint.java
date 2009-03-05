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
 * ClassicScope.java
 * Created on Nov 29, 2006
 *
 * $Header$
 */
package org.archive.state;


/**
 * Does not allow <code>null</code> as a value.
 * 
 * @author pjack
 */
public class NonNullConstraint implements Constraint<Object> {

    /**
     * For serialization. 
     */
    private static final long serialVersionUID = 1L;

    
    /**
     * Singleton instance.
     */
    final public static NonNullConstraint INSTANCE = new NonNullConstraint();
    

    /**
     * Constructor.  Private to enforce singleton.
     */
    private NonNullConstraint() {
    }
    

    /**
     * Returns false if the given object is null, or true if the given object
     * is not null.
     * 
     * @return  true if the given object is non-null
     */
    public boolean allowed(Object object) {
        return object != null;
    }

    /* (non-Javadoc)
     * @see org.archive.state.Constraint#description()
     */
    public String description() {
        return "Value must not be null";
    }
    
    public String toString() {
        return "This setting does not permit null values.";
    }

}
