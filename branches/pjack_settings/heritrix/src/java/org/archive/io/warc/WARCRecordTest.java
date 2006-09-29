/* $Id$
 *
 * Created on August 25th, 2006
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
import java.io.IOException;

import junit.framework.TestCase;

public class WARCRecordTest extends TestCase {
    public void testParseHeaderLine() throws IOException {
        final String body = "GET /robots.txt HTTP/1.0";
        final String hdr = "WARC/0.9 000000000496 request " +
            "http://foo.maths.uq.edu.au/robots.txt 20060825210905 " +
            "uuri:dbf1ef20-db01-4cc6-ba93-6baf7a906356;type=request " +
            "application/http; msgtype=request\r\n" +
            "Related-Record-ID: uuri:dbf1ef20-db01-4cc6-ba93-6baf7a906356\r\n" +
            "\r\n" +
            body +
            "\r\n\r\n";
        WARCRecord r = new WARCRecord(new ByteArrayInputStream(hdr.getBytes()),
             "READER_IDENTIFIER", 0, false, true);
        assertEquals(r.getHeader().getLength(), 496);
        assertEquals(r.getHeader().getMimetype(),
            "application/http; msgtype=request");
        assertEquals(r.getHeader().getRecordIdentifier(),
            "uuri:dbf1ef20-db01-4cc6-ba93-6baf7a906356;type=request");
        byte [] b = new byte[body.length()];
        r.read(b);
        String bodyRead = new String(b);
        assertEquals(body, bodyRead);
    }
}
