package org.archive.modules.deciderules;

import org.archive.modules.ProcessorURI;

public class SpecialURIDecideRule extends PredicatedAcceptDecideRule {
	private static final long serialVersionUID = 1L;
	
	public SpecialURIDecideRule() {
		
	}

	@Override
    protected boolean evaluate(ProcessorURI uri) {
        return uri.getUURI().getScheme().equalsIgnoreCase("x-jseval");
    }
}
