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
 * ExampleConcreteProcessor.java
 * Created on October 5, 2006
 *
 * $Header$
 */
package org.archive.state;


/**
 * A (useless) example of a concrete processor implementation.  This class
 * implements integer division on two arguments.  The superclass defined
 * the Keys for the arguments to the integer operationn; this class provides
 * additional Keys for controlling the result of a division-by-zero.
 * 
 * <p>This class really only exists for unit testing purposes.  FIXME:
 * We really need a separate test source directory for this sort of thing.
 * 
 * @author pjack
 */
public class ExampleConcreteProcessor extends ExampleAbstractProcessor {

   
    /**
     * Determines whether or not division-by-zero errors should be caught.
     */
    final public static Key<Boolean> CATCH_DIVISION_BY_ZERO = Key.make(false);

    
    /**
     * The result to return if a division-by-zero error is caught.
     */
    final public static Key<Integer> DIVISION_BY_ZERO_RESULT = Key.make(0);


    /**
     * This is necessary because keys must be registered with the manager 
     * before they are used.
     */
    static {
        KeyManager.addKeys(ExampleConcreteProcessor.class);
    }


    /**
     * Processes the integer division operation. 
     * 
     * @state  the state that provides the arguments for the operation, and
     *   also controls how division-by-zero is handled
     * @return  the result of the division
     */
    public Integer process(ExampleStateProvider state) {
        try {
            return state.get(this, LEFT) / state.get(this, RIGHT);
        } catch (ArithmeticException e) {
            if (state.get(this, CATCH_DIVISION_BY_ZERO)) {
                return state.get(this, DIVISION_BY_ZERO_RESULT);
            } else {
                throw e;
            }
        }
    }
    
}
