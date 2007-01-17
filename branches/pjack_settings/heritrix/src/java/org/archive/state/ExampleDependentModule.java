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
public class ExampleDependentModule extends ExampleAbstractProcessor {


    @Dependency
    final public static Key<Runnable> RUNNABLE = 
        Key.make(Runnable.class, new Thread());

    @Dependency
    final public static Key<CharSequence> CHARSEQUENCE = 
        Key.make(CharSequence.class, new StringBuilder());

    /**
     * This is necessary because keys must be registered with the manager 
     * before they are used.
     */
    static {
        KeyManager.addKeys(ExampleDependentModule.class);
    }

    
    final private CharSequence sequence;
    final private Runnable runnable;
    
    public ExampleDependentModule(CharSequence sequence, Runnable runnable) {
        this.runnable = runnable;
        this.sequence = sequence;
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
            runnable.run();
            return 0;
        } catch (RuntimeException e) {
            System.err.println(sequence);
            return -1;
        }
    }
    
}
