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
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.archive.util.ArchiveUtils;


/**
 * Test ARCWriter class.
 * 
 * This code exercises ARCWriter AND ARCReader.  First it writes ARCs w/ 
 * ARCWriter.  Then it validates what was written w/ ARCReader.
 * 
 * @author stack
 */
public class ARCWriterTest
    extends ARCTest
    implements ARCConstants, FileFilter
{
    /**
     * Prefix to use for ARC files made by JUNIT.
     */
    private static final String PREFIX = DEFAULT_ARC_FILE_PREFIX;
    
    /**
     * Store in here the file prefix for filefiltering done by some of the 
     * unit tests.
     */
    private String fileFilterPrefix = null;
  
    
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
        byte[] fetchDate = now.getBytes();
        baos.write(fetchDate);
        baos.write("\n".getBytes());
        int recordLength = fetchDate.length + 1; 
        byte[] record = ("HTTP/1.1 200 OK\n" +
            "Content-Type: text/html\n" +
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
        ARCWriter arcWriter = new ARCWriter(this.tmpDir,
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
        ARCReader reader = new ARCReader(arcFile);
        assertNotNull(reader);
        if (recordCount == -1)
        {
            reader.validate();
        }
        else
        {
            reader.validate(recordCount);
        }
        reader.close();
    }

    public void testCheckARCFileSize()
        throws IOException
    {
        runCheckARCFileSizeTest("checkARCFileSize", false);
    }
    
    public void testCheckARCFileSizeCompressed()
        throws IOException
    {
        runCheckARCFileSizeTest("checkARCFileSize", false);
    }
    
    private void runCheckARCFileSizeTest(String baseName, boolean compress)
        throws FileNotFoundException, IOException
    {
        writeRecords(baseName, compress, 1024, 15);
        // Now validate all files just created.
        this.fileFilterPrefix = baseName;
        File [] files = this.tmpDir.listFiles(this);
        for (int i = 0; i < files.length; i++)
        {
            validate(files[i], -1);
        }        
    }
    
    /**
     * Delete any files left over from previous run.
     * 
     * @param baseName Base name of files we're to clean up.
     */
    private void cleanUpOldFiles(String baseName)
    {
        this.fileFilterPrefix = baseName;
        // Delete any files hanging about from last test run if any.
        File [] files = this.tmpDir.listFiles(this);
        for (int i = 0; i < files.length; i++)
        {
            files[i].delete();
        }
    }
    
    /**
     * Return files that begin w/ {@link #fileFilterPrefix}
     * 
     * Implementation of the FileFilter.accept method.
     * @param pathname File to filter.
     * @return True if we are to include the passed file.
     */
    public boolean accept(File pathname)
    {
        return pathname.getName().startsWith(this.fileFilterPrefix);
    }

    public void testWriteRecord()
        throws IOException
    {
        final int recordCount = 2;
        File arcFile = writeRecords("writeRecord", false,
                DEFAULT_MAX_ARC_FILE_SIZE, recordCount);
        validate(arcFile, recordCount);
    }
    
    public void testWriteRecordCompressed()
        throws IOException
    {
        final int recordCount = 2;
        File arcFile = writeRecords("writeRecordCompressed", true,
                DEFAULT_MAX_ARC_FILE_SIZE, recordCount);
        validate(arcFile, recordCount);
    }
    
    public void testGetOutputDir()
        throws IOException
    {
        ARCWriter arcWriter = new ARCWriter(this.tmpDir,
            "getOutputDir-" + PREFIX, false, DEFAULT_MAX_ARC_FILE_SIZE);
        assertEquals(this.tmpDir, arcWriter.getOutputDir());
        arcWriter.close();
    }
}
