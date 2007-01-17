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
 * ExampleInvalidDependency1.java
 * Created on January 8, 2007
 *
 * $Header$
 */
package org.archive.state;


/**
 * An example of an invalid module.  It's invalid because it defines a
 * dependency, but no constructor that takes that dependency.
 *  
 * <p>This class really only exists for unit testing purposes.  FIXME:
 * We really need a separate test source directory for this sort of thing.
 *
 * @author pjack
 */
public class ExampleInvalidDependency2 {

    
    @Dependency
    final public static Key<ExampleConcreteProcessor> X = 
        Key.make(new ExampleConcreteProcessor());

    
    @Dependency
    final public static Key<ExampleConcreteProcessor> Y = 
        Key.make(new ExampleConcreteProcessor());

    
    static {
        KeyManager.addKeys(ExampleInvalidDependency2.class);
    }
    
    
    public ExampleInvalidDependency2(ExampleConcreteProcessor x, 
            ExampleConcreteProcessor y) {
    }
}
