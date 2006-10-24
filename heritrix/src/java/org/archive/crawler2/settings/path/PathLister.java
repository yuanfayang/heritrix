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


public class PathLister {

    
    
    public static void listAll(Sheet sheet, PathListConsumer consumer) {
        SingleSheet defaults = sheet.getSheetManager().getDefault();
        for (NamedObject no: sheet.getSheetManager().getRoots()) {
            handleProcessor(defaults, sheet, consumer, no.getName(), no.getObject());
        }
    }


    
    private static void handleProcessor(
            SingleSheet declarer,
            Sheet resolver,
            PathListConsumer consumer,
            String path,
            Object processor) {        
        Class ptype = processor.getClass();
        consumer.consume(path + "._impl", declarer, ptype.getName());
        
        Collection<Key<Object>> declared = KeyManager.getKeys(ptype).values();
        
        for (Key<Object> k: declared) {
            Resolved<Object> r = resolver.resolve(processor, k);
            String kpath = path + "." + k.getFieldName();
            if (List.class.isAssignableFrom(k.getType())) {
                handleList(consumer, kpath, r.getSheet(), r.getValue());
            } else if (isSimple(k.getType())) {
                consumer.consume(path, r.getSheet(), r.getValue());
            } else {
                handleProcessor(r.getSheet(), r.getSheet(), consumer, kpath, 
                        r.getValue());
            }
        }

    }

    


    private static void handleList(PathListConsumer consumer, 
            String path, 
            SingleSheet sheet, 
            Object l) {
        @SuppressWarnings("unchecked")
        List<Object> list = (List<Object>)l;
        for (int i = 0; i < list.size(); i++) {
            Object element = list.get(i);
            String lpath = path + "." + i;
            if (element == null) {
                consumer.consume(path, sheet, null);
            } else if (List.class.isInstance(element)) {
                handleList(consumer, lpath, sheet, element);
            } else if (isSimple(element.getClass())) {
                throw new IllegalStateException("FIXME");
//                result.add(new Listed(sheet.getName(), lpath, element.toString()));
            } else {
                handleProcessor(sheet, sheet, consumer, lpath, element);
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
