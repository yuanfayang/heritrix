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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.LinkedList;
import java.util.List;

import org.archive.util.ArchiveUtils;

/**
 * Captures the checkpoint history and upcoming checkpoint name of a crawl.
 * Also has utility to help make checkpoint.  Maintains a directory named for
 * the checkpoint into which checkpointing modules can safely write.
 *
 * @author gojomo
 */
public class CheckpointContext
implements Serializable {
    private static final long serialVersionUID = -2339801205500280142L;

    /**
     * String to prefix any new checkpoint names.
     */
    private  String checkpointPrefix = "";
    
    /**
     * Next  overall series checkpoint number.
     */
    private int nextCheckpoint = 1;

    /**
     * All checkpoint names in chain prior to now. May not all still
     * exist on disk.
     */
    private List predecessorCheckpoints = new LinkedList();

    /**
     * Base directory to place checkpoints in.
     */
    private transient File baseCheckpointDirectory = null;

    /**
     * If a checkpoint has begun, its directory under
     * <code>checkpointDirectory</code>.
     */
    private transient File checkpointInProgressDir = null;

    /**
     * If the checkpoint in progress has encountered fatal errors.
     */
    private transient boolean checkpointErrors = false;
    

    private static final String SERIALIZED_CLASS_SUFFIX = ".serialized";

    /**
     * @return Returns the nextCheckpoint.
     */
    public int getNextCheckpoint() {
        return nextCheckpoint;
    }

    /**
     * Create a new CheckpointContext with the given store directory
     * @param checkpointDir Where to store checkpoint.
     */
    public CheckpointContext(File checkpointDir) {
        this(checkpointDir, "");
    }
    
    /**
     * Create a new CheckpointContext with the given store directory
     *
     * @param checkpointDir Where to store checkpoint.
     * @param prefix Prefix for checkpoint label.
     */
    public CheckpointContext(File checkpointDir, String prefix) {
        super();
        this.baseCheckpointDirectory = checkpointDir;
        this.checkpointPrefix = prefix;
    }

    public void begin() {
        this.checkpointInProgressDir = new File(baseCheckpointDirectory,
            getNextCheckpointName());
        // Clear the checkpoint errors.
        checkpointErrors = false;
    }

    /**
     * @return next checkpoint name (zero-padding string).
     */
    public String getNextCheckpointName() {
        return this.checkpointPrefix +
            (new DecimalFormat("00000")).format(nextCheckpoint);
    }

    public void end() {
        if(checkpointErrors == false) {
            writeValidity();
        }
        this.checkpointInProgressDir = null;
        this.nextCheckpoint++;
    }

    private void writeValidity() {
        File valid = new File(this.checkpointInProgressDir,
            Checkpoint.VALIDITY_STAMP_FILENAME);
        try {
            FileOutputStream fos = new FileOutputStream(valid);
            fos.write(ArchiveUtils.get14DigitDate().getBytes());
            fos.close();
        } catch (IOException e) {
            valid.delete();
        }
    }

    /**
     * @return Checkpoint directory. Name of the directory is the name of this
     * current checkpoint.
     */
    public File getCheckpointInProgressDirectory() {
        return this.checkpointInProgressDir;
    }
    
    /**
     * @return True if a checkpoint is in progress.
     */
    public boolean isCheckpointInProgress() {
        return this.checkpointInProgressDir != null;
    }

    /**
     * Note that a checkpoint failed
     *
     * @param e Exception checkpoint failed on.
     */
    public void checkpointFailed(Exception e) {
        e.printStackTrace();
        this.checkpointErrors = true;
    }
    
    /**
     * @return True if current/last checkpoint failed.
     */
    public boolean isCheckpointFailed() {
        return this.checkpointErrors;
    }

    /**
     * @return Return whether this context is at a new crawl, never-
     * checkpointed state.
     */
    public boolean isAtBeginning() {
        return nextCheckpoint == 1;
    }

    /**
     * Call when recovering a checkpoint.
     * Call this after instance has been revivifyied post-serialization to
     * amend counters and directories that effect where checkpoints get stored
     * from here on out.
     * @param newdir New base dir to use future checkpointing.
     */
    public void recoveryAmendments(File newdir) {
        this.nextCheckpoint += 1;
        this.baseCheckpointDirectory = newdir;
        // Prepend the checkpoint name with a little 'r' so we tell apart
        // checkpoints made from a recovery.  Allow for there being
        // multiple 'r' prefixes.
        this.checkpointPrefix += ('r' + this.checkpointPrefix);
    }
    
    /**
     * @return Returns the predecessorCheckpoints.
     */
    public List getPredecessorCheckpoints() {
        return this.predecessorCheckpoints;
    }
    
    public static File getBdbSubDirectory(File checkpointDir) {
        return new File(checkpointDir, "bdbje-logs");
    }
    
    /**
     * @return Instance of filename filter that will let through files ending
     * in '.jdb' (i.e. bdb je log files).
     */
    public static FilenameFilter getJeLogsFilter() {
        return new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name != null && name.toLowerCase().endsWith(".jdb");
            }
        };
    }
    
    public static File getClassCheckpointFile(File checkpointDir, Class c) {
        return new File(checkpointDir, getClassCheckpointFilename(c));
    }
    
    public static String getClassCheckpointFilename(Class c) {
        return c.getName() + SERIALIZED_CLASS_SUFFIX;
    }
    
    /**
     * Utility function to serialize an object to a file in current checkpoint
     * dir. Facilities
     * to store related files alongside the serialized object in a directory
     * named with a <code>.auxillary</code> suffix.
     *
     * @param o Object to serialize.
     * @param dir Directory to serialize into.
     * @throws IOException
     */
    public static void writeObjectToFile(Object o, File dir)
    throws IOException {
        dir.mkdirs();
        ObjectOutputStream out = new ObjectOutputStream(
            new FileOutputStream(getClassCheckpointFile(dir, o.getClass())));
        try {
            out.writeObject(o);
        } finally {
            out.close();
        }
    }
    
    public static Object readObjectFromFile(Class c, File dir)
    throws FileNotFoundException, IOException, ClassNotFoundException {
        ObjectInputStream in = new ObjectInputStream(
            new FileInputStream(getClassCheckpointFile(dir, c)));
        Object o = null;
        try {
            o = in.readObject();
        } finally {
            in.close();
        }
        return o;
    }
}
