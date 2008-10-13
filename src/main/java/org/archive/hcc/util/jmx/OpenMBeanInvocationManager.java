
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

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import javax.management.MBeanException;
import javax.management.ReflectionException;
import javax.management.openmbean.OpenMBeanOperationInfo;

public class OpenMBeanInvocationManager {
    private List<MBeanOperation> operations = new ArrayList<MBeanOperation>();

    private OpenMBeanOperationInfo[] info = new OpenMBeanOperationInfo[0];

    public Object invoke(String actionName, Object[] params, String[] signature)
            throws MBeanException,
            ReflectionException {
        // get the operation
        MBeanOperation operation = getOperation(actionName, params, signature);

        if (operation != null) {
            MBeanInvocation invocation = operation.createInvocation(
                    params,
                    signature);
            return invocation.invoke();
        }

        throw new ReflectionException(new InvocationTargetException(
                null,
                "no operation found with name and " + "signature: actionName="
                        + actionName + "; params=" + params + "; signature"));

    }

    protected MBeanOperation getOperation(
            String actionName,
            Object[] params,
            String[] signature) throws ReflectionException {

        for (MBeanOperation operation : operations) {
            if (operation.matches(actionName, signature)) {
                return operation;
            }

        }

        throw new ReflectionException(new InvocationTargetException(
                null,
                "The method with the specified signature was not found:"
                        + actionName + "; signature=" + signature));

    }

    public void addMBeanOperation(MBeanOperation operation) {
        this.operations.add(operation);
        setInfo();
    }

    public OpenMBeanOperationInfo[] getInfo() {
        return this.info;
    }

    private void setInfo() {
        OpenMBeanOperationInfo[] newInfo = new OpenMBeanOperationInfo[this.operations
                .size()];
        for (int i = 0; i < newInfo.length; i++) {
            newInfo[i] = this.operations.get(i).getInfo();
        }

        this.info = newInfo;
    }
}
