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
 * ExampleInvalidProcessor4.java
 * Created on October 5, 2006
 *
 * $Header$
 */
package org.archive.state;


/**
 * Another example of an invalid processor.  It's invalid because it defines
 * a non-public Key.
 * 
 * @author pjack
 */
public abstract class ExampleInvalidProcessor4 
extends ExampleAbstractProcessor {

    
    /**
     * A non-public and therefore invalid key.
     */
    final static Key<String> KEY = Key.make("");
   
    
    /**
     * Will raise an ExceptionInInitializerError, as the KeyManager will
     * reject the class.
     */
    static {
        KeyManager.addKeys(ExampleInvalidProcessor4.class);
    }
}
