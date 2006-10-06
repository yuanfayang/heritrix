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
 * KeyMaker.java
 * Created on October 5, 2006
 *
 * $Header$
 */
package org.archive.state;


import java.util.HashSet;
import java.util.Set;


/**
 * Aids construction of Key instances.  This class is fully mutable and
 * provides a way to legibly construct a new Key:
 * 
 * <pre>
 * KeyMaker<String> km = KeyMaker.make("default value");
 * km.constraints.add(new OneStringConstraint());
 * km.constraints.add(new TwoStringConstraint());
 * km.expert = true;
 * MY_KEY = km.toKey();
 * </pre>
 * 
 * In the absence of KeyMaker, the above code would look something like this:
 * 
 * <pre>
 * MY_KEY = new Key<String>(String.class, "default value", true, 
 *   new HashSet<Constraint<String>>(Arrays.asList(new Constraint<String>[] {
 *     new OneStringConstraint(), new TwoStringConstraint() 
 *   })));
 * </pre>
 * 
 * It would get even worse as new Key attributes were defined, like the 
 * currently missing "overrideable" flag; or if we decided that descriptions
 * and names belonged in code after all.
 * 
 * @author pjack
 *
 * @param <T>   the type of the generated key's values
 */
public class KeyMaker<T> {

    
    public Class<T> type;
    public Set<Constraint<T>> constraints;
    public T def;
    public boolean expert = false;


    /** Constructor. */
    public KeyMaker() {
        constraints = new HashSet<Constraint<T>>();
    }
    
    
    /**
     * Resets this KeyMaker.  All fields are set to null, and the constraint
     * set is cleared.  There's usually no reason to explicitly invoke this
     * method; it will be called when a key is produced by this maker.
     */
    public void reset() {
        type = null;
        constraints.clear();
        def = null;
    }


    /**
     * Validates that the current contents of this KeyMaker can produce 
     * a valid key.
     * 
     * @throws IllegalArgumentException  if any of this KeyMaker's fields
     *   are invalid
     */
    void validate() {
        if (type == null) {
            throw new IllegalArgumentException("type may not be null."); 
        }
        // allow null default values
        // allow empty constraints        
    }
    
    
    public Key<T> toKey() {
        return new Key<T>(this);
    }

    
    public static <T> KeyMaker<T> make(T def) {
        @SuppressWarnings("unchecked")
        Class<T> c = (Class<T>)def.getClass();

        KeyMaker<T> result = new KeyMaker<T>();
        result.type = c;
        result.def = def;
        return result;
    }

}
