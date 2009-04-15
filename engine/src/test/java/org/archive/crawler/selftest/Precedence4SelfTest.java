package org.archive.crawler.selftest;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import org.archive.util.IoUtils;


/**
 * Tests that operators can manually assign precedence values to individual 
 * URLs.
 * 
 * <p>This class crawls the same directory structure as 
 * {@link Precedence1SelfTest}, using the same number of sheets.  However, 
 * insteading of creating groups of URIs using SURT prefixes, the HiPri and
 * LoPri sheets are assigned to two individual URIs.  The test then assures
 * that the HiPri URI is crawled before anything else, and that the LoPri
 * URL is crawled after everything else.
 * 
 * @author pjack
 */
public class Precedence4SelfTest extends Precedence1SelfTest {


    @Override
    protected void verify() throws Exception {
        File crawlLog = new File(getLogsDir(), "crawl.log");
        BufferedReader br = null;
        List<String> crawled = new ArrayList<String>();
        try {
            br = new BufferedReader(new FileReader(crawlLog));
            for (String s = br.readLine(); s != null; s = br.readLine()) {
                s = s.substring(42);
                int i = s.indexOf(' ');
                s = s.substring(0, i);
                crawled.add(s);
            }
        } finally {
            IoUtils.close(br);
        }
        
        //assertEquals("dns:localhost", crawled.get(0));
        assertEquals("http://127.0.0.1:7777/robots.txt", crawled.get(0));
        assertEquals("http://127.0.0.1:7777/five/a.html", crawled.get(1));
        assertEquals("http://127.0.0.1:7777/five/b.html", crawled.get(crawled.size() - 1));
    }
    
    
    @Override
    protected void configure(/*JMXSheetManager sm*/) {
        //TODO:Springy fixme
//        sm.associate("HiPri", "http://(127.0.0.1:7777)/five/a.html");
//        sm.associate("LoPri", "http://(127.0.0.1:7777)/five/b.html");        
    }


}
