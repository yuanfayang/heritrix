package org.archive.crawler.scope;

import org.archive.crawler.framework.CrawlScope;
import org.archive.processors.ProcessorURI;
import org.archive.processors.deciderules.DecideResult;

public class EmptyScope extends CrawlScope {

    public EmptyScope() {
        super(null); // FIXME: Ew
    }
    
    
    protected DecideResult innerDecide(ProcessorURI uri) {
        return DecideResult.REJECT;
    }
    
}
