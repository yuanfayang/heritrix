/* Copyright (C) 2003 Internet Archive.
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
 * SchedulingComparator.java
 * Created on Jul 17, 2003
 *
 * $Header$
 */
package org.archive.crawler.basic;

import java.util.Comparator;

/**
 *
 * @author Gordon Mohr
 */
public class SchedulingComparator implements Comparator {

    /* (non-Javadoc)
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    public int compare(Object o1, Object o2) {
        if(o1==o2) {
            return 0; // for exact identity only
        }
        if (((KeyedQueue)o1).getWakeTime() > ((KeyedQueue)o2).getWakeTime()) {
            return 1;
        }
        if (((KeyedQueue)o1).getWakeTime() < ((KeyedQueue)o2).getWakeTime()) {
            return -1;
        }
        // at this point, the ordering is arbitrary, but still
        // must be consistent/stable over time

        return ((KeyedQueue)o1).getSortFallback().compareTo(((KeyedQueue)o2).getSortFallback());
    }

}
