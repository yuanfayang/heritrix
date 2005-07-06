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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;

import org.archive.io.ObjectPlusFilesInputStream;
import org.archive.io.ObjectPlusFilesOutputStream;
import org.archive.util.FileUtils;

/**
 * Record of a specific checkpoint on disk.
 * @author gojomo
 */
public class Checkpoint {
    /**
     * Flag label for invalid Checkpoints
     */
    private static final String INVALID = "INVALID";
    
    /** 
     * Name of file written with timestamp into valid checkpoints.
     */
    static final String VALIDITY_STAMP_FILENAME = "valid";
    
    private String timestamp;
    private File directory;

    private static final String AUX_SUFFIX = ".auxillary";
    
    private int arcWriterSerialNo = 0;
    
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
     * @param directory Directory that holds checkpoint.
     */
    public Checkpoint(File directory) {
        this.directory = directory;
        File validityStamp = new File(directory, VALIDITY_STAMP_FILENAME);
        if (validityStamp.exists() == false) {
            timestamp = INVALID;
        } else {
            try {
                timestamp = FileUtils.readFileAsString(validityStamp).trim();
            } catch (IOException e) {
                e.printStackTrace();
                timestamp = INVALID;
            }
        }
    }

    /**
     * 
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
     * Utility function to serialize an object to a file, using
     * the enhanced ObjectPlusFilesOutputStream, which offers facilities
     * to store related files alongside the serialized object in a directory
     * named with a <code>.auxillary</code> suffix.
     *
     * @param o Object to serialize.
     * @param filename Name to serialize to.
     * @throws IOException
     */
    public void writeObjectPlusToFile(Object o, String filename)
    throws FileNotFoundException, IOException {
        this.directory.mkdirs();
        ObjectPlusFilesOutputStream out =
            new ObjectPlusFilesOutputStream(
                new FileOutputStream(new File(this.directory, filename)), 
                    new File(this.directory, filename + AUX_SUFFIX));
        try {
            out.writeObject(o);
        } finally {
            out.close();
        }
    }

    /**
     * Utility function to deserialize an object, plus any
     * related files stored alongside it, from a file and
     * accompanying directory structure.
     *
     * @param filename
     * @throws IOException
     * @throws ClassNotFoundException
     * @return Object read from file.
     */
    public Object readObjectFromFile(String filename)
    throws IOException, ClassNotFoundException {
        ObjectPlusFilesInputStream in =
            new ObjectPlusFilesInputStream(
                    new FileInputStream(
                            new File(this.directory,filename)),
                    new File(this.directory, filename + AUX_SUFFIX));
        Object o = null;
        try {
            o = in.readObject();
        } finally {
            in.close();
        }
        return o;
    }

    /**
     * @return Returns the checkpoint directory.
     */
    public File getDirectory() {
        return this.directory;
    }

    public File getBdbSubDirectory() {
        return new File(getDirectory(), "bdbje-logs");
    }
    
    public FilenameFilter getJeLogsFilter() {
        return new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name != null && name.toLowerCase().endsWith(".jdb");
            }
        };
    }

    /**
     * @return Returns ARCWriter serial number stored into this checkpoint.
     */
    public int getArcWriterSerialNo() {
        return this.arcWriterSerialNo;
    }

    /**
     * Save in here the ARCWriter serial number for restoration on checkpoint
     * recover.
     * Save serialno here because ARCWriter is behind ARCPool which is held by
     * ARCWriterProcessor -- serializing ARCWriter, a non-processor, would
     * be awkward.
     * @param arcWriterSerialNo
     */
    public void setArcWriterSerialNo(int arcWriterSerialNo) {
        this.arcWriterSerialNo = arcWriterSerialNo;
    }
}