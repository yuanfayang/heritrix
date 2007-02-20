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
 * CrawlStatusAdapter.java
 *
 * Created on Feb 20, 2007
 *
 * $Id:$
 */

package org.archive.crawler.event;

import java.io.File;

import org.archive.state.StateProvider;

/**
 * @author pjack
 *
 */
public class CrawlStatusAdapter implements CrawlStatusListener {

    public void crawlCheckpoint(StateProvider provider, File checkpointDir) 
    throws Exception {
    }

    public void crawlEnded(String sExitMessage) {
    }

    public void crawlEnding(String sExitMessage) {
    }

    public void crawlPaused(String statusMessage) {
    }

    public void crawlPausing(String statusMessage) {
    }

    public void crawlResuming(String statusMessage) {
    }

    public void crawlStarted(String message) {
    }

}
