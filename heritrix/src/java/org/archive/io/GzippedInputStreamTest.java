/* GzippedInputStreamTest
 * 
 * Created on May 4, 2005
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

import it.unimi.dsi.mg4j.io.RepositionableStream;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;

import org.archive.util.TmpDirTestCase;

/**
 * @author stack
 * @version $Date$, $Revision$
 */
public class GzippedInputStreamTest extends TmpDirTestCase {
    /**
     * Number of records in gzip member file.
     */
    final static int GZIPMEMBER_COUNT = 4;
    final static String TEXT = "Some old text to compress.";

    public static void main(String [] args) {
        junit.textui.TestRunner.run(GzippedInputStreamTest.class);
    }
    
    protected class RepositionableRandomAccessInputStream
    extends RandomAccessInputStream
    implements RepositionableStream {
        public RepositionableRandomAccessInputStream(File file)
        throws FileNotFoundException {
            super(file);
        }
    }

    protected File createMultiGzipMembers() throws IOException {
        // Make a file made up of gzipped members.
        final File compressedFile =
            new File(getTmpDir(), this.getClass().getName() + ".gz");
        OutputStream os =
            new BufferedOutputStream(new FileOutputStream(compressedFile));
        for (int i = 0; i < GZIPMEMBER_COUNT; i++) {
            os.write(GzippedInputStream.gzip(TEXT.getBytes()));
        }
        os.close();
        return compressedFile;
    }
    
    public void testGzippedInputStreamInputStream()
    throws IOException {
        File compressedFile = createMultiGzipMembers();
        // Test we get right count of members.
        InputStream is =
            new RepositionableRandomAccessInputStream(compressedFile);
        GzippedInputStream gis = new GzippedInputStream(is);
        int records = 0;
        long offsetOfSecondRecord = -1;
        for (Iterator i = gis.iterator(); i.hasNext();) {
            long offset = gis.position();
            if (records == 2) {
                offsetOfSecondRecord = offset;
            }
            is = (InputStream)i.next();
            records++;
        }
        assertTrue("Record count is off " + records,
            records == GZIPMEMBER_COUNT);
        
        // Test random record read.
        is = new RepositionableRandomAccessInputStream(compressedFile);
        gis = new GzippedInputStream(is);
        byte [] buffer = new byte[TEXT.length()];
        // Seek to second record, read in gzip header.
        gis.gzipMemberSeek(offsetOfSecondRecord);
        gis.read(buffer);
        String readString = new String(buffer);
        assertEquals("Failed read", TEXT, readString);
    }
}
