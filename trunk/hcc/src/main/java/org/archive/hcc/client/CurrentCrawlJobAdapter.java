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
package org.archive.hcc.client;

import java.util.Map;

public class CurrentCrawlJobAdapter implements CurrentCrawlJobListener {

    public CurrentCrawlJobAdapter() {
        super();
        // TODO Auto-generated constructor stub
    }

    public void crawlJobStarted(CurrentCrawlJob job) {
        // TODO Auto-generated method stub

    }

    public void crawlJobPaused(CurrentCrawlJob job) {
        // TODO Auto-generated method stub

    }

    public void crawlJobStopping(CurrentCrawlJob job) {
        // TODO Auto-generated method stub

    }

    public void crawlJobCompleted(CompletedCrawlJob job) {

    }

    public void statisticsChanged(CurrentCrawlJob job, Map<String,Object> statistics) {
        // TODO Auto-generated method stub

    }

    public void crawlJobResumed(CurrentCrawlJob job) {
        // TODO Auto-generated method stub

    }
}