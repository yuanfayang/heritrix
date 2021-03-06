/* MatchesRegExpDecideRule
*
* $Id$
*
* Created on Apr 4, 2005
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

import java.util.regex.Pattern;

import org.archive.modules.ProcessorURI;
import org.archive.state.Key;
import org.archive.state.KeyManager;


/**
 * Rule applies configured decision to any ProcessorURIs whose String URI
 * matches the supplied regexp.
 *
 * @author gojomo
 */
public class MatchesRegExpDecideRule extends PredicatedAcceptDecideRule {

    private static final long serialVersionUID = 2L;
    
    public static final Key<Pattern> REGEXP = Key.make(Pattern.compile("."));

    static {
        KeyManager.addKeys(MatchesRegExpDecideRule.class);
    }
    
    /**
     * Usual constructor. 
     */
    public MatchesRegExpDecideRule() {
    }
    
    
    /**
     * Evaluate whether given object's string version
     * matches configured regexp
     * 
     * @param object
     * @return true if regexp is matched
     */
    @Override
    protected boolean evaluate(ProcessorURI uri) {
        Pattern p = getPattern(uri);
        return p.matcher(getString(uri)).matches();
    }

    
    protected Pattern getPattern(ProcessorURI uri) {
        return uri.get(this, REGEXP);
    }
    
    protected String getString(ProcessorURI uri) {
        return uri.toString();
    }
}
