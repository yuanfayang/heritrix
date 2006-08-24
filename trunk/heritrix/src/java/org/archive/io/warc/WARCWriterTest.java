/* $Id$
 *
 * Created on August 23rd, 2006.
 *
 * Copyright (C) 2006 Internet Archive.
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
package org.archive.io.warc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.archive.io.ArchiveRecord;
import org.archive.io.ArchiveRecordHeader;
import org.archive.io.WriterPoolMember;
import org.archive.uid.GeneratorFactory;
import org.archive.util.ArchiveUtils;
import org.archive.util.FileUtils;
import org.archive.util.TmpDirTestCase;
import org.archive.util.anvl.ANVLRecord;


/**
 * Test WARCWriter class.
 *
 * This code exercises WARCWriter AND WARCReader.  First it writes WARCs w/
 * WARCWriter.  Then it validates what was written w/ WARCReader.
 *
 * @author stack
 */
public class WARCWriterTest
extends TmpDirTestCase implements WARCConstants {
    /**
     * Prefix to use for ARC files made by JUNIT.
     */
    private static final String PREFIX = "IAH";
    
    private static final String SOME_URL = "http://www.archive.org/test/";
    
    /**
     * @return Generic HTML Content.
     */
    protected static String getContent() {
        return getContent(null);
    }
    
    /**
     * @return Generic HTML Content with mention of passed <code>indexStr</code>
     * in title and body.
     */
    protected static String getContent(String indexStr) {
        String page = (indexStr != null)? "Page #" + indexStr: "Some Page";
        return "HTTP/1.1 200 OK\r\n" +
        "Content-Type: text/html\r\n\r\n" +
        "<html><head><title>" + page +
        "</title></head>" +
        "<body>" + page +
        "</body></html>";
    }
    
    private static URI getRecordID() throws IOException {
        URI result;
        try {
            result = GeneratorFactory.getFactory().getRecordID();
        } catch (URISyntaxException e) {
            throw new IOException(e.toString());
        }
        return result;
    }

    /**
     * Write random HTML Record.
     * @param w Where to write.
     * @param index An index to put into content.
     * @return Length of record written.
     * @throws IOException
     */
    protected int writeRandomHTTPRecord(ExperimentalWARCWriter w, int index)
    throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        String indexStr = Integer.toString(index);
        byte[] record = (getContent(indexStr)).getBytes();
        int recordLength = record.length;
        baos.write(record);
        // Add named fields for ip, checksum, and relate the metadata
        // and request to the resource field.
        ANVLRecord r = new ANVLRecord(1);
        r.addLabelValue(NAMED_FIELD_IP_LABEL, "127.0.0.1");
        w.writeResourceRecord(
            "http://www.one.net/id=" + indexStr,
            ArchiveUtils.get14DigitDate(),
            "text/html; charset=UTF-8",
            getRecordID(),
            r,
            new ByteArrayInputStream(baos.toByteArray()),
            recordLength);
        return recordLength;
    }

    /**
     * Fill a WARC with HTML Records.
     * @param baseName WARC basename.
     * @param compress Whether to compress or not.
     * @param maxSize Maximum WARC size.
     * @param recordCount How many records.
     * @return The written file.
     * @throws IOException
     */
    private File writeRecords(String baseName, boolean compress,
        int maxSize, int recordCount)
    throws IOException {
        cleanUpOldFiles(baseName);
        File [] files = {getTmpDir()};
        ExperimentalWARCWriter w = new ExperimentalWARCWriter(
            Arrays.asList(files), baseName + '-' + PREFIX, "", compress,
            maxSize, null);
        assertNotNull(w);
        for (int i = 0; i < recordCount; i++) {
            writeRandomHTTPRecord(w, i);
        }
        w.close();
        assertTrue("Doesn't exist: " +  w.getFile().getAbsolutePath(), 
            w.getFile().exists());
        return w.getFile();
    }

    /**
     * Run validation of passed file.
     * @param f File to validate.
     * @param recordCount Expected count of records.
     * @throws FileNotFoundException
     * @throws IOException
     */
    private void validate(File f, int recordCount)
    throws FileNotFoundException, IOException {
        WARCReader reader = WARCReaderFactory.get(f);
        assertNotNull(reader);
        List headers = null;
        if (recordCount == -1) {
            headers = reader.validate();
        } else {
            headers = reader.validate(recordCount);
        }
        reader.close();
        
        // Now, run through each of the records doing absolute get going from
        // the end to start.  Reopen the arc so no context between this test
        // and the previous.
        reader = WARCReaderFactory.get(f);
        for (int i = headers.size() - 1; i >= 0; i--) {
            ArchiveRecordHeader h = (ArchiveRecordHeader)headers.get(i);
            ArchiveRecord r = reader.get(h.getOffset());
            String mimeType = r.getHeader().getMimetype();
            assertTrue("Record is bogus",
                mimeType != null && mimeType.length() > 0);
        }
        reader.close();
        
        assertTrue("Metadatas not equal", headers.size() == recordCount);
        for (Iterator i = headers.iterator(); i.hasNext();) {
            ArchiveRecordHeader r = (ArchiveRecordHeader)i.next();
            assertTrue("Record is empty", r.getLength() > 0);
        }
    }

    public void testCheckWARCFileSize()
    throws IOException {
        // TODO runCheckWARCFileSizeTest("checkWARCFileSize", false);
    }

    public void testCheckWARCFileSizeCompressed()
    throws IOException {
        // TODO runCheckWARCFileSizeTest("checkWARCFileSize", true);
    }


    public void testWriteRecord() throws IOException {
        final int recordCount = 2;
        File f = writeRecords("writeRecord", false, DEFAULT_MAX_WARC_FILE_SIZE,
            recordCount);
        // TODO validate(f, recordCount  + 1); // Header record.
    }
    
    public void testRandomAccess() throws IOException {
        /* TODO
        final int recordCount = 3;
        File f = writeRecords("writeRecord", true, DEFAULT_MAX_WARC_FILE_SIZE,
            recordCount);
        WARCReader reader = WARCReaderFactory.get(f);
        // Get to second record.  Get its offset for later use.
        boolean readFirst = false;
        String url = null;
        long offset = -1;
        long totalRecords = 0;
        boolean readSecond = false;
        for (final Iterator i = reader.iterator(); i.hasNext();
                totalRecords++) {
            WARCRecord ar = (WARCRecord)i.next();
            if (!readFirst) {
                readFirst = true;
                continue;
            }
            if (!readSecond) {
                url = ar.getHeader().getUrl();
                offset = ar.getHeader().getOffset();
                readSecond = true;
            }
        }
        
        reader = WARCReaderFactory.get(f, offset);
        ArchiveRecord ar = reader.get();
        assertEquals(ar.getHeader().getUrl(), url);
        ar.close();
        
        // Get reader again.  See how iterator works with offset
        reader = WARCReaderFactory.get(f, offset);
        int count = 0;
        for (final Iterator i = reader.iterator(); i.hasNext(); i.next()) {
            count++;
        }
        reader.close();
        assertEquals(totalRecords - 1, count);
        */
    }

    public void testWriteRecordCompressed() throws IOException {
        final int recordCount = 2;
        File arcFile = writeRecords("writeRecordCompressed", true,
            DEFAULT_MAX_WARC_FILE_SIZE, recordCount);
        // TODO: validate(arcFile, recordCount + 1 /*Header record*/);
    }
    
    private void runCheckWARCFileSizeTest(String baseName, boolean compress)
    throws FileNotFoundException, IOException {
        writeRecords(baseName, compress, 1024, 15);
        // Now validate all files just created.
        File [] files = FileUtils.getFilesWithPrefix(getTmpDir(), PREFIX);
        for (int i = 0; i < files.length; i++) {
            validate(files[i], -1);
        }
    }
    
    protected ExperimentalWARCWriter createWARCWriter(String NAME,
            boolean compress) {
        File [] files = {getTmpDir()};
        return new ExperimentalWARCWriter(Arrays.asList(files), NAME, "",
            compress, DEFAULT_MAX_WARC_FILE_SIZE, null);
    }
    
    protected static ByteArrayOutputStream getBaos(String str)
    throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(str.getBytes());
        return baos;
    }
    
    protected static void writeRecord(ExperimentalWARCWriter w, String url,
        String mimetype, int len, ByteArrayOutputStream baos)
    throws IOException {
        w.writeResourceRecord(url,
            ArchiveUtils.get14DigitDate(),
            mimetype,
            getRecordID(),
            null,
            new ByteArrayInputStream(baos.toByteArray()),
            len);
    }
    
    protected int iterateRecords(WARCReader r)
    throws IOException {
        int count = 0;
        for (Iterator<ArchiveRecord> i = r.iterator(); i.hasNext();) {
            ArchiveRecord ar = i.next();
            ar.close();
            if (count != 0) {
                assertTrue("Unexpected URL " + ar.getHeader().getUrl(),
                    ar.getHeader().getUrl().equals(SOME_URL));
            }
            count++;
        }
        return count;
    }
    
    protected ExperimentalWARCWriter createWithOneRecord(String name,
        boolean compressed)
    throws IOException {
        ExperimentalWARCWriter writer = createWARCWriter(name, compressed);
        String content = getContent();
        writeRecord(writer, SOME_URL, "text/html",
            content.length(), getBaos(content));
        return writer;
    }
    
    public void testSpaceInURL() {
        /* TODO
        String eMessage = null;
        try {
            holeyUrl("testSpaceInURL-" + PREFIX, false, " ");
        } catch (IOException e) {
            eMessage = e.getMessage();
        }
        assertTrue("Didn't get expected exception: " + eMessage,
            eMessage.startsWith("Metadata line doesn't match"));
            */
    }

    public void testTabInURL() {        
        /* TODO
        String eMessage = null;
        try {
            holeyUrl("testTabInURL-" + PREFIX, false, "\t");
        } catch (IOException e) {
            eMessage = e.getMessage();
        }
        assertTrue("Didn't get expected exception: " + eMessage,
            eMessage.startsWith("Metadata line doesn't match"));
            */
    }
    
    protected void holeyUrl(String name, boolean compress, String urlInsert)
    throws IOException {
        ExperimentalWARCWriter writer = createWithOneRecord(name, compress);
        // Add some bytes on the end to mess up the record.
        String content = getContent();
        ByteArrayOutputStream baos = getBaos(content);
        writeRecord(writer, SOME_URL + urlInsert + "/index.html", "text/html",
            content.length(), baos);
        writer.close();
    }
    
    /**
     * Write an arc file for other tests to use.
     * @param arcdir Directory to write to.
     * @param compress True if file should be compressed.
     * @return ARC written.
     * @throws IOException 
     */
    public static File createWARCFile(File arcdir, boolean compress)
    throws IOException {
        File [] files = {arcdir};
        ExperimentalWARCWriter writer =
            new ExperimentalWARCWriter(Arrays.asList(files),
            "test", "", compress, DEFAULT_MAX_WARC_FILE_SIZE, null);
        String content = getContent();
        writeRecord(writer, SOME_URL, "text/html", content.length(),
            getBaos(content));
        writer.close();
        return writer.getFile();
    }
    
//    public void testSpeed() throws IOException {
//        ARCWriter writer = createArcWithOneRecord("speed", true);
//        // Add a record with a length that is too long.
//        String content = getContent();
//        final int count = 100000;
//        logger.info("Starting speed write of " + count + " records.");
//        for (int i = 0; i < count; i++) {
//            writeRecord(writer, SOME_URL, "text/html", content.length(),
//                    getBaos(content));
//        }
//        writer.close();
//        logger.info("Finished speed write test.");
//    }
    
    
    public void testValidateMetaLine() throws Exception {
        ExperimentalWARCWriter w =
            createWARCWriter("testValidateMetaLine", true);
        try {
            // TODO.
        } finally {
            w.close();
        }
    }
    
    public void testArcRecordOffsetReads() throws Exception {
        /* TODO
    	// Get an ARC with one record.
		WriterPoolMember w =
			createWithOneRecord("testArcRecordInBufferStream", true);
		w.close();
		// Get reader on said ARC.
		WARCReader r = WARCReaderFactory.get(w.getFile());
		final Iterator<ArchiveRecord> i = r.iterator();
		// Skip first ARC meta record.
		ArchiveRecord ar = i.next();
		i.hasNext();
		// Now we're at first and only record in ARC.
		ar = (WARCRecord) i.next();
		// Now try getting some random set of bytes out of it 
		// at an odd offset (used to fail because we were
		// doing bad math to find where in buffer to read).
		final byte[] buffer = new byte[17];
		final int maxRead = 4;
		int totalRead = 0;
		while (totalRead < maxRead) {
			totalRead = totalRead
			    + ar.read(buffer, 13 + totalRead, maxRead - totalRead);
			assertTrue(totalRead > 0);
		}
        */
	}
}