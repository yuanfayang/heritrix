/* Copyright (C) 2003 Internet Archive.
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
 * utf-8 eth test : ð
 * 
 * ARCReader.java
 * Created on Sep 26, 2003
 *
 * $Header$
 */
package org.archive.io;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

/**
 * Utility class for reading ARC files, including .arc.gz
 * files. 
 * 
 * IN PROGRESS / THINKING OUT LOUD CODE
 * 
 * @author gojomo
 *
 */
public class ARCReader
{
    protected InputStream inStream;
    protected FileInputStream arcStream;
    protected ARCResource lastResource;
    protected int resourcePosition;
    protected long filePosition;
    
    /**
     * 
     */
    public ARCReader() {
        super();
    }

    public void open(String filename) throws IOException {
        String flattenedFilename = filename.toLowerCase();
        assert flattenedFilename.endsWith(".arc") || 
            flattenedFilename.endsWith(".arc.gz") : 
                "non-arc filename extension";
        arcStream = new FileInputStream(filename);
        inStream = new BufferedInputStream(arcStream,4096);
        if (flattenedFilename.endsWith(".gz")) {
            inStream = new GZIPInputStream(inStream);
        }
    }
    
    public ARCResource getNextResource() {
        return null;
    }
}
