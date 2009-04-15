package org.archive.crawler;


import java.io.File;

import org.archive.spring.PathSharingContext;
import org.archive.util.FileUtils;
import org.archive.util.TmpDirTestCase;
import org.springframework.beans.factory.BeanCreationException;

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

//        File config = new File(configDir, "config.txt");
//        new FileOutputStream(config).close();
//        
//        int errors = Validator.validate(config.getAbsolutePath());
        
        int errors = 0; 
        
        PathSharingContext ac = null;
        try {
            File config = new File(configDir,"crawler-beans.xml");
            ac = new PathSharingContext(config.getAbsolutePath());
        } catch (BeanCreationException bce){
            // TODO: change to more informative exception
            if(bce.getCause() instanceof IllegalArgumentException) {
//                errors = 1;
            }
        } finally {
            if(ac!=null) {
                ac.destroy();
            }
        }
        
        // We expect one error:
        //  root:metadata:operator-contact-url=string, ENTER-A-CONTACT-HTTP-URL-FOR-CRAWL-OPERATOR
        assertEquals(1, errors);
    }
}
