/* ANVLRecordTest
*
* $Id$
*
* Created on July 26, 2006.
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
package org.archive.util.anvl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import junit.framework.TestCase;

public class ANVLRecordTest extends TestCase {
    public void testAdd() throws Exception {
        ANVLRecord am = new ANVLRecord();
        am.add(new Element(new Label("entry")));
        am.add(new Element(new Comment("First draft")));
        am.add(new Element(new Label("who"),
            new Value("Gilbert, W.S. | Sullivan, Arthur")));
        am.add(new Element(new Label("what"),
                new Value("\rThe Yeoman of \rthe guard")));
        am.add(new Element(new Label("what"),
            new Value("The Yeoman of\r\n  the guard")));
        am.add(new Element(new Label("what"),
                new Value("The Yeoman of \n\tthe guard")));
        am.add(new Element(new Label("what"),
                new Value("The Yeoman of \r        the guard")));
        am.add(new Element(new Label("when/created"),
            new Value("1888")));
        System.out.println(am.toString());
    }
    
    public void testEmptyRecord() throws Exception {
    	byte [] b = ANVLRecord.EMPTY_ANVL_RECORD.getUTF8Bytes();
    	assertEquals(b.length, 2);
    	assertEquals(b[0], '\r');
    	assertEquals(b[1], '\n');
    }
    
    public void testFolding() throws Exception {
        ANVLRecord am = new ANVLRecord();
        Exception e = null;
        try {
            am.addLabel("Label with \n in it");
        } catch (IllegalArgumentException iae) {
            e = iae;
        }
        assertTrue(e != null && e instanceof IllegalArgumentException);
        am.addLabelValue("label", "value with \n in it");
    }
    
    public void testParse() throws UnsupportedEncodingException, IOException {
        final String record = "   a: b\r\n\r\nsdfsdsdfds";
        ANVLRecord r = ANVLRecord.load(new ByteArrayInputStream(
            record.getBytes("ISO-8859-1")));
        System.out.println(r);
    }
}
