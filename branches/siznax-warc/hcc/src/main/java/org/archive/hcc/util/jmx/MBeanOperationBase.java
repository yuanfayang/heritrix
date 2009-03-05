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

import javax.management.MBeanException;
import javax.management.MBeanParameterInfo;
import javax.management.ReflectionException;
import javax.management.openmbean.OpenMBeanOperationInfo;

public abstract class MBeanOperationBase implements
        MBeanOperation {

    private OpenMBeanOperationInfo info;

    public MBeanOperationBase(OpenMBeanOperationInfo info) {
        this.info = info;
    }

    public OpenMBeanOperationInfo getInfo() {
        return this.info;
    }

    public boolean matches(String name, String[] signature) {
        if (!name.equals(this.info.getName())) {
            return false;
        }
        MBeanParameterInfo[] infoSignature = this.info.getSignature();
        if (signature.length != infoSignature.length) {
            return false;
        }

        for (int i = 0; i < signature.length; i++) {
            boolean namesMatch = signature[i]
                    .equals(infoSignature[i].getType());
            if (!namesMatch) {
                return false;
            }
        }

        return true;

    }

    public boolean validate(String name, Object[] parameters, String[] signature) {
        if (!name.equals(this.info.getName())) {
            return false;
        }
        MBeanParameterInfo[] infoSignature = this.info.getSignature();
        if (signature.length != infoSignature.length) {
            return false;
        }

        for (int i = 0; i < signature.length; i++) {
            boolean namesMatch = signature[i]
                    .equals(infoSignature[i].getName());
            boolean typesMatch = parameters[i].getClass().getName().equals(
                    infoSignature[i].getType());

            if (!namesMatch || !typesMatch) {
                return false;
            }
        }

        return true;

    }

    public MBeanInvocation createInvocation(
            Object[] parameters,
            String[] signature) throws MBeanException, ReflectionException {

        return getInvocation(parameters);
    }

    protected abstract MBeanInvocation getInvocation(Object[] params);

}
