/* HopsPathMatchesRegExpDecideRule
*
* $Id$
*
* Created on June 23, 2005
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
package org.archive.processors.deciderules;

import org.archive.processors.ProcessorURI;


/**
 * Rule applies configured decision to any CrawlURIs whose 'hops-path'
 * (string like "LLXE" etc.) matches the supplied regexp.
 *
 * @author gojomo
 */
public class HopsPathMatchesRegExpDecideRule extends MatchesRegExpDecideRule {

    private static final long serialVersionUID = 3L;


    /**
     * Usual constructor. 
     * @param name
     */
    public HopsPathMatchesRegExpDecideRule() {
    }

    
    @Override
    protected String getString(ProcessorURI uri) {
        return uri.getPathFromSeed();
    }
    
}