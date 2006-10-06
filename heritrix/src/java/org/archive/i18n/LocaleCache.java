package org.archive.i18n;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * A cache of locale-sensitive properties.  This class is similar to 
 * {@link java.util.PropertyResourceBundle}, except that the file format 
 * it uses is hopefully easier to work with.
 * 
 * <p>First, PropertyResourceBundle files are saved using the ASCII character
 * encoding.  Although the JVM provides tools for converting between native
 * character encodings and ASCII, I think UTF8 is a better encoding format.
 * Most text editors these days (notably Eclipse) support UTF8, and this 
 * way a translator can just save new resource files directly to disk.
 * 
 * <p>Also, the file format used by this class is multiline.  Instead of 
 * having key/value pairs on one line separated by an equals sign, the key
 * appears on its own line and the value occupies the next <i>n</i> lines
 * after that; values are delimited by blank lines or the end-of-file.
 * If this were a Java properties file:
 * 
 * <pre>
 * pi=3.14
 * desc=This is an example of long description text.
 * </pre>
 * 
 * Then this is an equivalent LocaleCache resource file:
 * 
 * <pre>
 * pi:
 * 3.14
 * 
 * desc:
 * This is an example
 * of long description
 * text.
 * </pre>
 * 
 * @author pjack
 */
public class LocaleCache {

    /** Logger. */
    final private static Logger LOGGER = Logger.getLogger(LocaleCache.class.getName());

    /**
     * A class and a locale.  Used as a hashtable key.
     */
    private static class ClassLocale {
        public Class c;
        public Locale locale;
        
        public ClassLocale(Class c, Locale locale) {
            this.c = c;
            this.locale = locale;
        }
        
        
        public boolean equals(Object o) {
            if (!(o instanceof ClassLocale)) {
                return false;
            }
            ClassLocale cl = (ClassLocale)o;
            return cl.c.equals(c) && cl.locale.equals(locale);
        }
        
        
        public int hashCode() {
            return c.hashCode() ^ locale.hashCode();
        }
    }
    
    
    /**
     * Maps class-and-locale to the properties for that class in that locale.
     */
    final static private Map<ClassLocale,Map<String,String>> properties 
     = new ConcurrentHashMap<ClassLocale,Map<String,String>>();


    /** Static utility library. */
    private LocaleCache() {
    }


    /**
     * Returns the properties for the given class and locale.  An attempt will
     * be made to load a UTF8-encoded resource file that contains the 
     * key/value pairs for the class and locale.  The name of the resource
     * is based on the class name, followed by a period, followed by the
     * locale, followed by the <code>.utf8</code> extension.
     * 
     * <p>Several files will be attempted based on the given locale.  If none
     * of those resource files are found, then this method returns an empty
     * map.  
     * 
     * <p>Let <i>class</i> be the given class's name, <i>locale1</i> be the 
     * given locale and <i>locale2</i> be the JVM's default locale.  Then
     * the resource filenames that will be attempted are:
     * 
     * <ol>
     * <li><code>clas_language1_country1_variant1.utf8</code></li>
     * <li><code>class_language1_country1.utf8</code></li>
     * <li><code>class_language1.utf8</code></li>
     * <li><code>class_language2_country2_variant2.utf8</code></li>
     * <li><code>class_language2_country2.utf8</code></li>
     * <li><code>class_language2.utf8</code></li>
     * </ol>
     * 
     * <p>That's the same search order used by PropertyResourceBundle, except
     * the extension is different.
     * 
     * @param c        the class whose locale-sensitive properties to return
     * @param locale   the locale 
     * @return  the properties for that class in that locale
     * @throws  IllegalStateException  if an IO error occurs
     */
    public static Map<String,String> load(Class c, Locale locale) {
        ClassLocale cl = new ClassLocale(c, locale);
        Map<String,String> result = properties.get(cl);
        if (result != null) {
            return result;
        }
        result = localizedInfo(c, locale);
        properties.put(cl, result);
        return result;
    }


    /**
     * Returns the base name of the given class.  Eg, returns "Foo" for
     * "org.archive.Foo" and return "Bar" for "Bar".
     * 
     * @param c  the class whose base name to return
     * @return   the base name of that class
     */
    private static String getBaseName(Class c) {
        String name = c.getName();
        int p = name.lastIndexOf('.');
        return (p >= 0) ? name.substring(p + 1) : name;
    }


    /**
     * Returns true if the string is non-null and non-blank.
     * 
     * @param s  the string to test
     * @return   true if the string is non-null and non-blank
     */
    private static boolean exists(String s) {
        if (s == null) {
            return false;
        }
        return s.length() > 0;
    }


    /**
     * Adds several variations of the given locale to the given list.  The
     * variations depend on whether the language, country and variant 
     * components of the locale exist or not.
     * 
     * @param list    the list of variants 
     * @param locale  the locale whose variants to add
     */
    private static void addWhatExists(List<String> list, Locale locale) {
        String language = locale.getLanguage();
        String country = locale.getCountry();
        String variant = locale.getVariant();
        boolean languageExists = exists(language);
        boolean countryExists = exists(country);
        boolean variantExists = exists(variant);
        if (languageExists && countryExists && variantExists) {
            list.add(language + "_" + country + "_" + variant);
        }
        if (languageExists && countryExists) {
            list.add(language + "_" + country);
        }
        if (languageExists) {
            list.add(language);
        }
    }


    /**
     * Returns the localized properties for the given class and locale.
     * This method bypasses the {@link #properties} map and loads directly
     * from disk.
     * 
     * @param c   the class whose locale-sensitive properties to return
     * @param locale  the locale
     * @return  the loaded properties
     * @throws  IllegalStateException   if an IO error occurs
     */
    private static Map<String,String> localizedInfo(Class c, Locale locale) {
        String base = getBaseName(c);
        List<String> attempts = new ArrayList<String>(6);
        addWhatExists(attempts, locale);
        addWhatExists(attempts, Locale.getDefault());
        Map<String,String> result = null;
        for (String s: attempts) {
            String resource = getBaseName(c) + "_" + s + ".utf8";
            result = attempt(c, resource);
            if (result != null) {
                return result;
            }
        }
        LOGGER.severe("No info for " + base + " in " + locale + ".");
        @SuppressWarnings("unchecked")
        Map<String,String> empty = Collections.EMPTY_MAP;
        return empty;
    }
    
    
    /**
     * Attempts to load a properties map from the given resource name.
     * If the resource does not exist, this method returns null.
     * 
     * @param c   the class that will load the resource
     * @param resource   the name of the resource to load
     * @return   the parsed properties from that resource, or null if the
     *           resource does not exist
     */
    private static Map<String,String> attempt(Class c, String resource) {
        InputStream inp = c.getResourceAsStream(resource);
        if (inp == null) {
            return null;
        }
        try {
            Reader r = new InputStreamReader(inp, "UTF-8");
            BufferedReader br = new BufferedReader(r);
            return read(br);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            try {
                inp.close();
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Couldn't close resource.", e);
            }
        }
    }


    /**
     * Reads properties from the given reader.
     * 
     * @param br  the reader 
     * @return   the properties in that reader
     * @throws IOException   if an IO error occurs
     */
    private static Map<String,String> read(BufferedReader br)
    throws IOException {
        Map<String,String> result = new HashMap<String,String>();
        String s = skipBlank(br);
        while (s != null) {
            if (s.endsWith(":")) {
                s = s.substring(0, s.length() - 1);
            }
            String value = readUntilBlank(br);
            result.put(s, value);
            s = skipBlank(br);
        }
        return result;
    }


    /**
     * Skips blank lines from the given reader, returning the first non-blank
     * line.
     * 
     * @param br   the reader
     * @return  the next non-blank line from the reader, or null if the 
     *   end-of-file is reached
     * @throws IOException   if an IO error occurs
     */
    private static String skipBlank(BufferedReader br) throws IOException {
        String s = br.readLine();
        while (s != null) {
            if (!isBlankLine(s)) {
                return s;
            }
            s = br.readLine();
        }
        return null;
    }


    /**
     * Reads multiple lines from the given reader into a string, until a blank
     * line is encountered.  
     * 
     * @param br  the reader to read from
     * @return  the read string
     * @throws IOException   if an IO error occurs
     */
    private static String readUntilBlank(BufferedReader br) 
    throws IOException {
        StringBuilder sb = new StringBuilder();
        String s = br.readLine();
        while (s != null) {
            if (isBlankLine(s)) {
                sb.deleteCharAt(sb.length() - 1);
                return sb.toString();
            }
            sb.append(s).append(' ');
            s = br.readLine();
        }
        // Delete trailing space.
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }
    
    
    /**
     * Returns true if the given line is blank.  A line containing only
     * whitespace is considered blank, as is any line that starts with #.
     * 
     * A null line is <i>not</i> considered blank, as it represents the 
     * end-of-file, which is usually a special case.
     * 
     * @param s  the line to check
     * @return  true if that line is blank
     */
    private static boolean isBlankLine(String s) {
        if (s == null) {
            return false;
        }
        s = s.trim();
        if (s.startsWith("#")) {
            return true;
        }
        if (s.length() == 0) {
            return true;
        }
        return false;
    }

}
