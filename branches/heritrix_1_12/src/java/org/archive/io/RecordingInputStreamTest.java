/* RecordingInputStreamTest
 *
 * $Id$
 *
 * Created on Aug 1, 2005
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
package org.archive.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import org.archive.util.TmpDirTestCase;


/**
 * Test cases for RecordingInputStream.
 *
 * @author gojomo
 */
public class RecordingInputStreamTest extends TmpDirTestCase
{


    /*
     * @see TmpDirTestCase#setUp()
     */
    protected void setUp() throws Exception
    {
        super.setUp();
    }

    /**
     * Test readFullyOrUntil soft (no exception) and hard (exception) 
     * length cutoffs.
     * 
     * @throws IOException
     * @throws InterruptedException
     * @throws RecorderTimeoutException
     */
    public void testReadFullyOrUntil() throws RecorderTimeoutException, IOException, InterruptedException
    {
        RecordingInputStream ris = new RecordingInputStream(16384, (new File(
                getTmpDir(), "testReadFullyOrUntil").getAbsolutePath()));
        ByteArrayInputStream bais = new ByteArrayInputStream(
                "abcdefghijklmnopqrstuvwxyz".getBytes());
        // test soft max
        ris.open(bais);
        ris.readFullyOrUntil(7,10,0,0);
        ris.close();
        ReplayInputStream res = ris.getReplayInputStream();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        res.readFullyTo(baos);
        assertEquals("soft max cutoff","abcdefg",new String(baos.toByteArray()));
        // test hard max
        bais.reset();
        baos.reset();
        ris.open(bais);
        boolean exceptionThrown = false; 
        try {
            ris.readFullyOrUntil(13,10,0,0);
        } catch (RecorderLengthExceededException ex) {
            exceptionThrown = true;
        }
        assertTrue("hard max exception",exceptionThrown);
        ris.close();
        res = ris.getReplayInputStream();
        res.readFullyTo(baos);
        assertEquals("hard max cutoff","abcdefghijk",
                new String(baos.toByteArray()));
        // TODO: test timeout?  
    }
}
