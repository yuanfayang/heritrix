package org.archive.modules.recrawl;

import org.archive.state.ModuleTestBase;

/**
 * @author pjack
 *
 */
public class PersistStoreProcessorTest extends ModuleTestBase {

    
    
    public void testHistory() {
        
    }

    @Override
    protected Class<?> getModuleClass() {
        return PersistStoreProcessor.class;
    }

    @Override
    protected Object makeModule() throws Exception {
        return new PersistStoreProcessor();
    }
    
}
