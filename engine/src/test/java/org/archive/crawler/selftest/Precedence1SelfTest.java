package org.archive.crawler.selftest;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.lang.management.ManagementFactory;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.archive.crawler.framework.Engine;
import org.archive.openmbeans.annotations.BeanProxy;
import org.archive.settings.jmx.JMXSheetManager;
import org.archive.util.IoUtils;

/**
 * Tests that operators can create precedence groups for URIs, and that URIs
 * in one group are crawled before URIs in another group per operator preference.
 * 
 * <p>The embedded Jetty HTTP server for this test provides the following
 * document tree:
 * 
 * <ul>
 * <li>seed.html</li>
 * <li>one/</li>
 *     <ul>
 *     <li>a.html</li>
 *     <li>b.html</li>
 *     <li>c.html</li>
 *     </ul>
 * <li>five/</li>
 *     <ul>
 *     <li>a.html</li>
 *     <li>b.html</li>
 *     <li>c.html</li>
 *     </ul>
 * <li>ten/</li>
 *     <ul>
 *     <li>a.html</li>
 *     <li>b.html</li>
 *     <li>c.html</li>
 *     </ul>
 * </ul>
 * 
 * (See the <code>engine/testdata/selftest/Precedence1SelfTest</code>
 * directory to view these files.) The <code>seed.html</code> file contains
 * links to <code>five/a.html</code>, <code>ten/a.html</code>, and
 * <code>one/a.html</code>, in that order.  The <code>a.html</code> files link 
 * to to the <code>b.html</code> files, and the <code>b.html</code> link to 
 * the <code>c.html</code> files, which have no out links.
 *
 * <p>Ordinarily Heritrix would crawl these in (roughly) the order the links
 * are discovered:
 * 
 * <ol>
 * <li>seed.html</li>
 * <li>five/a.html</li>
 * <li>ten/a.html</li>
 * <li>one/a.html</li>
 * <li>five/b.html</li>
 * <li>ten/b.html</li>
 * <li>one/b.html</li>
 * <li>five/c.html</li>
 * <li>ten/c.html</li>
 * <li>one/c.html</li>
 * </ol>
 * 
 * <p>However, the crawl configuration for this test uses a 
 * {@link BaseUriPrecedencePolicy} instead of the default 
 * {@link org.archive.crawler.frontier.policy.CostUriPrecedencePolicy}.  The
 * <code>BasePrecedencePolicy</code> is configured so that all URIs have a 
 * precedence value of 5 unless otherwise specified.
 * 
 * <p>There is a sheet named <code>HiPri</code> that overrides the 
 * <code>base-precedence</code> to be 1 instead of 5; thus URIs associated
 * with the HiPri sheet should be crawled before other URIs.
 * Similarly, there is a sheet named <code>LoPri</code> that overrides
 * <code>base-precedence</code> to be 10 instead of 5.  URLs associated with
 * LoPri should be crawled after other URLs.
 * 
 * <p>The <code>one/</code> directory is associated with the HiPri sheet, and
 * the <code>ten/</code> directory is associated with the LoPri sheet.  This
 * creates three "groups" of URIs: one, five and ten.  All of the URIs in 
 * group "one" should be crawled before any of the URIs in group "five" are
 * crawled.  Similarly, all of the URIs in group "five" should be crawled before
 * any of the URIs in group "ten".
 *
 * <p>So the final order in which URLs should be crawled in this test is:
 * 
 * <ol>
 * <li>seed.html</li>
 * <li>one/a.html</li>
 * <li>one/b.html</li>
 * <li>one/c.html</li>
 * <li>five/a.html</li>
 * <li>five/b.html</li>
 * <li>five/c.html</li>
 * <li>ten/a.html</li>
 * <li>ten/b.html</li>
 * <li>ten/c.html</li>
 * </ol>
 * 
 * This tests ensures that the documents were crawled in the correct order.
 * 
 * <p>Although this test uses the directory structure of the URIs to group the URIs
 * into precedence groups, because the test executes on just one machine.
 * But the same basic configuration could be used to group URIs by any SURT
 * prefix -- by host or by domain, even by top-level domain.  So an operator
 * could associate HiPri with all .gov sites to ensure that all .gov URIs
 * are crawled before any non-.gov URIs.
 * 
 * @author pjack
 */
public class Precedence1SelfTest extends SelfTestBase {


    /**
     * Expected results of the crawl.
     */
    final private static String EXPECTED =
        "http://127.0.0.1:7777/robots.txt\n" + 
        "http://127.0.0.1:7777/seed.html\n" + 
        "http://127.0.0.1:7777/one/a.html\n" + 
        "http://127.0.0.1:7777/one/b.html\n" + 
        "http://127.0.0.1:7777/one/c.html\n" + 
        "http://127.0.0.1:7777/five/a.html\n" + 
        "http://127.0.0.1:7777/five/b.html\n" + 
        "http://127.0.0.1:7777/five/c.html\n" + 
        "http://127.0.0.1:7777/ten/a.html\n" + 
        "http://127.0.0.1:7777/ten/b.html\n" +
        "http://127.0.0.1:7777/ten/c.html\n";
    
    
    @Override
    protected void verify() throws Exception {
        File crawlLog = new File(getLogsDir(), "crawl.log");
        BufferedReader br = null;
        String crawled = "";
        try {
            br = new BufferedReader(new FileReader(crawlLog));
            for (String s = br.readLine(); s != null; s = br.readLine()) {
                s = s.substring(42);
                int i = s.indexOf(' ');
                s = s.substring(0, i);
                crawled = crawled + s + "\n";
            }
        } finally {
            IoUtils.close(br);
        }
        
        assertEquals(EXPECTED, crawled);
    }

    @Override
    protected String changeGlobalConfig(String globalSheetText) {
        int p1 = globalSheetText.indexOf("root:controller:frontier=");
        int p2 = globalSheetText.indexOf("\n", p1);
        String head = globalSheetText.substring(0, p2 + 1);
        String tail = globalSheetText.substring(p2);
        return head + 
            "root:controller:frontier:uri-precedence-policy=object, org.archive.crawler.frontier.precedence.BaseUriPrecedencePolicy\n" + 
            "root:controller:frontier:uri-precedence-policy:base-precedence=int, 5\n" +
            tail;
    }

    
    protected void configureHeritrix() throws Exception {
        ObjectName engineName = getEngine();
        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        Engine engine = BeanProxy.proxy(server, engineName, Engine.class);
        ObjectName smName = engine.getSheetManagerStub("ready-basic");
        JMXSheetManager sm = BeanProxy.proxy(server, smName, JMXSheetManager.class);
        configure(sm);
    }
    

    protected void configure(JMXSheetManager sm) {
        sm.associate("HiPri", "http://(127.0.0.1:7777)/one");
        sm.associate("LoPri", "http://(127.0.0.1:7777)/ten");        
    }
    

}
