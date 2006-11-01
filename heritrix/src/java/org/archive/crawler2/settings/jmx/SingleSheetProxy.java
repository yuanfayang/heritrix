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
 * SingleSheetProxy.java
 * Created on October 24, 2006
 *
 * $Header$
 */
package org.archive.crawler2.settings.jmx;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.management.openmbean.CompositeData;


import org.archive.crawler2.settings.NamedObject;
import org.archive.crawler2.settings.SheetManager;
import org.archive.crawler2.settings.SingleSheet;
import org.archive.crawler2.settings.path.PathValidator;
import org.archive.openmbeans.annotations.Bean;
import org.archive.openmbeans.annotations.Header;
import org.archive.openmbeans.annotations.OpenTypes;
import org.archive.openmbeans.annotations.Operation;
import org.archive.openmbeans.annotations.Parameter;
import org.archive.state.Key;
import org.archive.state.KeyManager;


@Header(desc="A single sheet of settings.")
public class SingleSheetProxy extends Bean {

    
    final private SingleSheet sheet;


    public SingleSheetProxy(SingleSheet sheet) {
        this.sheet = sheet;
    }
    
    
    @Operation(impact=ACTION, desc="Associates a list of context strings with this sheet.")
    public void associate(
            @Parameter(
                    name="contexts", 
                    desc="A list of context strings to associate with this sheet.")
            String[] contexts) 
    {
        SheetManager sm = sheet.getSheetManager();
        sm.associate(sheet, Arrays.asList(contexts));
    }


   
    private <T> T getSimple(String path, String key, Class<T> type) {
        Object processor = PathValidator.validate(sheet, path);
        Map<String,Key<Object>> keys = KeyManager.getKeys(processor.getClass());
        @SuppressWarnings("unchecked")
        Key<Object> k = keys.get(key);
        if (k.getType() != type) {
            throw new IllegalArgumentException(key + " is not a " + type);
        }
        Object r = sheet.get(processor, k);
        if (r == null) {
            return null;
        }
        return type.cast(r);
    }

    
    private void setSimple(String path, String key, String value) {
        Object processor = PathValidator.validate(sheet, path);
        Map<String,Key<Object>> keys = KeyManager.getKeys(processor.getClass());
        @SuppressWarnings("unchecked")
        Key<Object> k = keys.get(key);
        Class type = k.getType();
        Object object = OpenTypes.parse(type, value);
        sheet.set(processor, k, object);
    }


    @Operation(desc="Configures one or more processors.")
    public void setMany(
            @Parameter(name="setData", desc="An array of path/values to set.",
             type="org.archive.crawler2.settings.jmx.Types.SET_DATA_ARRAY")
            CompositeData[] setData) 
    {
        for (CompositeData cd: setData) {
            String path = (String)cd.get("path");
            String key = (String)cd.get("key");
            String value = (String)cd.get("value");
            setSimple(path, key, value);
        }
    }
    
    
    public CompositeData[] listAll() {
        for (NamedObject no: sheet.getSheetManager().getRoots()) {
            Map<Key,Object> keys = sheet.getAll(no.getObject());
            if (keys != null) {
                list(null, no.getName(), no.getObject(), keys);
            }
        }
        return null;
    }

    
    private void list(
            List<CompositeData> r, 
            String path, 
            Object o,
            Map<Key,Object> keys) {
        if (keys == null) {
            return;
        }
        for (Map.Entry<Key,Object> entry: keys.entrySet()) {
            Key k = entry.getKey();
            if (isSimple(k.getType())) {
                
            } else if (k.getType() == List.class) {
                
            } else {
                String path2 = path + "." + k.getFieldName();
                Map<Key,Object> keys2 = sheet.getAll(entry.getValue());
                list(r, path2, entry.getValue(), keys2);
            }
        }
    }
    
    
    private static boolean isSimple(Class c) {
        return false;
    }
    
}
