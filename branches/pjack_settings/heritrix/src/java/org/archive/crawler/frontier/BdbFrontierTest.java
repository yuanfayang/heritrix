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
 * AdaptiveRevisitFrontierTest.java
 *
 * Created on Feb 5, 2007
 *
 * $Id:$
 */

package org.archive.crawler.frontier;

import org.archive.crawler.framework.CrawlerProcessorTestBase;
import org.archive.crawler.util.BloomUriUniqFilter;
import org.archive.crawler.util.NoopUriUniqFilter;


/**
 * @author pjack
 *
 */
public class BdbFrontierTest extends CrawlerProcessorTestBase {

    
    @Override
    protected Class getModuleClass() {
        return BdbFrontier.class;
    }
    
    
    @Override
    protected Object makeModule() throws Exception {
        return new BdbFrontier(controller, new BucketQueueAssignmentPolicy(), 
                new NoopUriUniqFilter());
    }


    // TODO TESTME

}

