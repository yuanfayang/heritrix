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
package org.archive.crawler2.settings.path;

import java.util.Arrays;
import java.util.List;

import org.archive.crawler2.settings.NamedObject;
import org.archive.crawler2.settings.Sheet;
import org.archive.crawler2.settings.SheetManager;
import org.archive.crawler2.settings.SingleSheet;
import org.archive.state.Key;
import org.archive.state.KeyManager;

public class PathValidator {
    
    private SheetManager manager;
    private SingleSheet defaults;
    private Sheet sheet;
    private String path;
    
    private StringBuilder subPath;
    private List<String> tokens;

    
    public PathValidator(Sheet sheet, String path) {
        this.manager = sheet.getSheetManager();
        this.defaults = manager.getDefault();
        this.sheet = sheet;
        this.path = path;
        this.subPath = new StringBuilder();
    }
    

    public Object validatePath() {
        List<NamedObject> roots = manager.getRoots();
        tokens = Arrays.asList(path.split("\\."));
        Object current = NamedObject.getByName(roots, tokens.get(0));
        advance();
        while (!tokens.isEmpty()) {
            String n = tokens.get(0);
            if (Character.isDigit(n.charAt(0))) {
                current = validateList(current, n);
            } else {
                current = validateKey(current, n);
            }
            advance();
        }
        return current;
    }

    
    private void advance() {
        subPath.append(tokens.get(0));
        tokens = tokens.subList(1, tokens.size());
    }
    
    private RuntimeException ex(String msg) {
        StringBuilder sb = new StringBuilder();
        for (String s: tokens) {
            if (sb.length() > 0) {
                sb.append('.');
            }
            sb.append(s);
        }
        String e = path + msg + subPath;
        return new IllegalArgumentException(e);
    }
    
    
    private Object validateList(Object current, String indexString) {
        if (!(current instanceof List)) {
            throw ex(" indexes a non-list at ");
        }
        int index;
        try {
            index = Integer.parseInt(indexString);
        } catch (NumberFormatException e) {
            throw ex(" has non-integer index at ");
        }
        
        @SuppressWarnings("unchecked")
        List<Object> list = (List<Object>)current;
        
        try {
            return list.get(index);
        } catch (IndexOutOfBoundsException e) {
            throw ex(" has out-of-bounds index at ");
        }
    }

    
    private Object validateKey(Object current, String keyName) {
        if (current == null) {
            throw ex(" has null pointer at ");
        }
        
        Class c = current.getClass();
        Key<Object> key = KeyManager.getKeys(c).get(keyName);
        if (key == null) {
            throw ex(" has invalid key field name at ");
        }
        
        Object result = sheet.get(current, key);
        if (result == null) {
            result = defaults.get(current, key);
        }
        if (result == null) {
            result = key.getDefaultValue();
        }
        return result;
    }


}
