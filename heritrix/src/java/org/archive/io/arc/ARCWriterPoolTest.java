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

import java.util.Date;
import java.util.NoSuchElementException;

import org.archive.util.TmpDirTestCase;


/**
 * Test ARCWriterPool
 */
public class ARCWriterPoolTest extends TmpDirTestCase
{
    /*
     * Class to test for void ARCWriterPool(File, int, int)
     */
    public void testARCWriterPool()
        throws Exception
    {
        final int MAX_ACTIVE = 3;
        final int MAX_WAIT_MILLISECONDS = 100;
        ARCWriterPool pool = new ARCWriterPool(getTmpDir(), "TEST", 
            true, MAX_ACTIVE, MAX_WAIT_MILLISECONDS);
        ARCWriter [] writers = new ARCWriter[MAX_ACTIVE];
        for (int i = 0; i < MAX_ACTIVE; i++)
        {
            writers[i] = pool.borrowARCWriter();
            assertEquals("Number active", i + 1, pool.getNumActive());
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
            if ((end - start) > (MAX_WAIT_MILLISECONDS * 2))
            {
                fail("More than " + MAX_WAIT_MILLISECONDS + " elapsed.");
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
