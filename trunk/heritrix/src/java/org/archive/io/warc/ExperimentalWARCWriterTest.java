/*
 * ExperimentalWARCWriterTest
 *
 * $Id$
 *
 * Created on July 27th, 2006
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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;

import org.archive.io.UTF8Bytes;
import org.archive.uid.GeneratorFactory;
import org.archive.util.ArchiveUtils;
import org.archive.util.TmpDirTestCase;
import org.archive.util.anvl.ANVLRecord;


public class ExperimentalWARCWriterTest
extends TmpDirTestCase implements WARCConstants {
    public void testCheckHeaderLineValue() throws Exception {
        ExperimentalWARCWriter writer = new ExperimentalWARCWriter();
        writer.checkHeaderLineValue("one");
        IOException exception = null;
        try {
            writer.checkHeaderLineValue("with space");
        } catch(IOException e) {
            exception = e;
        }
       assertNotNull(exception);
       exception = null;
       try {
           writer.checkHeaderLineValue("with\0x0000controlcharacter");
       } catch(IOException e) {
           exception = e;
       }
      assertNotNull(exception);
    }
    
    public void testWriteRecord() throws IOException {
    	File [] files = {getTmpDir()};
        
    	// Write uncompressed.
        ExperimentalWARCWriter writer =
        	new ExperimentalWARCWriter(Arrays.asList(files),
        			this.getClass().getName(), "suffix", false, -1, null);
        writeFile(writer);
        
        // Write compressed.
        writer = new ExperimentalWARCWriter(Arrays.asList(files),
        		this.getClass().getName(), "suffix", true, -1, null);
        writeFile(writer);
    }
    
    private void writeFile(final ExperimentalWARCWriter writer)
    throws IOException {
        try {
            writeWarcinfoRecord(writer);
            writeBasicRecords(writer);
        } finally {
            writer.close();
            writer.getFile().delete();
        }
    }
    
    private void writeWarcinfoRecord(ExperimentalWARCWriter writer)
    throws IOException {
    	ANVLRecord meta = new ANVLRecord();
    	meta.addLabelValue("size", "500mb");
    	meta.addLabelValue("operator", "igor");
    	byte [] bytes = meta.getUTF8Bytes();
    	writer.writeWarcinfoRecord(ANVLRecord.MIMETYPE, null,
    		new ByteArrayInputStream(bytes), bytes.length);
	}

	protected void writeBasicRecords(final ExperimentalWARCWriter writer)
    throws IOException {
    	ANVLRecord headerFields = new ANVLRecord();
    	headerFields.addLabelValue("x", "y");
    	headerFields.addLabelValue("a", "b");
    	
    	URI rid = null;
    	try {
    		rid = GeneratorFactory.getFactory().
    			getQualifiedRecordID(TYPE, METADATA);
    	} catch (URISyntaxException e) {
    		// Convert to IOE so can let it out.
    		throw new IOException(e.getMessage());
    	}
    	final String content = "Any old content.";
    	for (int i = 0; i < 10; i++) {
    		String body = i + ". " + content;
    		byte [] bodyBytes = body.getBytes(UTF8Bytes.UTF8);
    		writer.writeRecord(METADATA, "http://www.archive.org/",
    			ArchiveUtils.get14DigitDate(), "no/type",
    			rid, headerFields,
    			(InputStream)new ByteArrayInputStream(bodyBytes),
    			(long)bodyBytes.length);
    	}
    }
}
