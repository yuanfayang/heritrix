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

import java.util.LinkedList;
import java.util.Map;

/**
 * Interface for objects that can contribute 'overrides' to replace the
 * usual values in configured objects. 
 * @contributor gojomo
 */
public interface OverrideContext {
    /** test if this context has actually been configured with overrides
     * (even if in fact no overrides were added) */
    public boolean haveOverrideNamesBeenSet();
    /** return a list of the names of override maps to consider */ 
    LinkedList<String> getOverrideNames();
    /** get the map corresponding to the override name */ 
    Map<String,Object> getOverrideMap(String name);
}
