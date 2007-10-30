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
 * Setting.java
 *
 * Created on Jun 6, 2007
 *
 * $Id:$
 */

package org.archive.crawler.webui;

import org.archive.settings.path.PathChanger;
import org.archive.settings.path.PathValidator;
import org.archive.state.Key;

/**
 * @author pjack
 *
 */
public class Setting {

    final private static Object NO_KEY = new Object(); 
    
    private String path;
    private String type;
    private Class actualType;
    private String value;
    private String errorMessage;
    private String[] sheets;
    private Object key;
    private boolean pathInvalid;
    
    public Setting() {
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setPathInvalid(boolean flag) {
        this.pathInvalid = flag;
    }

    public boolean isPathInvalid() {
        return this.pathInvalid;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String[] getSheets() {
        return sheets;
    }
    
    public void setSheets(String[] sheets) {
        this.sheets = sheets;
    }


    public void setActualType(Class c) {
        this.actualType = c;
    }

    public Class getActualType() {
        return actualType;
    }

    public boolean isDirectChild(String parentPath) {
        if (parentPath.equals(path)) {
            return false;
        }
        if (!path.startsWith(parentPath)) {
            return false;
        }
        return path.indexOf(PathValidator.DELIMITER, 
                parentPath.length() + 1) < 0;
    }

    
    boolean isKeySet() {
        return key != null;
    }
    

    Key getKey() {
        if (key == NO_KEY) {
            return null;
        } else {
            return (Key)key;
        }
    }
    
    
    void setKey(Key key) {
        this.key = key;
    }
    
    
    void setNoKey() {
        this.key = NO_KEY;
    }

    
    public boolean isObjectType() {
        return PathChanger.isObjectTag(type);
    }
}
