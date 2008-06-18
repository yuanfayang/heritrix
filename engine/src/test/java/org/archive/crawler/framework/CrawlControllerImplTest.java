/* 
 * Copyright (C) 2007 Internet Archive.
 *
 * This file is part of the Heritrix web crawler (crawler.archive.org).
 *
 * Heritrix is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * any later version.
 *
 * Heritrix is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser Public License for more details.
 *
 * You should have received a copy of the GNU Lesser Public License
 * along with Heritrix; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * CrawlControllerTest.java
 *
 * Created on Feb 1, 2007
 *
 * $Id:$
 */

package org.archive.crawler.framework;

import static org.archive.util.TmpDirTestCase.DEFAULT_TEST_TMP_DIR;
import static org.archive.util.TmpDirTestCase.TEST_TMP_SYSTEM_PROPERTY_NAME;

import java.io.File;
import java.io.FileWriter;

import org.archive.modules.net.BdbServerCache;
import org.archive.settings.MemorySheetManager;
import org.archive.settings.SingleSheet;
import org.archive.settings.file.BdbModule;
import org.archive.state.ModuleTestBase;
import org.archive.state.Path;
import org.archive.util.IoUtils;

/**
 * 
 * @author pjack
 */
public class CrawlControllerImplTest extends ModuleTestBase {
    
    
    @Override
    protected Class<?> getModuleClass() {
        return CrawlControllerImpl.class;
    }

    // Public so other tests can use it
    @Override
    public Object makeModule() throws Exception {
        return makeTempCrawlController();
    }
    
    
    
    // TODO TESTME

    
    public static CrawlControllerImpl makeTempCrawlController() throws Exception {
        String tmpPath = System.getProperty(TEST_TMP_SYSTEM_PROPERTY_NAME);
        if (tmpPath == null) {
            tmpPath = DEFAULT_TEST_TMP_DIR;
        }
        File tmp = new File(tmpPath);
        if (!tmp.exists()) {
            tmp.mkdirs();
        }
        
        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter(new File(tmp, "seeds.txt"));
            fileWriter.write("http://www.pandemoniummovie.com");
            fileWriter.close();
        } finally {
            IoUtils.close(fileWriter);
        }

        MemorySheetManager manager = new MemorySheetManager();
        SingleSheet def = manager.getGlobalSheet();
        
        File state = new File(tmp, "state");
        state.mkdirs();
        
        File checkpoints = new File(tmp, "checkpoints");
        checkpoints.mkdirs();
        
        BdbModule bdb = new BdbModule();
        bdb.setPath(state.getAbsolutePath());
//        def.set(bdb, BdbModule.DIR, state.getAbsolutePath());
        bdb.initialTasks(def);
        
        Path cp = new Path(checkpoints.getAbsolutePath());
        
        CrawlControllerImpl controller = new CrawlControllerImpl();
        def.set(controller, CrawlControllerImpl.SHEET_MANAGER, manager);
        def.set(controller, CrawlControllerImpl.SERVER_CACHE, 
                new BdbServerCache());
        def.set(controller, CrawlControllerImpl.CHECKPOINTS_DIR, cp);
        controller.initialTasks(def);
        return controller;
    }

    
    @Override
    protected void verifySerialization(Object first, byte[] firstBytes, 
            Object second, byte[] secondBytes) throws Exception {
        // TODO TESTME
    }

    
    
}
