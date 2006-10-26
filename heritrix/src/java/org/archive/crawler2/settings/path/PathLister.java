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
package org.archive.crawler2.settings.path;


import java.util.Collection;
import java.util.List;

import org.archive.crawler2.settings.NamedObject;
import org.archive.crawler2.settings.Resolved;
import org.archive.crawler2.settings.Sheet;
import org.archive.crawler2.settings.SingleSheet;
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
     * for the algorithm.
     */
    private void list() {
        SingleSheet defaults = startSheet.getSheetManager().getDefault();
        for (NamedObject no: startSheet.getSheetManager().getRoots()) {
            String path = no.getName();
            Object processor = no.getObject();
            consumer.consume(path, defaults, processor);
            handleProcessor(startSheet, path, processor);
        }
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
     * Handles a processor object.  When this method is invoked, the processor
     * object itself has already been consumed.  This method checks the 
     * the KeyManager for keys defined by the object.  If any of those
     * key values are themselves processors, then this method is called 
     * recursively for those processors.
     * 
     * <p>Note that the given sheet may or may not be the same as 
     * {@link #startSheet}: List elements need to be resolved using the same
     * sheet that defined the List.
     * 
     * @param sheet       the sheet to use to resolve values
     * @param path        the path that leads to the processor
     * @param processor   the processor to handle
     */
    private void handleProcessor(
            Sheet resolver,
            String path,
            Object processor) {
        // Get the keys for the processor
        Class ptype = processor.getClass();
        Collection<Key<Object>> declared = KeyManager.getKeys(ptype).values();
        
        for (Key<Object> k: declared) {
            handleKey(resolver, path, processor, k);
        }
    }

    
    /**
     * Handles a single Key of a processor.  The value for the Key is fetched
     * from the sheet (or perhaps the default sheet), and the value is then
     * processed depending on on its type.
     * 
     * @param resolver    the sheet to use to resolve settings
     * @param path        the path that leads to the processor
     * @param processor   the processor 
     * @param k           the key
     */
    private void handleKey(Sheet resolver, String path, Object processor, 
            Key<Object> k) {
        Resolved<Object> r = fetch(resolver, processor, k);
        if (r == null) {
            // We are in "get" mode (resolve==false), and the given sheet
            // does not define the key.  Just skip it.
            return;
        }
        String kpath = path + "." + k.getFieldName();
        
        // What happens next depends on type.  Lists are treated specially.
        // Known "simple" types (eg, String objects) are leaf nodes, since
        // they cannot define any keys.
        // Anything else is assumed to be a processor, meaning the KeyManager
        // will be consulted to find more keys.  This is safe since the
        // KeyManager will just return an empty set of Keys if the object
        // isn't really a processor.

        consumer.consume(kpath, r.getSheet(), r.getValue());
        if (List.class.isAssignableFrom(k.getType())) {
            handleList(kpath, r.getSheet(), r.getValue());
        } else if (!isSimple(k.getType())) {
            // Assume it's another processor.
            SingleSheet ss = r.getSheet();
            handleProcessor(ss, kpath, r.getValue());
        }
    }


    /**
     * Fetches a setting from the given sheet.  The returned value depends on
     * whether or not we're in "resolve" mode.  
     * 
     * <p>If {@link #resolve}==true, then the sheet's 
     * {@link Sheet#resolve(Object, Key)} method is used to find the value.
     * The resolve method always checks the default sheet if the man sheet
     * (or its child sheets) do not define a setting.  In other words, when
     * resolve==true, this method always returns something non-null.
     * 
     * <p>If {@link #resolve}==false, then the sheet's 
     * {@link Sheet#get(Object, Key)} method is used to find the value.
     * The get method may return null if the sheet itself does not define
     * the value.  In that case, this method also returns null, indicating
     * that the setting should not be consumed.
     * 
     * @param sheet      the sheet to use to find settings
     * @param processor   the processor whose settings to find
     * @param key         the Key of the setting
     * @return   the value for that setting, or null if we're in get mode
     *    and the sheet does not define such a value
     */
    private Resolved<Object> fetch(
            Sheet sheet, 
            Object processor, 
            Key<Object> key) {
        if (resolve) {
            return sheet.resolve(processor, key);
        }
        
        SingleSheet ss = (SingleSheet)sheet;
        Object v = ss.get(processor, key);
        if (v == null) {
            return null;
        }
        return new Resolved<Object>(ss, processor, key, v);
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
    private void handleList(String path, SingleSheet sheet, Object l) {
        @SuppressWarnings("unchecked")
        List<Object> list = (List<Object>)l;
        for (int i = 0; i < list.size(); i++) {
            Object element = list.get(i);
            String lpath = path + "." + i;
            if (element == null) {
                consumer.consume(path, sheet, null);
            } else if (List.class.isInstance(element)) {
                handleList(lpath, sheet, element);
            } else {
                handleProcessor(sheet, lpath, element);
            }
        }
    }


    private static boolean isSimple(Class c) {
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


}
