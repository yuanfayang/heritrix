/* ARCWriterTest
 *
 * $Id$
 *
 * Created on Dec 31, 2003.
 *
 * Copyright (C) 2003 Internet Archive.
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
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.archive.util.ArchiveUtils;
import org.archive.util.FileUtils;
import org.archive.util.TmpDirTestCase;


/**
 * Test ARCWriter class.
 *
 * This code exercises ARCWriter AND ARCReader.  First it writes ARCs w/
 * ARCWriter.  Then it validates what was written w/ ARCReader.
 *
 * @author stack
 */
public class ARCWriterTest
    extends TmpDirTestCase
    implements ARCConstants
{
    /**
     * Prefix to use for ARC files made by JUNIT.
     */
    private static final String PREFIX = DEFAULT_ARC_FILE_PREFIX;


    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception
    {
        super.setUp();
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception
    {
        super.tearDown();
    }

    protected int writeRandomHTTPRecord(ARCWriter arcWriter, int index)
        throws IOException
    {
        String indexStr = Integer.toString(index);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        // Start the record with an arbitrary 14-digit date per RFC2540
        String now = ArchiveUtils.get14DigitDate();
        int recordLength = 0;
        byte[] record = ("HTTP/1.1 200 OK\r\n" +
            "Content-Type: text/html\r\n\r\n" +
            "<html><head><title>Page #" + indexStr +
            "</title></head>" +
            "<body>Page #" + indexStr +
            "</body></html>").getBytes();
        recordLength += record.length;
        baos.write(record);
        // Add the newline between records back in
        baos.write("\n".getBytes());
        recordLength += 1;
        arcWriter.write("http://www.one.net/id=" + indexStr, "text/html",
            "0.1.2.3", Long.parseLong(now), recordLength, baos);
        return recordLength;
    }

    private File writeRecords(String baseName, boolean compress,
            int maxSize, int recordCount)
        throws IOException
    {
        cleanUpOldFiles(baseName);
        ARCWriter arcWriter = new ARCWriter(getTmpDir(),
            baseName + '-' + PREFIX, compress, maxSize);
        assertNotNull(arcWriter);
        for (int i = 0; i < recordCount; i++)
        {
            writeRandomHTTPRecord(arcWriter, i);
        }
        File arcFile = arcWriter.getArcFile();
        assertTrue(arcFile.exists());
        arcWriter.close();
        return arcFile;
    }

    private void validate(File arcFile, int recordCount)
        throws FileNotFoundException, IOException
    {
        ARCReader reader = ARCReaderFactory.get(arcFile);
        assertNotNull(reader);
        List metaDatas = null;
        if (recordCount == -1) {
            metaDatas = reader.validate();
        } else {
            metaDatas = reader.validate(recordCount);
        }
        reader.close();
        // Now, run through each of the records doing absolute get going from
        // the end to start.  Reopen the arc so no context between this test
        // and the previous.
        reader = ARCReaderFactory.get(arcFile);
        for (int i = metaDatas.size() - 1; i >= 0; i--) {
            ARCRecordMetaData meta = (ARCRecordMetaData)metaDatas.get(i);
            ARCRecord r = reader.get(meta.getOffset());
            String mimeType = r.getMetaData().getMimetype();
            assertTrue("Record is bogus",
                mimeType != null && mimeType.length() > 0);
        }
        reader.close();
        assertTrue("Metadatas not equal", metaDatas.size() == recordCount);
        for (Iterator i = metaDatas.iterator(); i.hasNext();) {
                ARCRecordMetaData r = (ARCRecordMetaData)i.next();
                assertTrue("Record is empty", r.getLength() > 0);
        }
    }

    public void testCheckARCFileSize()
        throws IOException
    {
        runCheckARCFileSizeTest("checkARCFileSize", false);
    }

    public void testCheckARCFileSizeCompressed()
        throws IOException
    {
        runCheckARCFileSizeTest("checkARCFileSize", true);
    }


    public void testWriteRecord() throws IOException {
        final int recordCount = 2;
        File arcFile = writeRecords("writeRecord", false,
                DEFAULT_MAX_ARC_FILE_SIZE, recordCount);
        validate(arcFile, recordCount  + 1); // Header record.
    }
    

    public void testGetOutputDir() throws IOException {
        ARCWriter arcWriter = new ARCWriter(getTmpDir(),
            "getOutputDir-" + PREFIX, null, false, DEFAULT_MAX_ARC_FILE_SIZE,
            null);
        assertEquals(getTmpDir(), arcWriter.getArcsDir());
        arcWriter.close();
    }

    public void testWriteRecordCompressed() throws IOException {
        final int recordCount = 2;
        File arcFile = writeRecords("writeRecordCompressed", true,
                DEFAULT_MAX_ARC_FILE_SIZE, recordCount);
        validate(arcFile, recordCount + 1 /*Header record*/);
    }
    
    private void runCheckARCFileSizeTest(String baseName, boolean compress)
        throws FileNotFoundException, IOException
    {
        writeRecords(baseName, compress, 1024, 15);
        // Now validate all files just created.
        File [] files = FileUtils.getFilesWithPrefix(getTmpDir(), PREFIX);
        for (int i = 0; i < files.length; i++) {
            validate(files[i], -1);
        }
    }
}
