/* Registry
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

import javax.management.AttributeNotFoundException;



/**
 * Registry of application configuration.
 * Come here to get component configuration.
 * <p><<Prototype>>
 * <p>TODO: Add serializing Visitor.  Pass on construction or to an init.
 * The Visitor will know how to load configuration.  Must work on a
 * per-component basis so can load single component's config. only.
 * @author stack
 * @see <a href="http://crawler.archive.org/cgi-bin/wiki.pl?SettingsFrameworkRefactoring">Settings Framework Refactoring</a>
 */
public interface Registry {
    /**
     * System property key of where on disk to persist configuration.
     */
    public static final String STORE_DIR_KEY =
        "org.archive.configuration.store-dir";
    
    public static final String NAME_KEY = "name";
    public static final String TYPE_KEY = "type";
    
    
    /**
     * Get <code>attributeName</code> on <code>component</code>.
     * @param attributeName Name of component atribute to get.
     * @param componentName Name of component that has
     * <code>attributeName</code>.
     * @return Value of <code>attributeName</code>.
     * @throws AttributeNotFoundException
     */
    public Object get(final String attributeName, final String componentName)
    throws AttributeNotFoundException;
    
    /**
     * Get <code>attributeName</code> on <code>component</code>.
     * @param attributeName Name of component atribute to get.
     * @param componentName Name of component that has
     * <code>attributeName</code>.
     * @param componentType Type of component.
     * @return Value of <code>attributeName</code>.
     * @throws AttributeNotFoundException
     */
    public Object get(final String attributeName,
         final String componentName, final Class<?> componentType)
    throws AttributeNotFoundException;
    
    /**
     * Get <code>attributeName</code> on <code>component</code>
     * in <code>domain</code>.
     * @param attributeName Name of component atribute to get.
     * @param componentName Name of component that has
     * <code>attributeName</code>.
     * @param componentType Type of component.
     * @param domain Domain scope for Settings.  Domain should be
     * specified reversed as is done in java packaging: e.g. To find Settings
     * for 'archive.org', pass the domain written as 'org.archive'.
     * @return Value of <code>attributeName</code>.
     * @throws AttributeNotFoundException
     */
    public Object get(final String attributeName,
        final String componentName, final Class<?> componentType,
        final String domain)
    throws AttributeNotFoundException;
    
    /**
     * Register a configuration object.
     * Used by configuration applications such as UI adding in new configurations.
     * @param componentName Component name to register the object against.
     * @param componentType Type of component.
     * @param instance Object to register.
     * @return Object to use referring subsequently to instance (Pass this
     * object to {@link #deRegister(Object)}.
     */
    public Object register(final String componentName,
         final Class<?> componentType, final Configuration instance)
    throws ConfigurationException;
    
    /**
     * Register a configuration object.
     * Used by configuration applications such as UI adding in new configurations.
     * @param componentName Component name to register the object against.
     * @param componentType Type of component.
     * @param domain Domain to register against. Domain should be
     * specified reversed as is done in java packaging: e.g. To find Settings
     * for 'archive.org', pass the domain written as 'org.archive'.
     * @param instance Object to register.
     * @return Object to use referring subsequently to instance (Pass this
     * object to {@link #deRegister(Object)}.
     */
    public Object register(final String componentName,
         final Class<?> componentType, final String domain,
         final Configuration instance)
    throws ConfigurationException;
    
    /**
     * @param componentName Component name to look for.
     * @param componentType Type of component.
     * @return True if registered.
     */
    public boolean isRegistered(final String componentName,
        final Class<?> componentType);
    
    /**
     * @param componentName Component name to look for.
     * @param componentType Type of component.
     * @param domain Domain to search against. Domain should be
     * specified reversed as is done in java packaging: e.g. To find Settings
     * for 'archive.org', pass the domain written as 'org.archive'.
     * @return True if registered.
     */
    public boolean isRegistered(final String componentName,
         final Class<?> componentType, final String domain);
    
    /**
     * Unregister named object.
     * @param obj Identifier for registered Settings.
     */
    public void deRegister(final Object obj);
    
    /**
     * Unregister named object.
     * @param componentName Component name to look for.
     * @param componentType Type of component.
     */
    public void deRegister(final String componentName,
        final Class<?> componentType);
    
    /**
     * Unregister named object.
     * @param componentName Component name to look for.
     * @param componentType Type of component.
     * @param domain Domain to search against. Domain should be
     */
    public void deRegister(final String componentName,
        final Class<?> componentType, final String domain);
    
    /**
     * Load (or reload) settings from store.
     * Part of loading is registering each component.  Should be synchronized.
     * @param domain Do loading of items from this <code>domain</code>.  Can
     * be subdomain or base domain.
     * @throws IOException
     */
    public void load(final String domain) throws IOException;
    
    /**
     * Save current state of settings.
     * Part of saving is unregistering each component.  Should be synchronized.
     * @param domain Do loading of items from this <code>domain</code> only.
     * @throws IOException
     */
    public void save(final String domain) throws IOException;
}