/* Checkpoint
*
* $Id$
*
* Created on Apr 25, 2004
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
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

import org.archive.util.FileUtils;

/**
 * Record of a specific checkpoint on disk.
 * Used recovering from a checkpoint or displaying list of checkpoints done
 * so far.
 * @author gojomo
 */
public class Checkpoint implements Serializable {
    private static final long serialVersionUID = 8196988571190409200L;

    /**
     * Flag label for invalid Checkpoints
     */
    private static final String INVALID = "INVALID";
    
    /** 
     * Name of file written with timestamp into valid checkpoints.
     */
    static final String VALIDITY_STAMP_FILENAME = "valid";
    
    
    private transient String timestamp;
    private File directory;
    
    /**
     * Publically inaccessible default constructor.
     */
    protected Checkpoint() {
        super();
    }

    /**
     * Create a Checkpoint instance based on the given prexisting
     * checkpoint directory
     *
     * @param checkpointDir Directory that holds checkpoint.
     */
    public Checkpoint(File checkpointDir) {
        this.directory = checkpointDir;
        readValid();
    }
    
    private void readObject(ObjectInputStream s)
    throws IOException, ClassNotFoundException {
        s.defaultReadObject();
        readValid();
    }
    
    protected void readValid() {
        File validityStamp = new File(this.directory,
            VALIDITY_STAMP_FILENAME);
        if (validityStamp.exists() == false) {
            this.timestamp = INVALID;
        } else {
            try {
                this.timestamp = FileUtils.readFileAsString(validityStamp).
                    trim();
            } catch (IOException e) {
                e.printStackTrace();
                this.timestamp = INVALID;
            }
        }
    }

    /**
     * @return Return true if this checkpoint appears complete/resumable
     * (has 'valid' stamp file).
     */
    public boolean isValid() {
        return timestamp != INVALID;
    }

    /**
     * @return Returns name of this Checkpoint
     */
    public String getName() {
        return this.directory.getName();
    }

    /**
     * @return Return the combination of given name and timestamp most commonly
     * used in administrative interface.
     */
    public String getDisplayName() {
        return getName() + " [" + getTimestamp() + "]";
    }

    /**
     * @return Returns the timestamp.
     */
    public String getTimestamp() {
        return timestamp;
    }

    /**
     * @return Returns the checkpoint directory.
     */
    public File getDirectory() {
        return this.directory;
    }
}