/*
 *  This file is part of the Heritrix web crawler (crawler.archive.org).
 *
 *  Licensed to the Internet Archive (IA) by one or more individual 
 *  contributors. 
 *
 *  The IA licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.archive.spring;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

/**
 * ConfigPath with added implication that it is an individual,
 * readable/writable File. 
 */
public class ConfigFile extends ConfigPath implements ReadSource, WriteTarget {
    private static final long serialVersionUID = 1L;

    public ConfigFile() {
        super();
    }

    public ConfigFile(String name, String path) {
        super(name, path);
    }

    public Reader getReader() {
        try {
            if(!getFile().exists()) {
                getFile().createNewFile();
            }
            return new FileReader(getFile());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Writer getWriter() {
        return getWriter(false); 
    }
    
    public Writer getWriter(boolean append) {
        try {
            return new FileWriter(getFile(), append);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
