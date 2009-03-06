/* RandomUriPrecedencePolicy
*
* $Id: CostAssignmentPolicy.java 4981 2007-03-12 07:06:01Z paul_jack $
*
* Copyright (C) 2008 Internet Archive.
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
package org.archive.crawler.frontier.precedence;

import java.util.Random;

import org.archive.crawler.datamodel.CrawlURI;
import org.archive.state.Key;
import org.archive.state.KeyManager;

/**
 * UriPrecedencePolicy which assigns URIs a random precedence in the range 
 * 1 to its configured max. For stress-testing queue precedence changes.
 */
public class RandomUriPrecedencePolicy extends BaseUriPrecedencePolicy {
    private static final long serialVersionUID = 8311889128770859329L;

    /** max to assign */
    final public static Key<Integer> MAX_PRECEDENCE = 
        Key.make(10);
    
    Random rnd = new Random(); 
    
    /* (non-Javadoc)
     * @see org.archive.crawler.frontier.precedence.BaseUriPrecedencePolicy#calculatePrecedence(org.archive.crawler.datamodel.CrawlURI)
     */
    @Override
    protected int calculatePrecedence(CrawlURI curi) {
        if(curi.isPrerequisite()|| curi.isSeed()) {
            return 1; 
        }
        return super.calculatePrecedence(curi) + 
            rnd.nextInt(curi.get(this,MAX_PRECEDENCE)) + 1;
    }
    
    // good to keep at end of source: must run after all per-Key
    // initialization values are set.
    static {
        KeyManager.addKeys(RandomUriPrecedencePolicy.class);
    }
}
