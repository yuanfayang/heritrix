package org.archive.modules.recrawl;

import org.archive.state.ModuleTestBase;

/**
 * @author pjack
 *
 */
public class PersistLoadProcessorTest extends ModuleTestBase {

    
    
    public void testHistory() {
        
    }

    @Override
    protected Class<?> getModuleClass() {
        return PersistLoadProcessor.class;
    }

    @Override
    protected Object makeModule() throws Exception {
        return new PersistLoadProcessor();
    }
    
}
