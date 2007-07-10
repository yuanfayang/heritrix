/* FetchStatusDecideRule
*
* $Id$
*
* Created on Aug 11, 2006
*
* Copyright (C) 2006 Internet Archive.
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
package org.archive.processors.deciderules;

import org.archive.processors.ProcessorURI;
import org.archive.state.Key;
import org.archive.state.KeyManager;



/**
 * Rule applies the configured decision for any URI which has a
 * fetch status equal to the 'target-status' setting. 
 *
 * @author gojomo
 */
public class FetchStatusDecideRule extends PredicatedAcceptDecideRule {

    private static final long serialVersionUID = 3L;

    final public static Key<Integer> TARGET_STATUS = Key.make(0);
    
    /**
     * Default access so available to test code.
     */
    static final Integer DEFAULT_TARGET_STATUS = new Integer(0);
    
    
    static {
        KeyManager.addKeys(FetchStatusDecideRule.class);
    }
    
    /**
     * Usual constructor. 
     */
    public FetchStatusDecideRule() {
    }

    /**
     * Evaluate whether given object is over the threshold number of
     * hops.
     */
    @Override
    protected boolean evaluate(ProcessorURI uri) {
        return uri.getFetchStatus() == uri.get(this, TARGET_STATUS);
    }

}
