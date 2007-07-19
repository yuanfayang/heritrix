package org.archive.mojo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.JavaClass;


/**
 * Generates lists of subclasses based on a directory of .class files.
 * This class will recursively search a directory for .class files, building
 * a giant in-memory map of what extends what.  Once all the class files are
 * read & parsed, text files are created in a designated output directory.
 * 
 * <p>The text files are laid out similar to .class files.  The path to the
 * text file indicates the Java package name, and the name of the text file
 * is base class name.  The text file will contain one line per known subclass
 * of that Java class.  (Note that only public, nonabstract classes are listed.)
 * 
 * <p>Eg, when run against the Archive commons project, there will be a file
 * named <code>java/util/Iterator</code> in the output directory, and its
 * contents will look something like:
 * 
 * <pre>
 * org.archive.util.iterator.CompositeIterator
 * org.archive.util.iterator.LineReadingIterator
 * org.archive.util.iterator.LookaheadIterator
 * org.archive.util.iterator.RegexpLineIterator
 * org.archive.util.iterator.TransformingIteratorWrapper
 * </pre>
 * 
 * @author pjack
 *
 */
public class Generator {

    
    final private static String EXTENSION = ".subclasses";
    
    
    private File inputDir;
    private File outputDir;
    
    
    /**
     * Fully-qualified class names of public, nonabstract classes.
     */
    private Set<String> implementations = new HashSet<String>();
    
    
    /**
     * Maps subclass name to list of immediate superclass/interfaces.
     */
    private Map<String,Set<String>> superclasses 
        = new HashMap<String,Set<String>>();

    
    /**
     * Maps subclass name to list of all superclass/interfaces.
     * Includes the superclasses/superinterfaces of immediate
     * superclasses/superinterfaces.
     */
    private Map<String,Set<String>> expanded
        = new HashMap<String,Set<String>>();
    
    /**
     * Maps superclass name to list of implementations.
     */
    private Map<String,Set<String>> subclasses
        = new HashMap<String,Set<String>>();


    
    private Set<String> exclusions = new HashSet<String>();
    
    
    /**
     * Constructor.
     * 
     * @param config  Configuration parameters
     */
    public Generator(Config config) {
        this.inputDir = config.getInputDir();
        this.outputDir = config.getOutputDir();
        this.superclasses = new HashMap<String,Set<String>>();
    }


    /**
     * Adds the given collection of superclass names to the subclass->superclass
     * map {@link #superclasses}.  This is non-recursive:  No attempt is 
     * made to add the superclasses of the given superclasses.  That 
     * recursive expansion happens in {@link #expandSupers}, after all 
     * .class files have been parsed.
     * 
     * <p>For instance, when this phase is run against <code>rt.jar</code>
     * of the JDK, then {@link #superclasses} should include the following
     * mapping:
     * 
     * <pre>
     * java.awt.Button->{java.awt.Component, javax.accessibility.Accessible}
     * </pre>
     * 
     * @param sub      the fully-qualified name of the subclass
     * @param supers   the fully-qualified names of superclasses 
     *                     and/orinterfaces of that <code>sub</code>
     */
    private void addSupers(String sub, Collection<String> supers) {
        Set<String> set = superclasses.get(sub);
        if (set == null) {
            set = new HashSet<String>();
            superclasses.put(sub, set);
        }
        set.addAll(supers);
    }
    

    
    /**
     * Create the {@link #expanded} map based on the {@link #superclasses} map.
     * Recursively expand subclass->superclass map.  All known superclasses
     * of the listed superclasses are included.
     * 
     * <p>For instance, if {@link #superclasses} contained these mappings:
     * 
     * <pre>
     * java.awt.Button->{java.awt.Component, javax.accessibility.Accessible}
     * java.awt.Component->
     * {java.lang.Object, java.awt.image.ImageObserver, 
     * java.awt.MenuContainer, java.io.Serializable}
     * </pre>
     * 
     * Then after a call to this method, its mappings would change to:
     * 
     * <pre>
     * java.awt.Button->
     * {java.awt.Component, javax.accessibility.Accessible,
     * java.lang.Object, java.awt.image.ImageObserver,  java.awt.MenuContainer, 
     * java.io.Serializable}
     * java.awt.Component->
     * {java.lang.Object, java.awt.image.ImageObserver, 
     * java.awt.MenuContainer, java.io.Serializable}
     * </pre>
     */
    private void expandSupers() {
        for (Map.Entry<String,Set<String>> me: superclasses.entrySet()) {
            expandSupers(me.getKey(), me.getValue());
        }
        superclasses = null;
    }
    

    
    private void expandSupers(String sub, Set<String> supers) {
        Set<String> ex = expanded.get(sub);
        if (ex == null) {
            ex = new HashSet<String>();
            ex.addAll(superclasses.get(sub));
            expanded.put(sub, ex);
        }
        ex.addAll(supers);
        for (String s: supers) {
            Set<String> n = superclasses.get(s);
            if (n != null) {
                expandSupers(sub, n);
            }
        }
    }
    
    
    /**
     * Create the {@link #subclasses} map based on the {@link #expanded}
     * map.  
     */
    private void createSubs() {
        for (Map.Entry<String,Set<String>> me: expanded.entrySet()) {
            if (implementations.contains(me.getKey())) {
                for (String superc: me.getValue()) {
                    Set<String> subs = subclasses.get(superc);
                    if (subs == null) {
                        subs = new TreeSet<String>();
                        subclasses.put(superc, subs);
                    }
                    subs.add(me.getKey());
                }
            }
        }
        expanded = null;
    }

    
    /**
     * Generates 
     * @throws IOException
     */
    public void generate() throws IOException {
        generate(inputDir);
        expandSupers();
        createSubs();
        implementations.removeAll(exclusions);
        writeSubs();
    }
    
    
    private void generate(File dir) throws IOException {
        for (File f: dir.listFiles()) {
            if (f.isDirectory()) {
                generate(f);
            } else if (f.getName().endsWith(".class")) {
                processClass(f);
            }
        }
    }
    
    
    private void processClass(File f) throws IOException {
        FileInputStream finp = null;
        try {
            finp = new FileInputStream(f);
            ClassParser cp = new ClassParser(finp, f.getName());
            JavaClass clazz = cp.parse();
            String sub = clazz.getClassName();
            addSupers(sub, Collections.singleton(clazz.getSuperclassName()));
            addSupers(sub, Arrays.asList(clazz.getInterfaceNames()));
            if (clazz.isPublic() && !clazz.isAbstract()) {
                implementations.add(sub);
            }
        } finally {
            if (finp != null) try {
                finp.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    
    private void writeSubs() throws IOException {
        for (Map.Entry<String,Set<String>> me: subclasses.entrySet()) {
            if (!me.getValue().isEmpty()) {
                String superclass = me.getKey();
                String path = superclass.replace('.', '/');
                File file = new File(outputDir, path + EXTENSION);
                file.getParentFile().mkdirs();
    
                FileWriter fw = null;
                try {
                    fw = new FileWriter(file, true);
                    for (String sub: me.getValue()) {
                        fw.write(sub);
                        fw.write('\n');
                    }
                } finally {
                    if (fw != null) try {
                        fw.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }


    public static void main(String args[]) throws Exception {
        Config config = new Config();
        File input = new File(args[0]);
        File output = new File(args[1]);
        output.mkdirs();
        
        config.setInputDir(input);
        config.setOutputDir(output);
        Generator engine = new Generator(config);
        engine.generate();
    }

}
