/* SettingsHandler
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
package org.archive.settings;

import java.net.URL;

import javax.management.Attribute;
import javax.management.AttributeNotFoundException;
import javax.management.MBeanException;
import javax.management.ReflectionException;

/**
 * Gateway into settings system.
 * <p>TODO: Default does java serializing.  Override to
 * do do other serializations (SettingsHandlerFactory?).
 * <p><<Prototype>>
 * @author stack
 * @see <a href="http://crawler.archive.org/cgi-bin/wiki.pl?SettingsFrameworkRefactoring">Settings Framework Refactoring</a>.
 */
class SettingsHandler {
    protected SettingsHandler() {
        super();
    }
    
    public void initialization(final URL store) {
        // TODO: Default action is to load settings from passed store.
    }
    
    public Settings getSettings(final String componentName) {
        return getSettings(componentName, null);
    }
    
    public Settings getSettings(final String componentName,
            final String domain) {
        return null;
    }
    
    public Object getAttribute(final String componentName,
            final String attributeName, final String domain) {
        return null;
    }

    public static void main(String[] args)
    throws AttributeNotFoundException, MBeanException, ReflectionException {
        SettingsHandler handler = new SettingsHandler();
        // Examples
        Settings bdbFrontierSettings =
            handler.getSettings("order.bdbfrontier");
        Attribute a = (Attribute)bdbFrontierSettings.getAttribute("uri-included-structure");
        handler.getSettings("order.extractors.extractorhtml");
    }
}
