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
 * ExampleAbstractProcessor.java
 * Created on October 5, 2006
 *
 * $Header$
 */
package org.archive.state;


/**
 * A (useless) example of an abstract StateProcessor.  This class loosely models 
 * an operation on two integer arguments.  It's an example of a class that
 * defines the parameters of a process without actually implementing the 
 * process.  The {@link ExampleConcreteProcessor} class actually implements
 * an integer operation (division).
 * 
 * <p>This class really only exists for unit testing purposes.  FIXME:
 * We really need a separate test source directory for this sort of thing.
 * 
 * @author pjack
 */
public abstract class ExampleAbstractProcessor {


    /**
     * The left operand of the operation.
     */
    final public static Key<Integer> LEFT = Key.make(0);
    
    
    /**
     * The right operand of the operation.
     */
    final public static Key<Integer> RIGHT = Key.make(0);

    
    /**
     * This is necessary because keys must be registered with the manager 
     * before they are used.
     */
    static {
        KeyManager.addKeys(ExampleAbstractProcessor.class);
    }

}
