/* Handler
*
* $Id$
*
* Created Dec 28, 2005
*
* Copyright (C) 2005 Internet Archive.
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
*/
package org.archive.configuration;

import java.io.IOException;



/**
 * Handler is gateway into configuration system.
 * Use to obtain component configuration.
 * <p><<Prototype>>
 * <p>TODO: Add serializing Visitor.  Pass on construction or to an init.
 * The Visitor will know how to load configuration.  Must work on a
 * per-component basis so can load single component's config. only.
 * @author stack
 * @see <a href="http://crawler.archive.org/cgi-bin/wiki.pl?SettingsFrameworkRefactoring">Settings Framework Refactoring</a>
 */
public interface Handler {
    /**
     * Get <code>attributeName</code> on <code>component</code>.
     * @param attributeName Name of component atribute to get.
     * @param component Name of component that has <code>attributeName</code>.
     * @return Value of <code>attributeName</code>.
     */
    public Object get(final String attributeName, final String component);
    
    /**
     * Get <code>attributeName</code> on <code>component</code>
     * in <code>domain</code>.
     * @param attributeName Name of component atribute to get.
     * @param component Name of component that has <code>attributeName</code>.
     * @param domain Domain scope for Settings.  Domain should be
     * specified reversed as is done in java packaging: e.g. To find Settings
     * for 'archive.org', pass the domain written as 'org.archive'.
     * @return Value of <code>attributeName</code>.
     */
    public Object get(final String attributeName, final String component,
            final String domain);
    
    /**
     * Register a settings object.
     * @param component Component name to register the object against.
     * @param domain Domain to register against. Domain should be
     * specified reversed as is done in java packaging: e.g. To find Settings
     * for 'archive.org', pass the domain written as 'org.archive'.
     * @param instance Object to register.
     * @return Object to use referring subsequently to instance (Pass this
     * object to {@link #deregister(Object)}.
     */
    public Object register(final String component, final String domain,
            final Object instance);
    
    /**
     * Unregister named object.
     * @param registeredObjectName Identifier for registered Settings.
     */
    public void deregister(final Object registeredObjectName);
    
    /**
     * Load (or reload) settings from store.
     */
    public void load() throws IOException;
    
    /**
     * Load (or reload) settings from store.
     * @param domain Do loading of items from this <code>domain</code> only.
     */
    public void load(final String domain) throws IOException;
}