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
import java.io.IOException;

import org.archive.util.FileUtils;

/**
 * Record of a specific checkpoint on disk.
 *
 * @author gojomo
 */
public class Checkpoint {
    /** flag label for invalid Checkpoints */
    public static final String INVALID = "INVALID";
    /** name of file written with timestamp into valid checkpoints */
    public static final String VALIDITY_STAMP_FILENAME = "valid";
    int seriesNumber;
    String name;
    String timestamp;
    File directory;

    /**
     * Create a Checkpoint instance based on the given prexisting
     * checkpoint directory
     *
     * @param directory
     */
    public Checkpoint(File directory) {
        this.directory = directory;
        name = directory.getName();
        seriesNumber = Integer.parseInt(name);
        File validityStamp = new File(directory,VALIDITY_STAMP_FILENAME);
        if(validityStamp.exists()==false) {
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

//    private void calculateSeriesFromHistoryName() {
//        String[] numbers = historyName.split("\\+");
//        int total = 0;
//        for (int i = 0; i<numbers.length; i++) {
//            if(numbers[i].length()>0) {
//                total += Integer.parseInt(numbers[i]);
//            }
//        }
//        seriesNumber = total;
//        seriesName = (new DecimalFormat("00000")).format(seriesNumber);
//    }

    /**
     * Return whether this checkpoint appears complete/resumable
     * (has 'valid' stamp file)
     * @return true if valid
     */
    public boolean isValid() {
        return timestamp != INVALID;
    }

    /**
     * Get the name of this Checkpoint (eg "+2+1")
     *
     * @return Returns the name.
     */
    public String getName() {
        return name;
    }

    /**
     * Return the combination of given name and timestamp most commonly
     * used in administrative interface.
     *
     * @return checkpoint's name with timestamp notation
     */
    public String getDisplayName() {
        return getName() + " ["+getTimestamp()+"]";
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
     * to store related files alongside the serialized object.
     *
     * @param o
     * @param filename
     * @throws FileNotFoundException
     * @throws IOException
     */
    public void writeObjectPlusToFile(Object o, String filename) throws FileNotFoundException, IOException {
        this.directory.mkdirs();
        File storeDir = new File(this.directory, filename+".aux");
        ObjectPlusFilesOutputStream out =
            new ObjectPlusFilesOutputStream(
                    new FileOutputStream(
                            new File(this.directory,filename)), storeDir);
        out.writeObject(o);
        out.close();
    }

    /**
     * Utility function to deserialize an object, plus any
     * related files stored alongside it, from a file and
     * accompanying directory structure.
     *
     * @param filename
     * @throws IOException
     * @throws ClassNotFoundException
     * @return
     */
    public Object readObjectFromFile(String filename) throws IOException, ClassNotFoundException {
        ObjectPlusFilesInputStream in =
            new ObjectPlusFilesInputStream(
                    new FileInputStream(
                            new File(this.directory,filename)),
                    new File(this.directory, filename+".aux"));
        Object o = null;
        try {
            o = in.readObject();
        } finally {
            in.close();
        }
        return o;
    }

}
