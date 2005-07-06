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
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.archive.util.FileUtils;

/**
 * Record of a specific checkpoint on disk.
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
    
    private static final String SERIALIZED_CLASS_SUFFIX = ".serialized";
    
    private transient String timestamp;
    private File directory;
    
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
    public void writeObjectToFile(Object o, String filename)
    throws FileNotFoundException, IOException {
        this.directory.mkdirs();
        ObjectOutputStream out = new ObjectOutputStream(
            new FileOutputStream(new File(this.directory, filename)));
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
        return readObjectFromFile(new File(this.directory, filename));
        
    }
    
    public static Object readObjectFromFile(File absolutePath)
    throws FileNotFoundException, IOException, ClassNotFoundException {
        ObjectInputStream in =
            new ObjectInputStream(new FileInputStream(absolutePath));
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
    
    public static String getClassCheckpointFilename(Class c) {
        return c.getName() + SERIALIZED_CLASS_SUFFIX;
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