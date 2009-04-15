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
 * BdbModuleTest.java
 *
 * Created on Mar 8, 2007
 *
 * $Id:$
 */

package org.archive.settings.file;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.archive.settings.Checkpointer;
import org.archive.settings.DefaultCheckpointRecovery;
import org.archive.spring.ConfigPath;
import org.archive.util.FileUtils;
import org.archive.util.TmpDirTestCase;



/**
 * @author pjack
 *
 */
public class BdbModuleTest extends TmpDirTestCase {

    
    
    public void testCheckpoint() throws Exception {
        doCheckpoint();
        
        File first = new File(getTmpDir(), "first");
        File checkpointDir = new File(getTmpDir(), "checkpoint");
        
        File second = new File(getTmpDir(), "second");
        FileUtils.deleteDir(second);
        
        DefaultCheckpointRecovery cr = new DefaultCheckpointRecovery("job");
        cr.getFileTranslations().put(first.getAbsolutePath(), 
                second.getAbsolutePath());
//        SheetManager mgr2 = Checkpointer.recover(checkpointDir, cr);
//        BdbModule bdb2 = (BdbModule)mgr2.getRoot().get("module");
//        Map<String,String> testData2 = bdb2.getBigMap("testData", false,
//                String.class, String.class);
        Map<String,String> map1 = new HashMap<String,String>();
        for (int i = 0; i < 1000; i++) {
            map1.put(String.valueOf(i), String.valueOf(i * 2));
        }

//        Map<String,String> map2 = dump(testData2);
//        assertEquals(map1, map2);
    }

    
    private void doCheckpoint() throws Exception {
        File first = new File(getTmpDir(), "first");
        FileUtils.deleteDir(first);
        
        File firstState = new File(first, "state");
//        MemorySheetManager mgr = new MemorySheetManager();
        
        BdbModule bdb = new BdbModule();
//        mgr.getRoot().put("module", bdb);
        bdb.setDir(new ConfigPath("test",firstState.getAbsolutePath()));
//        mgr.getGlobalSheet().set(bdb, BdbModule.DIR, firstState.getAbsolutePath());
        bdb.start();
        
        BdbModule.BdbConfig config = new BdbModule.BdbConfig();
        config.setAllowCreate(true);
        bdb.openDatabase("testOpen", config, false);
        
        Map<String,String> testData = bdb.getBigMap("testData", false, 
                String.class, String.class);
        for (int i = 0; i < 1000; i++) {
            testData.put(String.valueOf(i), String.valueOf(i * 2));
        }
        
        File checkpointDir = new File(getTmpDir(), "checkpoint");
        checkpointDir.mkdirs();
//        Checkpointer.checkpoint(mgr, checkpointDir);        
        bdb.stop();
    }
    
    private Map<String,String> dump(Map<String,String> src) {
        HashMap<String,String> dest = new HashMap<String,String>();
        for (String k: src.keySet()) {
            dest.put(k, src.get(k));
        }
        return dest;
    }
}
