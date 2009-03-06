package org.archive.crawler.selftest;

import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.frontier.precedence.BaseUriPrecedencePolicy;
import org.archive.state.KeyManager;


/**
 * 
 * @author pjack
 *
 */
public class KeyWordUriPrecedencePolicy extends BaseUriPrecedencePolicy {

    @Override
    protected int calculatePrecedence(CrawlURI curi) {
        if (curi.getPrecedence() > 0) {
            return curi.getPrecedence();
        }
        return super.calculatePrecedence(curi);
    }


    static {
        KeyManager.addKeys(KeyWordUriPrecedencePolicy.class);
    }

}
