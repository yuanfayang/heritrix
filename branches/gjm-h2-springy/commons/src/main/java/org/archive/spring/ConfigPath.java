/* This file is part of the Heritrix web crawler (crawler.archive.org).
 * 
 * Heritrix is free software!
 * 
 * Copyright 2008, Internet Archive Heritrix Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *   
 * $Header$
 */
package org.archive.spring;

import java.io.File;
import java.io.Serializable;

/**
 * A filesystem path, as a bean, for the convenience of configuration
 * via srping beans.xml or user interfaces to same. 
 * 
 * Adds an optional relative-to base path and symbolic handle. 
 * 
 * See also ConfigPath
 */
public class ConfigPath implements Serializable {
    String name; 
    String path; 
    ConfigPath base; 
    transient File resolved;
    
    public ConfigPath(String name, String path) {
        super();
        this.name = name;
        this.path = path;
    }

    public ConfigPath getBase() {
        return base;
    }

    public void setBase(ConfigPath base) {
        this.base = base;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    } 
    
    public File getFile() {
        return (base == null)
                        ? new File(path) 
                        : new File(base.getFile(), path);
    }
    
    public ConfigPath merge(ConfigPath previous) {
        if(name==null) {
            setName(previous.getName()); 
        }
        if(path==null) {
            setPath(previous.getPath()); 
        }
        return this; 
    }
}
