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
 * PathValidator.java
 * Created on October 24, 2006
 *
 * $Header$
 */
package org.archive.settings.path;


import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.archive.settings.Offline;
import org.archive.settings.Resolved;
import org.archive.settings.Sheet;
import org.archive.state.Key;
import org.archive.state.KeyManager;


/**
 * Checks that a given path is valid.  For a sheet and a path, this class 
 * will start at the root object indicated by the path and "walk" the rest
 * of the path, returning the object the path represents.
 * 
 * @author pjack
 */
public class PathValidator {

    
    final static String ROOT_NAME = "root";

    
    /**
     * The sheet being used to resolve the path.
     */
    final private Sheet sheet;
    
    
    /**
     * The path to resolve.
     */
    final private String path;
    
    
    /**
     * The portion of the path that has already been resolved.  Used for
     * error reporting.
     */
    private StringBuilder subPath;
    
    
    /**
     * The tokens in the path.  Shrinks as tokens are resolved.
     */
    private List<String> tokens;


    /**
     * Private constructor.  The public API for this class is a single
     * static utility method.  As the state recorded during validation
     * mostly revolves around error reporting, a future version of this class
     * could trade memory usage for terser error messages.
     * 
     * @param sheet   the sheet to use to resolve the path
     * @param path    the path to resolve
     */
    private PathValidator(Sheet sheet, String path) {
        this.sheet = sheet;
        this.path = path;
        this.subPath = new StringBuilder();
        this.tokens = new LinkedList<String>(Arrays.asList(path.split("\\.")));
    }
    

    /**
     * Validates the path.
     * 
     * @return   the object the path represents
     */
    private Object validatePath() {
        String first = tokens.get(0);
        advance();
        if (!first.equals(ROOT_NAME)) {
            throw ex("No root.");
        }

        Object current = sheet.getSheetManager().getRoot();
        
        // While there are more tokens, process the next token.
        while (!tokens.isEmpty()) {
            String n = tokens.get(0);
            if (current instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String,Object> map = (Map<String,Object>)current;
                current = validateMap(map, n);
            } else if (current instanceof List) {
                @SuppressWarnings("unchecked")
                List<Object> list = (List<Object>)current;
                current = validateList(list, n);
            } else {
                current = validateKey(current, n);
            }
            advance();
        }
        return current;
    }


    /**
     * Advances to the next token.  Appends the the next token to the 
     * subPath for error reporting, and shrinks the 
     *
     */
    private void advance() {
        if (subPath.length() > 0) {
            subPath.append('.');
        }
        subPath.append(tokens.get(0));
        tokens.remove(0);
    }

    
    /**
     * Returns a new InvalidPathException including the original path, the
     * current subpath and the given error text.
     * 
     * @param msg   the error message to report
     * @return   a new InvalidPathException
     */
    private RuntimeException ex(String msg) {
        String e = path + msg + subPath;
        return new InvalidPathException(e);
    }
    

    /**
     * Validates an index token.
     * 
     * @param current       the currently resolved object (which must be a List)
     * @param indexString   the index as a string
     * @return   the element at that index in the list
     */
    private Object validateList(List<Object> current, String indexString) {
        int index;
        try {
            index = Integer.parseInt(indexString);
        } catch (NumberFormatException e) {
            throw ex(" has non-integer index at ");
        }
        
        try {
            return current.get(index);
        } catch (IndexOutOfBoundsException e) {
            throw ex(" has out-of-bounds index at ");
        }
    }
    
    
    private Object validateMap(Map<String,Object> current, String keyString) {
        // FIXME: Have some sort of escaping rules for keyString
        Object r = current.get(keyString);
        return r;
    }

    
    /**
     * Validates a key field token.
     * 
     * @param current   the currently resolved object
     * @param keyName   the name of the key field
     * @return   the value of that Key for that processor
     */
    private Object validateKey(Object current, String keyName) {
        if (current == null) {
            throw ex(" has null pointer at ");
        }
        
        Class c;
        if (current instanceof Offline) {
            c = ((Offline)current).getType();
        } else {
            c = current.getClass();
        }
        Key<Object> key = KeyManager.getKeys(c).get(keyName);
        if (key == null) {
            throw ex(" has invalid key field name at ");
        }
        
        Resolved r = sheet.resolve(current, key);
        return r.getValue();
    }


    /**
     * Validates a path.
     * 
     * @param sheet   the sheet to use to resolve key fields
     * @param path    the path to validate
     * @return   the object represented by that path
     */
    public static Object validate(Sheet sheet, String path) {
        return new PathValidator(sheet, path).validatePath();
    }

}
