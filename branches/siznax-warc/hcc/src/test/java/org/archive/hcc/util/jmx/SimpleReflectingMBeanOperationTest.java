/* SimpleReflectingMBeanOperationTest
* 
* $Id$
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
import javax.management.ReflectionException;

import org.archive.hcc.util.jmx.SimpleReflectingMBeanOperation;
 
public class SimpleReflectingMBeanOperationTest
        extends
            MBeanTestCase {

    public void test(){
        SimpleReflectingMBeanOperation op = 
            new SimpleReflectingMBeanOperation(this, 
                    createInfo());
        
        try {
            String returnStr = (String)op.getInvocation(new Object[0]).invoke();
            assertEquals("test", returnStr);
        } catch (MBeanException e) {
            assertFalse(true);
            e.printStackTrace();
        } catch (ReflectionException e) {
            assertFalse(true);
            e.printStackTrace();
        }
        
    }
    
    public String invoke(){
        return "test";
    }
}
