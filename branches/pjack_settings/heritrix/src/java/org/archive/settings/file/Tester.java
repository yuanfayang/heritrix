package org.archive.settings.file;

import java.io.File;
import java.lang.management.ManagementFactory;

import javax.management.MBeanServer;

import org.archive.settings.NamedObject;
import org.archive.settings.jmx.JMXSheetManager;

public class Tester {


    public static void main(String args[]) throws Exception {
/*        File main = new File("/Users/pjack/Desktop/test");
        File sheets = new File(main, "sheets");
        File assoc = new File(main, "assoc");
        main.mkdir();
        sheets.mkdir();
        assoc.mkdir();
        
        File roots = new File(main, "roots.txt");
        new FileWriter(roots).close();
        
        File defaults = new File(sheets, "default.single");
        new FileWriter(defaults).close();
        
        FileSheetManager fsm = new FileSheetManager(main, 5);
        fsm.addRoot("html", new ExtractorHTML());
        fsm.addRoot("css", new ExtractorCSS());
        fsm.addRoot("js", new ExtractorJS());
        
        fsm.save(); */
        File main = new File("/Users/pjack/Desktop/test");
        FileSheetManager fsm = new FileSheetManager(main, 5);
        for (NamedObject no: fsm.getRoots()) {
            System.out.println(no.getName());
            System.out.println(no.getObject().getClass());
        }
        
        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        JMXSheetManager jsm = new JMXSheetManager(server, fsm);
        jsm.toString();

        System.out.println("Waiting.");
        Object w = new Object();
        synchronized (w) {
            w.wait();
        }
    }

}
