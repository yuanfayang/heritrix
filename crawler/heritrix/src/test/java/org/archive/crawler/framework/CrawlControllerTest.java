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
import java.util.Map;

import org.archive.crawler.datamodel.CrawlOrder;
import org.archive.crawler.scope.DecidingScope;
import org.archive.settings.MemorySheetManager;
import org.archive.settings.SettingsMap;
import org.archive.settings.SingleSheet;
import org.archive.settings.file.BdbModule;
import org.archive.state.StateProcessorTestBase;
import org.archive.util.IoUtils;

/**
 * 
 * @author pjack
 */
public class CrawlControllerTest extends StateProcessorTestBase {
    
    
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
        SingleSheet def = manager.getDefault();
        
        File state = new File(tmp, "state");
        state.mkdirs();
        
        CrawlOrder order = new CrawlOrder();
        Map<String,String> headers = new SettingsMap<String>(def, String.class);
        headers.put("user-agent", "Heritrix (+http://www.archive.org) abc");
        headers.put("from", "info@archive.org");
        
        def.set(order, CrawlOrder.DISK_PATH, tmp.getAbsolutePath());
        def.set(order, CrawlOrder.HTTP_HEADERS, headers);
        
        BdbModule bdb = new BdbModule();
        def.set(bdb, BdbModule.DIR, state.getAbsolutePath());
        bdb.initialTasks(def);
        
        CrawlController controller = new CrawlController();
        CrawlScope scope = new DecidingScope();
        scope.initialTasks(def);
        def.set(controller, CrawlController.SHEET_MANAGER, manager);
//        def.set(controller, CrawlController.BDB, bdb);
        def.set(controller, CrawlController.ORDER, order);
//        def.set(controller, CrawlController.SCOPE, scope);
        def.set(controller, CrawlController.SERVER_CACHE, 
                new CrawlerServerCache());
        controller.initialTasks(def);
        return controller;
    }

    
    @Override
    protected void verifySerialization(Object first, byte[] firstBytes, 
            Object second, byte[] secondBytes) throws Exception {
        // TODO TESTME
    }

    
    
}
