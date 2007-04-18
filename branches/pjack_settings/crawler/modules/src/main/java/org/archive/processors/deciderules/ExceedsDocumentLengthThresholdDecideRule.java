package org.archive.processors.deciderules;

import org.archive.processors.ProcessorURI;



public class ExceedsDocumentLengthThresholdDecideRule 
extends NotExceedsDocumentLengthTresholdDecideRule {


    private static final long serialVersionUID = 3L;


    boolean decision(ProcessorURI curi, int contentlength) {
        return contentlength > curi.get(this, CONTENT_LENGTH_THRESHOLD);        
    }
    
}
