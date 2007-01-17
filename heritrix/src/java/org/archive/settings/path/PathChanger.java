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


    /**
     * Transformers used to convert simple objects from strings.
     */
    final private Map<String,Transformer<String,Object>> transformers;


    /**
     * Constructs a new PathChanger with the default set of transformers.
     * The default set of transformers consist of every final static field
     * of this class, eg {@link #BOOLEAN_TRANSFORMER}, {@link #INT_TRANSFORMER},
     * and so on.
     */
    public PathChanger() {
        transformers = new HashMap<String,Transformer<String,Object>>();
        registerTransformer("boolean", BOOLEAN_TRANSFORMER);
        registerTransformer("byte", BYTE_TRANSFORMER);
        registerTransformer("char", CHAR_TRANSFORMER);
        registerTransformer("double", DOUBLE_TRANSFORMER);
        registerTransformer("float", FLOAT_TRANSFORMER);
        registerTransformer("int", INT_TRANSFORMER);
        registerTransformer("long", LONG_TRANSFORMER);
        registerTransformer("short", SHORT_TRANSFORMER);
        registerTransformer("string", STRING_TRANSFORMER);
        registerTransformer("pattern", PATTERN_TRANSFORMER);
        //registerTransformer("object", OBJECT_TRANSFORMER);
    }


    /**
     * Registers a transformer for the given type of object.
     * 
     * @param <T>  the type of object to transform strings into
     * @param cls   the type of object to transform strings into
     * @param transformer   the transformer to use 
     */
    public <T> void registerTransformer(String suffix, 
            Transformer transformer) {
        @SuppressWarnings("unchecked")
        Transformer<String,Object> t = transformer;
        transformers.put(suffix, t);
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
        String suffix = pair.getType();
        String value = pair.getValue();
        
        if (suffix.equals("object")) {
            Object v = Construction.construct(sheet, value);
            finish(sheet, path, v);
            return;
        }

        Transformer<String,Object> transformer = transformers.get(suffix);
        if (transformer == null) {
            throw new PathChangeException("No transformer for type " + suffix);
        }
        Object v = transformer.transform(value);
        finish(sheet, path, v);        
    }


    private void finish(SingleSheet sheet, String path, Object value) {
        int p = path.lastIndexOf('.');
        if (p < 0) {
            if (path.equals(PathValidator.ROOT_NAME)) {
                sheet.getSheetManager().setRoot(value);
                return;
            } else {
                throw new PathChangeException("Missing controller.");
            }
        }
        
        String previousPath = path.substring(0, p);
        String lastToken = path.substring(p + 1);
        Object previous = PathValidator.validate(sheet, previousPath);
        if (previous instanceof List) {
            int index;
            try {
                index = Integer.parseInt(lastToken);
            } catch (NumberFormatException e) {
                throw new PathChangeException(e);
            }
            @SuppressWarnings("unchecked")
            List<Object> list = (List<Object>)previous;
            if (index < list.size()) {
                list.set(index, value);
            } else if (index == list.size()) {
                list.add(value);
            } else {
                throw new PathChangeException("Incorrect index: " 
                        + path + " (expected " + list.size() + ")");
            }
            return;
        }
        
        if (previous instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String,Object> map = (Map<String,Object>)previous;
            map.put(lastToken, value);
            return;
        }

        Map<String,Key<Object>> keys = KeyManager.getKeys(previous.getClass());
        Key<Object> key = keys.get(lastToken);
        if (key == null) {
            throw new PathChangeException("No such key: " + path);
        }
        sheet.set(previous, key, value);
    }

}
