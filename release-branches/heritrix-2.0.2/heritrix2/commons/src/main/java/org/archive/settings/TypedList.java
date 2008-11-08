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
 * TypedList.java
 *
 * Created on Jun 1, 2007
 *
 * $Id:$
 */

package org.archive.settings;

import java.util.List;

/**
 * 
 * 
 * @author pjack
 *
 */
public interface TypedList<T> extends List<T> {


    /**
     * Returns the element type of the list.
     * 
     * @return  the element type of the list.
     */
    Class<T> getElementType();

    /**
     * Returns the sheets that defined the element at the given index.
     * 
     * @param index    the index of the element
     * @return         the sheets that defined that element
     */
    List<Sheet> getSheets(int index);


}
