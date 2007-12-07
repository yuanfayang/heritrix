package org.archive.crawler;


import java.io.File;
import java.io.FileOutputStream;

import org.archive.settings.file.Validator;
import org.archive.util.FileUtils;
import org.archive.util.TmpDirTestCase;

/**
 * @author pjack
 *
 */
public class BasicProfileTest extends TmpDirTestCase {

    
    /**
     * Tests the default profile that gets put in the heritrix tarball.
     */
    public void testDefaultProfile() throws Exception {
        File srcDir = new File("src/main/conf/jobs");
        if (!srcDir.exists()) {
            srcDir = new File("dist/src/main/conf/jobs");
        }
        if (!srcDir.exists()) {
            throw new IllegalStateException("Couldn't find default profile.");
        }
        for (File f: srcDir.listFiles()) {
            if (f.isDirectory() && !f.getName().startsWith(".")) {
                doTest(f);
            }
        }
    }

    private void doTest(File srcDir) throws Exception {
        System.out.println("\nNow testing " + srcDir.getName());
        File tmpDir = new File(getTmpDir(), "validatorTest");
        File configDir = new File(tmpDir, srcDir.getName());
        configDir.mkdirs();
        FileUtils.copyFiles(srcDir, configDir);

        File config = new File(configDir, "config.txt");
        new FileOutputStream(config).close();
        
        int errors = Validator.validate(config.getAbsolutePath());
        // We expect two errors:
        //  root:metadata:operator-contact-url=string, ENTER-A-CONTACT-HTTP-URL-FOR-CRAWL-OPERATOR
        //  root:metadata:operator-from=string, ENTER-A-CONTACT-EMAIL-FOR-CRAWL-OPERATOR
        assertEquals(2, errors);
    }
}
