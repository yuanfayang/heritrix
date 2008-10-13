/* $Id$
 *
 * Created on Dec 12, 2005
 *
 * Copyright (C) 2005 Internet Archive.
 *  
 * This file is part of the Heritrix Cluster Controller (crawler.archive.org).
 *  
 * HCC is free software; you can redistribute it and/or modify
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
package org.archive.hcc.util;

import java.util.LinkedList;
import java.util.List;


public class Delegator {

    public enum DelegatorPolicy {
        ACCEPT_ALL, ACCEPT_FIRST
    };

    protected List<Delegatable> handlers = new LinkedList<Delegatable>();

    protected DelegatorPolicy policy;

    public Delegator() {
        this(DelegatorPolicy.ACCEPT_FIRST);
    }

    public Delegator(DelegatorPolicy policy) {
        this.policy = policy;
    }

    public boolean delegate(Object object) {
        boolean consumedAtLeastOne = false;
        for (Delegatable h : handlers) {
            boolean accepted = h.delegate(object);
            if (accepted) {
                consumedAtLeastOne = true;
            }
            if (accepted && policy == DelegatorPolicy.ACCEPT_FIRST) {
                return true;
            }
        }

        return consumedAtLeastOne;

    }

    public void addDelegatable(Delegatable handler) {
        this.handlers.add(handler);
    }
}