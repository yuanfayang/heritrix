/* ARCWriterPoolTest
 *
 * $Id$
 *
 * Created on Jan 22, 2004
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
package org.archive.io.arc;

import java.io.ByteArrayOutputStream;
import java.util.Date;
import java.util.NoSuchElementException;

import org.archive.util.TmpDirTestCase;


/**
 * Test ARCWriterPool
 */
public class ARCWriterPoolTest extends TmpDirTestCase {
    /*
     * Class to test for void ARCWriterPool(File, int, int)
     */
    public void testARCWriterPool()
        throws Exception
    {
        final int MAX_ACTIVE = 3;
        final int MAX_WAIT_MILLISECONDS = 100;
        cleanUpOldFiles("TEST");
        ARCWriterPool pool = new ARCWriterPool(getTmpDir(), "TEST",
            true, ARCConstants.DEFAULT_MAX_ARC_FILE_SIZE,
            MAX_ACTIVE, MAX_WAIT_MILLISECONDS);
        ARCWriter [] writers = new ARCWriter[MAX_ACTIVE];
        final String CONTENT = "Any old content";
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(CONTENT.getBytes());
        for (int i = 0; i < MAX_ACTIVE; i++)
        {
            writers[i] = pool.borrowARCWriter();
            assertEquals("Number active", i + 1, pool.getNumActive());
            writers[i].write("http://one.two.three", "no-type", "0.0.0.0",
                1234567890, CONTENT.length(), baos);
        }

        // Pool is maxed out.  Try and get a new ARCWriter.  We'll block for
        // MAX_WAIT_MILLISECONDS.  Should get exception.
        long start = (new Date()).getTime();
        boolean isException = false;
        try
        {
            pool.borrowARCWriter();
        }
        catch(NoSuchElementException e)
        {
            isException = true;
            long end = (new Date()).getTime();
            // This test can fail on a loaded machine if the wait period is
            // only MAX_WAIT_MILLISECONDS.  Up the time to wait.
            final int WAIT = MAX_WAIT_MILLISECONDS * 100;
            if ((end - start) > (WAIT))
            {
                fail("More than " + MAX_WAIT_MILLISECONDS + " elapsed: "
                    + WAIT);
            }
        }
        assertTrue("Did not get NoSuchElementException", isException);

        for (int i = (MAX_ACTIVE - 1); i >= 0; i--)
        {
            pool.returnARCWriter(writers[i]);
            assertEquals("Number active", i, pool.getNumActive());
            assertEquals("Number idle", MAX_ACTIVE - pool.getNumActive(),
                    pool.getNumIdle());
        }
    }
}
