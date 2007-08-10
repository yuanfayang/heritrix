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

import org.archive.crawler.datamodel.CrawlOrder;
import org.archive.crawler.scope.DecidingScope;
import org.archive.settings.MemorySheetManager;
import org.archive.settings.SingleSheet;
import org.archive.settings.file.BdbModule;
import org.archive.state.FileModule;
import org.archive.state.ModuleTestBase;
import org.archive.util.IoUtils;

/**
 * 
 * @author pjack
 */
public class CrawlControllerTest extends ModuleTestBase {
    
    
    @Override
    protected Class getModuleClass() {
        return CrawlController.class;
    }

    // Public so other tests can use it
    @Override
    public Object makeModule() throws Exception {
        return makeTempCrawlController();
    }
    
    
    
    // TODO TESTME

    
    public static CrawlController makeTempCrawlController() throws Exception {
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
        
        CrawlOrder order = new CrawlOrder();
                
        def.set(order, CrawlOrder.DISK_PATH, tmp.getAbsolutePath());
        def.set(order, CrawlOrder.HTTP_USER_AGENT, "Heritrix (+@OPERATOR_CONTACT_URL@) abc");
        def.set(order, CrawlOrder.OPERATOR_CONTACT_URL, "http://www.example.com/OurCrawlDetails");
        def.set(order, CrawlOrder.OPERATOR_FROM, "complain@example.com");
        
        BdbModule bdb = new BdbModule();
        def.set(bdb, BdbModule.DIR, state.getAbsolutePath());
        bdb.initialTasks(def);
        
        FileModule cp = new FileModule();
        def.set(cp, FileModule.PATH, checkpoints.getAbsolutePath());
        cp.initialTasks(def);
        
        CrawlController controller = new CrawlController();
        CrawlScope scope = new DecidingScope();
        scope.initialTasks(def);
        def.set(controller, CrawlController.SHEET_MANAGER, manager);
//        def.set(controller, CrawlController.BDB, bdb);
        def.set(controller, CrawlController.ORDER, order);
//        def.set(controller, CrawlController.SCOPE, scope);
        def.set(controller, CrawlController.SERVER_CACHE, 
                new CrawlerServerCache());
        def.set(controller, CrawlController.CHECKPOINTS_DIR, cp);
        controller.initialTasks(def);
        return controller;
    }

    
    @Override
    protected void verifySerialization(Object first, byte[] firstBytes, 
            Object second, byte[] secondBytes) throws Exception {
        // TODO TESTME
    }

    
    
}
