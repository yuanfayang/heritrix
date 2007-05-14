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
 * PathListConsumer.java
 * Created on October 24, 2006
 *
 * $Header$
 */
package org.archive.settings.path;


import java.util.List;

import org.archive.settings.Sheet;


/**
 * Consumes settings as they are listed.
 * 
 * @author pjack
 * @see PathLister
 */
public interface PathListConsumer {

    /**
     * Consumes a setting.
     * 
     * @param path    the path to the setting
     * @param sheet   the sheet that defined the setting
     * @param value   the value of the setting
     * @param seenPath   null if the value is a complex type and has never
     *  been seen before; or, the path where the value was previously seen.
     */
    void consume(String path, List<Sheet> sheet, Object value, 
            String seenPath);

}
