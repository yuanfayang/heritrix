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
 * ValidatorTest.java
 *
 * Created on Jun 25, 2007
 *
 * $Id:$
 */

package org.archive.settings.file;

import java.io.File;

import org.archive.util.FileUtils;
import org.archive.util.TmpDirTestCase;

/**
 * @author pjack
 *
 */
public class ValidatorTest extends TmpDirTestCase {

    
    /**
     * Tests the default profile that gets put in the heritrix tarball.
     */
    public void testDefaultProfile() throws Exception {
	File srcDir = new File("../src/main/conf/profiles/default");
        if (!srcDir.exists()) {
            srcDir = new File("src/main/conf/profiles/default");
        }
        if (!srcDir.exists()) {
//            throw new IllegalStateException("Couldn't find default profile.");
            return;
        }

        File tmpDir = new File(getTmpDir(), "validatorTest");
        FileUtils.copyFiles(srcDir, tmpDir);

        File config = new File(tmpDir, "config.txt");
        
        int errors = Validator.validate(config.getAbsolutePath());
        assertEquals(0, errors);
    }
}