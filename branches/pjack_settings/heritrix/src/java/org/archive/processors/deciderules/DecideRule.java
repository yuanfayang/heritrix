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
 * DecideRule.java
 * Created on October 5, 2006
 *
 * $Header$
 */
package org.archive.processors.deciderules;


import org.archive.processors.ProcessorURI;
import org.archive.state.Key;


public abstract class DecideRule {

    
    final public static Key<Boolean> ENABLED = Key.make(true);
    
    final public static Key<Boolean> INVERT = Key.make(false);
    
    public DecideResult decisionFor(ProcessorURI uri) {
        if (!uri.get(this, ENABLED)) {
            return DecideResult.PASS;
        }
        DecideResult result = innerDecide(uri);
        if (result == DecideResult.PASS) {
            return result;
        }
        if (uri.get(this, INVERT)) {
            return DecideResult.invert(result);
        } else {
            return result;
        }
    }
    
    
    protected abstract DecideResult innerDecide(ProcessorURI uri);
    
    
    public DecideResult onlyDecision(ProcessorURI uri) {
        return null;
    }

}
