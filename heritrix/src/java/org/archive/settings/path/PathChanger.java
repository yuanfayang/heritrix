/* Copyright (C) 2006 Internet Archive.
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
 * PathChanger.java
 * Created on October 24, 2006
 *
 * $Header$
 */
package org.archive.settings.path;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.archive.crawler.util.Transformer;
import org.archive.settings.SingleSheet;
import org.archive.settings.path.PathValidator;
import org.archive.state.Key;
import org.archive.state.KeyManager;


/**
 * Applies one or more changes to a sheet.  The changes to take place are 
 * specified using strings.  Objects to change are identified by their
 * paths, and the new value for those paths are also provided as strings.
 * 
 * @author pjack
 *
 */
public class PathChanger {


    /** Default transformer for booleans. */
    final public static Transformer<String,Boolean> BOOLEAN_TRANSFORMER =
        new Transformer<String,Boolean>() {
            public Boolean transform(String s) {
                return Boolean.parseBoolean(s);
            }
        };

        
    /** Default transformer for bytes. */
    final public static Transformer<String,Byte> BYTE_TRANSFORMER =
        new Transformer<String,Byte>() {
            public Byte transform(String s) {
                return Byte.decode(s);
            }
        };

    
    /** Default transformer for chars. */
    final public static Transformer<String,Character> CHAR_TRANSFORMER =
        new Transformer<String,Character>() {
            public Character transform(String s) {
                if (s.length() != 1) {
                    throw new IllegalArgumentException();
                }
                return s.charAt(0);
            }
        };

        
    /** Default transformer for doubles. */
    final public static Transformer<String,Double> DOUBLE_TRANSFORMER =
        new Transformer<String,Double>() {
            public Double transform(String s) {
                return Double.parseDouble(s);
            }
        };

        
    /** Default transformer for floats. */
    final public static Transformer<String,Float> FLOAT_TRANSFORMER =
        new Transformer<String,Float>() {
            public Float transform(String s) {
                return Float.parseFloat(s);
            }
        };


    /** Default transformer for ints. */
    final public static Transformer<String,Integer> INT_TRANSFORMER =
        new Transformer<String,Integer>() {
            public Integer transform(String s) {
                return Integer.decode(s);
            }
        };


    /** Default transformer for longs. */
    final public static Transformer<String,Long> LONG_TRANSFORMER =
        new Transformer<String,Long>() {
            public Long transform(String s) {
                return Long.decode(s);
            }
        };


    /** Default transformer for shorts. */
    final public static Transformer<String,Short> SHORT_TRANSFORMER =
        new Transformer<String,Short>() {
            public Short transform(String s) {
                return Short.decode(s);
            }
        };


    /** Default transformer for strings. */
    final public static Transformer<String,String> STRING_TRANSFORMER =
        new Transformer<String,String>() {
            public String transform(String s) {
                return s;
            }
        };
        
    
    /** Default transformer for regular expressions. */
    final public static Transformer<String,Pattern> PATTERN_TRANSFORMER =
        new Transformer<String,Pattern>() {
            public Pattern transform(String s) {
                return Pattern.compile(s);
            }
        };


    
//    final private BufferedReader reader;
//    final private SingleSheet sheet;


    /**
     * Transformers used to convert simple objects from strings.
     */
    final private Map<Class,Transformer> transformers;


    /**
     * Constructs a new PathChanger with the default set of transformers.
     * The default set of transformers consist of every final static field
     * of this class, eg {@link #BOOLEAN_TRANSFORMER}, {@link #INT_TRANSFORMER},
     * and so on.
     */
    public PathChanger() {
        transformers = new HashMap<Class,Transformer>();
        registerTransformer(Boolean.class, BOOLEAN_TRANSFORMER);
        registerTransformer(Byte.class, BYTE_TRANSFORMER);
        registerTransformer(Character.class, CHAR_TRANSFORMER);
        registerTransformer(Double.class, DOUBLE_TRANSFORMER);
        registerTransformer(Float.class, FLOAT_TRANSFORMER);
        registerTransformer(Integer.class, INT_TRANSFORMER);
        registerTransformer(Long.class, LONG_TRANSFORMER);
        registerTransformer(Short.class, SHORT_TRANSFORMER);
        registerTransformer(String.class, STRING_TRANSFORMER);
        registerTransformer(Pattern.class, PATTERN_TRANSFORMER);
    }


    /**
     * Registers a transformer for the given type of object.
     * 
     * @param <T>  the type of object to transform strings into
     * @param cls   the type of object to transform strings into
     * @param transformer   the transformer to use 
     */
    public <T> void registerTransformer(Class<T> cls, 
            Transformer<String,T> transformer) {
        transformers.put(cls, transformer);
    }

    
    public void change(SingleSheet ss, Iterable<PathChange> changes) {
        change(ss, changes.iterator());
    }
    
    public void change(SingleSheet ss, Iterator<PathChange> changes) {
        while (changes.hasNext()) {
            processChange(ss, changes.next());
        }
    }
    
    
    private void processChange(SingleSheet sheet, PathChange pair) {
        String path = pair.getPath();
        String value = pair.getValue();
        if (path.endsWith("._impl")) {
            processObject(sheet, path, value);
            return;
        }

        int p = path.lastIndexOf('.');
        if (p < 0) {
            throw new PathChangeException("Root objects can't be primitive: " + path);
        }
        String previous = path.substring(0, p);
        String lastToken = path.substring(p + 1);
        Object processor = PathValidator.validate(sheet, previous);
        Map<String,Key<Object>> keys = KeyManager.getKeys(processor.getClass());
        Key<Object> key = keys.get(lastToken);
        if (key == null) {
            throw new PathChangeException("No such key: " + lastToken);
        }
        Object v;
        @SuppressWarnings("unchecked")
        Transformer<String,Object> t = this.transformers.get(key.getType());
        if (t == null) {
            throw new PathChangeException("Can't transform " 
                    + key.getType().getName());
        }
        try {
            v = t.transform(value);
        } catch (Exception e) {
            throw new PathChangeException("Could not parse value: " + value);
        }
        sheet.set(processor, key, v);
    }


    private void processObject(SingleSheet sheet, String path, 
            String className) {
        // Instantiate the new object.
        // FIXME: Somehow allow constructors?
        className = className.trim();
        Object object;
        try {
            object = Class.forName(className).newInstance();
        } catch (Exception e) {
            throw new PathChangeException(e);
        }

        // Now we need to figure out where to store the object.
        // There are three possibilities:
        // (1) The newly minted object is a root object, and goes in the
        //     Sheet Manager.
        // (2) The newly minted object is a member of a List.
        // (3) The newly minted object is the value of some other object's
        //     Key field.
        
        // Eliminate the trailing "._impl" from the given path
        path = path.substring(0, path.length() - 6);

        // If the path is now 1 token long, then it's a root object.
        // (Eg, if given path was X._impl, then it defined root object X.
        // FIXME: If root with that name already exists, replace that root (ew)
        int p = path.lastIndexOf('.');
        if (p < 0) {
            Object root = sheet.getSheetManager().getRoot(path);
            if (root == null) {
                sheet.getSheetManager().addRoot(path, object);
            } else {
                sheet.getSheetManager().swapRoot(path, object);
            }
            return;
        }
        
        String previous = path.substring(0, p);
        Object processor = PathValidator.validate(sheet, previous);
        String lastToken = path.substring(p + 1);
        
        // Check to see if we're appending to a list.
        // If so, the last token will be an integer.
        // Eg, we were passed X.0._impl
        int index = index(lastToken);
        if (index >= 0) {
            if (!(processor instanceof List)) {
                throw new PathChangeException("Not a list: " + previous);
            }
            @SuppressWarnings("unchecked")
            List<Object> list = (List<Object>)processor;
            if (index < list.size()) {
                list.set(index, object);
            } else if (index == list.size()) {
                list.add(object);
            } else {
                throw new PathChangeException("Incorrect index: " 
                        + path + " (expected " + list.size() + ")");
            }
            return;
        }

        // Not a root object, not a list member.
        // Must be a value for some other processor's Key.
        // The lastToken is therefore a Key field name.
        // Check the KeyManager for that key, then set its value in the sheet.
        Map<String,Key<Object>> keys = KeyManager.getKeys(processor.getClass());
        Key<Object> key = keys.get(lastToken);
        if (key == null) {
            throw new PathChangeException("No such key: " + path);
        }
        sheet.set(processor, key, object);
    }

    
    private static int index(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

}
