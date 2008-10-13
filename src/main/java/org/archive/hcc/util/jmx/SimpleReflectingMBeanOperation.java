/* $Id$
 *
 * (Created on Dec 12, 2005
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

package org.archive.hcc.util.jmx;

import javax.management.openmbean.OpenMBeanOperationInfo;

/**
 * This class binds an open mbean operation definition to a specific invocation
 * target. It saves the user from having to write the code to marshall and
 * unmarshal simple open mbean invocations.
 * 
 * @author Daniel Bernstein (dbernstein@archive.org)
 * 
 */
public class SimpleReflectingMBeanOperation
        extends
            MBeanOperationBase {

    /**
     * The invocation target.
     */
    private Object target;

    /**
     * @param info
     */
    public SimpleReflectingMBeanOperation(
            Object target,
            OpenMBeanOperationInfo info) {
        super(info);
        this.target = target;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.archive.hcc.util.jmx.MBeanOperationBase#getInvocation(java.lang.Object[])
     */
    public MBeanInvocation getInvocation(Object[] params) {
        return new SimpleReflectingMbeanInvocation(
                this.target,
                getInfo(),
                params);
    }

}
