/* ARCReaderFactoryTest.java
 *
 * $Id$
 *
 * Created Jul 15, 2005
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
package org.archive.io.arc;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;

import org.archive.util.TmpDirTestCase;

public class ARCReaderFactoryTest extends TmpDirTestCase {
    
    /**
     * Test URL.
     * @throws MalformedURLException
     * @throws IOException
     */
    public void testGetURL() throws MalformedURLException, IOException {
        File arc = ARCWriterTest.createARCFile(getTmpDir(), true);
        ARCReader reader = null;
        File tmpFile = null;
        try {
            reader = ARCReaderFactory.
                get(new URL("file:////" + arc.getAbsolutePath()));
            tmpFile = null;
            for (Iterator i = reader.iterator(); i.hasNext();) {
                ARCRecord r = (ARCRecord)i.next();
                if (tmpFile == null) {
                    tmpFile = r.getMetaData().getArcFile();
                }
            }
            assertTrue(tmpFile.exists());
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
        assertFalse(tmpFile.exists());
    }
    
    protected void doGetUrl(File arc)
    throws MalformedURLException, IOException {
        ARCReader reader = null;
        File tmpFile = null;
        try {
            reader = ARCReaderFactory.
                get(new URL("file:////" + arc.getAbsolutePath()));
            tmpFile = null;
            for (Iterator i = reader.iterator(); i.hasNext();) {
                ARCRecord r = (ARCRecord)i.next();
                if (tmpFile == null) {
                    tmpFile = r.getMetaData().getArcFile();
                }
            }
            assertTrue(tmpFile.exists());
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
        assertFalse(tmpFile.exists());
    }
    
    /**
     * Test path or url.
     */
    public void testGetPathOrURL() throws MalformedURLException, IOException {
        File arc = ARCWriterTest.createARCFile(getTmpDir(), true);
        ARCReader reader = ARCReaderFactory.get(arc.getAbsoluteFile());
        assertNotNull(reader);
        reader.close();
        doGetUrl(arc);
    }   
}
