package org.archive.modules.recrawl;

import org.archive.state.ModuleTestBase;

/**
 * @author pjack
 *
 */
public class PersistLogProcessorTest extends ModuleTestBase {

    
    
    public void testHistory() {
        
    }

    @Override
    protected Class<?> getModuleClass() {
        return PersistLogProcessor.class;
    }

    @Override
    protected Object makeModule() throws Exception {
        return new PersistLogProcessor();
    }
    
}
