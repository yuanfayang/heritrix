/* CheckpointContext
*
* $Id$
*
* Created on Apr 19, 2004
*
* Copyright (C) 2004 Internet Archive.
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import org.archive.util.ArchiveUtils;

/**
 * Captures the checkpoint history and upcoming checkpoint name
 * of a crawl. 
 * 
 * Checkpoints are named according to the pseudo-pattern
 * "([plus-sign][checkpoint-number])+". A resume from a checkpoint
 * starts a new acsending number appended to its origin. So for
 * example, a crawl which checkpoints 5 times while running to 
 * completion would have checkpoints:
 * 
 *   +1
 *   +2
 *   +3
 *   +4
 *   +5
 * 
 * A resume from checkpoint +3 which then ran to completion, 
 * with two new checkpoints, would consist of the above
 * checkpoints '+1','+2','+3' and then new checkpoints:
 * 
 *   +3+1
 *   +3+2
 * 
 * In this way, checkpoint names within a single crawl need
 * never conflict. 
 * 
 * @author gojomo
 *
 */
public class CheckpointContext implements Serializable {
    /** String to prefix any new checkpoint names (essentially, name
     * of checkpoint that was resumed to create current context) */
    protected String checkpointPrefix = "";
    /** Next checkpoint series number */
    protected int nextCheckpoint = 1;
    /** All checkpoint names in chain prior to now. May not all still 
     * exist on disk */
    protected List predecessorCheckpoints = new LinkedList();
    
    /** directory to place checkpoints */
    protected File checkpointDirectory = null;
    
    /** if a checkpoint has begun, its directory */
    transient protected File checkpointInProgress = null;
    
    /** if the checkpoint in progress has encountered fatal errors */
    private boolean checkpointErrors = false;
        
    /**
     * Create a new CheckpointContext with the given store directory
     * 
     * @param checkpointDirectory
     */
    public CheckpointContext(File checkpointDirectory) {
        super();
        this.checkpointDirectory = checkpointDirectory;
    }

    /**
     * 
     */
    public void begin() {
        checkpointInProgress = new File(checkpointDirectory, nextName());
        checkpointErrors = false;
    }

    /**
     * @return
     */
    private String nextName() {
        return checkpointPrefix + "+" + nextCheckpoint;
    }

    /**
     * 
     */
    public void end() {
        if(checkpointErrors == false) {
            writeValidity();
        }
        checkpointInProgress = null;
        nextCheckpoint++;
    }

    private void writeValidity() {
        File valid = new File(checkpointInProgress,"valid");
        try {
            FileOutputStream fos = new FileOutputStream(valid);
            fos.write(ArchiveUtils.get14DigitDate().getBytes());
            fos.close();
        } catch (IOException e) {
            valid.delete();
        }
    }

    /**
     * @return
     */
    public File getCheckpointInProgressDirectory() {
        return checkpointInProgress;
    }

    /**
     * Note that a checkpoint failed
     * 
     * @param e
     */
    public void checkpointFailed(IOException e) {
        e.printStackTrace();
        checkpointErrors = true;
    }
    
    /**
     * Return whether this context is at a new crawl, never-
     * checkpointed state.
     * 
     * @return
     */
    public boolean isAtBeginning() {
        return checkpointPrefix.length() == 0;
    }

    /**
     * Advance thne context to reflect a resume-from-checkpoint.
     */
    public void noteResumed() {
        // extend name
        checkpointPrefix = nextName();
        // reset counter
        nextCheckpoint = 1;
    }
}
