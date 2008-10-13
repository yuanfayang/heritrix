/* MBeanTestCase
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

import javax.management.openmbean.OpenMBeanOperationInfoSupport;
import javax.management.openmbean.SimpleType;

import junit.framework.TestCase;
 
public abstract class MBeanTestCase extends TestCase{

    /**
     * 
     */
    public MBeanTestCase() {
        super();
        // TODO Auto-generated constructor stub
    }

    protected static OpenMBeanOperationInfoSupport createInfo(){
        return new OpenMBeanOperationInfoSupport(
                "invoke",
                "test operation",
                null,
                SimpleType.STRING,
                0);
    }
    
    public String invoke(){
        return "test";
    }
    
    
}
