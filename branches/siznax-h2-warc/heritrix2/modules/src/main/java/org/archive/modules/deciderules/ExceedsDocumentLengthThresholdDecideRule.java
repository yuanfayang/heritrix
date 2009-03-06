package org.archive.modules.deciderules;

import org.archive.modules.ProcessorURI;
import org.archive.state.KeyManager;



public class ExceedsDocumentLengthThresholdDecideRule 
extends NotExceedsDocumentLengthTresholdDecideRule {
    private static final long serialVersionUID = 3L;

    boolean decision(ProcessorURI curi, int contentlength) {
        return contentlength > curi.get(this, CONTENT_LENGTH_THRESHOLD);        
    }
    
    static {
        KeyManager.addKeys(ExceedsDocumentLengthThresholdDecideRule.class);
    }
}
