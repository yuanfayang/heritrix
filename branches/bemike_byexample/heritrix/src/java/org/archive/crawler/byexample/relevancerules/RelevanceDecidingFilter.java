package org.archive.crawler.byexample.relevancerules;

import java.util.logging.Logger;
import org.archive.crawler.deciderules.DecideRule;
import org.archive.crawler.deciderules.DecidingFilter;
import org.archive.crawler.framework.Filter;
import org.archive.crawler.settings.CrawlerSettings;

/**
 * Filter extending class similar to DecidingFilter, 
 * only that it's decide rules are defined internally via <i>setDecideRules</i> method
 * and not via UI settings. It is used in TermsIndexingProcessor to determine crawl sites relevance
 * 
 * @see org.archive.crawler.byexample.processors.TermsIndexingProcessor
 * @see org.archive.crawler.byexample.deciderules.DecidingFilter
 * 
 * @author Michael Bendersky
 *
 */
public class RelevanceDecidingFilter extends Filter{
    
    private static final Logger logger =
        Logger.getLogger(DecidingFilter.class.getName());
    private DecideRule myDecideRules; 
          
    public static final String ATTR_RELEVANCE_DECIDE_RULES = "relevance-decide-rules";

    public RelevanceDecidingFilter(String name) {
        super(name,
            "RelevanceDecidingFilter. A filter that applies one or " +
            "more DecideRules " +
            "to determine whether a URI is relevant (returns true) or " +
            "irrelevant (returns false).");        
    }
    
    public void setDecideRules(DecideRule decideRules){
        myDecideRules=decideRules;
    }
    
    protected boolean innerAccepts(Object o) {
        return myDecideRules.decisionFor(o) == DecideRule.ACCEPT;
    }

    /**
     * Note that configuration updates may be necessary. Pass to
     * constituent filters.
     */
    public void kickUpdate() {
        // TODO: figure out if there's any way to reconcile this with
        // overrides/refinement filters
        myDecideRules.kickUpdate();
    }

}
