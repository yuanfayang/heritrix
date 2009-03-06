/* ExceptionProcessor
 * 
 * Created on Oct 30, 2008
 *
 * Copyright (C) 2008 Internet Archive.
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
package org.archive.crawler.processor;

import org.archive.modules.Processor;
import org.archive.modules.ProcessorURI;

/**
 * Throws a RuntimeException for every URI which contains the string 
 * "WARNING" -- for testing exception catching/reporting/alerting
 * 
 * @author gojomo
 * @version $Date: 2008-06-26 16:46:48 -0700 (Thu, 26 Jun 2008) $, $Revision: 5841 $
 */
public class ExceptionProcessor extends Processor {
    private static final long serialVersionUID = 1L;

    @Override
    protected void innerProcess(ProcessorURI uri) throws InterruptedException {
        if(uri.toString().contains("WARNING")) {
            throw new RuntimeException("ExceptionProcessor: "+uri.toString());
        }
    }

    @Override
    protected boolean shouldProcess(ProcessorURI uri) {
        return true;
    }
    

}