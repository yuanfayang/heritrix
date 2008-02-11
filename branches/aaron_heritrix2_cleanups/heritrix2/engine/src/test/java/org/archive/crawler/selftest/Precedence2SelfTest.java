package org.archive.crawler.selftest;

import java.io.File;

import org.archive.crawler.frontier.precedence.PrecedenceLoader;


/**
 * Tests that precedence values for URIs can be imported from an offline 
 * analysis.  This test crawls the same directory structure as 
 * {@link PrecedenceSelfTest1} and expects the URIs to be crawled in the same
 * order.  However, the result is achieved using a 
 * {@link org.archive.crawler.frontier.precedence.PreloadedUriPrecedencePolicy}
 * to load per-URI precedence information from an external file.
 * 
 * <p>Such a file could be generated from PageRank analysis of a previously
 * completed crawl; see {@link http://webteam.archive.org/confluence/display/Heritrix/Offline+PageRank+Analysis+Notes}.
 * (For this minimal functional test, the PreloadedUriPrecedencePolicy input
 * file was simply hand-generated.)
 * 
 * @author pjack
 */
public class Precedence2SelfTest extends Precedence1SelfTest {


    @Override
    protected String changeGlobalConfig(String globalSheetText) {
        int p1 = globalSheetText.indexOf("root:controller:frontier=");
        int p2 = globalSheetText.indexOf("\n", p1);
        String head = globalSheetText.substring(0, p2 + 1);
        String tail = globalSheetText.substring(p2);
        return head + 
            "root:controller:frontier:uri-precedence-policy=object, org.archive.crawler.frontier.precedence.PreloadedUriPrecedencePolicy\n" + 
            "root:controller:frontier:uri-precedence-policy:base-precedence=int, 5\n" +
            tail;
    }

    @Override
    protected void configureHeritrix() throws Exception {
        File src = new File(getReadyJobDir(), "rank.txt");
        File dest = new File(getReadyJobDir(), "state");
        String[] args = new String[] { 
                src.getAbsolutePath(), 
                dest.getAbsolutePath() 
        };

        PrecedenceLoader.main(args);
    }

}
