/* RecordingOutputStreamTest
 * 
 * $Id$
 * 
 * Created on Jan 21, 2004
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
package org.archive.io;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.archive.util.TmpDirTestCase;

/**
 * 
 * 
 * @author stack
 */
public class RecordingOutputStreamTest extends TmpDirTestCase
{

    /*
     * @see TmpDirTestCase#setUp()
     */
    protected void setUp() throws Exception
    {
        super.setUp();
    }

    /**
     * Method to test for void write(int).
     * 
     * Uses small buffer size and small total size to test limits.
     * 
     * @throws IOException Failed open of backing file.
     */
    public void testWriteint() throws IOException
    {
        final int BUFFER_SIZE = 16;
        final int TOTAL_SIZE = 48;
        RecordingOutputStream ros = new RecordingOutputStream(BUFFER_SIZE, 
            (new File(getTmpDir(), "testWriteintBkng.txt")).getAbsolutePath(),
                TOTAL_SIZE);
        FileOutputStream fos
           = new FileOutputStream(new File(getTmpDir(), "testWriteintRrd.txt"));
        ros.open(fos);
        
        // Fill buffer.
        for (int i = 0; i < BUFFER_SIZE; i++)
        {
            ros.write(i + '0' /*Put ascii rather than literal in buffer*/);
        }
        
        // Overflow buffer.
        for (int i = 0; i < BUFFER_SIZE; i++)
        {
            ros.write(i + '0' /*Put ascii rather than literal in buffer*/);
        }
        
        // Overwrite.
        for (int i = 0; i < TOTAL_SIZE; i++)
        {
            ros.write(i + '0' /*Put ascii rather than literal in buffer*/);
        }
        
        ros.close();
        fos.close();
    }
    
    
}
