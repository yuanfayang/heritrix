package org.archive.crawler.framework;

import org.archive.crawler.datamodel.CrawlOrder;
import org.archive.crawler.datamodel.UriUniqFilter;
import org.archive.crawler.frontier.CostAssignmentPolicy;
import org.archive.crawler.frontier.QueueAssignmentPolicy;
import org.archive.state.Key;
import org.archive.state.KeyManager;

public class Bootstrap {

    
    final public static Key<CrawlOrder> ORDER = Key.makeNull(CrawlOrder.class);
    
    
    final public static Key<CrawlController> CONTROLLER = Key.makeNull(CrawlController.class);

    
    // FIXME: We're going to need multiple roots
    final public static Key<UriUniqFilter> ALREADY_SEEN = Key.makeNull(UriUniqFilter.class);
    
    
    final public static Key<QueueAssignmentPolicy> QUEUE_ASSIGNMENT_POLICY = Key.makeNull(QueueAssignmentPolicy.class);
    
    static {
        KeyManager.addKeys(Bootstrap.class);
    }

}
