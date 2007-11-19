/* PrecedencePolicy
*
* $Id: CostAssignmentPolicy.java 4981 2007-03-12 07:06:01Z paul_jack $
*
* Created on Nov 17, 2007
*
* Copyright (C) 2007 Internet Archive.
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
package org.archive.crawler.frontier;

import org.archive.state.Key;
import org.archive.state.KeyManager;

/**
 * QueuePrecedencePolicy that sets a uri-queue's precedence to a configured
 * single value. This value may vary for some queues (as in override 
 * settings sheets) or be changed by an operator mid-crawl (subject to limits 
 * on when such changes are noted in a uri-queues lifecycle). 
 * 
 */
public class SimpleQueuePrecedencePolicy extends QueuePrecedencePolicy {
    private static final long serialVersionUID = 8312032856661175869L;
    
    /** constant precedence to assign; default is 1 */
    final public static Key<Integer> PRECEDENCE = 
        Key.make(1);
    
    /* (non-Javadoc)
     * @see org.archive.crawler.frontier.QueuePrecedencePolicy#queueCreated(org.archive.crawler.frontier.WorkQueue)
     */
    @Override
    public void queueCreated(WorkQueue wq) {
        wq.setPrecedence(wq.get(this,PRECEDENCE));
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.frontier.QueuePrecedencePolicy#queueReevaluate(org.archive.crawler.frontier.WorkQueue)
     */
    @Override
    public void queueReevaluate(WorkQueue wq) {
        wq.setPrecedence(wq.get(this,PRECEDENCE));
    }

    // good to keep at end of source: must run after all per-Key
    // initialization values are set.
    static {
        KeyManager.addKeys(SimpleQueuePrecedencePolicy.class);
    }
}
