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
 * StateProvider.java
 * Created on October 5, 2006
 *
 * $Header$
 */
package org.archive.state;


/**
 * Provides state to some process.
 * 
 * @author pjack
 */
public interface StateProvider {

    /**
     * Returns the value of a property for the current module.
     * 
     * @param <T>  the type of the property to return
     * @param key  the key of the property to return
     * @return  the value of that property
     */
    <T> T get(Object module, Key<T> key);


}
