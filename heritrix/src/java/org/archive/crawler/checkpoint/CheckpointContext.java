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
import java.text.DecimalFormat;
import java.util.LinkedList;
import java.util.List;

import org.archive.util.ArchiveUtils;

/**
 * Captures the checkpoint history and upcoming checkpoint name
 * of a crawl.
 *
 * A checkpoint name is a series number, starting from 1,
 * typically zero-padded to 5 places.
 *
 * @author gojomo
 *
 */
public class CheckpointContext implements Serializable {
    /** String to prefix any new checkpoint names */
    private final String checkpointPrefix;
    /** Next  overall series checkpoint number */
    private int nextCheckpoint = 1;

    /** All checkpoint names in chain prior to now. May not all still
     * exist on disk */
    private List predecessorCheckpoints = new LinkedList();

    /** Directory to place checkpoints */
    private File checkpointDirectory = null;

    /** If a checkpoint has begun, its directory */
    private transient File checkpointInProgress = null;

    /** If the checkpoint in progress has encountered fatal errors */
    private boolean checkpointErrors = false;

    /**
     * @return Returns the nextCheckpoint.
     */
    public int getNextCheckpoint() {
        return nextCheckpoint;
    }

    /**
     * Create a new CheckpointContext with the given store directory
     * @param checkpointDirectory Where to store checkpoint.
     */
    public CheckpointContext(File checkpointDirectory) {
        super();
        this.checkpointDirectory = checkpointDirectory;
        this.checkpointPrefix = "";
    }
    
    /**
     * Create a new CheckpointContext with the given store directory
     *
     * @param checkpointDirectory Where to store checkpoint.
     * @param prefix Prefix for checkpoint label.
     */
    public CheckpointContext(File checkpointDirectory, String prefix) {
        super();
        this.checkpointDirectory = checkpointDirectory;
        this.checkpointPrefix = prefix;
    }

    public void begin() {
        checkpointInProgress =
            new File(checkpointDirectory, nextCheckpointDirectoryName());
        checkpointErrors = false;
    }

    /**
     * @return next checkpoint, as a zero-padding string
     */
    private String nextCheckpointDirectoryName() {
        return this.checkpointPrefix + (new DecimalFormat("00000")).format(nextCheckpoint);
    }

    public void end() {
        if(checkpointErrors == false) {
            writeValidity();
        }
        checkpointInProgress = null;
        nextCheckpoint++;
    }

    private void writeValidity() {
        File valid = new File(checkpointInProgress, "valid");
        try {
            FileOutputStream fos = new FileOutputStream(valid);
            fos.write(ArchiveUtils.get14DigitDate().getBytes());
            fos.close();
        } catch (IOException e) {
            valid.delete();
        }
    }

    /**
     * @return Checkpoint directory.
     */
    public File getCheckpointInProgressDirectory() {
        return checkpointInProgress;
    }

    /**
     * Note that a checkpoint failed
     *
     * @param e Exception checkpoint failed on.
     */
    public void checkpointFailed(Exception e) {
        e.printStackTrace();
        checkpointErrors = true;
    }

    /**
     * @return Return whether this context is at a new crawl, never-
     * checkpointed state.
     */
    public boolean isAtBeginning() {
        return nextCheckpoint == 1;
    }

    /**
     * Advance the context to reflect a resume-from-checkpoint.
     */
    public void noteResumed() {
        // extend name
        // checkpointPrefix = nextHistoryName();
        // update counters
        nextCheckpoint += 1;
    }
    
    /**
     * @return Returns the predecessorCheckpoints.
     */
    public List getPredecessorCheckpoints() {
        return this.predecessorCheckpoints;
    }
}
