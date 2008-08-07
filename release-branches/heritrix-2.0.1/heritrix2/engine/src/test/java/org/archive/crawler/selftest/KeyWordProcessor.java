package org.archive.crawler.selftest;

import java.util.regex.Pattern;

import org.archive.crawler.datamodel.CrawlURI;
import org.archive.modules.Processor;
import org.archive.modules.ProcessorURI;
import org.archive.state.Key;
import org.archive.state.KeyManager;


/**
 * An example analysis module that prioritizes outlinks of URIs that contain
 * a certain keyword over the outlinks of URIs that do not.
 * 
 * <p>This is just a proof-of-concept; it isn't appropriate for actual 
 * production crawls, and so it lives with the test code.  This module has 
 * the following limitations:
 * 
 * <ol>
 * <li>It doesn't parse HTML content; so trying to match a keyword of "body"
 * would match.</li>
 * <li>It doesn't do any language analysis (eg, "political" if "politics" is 
 * the specified keyword).</li>
 * <li>It can't match more than one keyword.</li>
 * <li>It doesn't consider the number of times the keyword appears.</li>
 * </ol>
 * 
 * And so on.  However, this module does provide a simple example of how to
 * modify precedence values of a URI's links based on that URI's content.
 * 
 * @author pjack
 */
public class KeyWordProcessor extends Processor {


    /**
     * Regular expression used to detect the presence of a keyword.
     */
    final public static Key<Pattern> PATTERN = 
        Key.make(Pattern.compile("\\bkeyword\\b"));

    /**
     * Precedence value to assign to discovered links of URIs that match
     * the pattern.
     */
    final public static Key<Integer> FOUND_PRECEDENCE =
        Key.make(1);

    
    /**
     * Precedence value to assign to discovered links of URIs that do not
     * match the pattern.
     */
    final public static Key<Integer> NOT_FOUND_PRECEDENCE =
        Key.make(10);

    
    @Override
    protected void innerProcess(ProcessorURI uri) throws InterruptedException {
        CrawlURI curi = (CrawlURI)uri;
        try {
            Pattern pattern = uri.get(this, PATTERN);
            CharSequence seq = uri.getRecorder().getReplayCharSequence();
            int precedence;
            if (pattern.matcher(seq).find()) {
                precedence = uri.get(this, FOUND_PRECEDENCE);
            } else {
                precedence = uri.get(this, NOT_FOUND_PRECEDENCE);
            }
            for (CrawlURI c: curi.getOutCandidates()) {
                c.setPrecedence(precedence);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected boolean shouldProcess(ProcessorURI uri) {
        if (!uri.getContentType().equals("text/html")) {
            return false;
        }
        return uri instanceof CrawlURI;
    }

    
    static {
        KeyManager.addKeys(KeyWordProcessor.class);
    }
    
}
