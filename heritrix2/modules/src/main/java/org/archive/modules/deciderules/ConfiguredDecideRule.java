/* ConfiguredDecideRule
*
* $Id: ConfiguredDecideRule.java 4649 2006-09-25 17:16:55Z paul_jack $
*
* Created on Mar 3, 2005
*
* Copyright (C) 2005 Internet Archive.
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
package org.archive.modules.deciderules;

import org.archive.modules.ProcessorURI;
import org.archive.state.Key;
import org.archive.state.KeyManager;


/**
 * Rule which can be configured to ACCEPT or REJECT at
 * operator's option.  
 *
 * @author gojomo
 */
public class ConfiguredDecideRule extends DecideRule {

    private static final long serialVersionUID = -7084695808452312555L;

    
    final public static Key<DecideResult> DECISION = 
        Key.make(DecideResult.ACCEPT);
    
    
    static {
        KeyManager.addKeys(ConfiguredDecideRule.class);
    }
    
    public ConfiguredDecideRule() {
    }

    
    protected DecideResult innerDecide(ProcessorURI uri) {
        return onlyDecision(uri);
    }

    
    public DecideResult onlyDecision(ProcessorURI uri) {
        return uri.get(this, DECISION);
    }

}
