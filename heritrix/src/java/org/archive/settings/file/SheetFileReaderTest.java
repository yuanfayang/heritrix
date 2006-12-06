/* Copyright (C) 2006 Internet Archive.
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
 * SheetFileReaderTest.java
 * Created on October 24, 2006
 *
 * $Header$
 */
package org.archive.settings.file;

import java.io.StringReader;

import org.archive.crawler2.extractor.ExtractorHTML;
import org.archive.settings.MemorySheetManager;
import org.archive.settings.SheetManager;
import org.archive.settings.SingleSheet;
import org.archive.settings.path.PathChanger;

import junit.framework.TestCase;

public class SheetFileReaderTest extends TestCase {

    final private static String TEST_DATA = 
        "# ignore me, I'm a comment\n" +
        "html1._impl=org.archive.crawler2.extractor.ExtractorHTML\n" +
        "html1.DECIDE_RULES.RULES._impl=java.util.ArrayList\n" + 
        "html1.DECIDE_RULES.RULES.0._impl=org.archive.crawler2.deciderules.AcceptDecideRule\n" +
        "html1.DECIDE_RULES.RULES.1._impl=org.archive.crawler2.deciderules.RejectDecideRule\n" +
//        "html1.DECIDE_RULES.RULES.1.ENABLED=false\n" +
        "html1.DECIDE_RULES.RULES.2._impl=org.archive.crawler2.deciderules.DecideRuleSequence\n" +
        "html1.DECIDE_RULES.RULES.2.RULES._impl=java.util.ArrayList\n" +
        "html1.DECIDE_RULES.RULES.2.RULES.0._impl=org.archive.crawler2.deciderules.AcceptDecideRule\n" +
        "html1.ENABLED=false";
    
    
    
    public void testSheetFileReader() throws Exception {
        StringReader sr = new StringReader(TEST_DATA);
        SheetManager mgr = new MemorySheetManager(new ExtractorHTML());
        SingleSheet sheet = mgr.getDefault();
        new PathChanger().change(sheet, new SheetFileReader(sr));
    }
}
