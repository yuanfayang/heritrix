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
 * PathLister.java
 * Created on October 24, 2006
 *
 * $Header$
 */
package org.archive.settings.path;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.archive.settings.Resolved;
import org.archive.settings.Sheet;
import org.archive.settings.SingleSheet;
import org.archive.state.Key;
import org.archive.state.KeyManager;


/**
 * Lists all of the paths contained in a given sheet.  There are actually
 * two modes of operation; "get" and "resolve".  
 * The {@link #getAll(SingleSheet, PathListConsumer)} method lists only
 * those settings that are defined by a given sheet.  The
 * {@link #resolveAll(Sheet, PathListConsumer)} method lists <i>every</i>
 * setting of every root object; if the setting is not defined by the given
 * sheet, then the default sheet's setting is listed instead.
 * 
 * @author pjack
 */
public class PathLister {

    
    /** The sheet we're resolving. */
    final private Sheet startSheet;
    
    /** The consumer to send resolved settings to. */
    final private PathListConsumer consumer;

    /** True if we're in "resolve" mode, false if we're in "get" mode. */
    final private boolean resolve;
    
    
    /**
     * Constructor.  Private so the API is a static utility library.
     * A future implementation might eliminate the need to instantiate 
     * a PathLister on every getAll or resolveAll invocation.
     * 
     * @param sheet     the sheet to resolve
     * @param c         the consumer 
     * @param resolve   true to use "resolve" mode, false to use "get" mode
     */
    private PathLister(Sheet sheet, PathListConsumer c, boolean resolve) {
        this.startSheet = sheet;
        this.resolve = resolve;
        this.consumer = c;
    }
    

    /**
     * Lists all the settings in the sheet.  This is the point of entry
     * for the algorithm.  It starts by consuming the root objects.
     */
    private void list() {
        Sheet defaults = startSheet.getSheetManager().getDefault();
        String path = "";
        Object module = startSheet.getSheetManager().getRoot();
        List<Sheet> list = Collections.singletonList(defaults);
        consume(path, list, module);
        list = Collections.singletonList(startSheet);
        advance(path, list, module);
    }


    /**
     * Lists all settings that are defined or not defined by the given 
     * sheet.  If a particular setting is not defined in the sheet, then the
     * default sheet is consulted instead.
     * 
     * @param sheet     the sheet whose settings to list
     * @param consumer  the consumer for the listed settings
     */
    public static void resolveAll(Sheet sheet, PathListConsumer consumer) {
        new PathLister(sheet, consumer, true).list();
    }


    /**
     * Lists only those settings that are specified by the given sheet.
     * 
     * @param sheet      the sheet whose settings to list
     * @param consumer   the consumer for the listed settings
     */
    public static void getAll(SingleSheet sheet, PathListConsumer consumer) {
        new PathLister(sheet, consumer, false).list();
    }


    /**
     * Handles a module object.  When this method is invoked, the module
     * object itself has already been consumed.  This method checks the 
     * the KeyManager for keys defined by the object.  If any of those
     * key values are themselves modules, then this method is called 
     * recursively for those modules.
     * 
     * <p>Note that the given sheet may or may not be the same as 
     * {@link #startSheet}: List elements need to be resolved using the same
     * sheet that defined the List.
     * 
     * @param sheet       the sheet to use to resolve values
     * @param path        the path that leads to the processor
     * @param processor   the processor to handle
     */
    private void handleModule(
            List<Sheet> sheets,
            String path,
            Object processor) {
        // Get the keys for the module
        Class ptype = processor.getClass();
        Collection<Key<Object>> declared = KeyManager.getKeys(ptype).values();
        
        for (Key<Object> k: declared) {
            handleKey(sheets, path, processor, k);
        }
    }

    
    /**
     * Handles a single Key of a processor.  The value for the Key is fetched
     * from the sheet (or perhaps the default sheet), and the value is then
     * processed depending on on its type.
     * 
     * @param sheets    the sheet to use to resolve settings
     * @param path        the path that leads to the processor
     * @param processor   the processor 
     * @param k           the key
     */
    private void handleKey(List<Sheet> sheets, String path, Object processor, 
            Key<Object> k) {
        Sheet last = sheets.get(sheets.size() - 1);
        Resolved<Object> r = last.resolve(processor, k);
        List<Sheet> resolvedList = new ArrayList<Sheet>(sheets);
        resolvedList.remove(resolvedList.size() - 1);
        resolvedList.addAll(r.getSheets());
        resolvedList = Collections.unmodifiableList(resolvedList);
        
        String kpath = appendPath(path, k.getFieldName());

        consume(kpath, resolvedList, r.getValue());
        advance(kpath, resolvedList, r.getValue());
    }


    /**
     * Consumes a setting.  If we are in "resolve" mode, then the supplied
     * parameters are passed to the consumer with no questions asked.
     * 
     * <p>Otherwise, we are in "get" mode, and the final sheet in the given
     * stack must be the same as the starting sheet, since we are only
     * interested in settings defined by that sheet.
     * 
     * @param path    the path to the setting
     * @param list    the list of sheets that led to the setting
     * @param value   the value of the setting
     */
    private void consume(String path, List<Sheet> list, Object value) {
        Sheet last = list.get(list.size() - 1);
        if (resolve || (last == startSheet)) {
            consumer.consume(path, list, value);
        }
    }


    private void advance(String path, List<Sheet> list, Object value) {
        if (value instanceof List) {
            handleList(path, list, value);
        } else if (value instanceof Map) {
            handleMap(path, list, value);
        } else {
            // Assume it's another module.
            handleModule(list, path, value);
        }
    }


    /**
     * Handles a list object.  First consumes the list object itself, then
     * consumes each element of the list.
     * 
     * @param path    the path to the list object
     * @param sheet   the sheet to use to resolve values
     * @param l       the list, as an object (easier to cast in the method
     *   body than at each call site)
     */
    private void handleList(String path, List<Sheet> sheets, Object l) {
        @SuppressWarnings("unchecked")
        List<Object> list = (List<Object>)l;
        for (int i = 0; i < list.size(); i++) {
            Object element = list.get(i);
            String lpath = appendPath(path, Integer.toString(i));
            consume(lpath, sheets, element);
            advance(lpath, sheets, element);
        }
    }
    
    
    private void handleMap(String path, List<Sheet> sheets, Object m) {
        @SuppressWarnings("unchecked")
        Map<String,Object> map = (Map<String,Object>)m;
        for (Map.Entry<String,Object> entry: map.entrySet()) {
            Object element = entry.getValue();
            String mpath = appendPath(path, entry.getKey()); // FXME: Escape keys
            consume(mpath, sheets, element);
            advance(mpath, sheets, element);
        }
    }

    

    static boolean isSimple(Class c) {
        // FIXME: Move this method somewhere else
        if (c.isPrimitive()) {
            return true;
        }
        if (c == String.class) {
            return true;
        }
        // FIXME: BigDecimal, BigInteger, Date, Pattern and so on...
        return false;
    }


    private static String appendPath(String path, String next) {
        if (path.length() == 0) {
            return next;
        }
        return path + "." + next;
    }
}
