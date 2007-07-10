/* 
 * Copyright (C) 2007 Internet Archive.
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
 * KeyMetadataMakerTest.java
 *
 * Created on Jan 31, 2007
 *
 * $Id:$
 */

package org.archive.state;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.Map;

import junit.framework.TestCase;

/**
 * @author pjack
 *
 */
public class KeyMetadataMakerTest extends TestCase {

    
    private static String TEST_DATA = 
        "\n" +
        "   /** Single-line comment. */ \n" +
        "   public static final Key<String> A = Key.make(null); \n" +
        "\n" +
        "   /** \n" +
        "    * Multi-line \n" +
        "    * comment. \n" +
        "    */    \n" +
        "              static public final Key<X> B = Key.make(null); \n" +
        "\n" +
        "\n" +
        "\n" +
        "       /** \n " +
        "        * Comment to ignore */ \n" +
        "\n" +
        "\n" + 
        "       /** \n " +
        "        * Comment to keep */ \n" +
        "    final public static Key<Z> KEY_THREE = Key.make(null); \n";
    
    
    
    
    public void testParseSourceFile() throws Exception {
        StringReader sr = new StringReader(TEST_DATA);
        BufferedReader br = new BufferedReader(sr);
        Map<String,String> map = KeyMetadataMaker.parseSourceFile(br);
        System.out.println(map);
    }
    
    
}
