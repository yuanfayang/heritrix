/**
 * 
 */
package org.archive.crawler.webui;

import java.io.PrintStream;
import java.util.Locale;
import java.util.TreeSet;

import org.archive.state.Key;
import org.archive.state.KeyManager;
import org.archive.state.Module;

/**
 * @author pjack
 *
 */
public class Wikify {

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
        for (String cname: Settings.getSubclasses(Module.class)) try {
            wikify(cname, System.out);
        } catch (ExceptionInInitializerError e) {
            e.printStackTrace();
        }

    }

    
    public static void wikify(String cname, PrintStream out) throws Exception {
        Class<?> c = Class.forName(cname);
        String moduleDesc = KeyManager.getModuleDescription(c, Locale.ENGLISH);
        out.println("=====" + c.getName());
        if (moduleDesc == null) {
            out.println(c.getName() + "; no description.");
        } else {
            out.println(moduleDesc);
        }
        out.println();
        TreeSet<String> keys = new TreeSet<String>(KeyManager.getKeys(c).keySet());
        for (String k: keys) {
            Key key = KeyManager.getKeys(c).get(k);
            out.print("* [");
            out.print(k);
            out.print("|");
            out.print(Text.baseName(key.getOwner().getName()));
            out.print(" ");
            out.print(k);
            out.println("]");
        }
    }
}
