/* URIWorkQueueStateComparator
*
* $Id$
*
* Created on May 24, 2004
*
* Copyright (C) 2004 Internet Archive.
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
import java.util.Comparator;
import java.util.List;



/**
 * Convenience comparator for sorting URIWorkQueues by their
 * current state.
 * 
 * @author gojomo
 *
 */
public class URIWorkQueueStateComparator implements Comparator {
    Object statesArray[] = {
            URIWorkQueue.READY,
            URIWorkQueue.BUSY,
            URIWorkQueue.SNOOZED,
            URIWorkQueue.EMPTY,
            URIWorkQueue.FROZEN,
            URIWorkQueue.INACTIVE,
            URIWorkQueue.DISCARDED};
    List states = Arrays.asList(statesArray);
    
    /* (non-Javadoc)
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    public int compare(Object o1, Object o2) {
        int v1 = valueFor(o1);
        int v2 = valueFor(o2);
        return v1-v2;
    }

    /**
     * @param o1
     * @return
     */
    private int valueFor(Object o1) {
        Object state = ((URIWorkQueue)o1).getState();
        return states.indexOf(state);
    }

}
