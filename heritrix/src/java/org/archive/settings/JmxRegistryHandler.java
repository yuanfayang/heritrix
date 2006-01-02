/* JmxRegistryHandler
 * 
 * $Id$
 * 
 * Created on Dec 30, 2005
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
package org.archive.settings;

/**
 * Implementation of Settings Handler that use JMX Agent as Settings registry.
 * Won't scale to hundreds of thousands of domains w/o customization
 * -- Interceptors or home-grown agent -- but will work prototyping, for
 * globals, and a small number of overrides.
 * <p>TODO: Get via a HandlerFactory.
 * @author stack
 * @version $Date$ $Revision$
 */
class JmxRegistryHandler implements Handler {
    public JmxRegistryHandler() {
        super();
    }

    /* (non-Javadoc)
     * @see org.archive.settings.SettingsHandler#getSettings(java.lang.String)
     */
    public Settings getSettings(String componentName) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.archive.settings.SettingsHandler#getSettings(java.lang.String, java.lang.String)
     */
    public Settings getSettings(String componentName, String domain) {
        // TODO Auto-generated method stub
        return null;
    }

    public static void main(String[] args) {
    }
}
