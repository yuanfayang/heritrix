/* Copyright (C) 2007 Internet Archive.
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
 *
 * SettingsSAXTest.java
 * Created on January 23, 2007
 *
 * $Header:$
 */
package org.archive.settings.file;

import java.io.StringReader;
import java.util.Arrays;
import java.util.LinkedList;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.archive.settings.path.PathChange;
import org.xml.sax.InputSource;

import junit.framework.TestCase;

public class SettingsSAXTest extends TestCase {

    
    final private static String TEST_DATA = 
        "<root>" +
        
        "<object name='foo' value='org.archive.Foo'/>" +
        "<object name='bar' value='org.archive.Bar'>" +
        "</object>" +
        
        "<object name='baz' value='org.archive.Baz'>" +
        "<dependencies>" +
        "</dependencies>" +
        "</object>" +
        
        "<object name='fubar' value='org.archive.Fubar'>" +
        "<dependencies>" +
        "<string name='foo' value='test string'/>" +
        "</dependencies>" +
        "</object>" +

        "<object name='snafu' value='org.archive.Snafu'>" +
        "<dependencies>" +
        "<string name='foo' value='test string2'/>" +
        "</dependencies>" +
        "<string name='bar' value='test string3'/>" +
        "</object>" +

        "</root>";
    
    
    final private static PathChange[] EXPECTED = new PathChange[] {
        new PathChange("foo", "object", "org.archive.Foo"),
        new PathChange("bar", "object", "org.archive.Bar"),
        new PathChange("baz", "object", "org.archive.Baz"),
        new PathChange("fubar", "object", "org.archive.Fubar"),
        new PathChange("snafu", "object", "org.archive.Snafu"),
        new PathChange("snafu.bar", "string", "test string3")
    };
    
    
    

    public void testParse() throws Exception {
        SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
        
        String test = TEST_DATA.replace('\'', '"');
        
        StringReader sr = new StringReader(test);
        InputSource is = new InputSource(sr);
        final LinkedList<PathChange> list = new LinkedList<PathChange>();
        PathChangeListener listener = new PathChangeListener() {
            public void change(PathChange pc) {
                list.add(pc);
            }
        };
        
        parser.parse(is, new SettingsSAX(listener));
        
        for (int i = 0; i < EXPECTED.length; i++) {
            if (!EXPECTED[i].equals(list.get(i))) {
                System.out.println(i + " did not match");
            }
        }
        
        for (PathChange pc: EXPECTED) {
            System.out.println(pc);
        }
        
        System.out.println("========");
        
        for (PathChange pc: list) {
            System.out.println(pc);
        }
        assertEquals(Arrays.asList(EXPECTED), list);
    }
    
    
}
