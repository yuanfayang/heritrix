/* DocTest
*
* Created on September 12, 2006
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
package org.archive.util.ms;

import java.io.FileInputStream;
import java.io.Reader;
import java.io.StringWriter;

import org.apache.poi.hdf.extractor.WordDocument;

import junit.framework.TestCase;

public class DocTest extends TestCase {


    
    public void testAgainstPOI() throws Exception {
//        testFileAgainstPOI("/Users/pjack/Desktop/15336-doc.doc");
    }


    private void testFileAgainstPOI(String filename) throws Exception {
        Reader reader = Doc.getText(filename);
        StringWriter writer = new StringWriter();
        char[] temp = new char[1024];
        for (int l = reader.read(temp); l > 0; l = reader.read(temp)) {
            writer.write(temp, 0, l);
        }
        reader.close();
        String docAPIResult = writer.toString();
        writer = new StringWriter();
                
        FileInputStream finp = new FileInputStream(filename);
        WordDocument wd = new WordDocument(finp);
        wd.writeAllText(writer);
        finp.close();
        String poiAPIResult = writer.toString();
        
        int docLen = docAPIResult.length();
        int poiLen = poiAPIResult.length();
        if (docLen != poiLen) {
            throw new Exception("Doc: " + docLen + " Poi: " + poiLen);
        }
        int max = Math.min(docAPIResult.length(), poiAPIResult.length());
        int errors = 0;
        for (int i = 0; i < max; i++) {
            char docCh = docAPIResult.charAt(i);
            char poiCh = correctPOI(poiAPIResult.charAt(i));
            if (docCh != poiCh) {
                errors++;
                int docInt = (int)docCh;
                int poiInt = (int)poiCh;
                String msg = "#" + i + ": doc=" + docInt + " poi=" + poiInt;
                System.out.println(msg);
                System.out.println("======== doc ========");
                System.out.println(context(docAPIResult, i));
                System.out.println("======== poi ========");
                System.out.println(context(poiAPIResult, i));
                System.out.println("=====================");
            }
        }
        
        if (errors > 0) {
            throw new Exception(errors + " errors found, see stdout.");
        }
    }

    
    private static String context(String s, int i) {
        int start = Math.max(0, i - 10);
        int end = Math.min(s.length(), i + 10);
        return s.substring(start, end);
    }


    /**
     * Corrects POI's Cp1252 output.  There's a bug somewhere in POI that
     * makes it produce incorrect characters.  Not sure where and don't have
     * time to track it down.  But I have visually checked the input 
     * documents to verify that Doc is producing the right character, and
     * that POI is not.
     * 
     * @param ch  the POI-produced character to check
     * @return    the corrected character
     */
    private static char correctPOI(char ch) {
        switch (ch) {
            case 8734:
                // POI produced the infinity sign when it should have 
                // produced the degrees sign.
                return 176;
            case 214:
                // POI produced an umat O instead of an ellipses mark.
                return 8230;
            default:
                return ch;
        }
    }

}
