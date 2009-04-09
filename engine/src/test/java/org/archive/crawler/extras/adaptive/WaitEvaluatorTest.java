/* 
 * Copyright (C) 2007 Internet Archive.
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
 * WaitEvaluator.java
 *
 * Created on Jan 31, 2007
 *
 * $Id:$
 */
package org.archive.crawler.extras.adaptive;


import org.archive.crawler.extras.adaptive.WaitEvaluator;
import org.archive.modules.ProcessorTestBase;


/**
 * Unit test for {@link WaitEvaluator}.
 *
 * @author pjack
 */
public class WaitEvaluatorTest extends ProcessorTestBase {

    
    @Override
    protected Class getModuleClass() {
        return WaitEvaluator.class;
    }
    
    
    @Override
    protected Object makeModule() {
        return new WaitEvaluator();
    }
    
    
    // TODO TESTME!
    
}