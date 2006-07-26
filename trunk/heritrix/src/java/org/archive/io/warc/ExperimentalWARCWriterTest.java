package org.archive.io.warc;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.archive.util.TmpDirTestCase;


public class ExperimentalWARCWriterTest extends TmpDirTestCase implements WARCConstants {
	private static int id = 0;
	
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
        ExperimentalWARCWriter writer = new ExperimentalWARCWriter(Arrays.asList(files),
            this.getClass().getName(), false, -1);
        try {
        	writeBasicRecords(writer);
        } finally {
        	writer.close();
        	writer.getFile().delete();
        }
        // Write compressed.
        writer = new ExperimentalWARCWriter(Arrays.asList(files), this.getClass().getName(),
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
    	for (int i = 0; i < 10; i++) {
    		writer.writeRecord(WARCConstants.METADATA,
    		    "http://www.archive.org/", "no/type", Integer.toString(id++),
    		    m, null, 0);
    	}
    }
}
