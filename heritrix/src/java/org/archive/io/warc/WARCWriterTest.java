package org.archive.io.warc;

import org.archive.util.TmpDirTestCase;
import java.io.IOException;


public class WARCWriterTest extends TmpDirTestCase implements WARCConstants {
    public void testCheckHeaderLineValue() throws Exception {
        WARCWriter writer = new WARCWriter();
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
}
