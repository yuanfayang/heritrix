/* SuccessCountsQueuePrecedencePolicy
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

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Transformer;
import org.archive.state.Key;
import org.archive.state.KeyManager;

/**
 * QueuePrecedencePolicy that sets a uri-queue's precedence to a configured
 * base value, then lowers its precedence with each tier of successful URIs
 * completed. Any number of comma-separated tier sizes may be provided, with 
 * the last value assumed to repeat indefinitely. For example, with a 
 * 'base-precedence' value of 2, and 'increment-counts' of "100,1000", the
 * queue will have a precedence of 2 until 100 URIs are successfully fetched, 
 * then a precedence of 3 for the next 1000 URIs successfully fetched, then 
 * continue to drop one precedence rank for each 1000 URIs successfully 
 * fetched.
 */
public class SuccessCountsQueuePrecedencePolicy extends QueuePrecedencePolicy {
    private static final long serialVersionUID = -4469760728466350850L;
    
    /** base precedence to assign */
    final public static Key<Integer> BASE_PRECEDENCE = 
        Key.make(1);
    
    /** comma-separated list of success-counts at which precedence is bumped*/
    final public static Key<String> INCREMENT_COUNTS = 
        Key.make("100,1000");
    
    /* (non-Javadoc)
     * @see org.archive.crawler.frontier.QueuePrecedencePolicy#queueCreated(org.archive.crawler.frontier.WorkQueue)
     */
    @Override
    public void queueCreated(WorkQueue wq) {
        wq.setPrecedence(wq.get(this,BASE_PRECEDENCE));
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.frontier.QueuePrecedencePolicy#queueReevaluate(org.archive.crawler.frontier.WorkQueue)
     */
    @SuppressWarnings("unchecked")
    @Override
    public void queueReevaluate(WorkQueue wq) {
        // FIXME: it's ridiculously inefficient to do this every time, 
        // and optimizing will probably require inserting stateful policy 
        // helper object into WorkQueue -- expected when URI-precedence is
        // also supported
        int precedence = wq.get(this,BASE_PRECEDENCE) - 1;
        Collection<Integer> increments = CollectionUtils.collect(
                Arrays.asList(wq.get(this,INCREMENT_COUNTS).split(",")),
                new Transformer() {
                    public Object transform(final Object string) {
                        return Integer.parseInt((String)string);
                    }});
        Iterator<Integer> iter = increments.iterator();
        int increment = iter.next(); 
        long successes = wq.getSubstats().getFetchSuccesses();
        while(successes>0) {
            successes -= increment;
            precedence++;
            increment = iter.hasNext() ? iter.next() : increment; 
        }
        wq.setPrecedence(precedence);
    }

    // good to keep at end of source: must run after all per-Key
    // initialization values are set.
    static {
        KeyManager.addKeys(SuccessCountsQueuePrecedencePolicy.class);
    }
}
