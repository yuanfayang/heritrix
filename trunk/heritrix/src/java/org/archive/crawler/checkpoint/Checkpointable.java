/*
 * Checkpointable
 *
 * $Id$
 *
 * Created on Jul 4, 2005
 *
 * Copyright (C) 2005 Internet Archive.
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
package org.archive.crawler.checkpoint;

import org.archive.crawler.framework.CrawlController;

/**
 * Implementors want to be called whenever a checkpoint is being run.
 * Implementors have state to checkpoint.
 * Implementors need to register themselves with CrawlController via
 * the {@link CrawlController#registerCheckpointable}.
 * @author stack
 */
public interface Checkpointable {
    public void recover(Checkpoint cp);
    
    /**
     * Called by {@link CrawlController} when checkpointing.
     * @param cp Current checkpoint instance.
     * @throws Exception A fatal exception.  Any exceptions
     * that are let out of this checkpoint are assumed fatal
     * and terminate further checkpoint processing.
     */
    public void checkpoint(Checkpoint cp) throws Exception;
}