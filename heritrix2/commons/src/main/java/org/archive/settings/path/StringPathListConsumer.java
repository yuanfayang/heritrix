/* 
 * Copyright (C) 2007 Internet Archive.
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
 * StringPathListConsumer.java
 *
 * Created on Jun 5, 2007
 *
 * $Id:$
 */

package org.archive.settings.path;

import java.util.List;
import java.util.Map;

import org.archive.settings.Stub;
import org.archive.settings.Sheet;
import org.archive.settings.SingleSheet;
import org.archive.settings.TypedList;
import org.archive.settings.TypedMap;
import org.archive.state.KeyTypes;

import static org.archive.settings.path.PathChanger.REFERENCE_TAG;
import static org.archive.settings.path.PathChanger.OBJECT_TAG;
import static org.archive.settings.path.PathChanger.LIST_TAG;
import static org.archive.settings.path.PathChanger.MAP_TAG;
import static org.archive.settings.path.PathChanger.PRIMARY_TAG;
import static org.archive.settings.path.PathChanger.AUTO_TAG;

/**
 * @author pjack
 *
 */
public abstract class StringPathListConsumer implements PathListConsumer {


    public void consume(
            String path, 
            List<Sheet> sheets, 
            Object value, 
            Class type, 
            String seenPath) {
        Sheet last = sheets.get(sheets.size() - 1);
        boolean primary;
        if ((last instanceof SingleSheet) && ((SingleSheet)last).isGlobal()) {
            primary = ((SingleSheet)last).isPrimary(value);
        } else {
            primary = last.getSheetManager().getGlobalSheet().isPrimary(value);
        }
        
        String[] snames = new String[sheets.size()];
        for (int i = 0; i < snames.length; i++) {
            snames[i] = sheets.get(i).getName();
        }
        
        Class actual = (value == null) ? type : value.getClass();
        
        String typeTag;
        String v;
        if (KeyTypes.isSimple(type)) {
            typeTag = KeyTypes.getSimpleTypeTag(type);
            v = KeyTypes.toString(value);
        } else if (seenPath != null) {
            if (primary) {
                typeTag = AUTO_TAG;
            } else {
                typeTag = REFERENCE_TAG;
            }
            v = seenPath;
        } else if (Map.class.isAssignableFrom(actual)) {
            typeTag = MAP_TAG;
            v = ((TypedMap)value).getElementType().getName();
        } else if (List.class.isAssignableFrom(actual)) {
            typeTag = LIST_TAG;
            v = ((TypedList)value).getElementType().getName();
        } else {
            if (primary) {
                typeTag = PRIMARY_TAG;
            } else {
                typeTag = OBJECT_TAG;
            }
            v = (value == null) ? "null" : Stub.getType(value).getName();
        }

        consume(path, snames, v, typeTag);
    }


    protected abstract void consume(String path, String[] sheets, String value,
            String type);


}
