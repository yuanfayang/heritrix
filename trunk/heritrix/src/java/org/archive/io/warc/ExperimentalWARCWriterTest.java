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

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.archive.io.warc.recordid.GeneratorFactory;
import org.archive.util.TmpDirTestCase;


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
        			this.getClass().getName(), false, -1);
        try {
        	writeBasicRecords(writer);
        } finally {
        	writer.close();
        	writer.getFile().delete();
        }
        // Write compressed.
        writer = new ExperimentalWARCWriter(Arrays.asList(files),
        		this.getClass().getName(),
            true, -1);
        try {
        	writeBasicRecords(writer);
        } finally {
        	writer.close();
        	writer.getFile().delete();
        }
    }
    
    protected void writeBasicRecords(final ExperimentalWARCWriter writer)
    throws IOException {
    	Map<String, String> m = new HashMap<String, String>();
    	m.put("x", "y");
    	URI rid = null;
    	try {
    		rid = GeneratorFactory.getFactory().getQualifiedRecordID(TYPE,
    			METADATA);
    	} catch (URISyntaxException e) {
    		// Convert to IOE so can let it out.
    		throw new IOException(e.getMessage());
    	}
    	for (int i = 0; i < 10; i++) {
    		writer.writeRecord(METADATA, "http://www.archive.org/", "no/type",
    			rid, m, null, 0);
    	}
    }
}
