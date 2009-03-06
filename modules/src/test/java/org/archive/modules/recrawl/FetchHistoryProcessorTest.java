package org.archive.modules.recrawl;

import org.archive.state.ModuleTestBase;

/**
 * @author pjack
 *
 */
public class FetchHistoryProcessorTest extends ModuleTestBase {

    
    
    public void testHistory() {
        
    }

    @Override
    protected Class<?> getModuleClass() {
        return FetchHistoryProcessor.class;
    }

    @Override
    protected Object makeModule() throws Exception {
        return new FetchHistoryProcessor();
    }
    
}
