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
 * KeyedItem.java
 * Created on Jun 3, 2003
 *
 * $Header$
 */
package org.archive.crawler.basic;

/**
 * Represents objects with a lifecycle, including snoozed times,
 * inside the Frontier: individual CandidateURIs and queues.
 *
 * @author gojomo
 *
 */
public interface URIStoreable {

    public static final Object FORGOTTEN = "FORGOTTEN".intern();
    public static final Object FINISHED = "FINISHED".intern();;
    public static final Object HELD = "HELD".intern();
    public static final Object IN_PROCESS = "IN_PROCESS".intern();
    public static final Object PENDING = "PENDING".intern();
    public static final Object READY = "READY".intern();
    public static final Object EMPTY = "EMPTY".intern();
    public static final Object SNOOZED = "SNOOZED".intern();

    String getClassKey();

    Object getStoreState();

    void setStoreState(Object s);

    long getWakeTime();

    void setWakeTime(long w);

    /**
     * a fallback string to use when wake times are equal
     * @return String
     */
    String getSortFallback();
}
