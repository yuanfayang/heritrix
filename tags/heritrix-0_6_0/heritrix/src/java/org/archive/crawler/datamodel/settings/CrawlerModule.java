/* CrawlerModule
 *
 * $Id$
 *
 * Created on Dec 17, 2003
 *
 * Copyright (C) 2004 Internet Archive.
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
package org.archive.crawler.datamodel.settings;

import javax.management.InvalidAttributeValueException;

/**
 * Superclass of all modules that should be configurable.
 *
 * @author John Erik Halse
 */
public class CrawlerModule extends ComplexType {
    /** Creates a new CrawlerModule.
     *
     * This constructor is made to help implementors of subclasses. It is an
     * requirement that subclasses at the very least implements a constructor
     * that takes only the name as an argument.
     *
     * @param name the name of the module.
     * @param description the description of the module.
     */
    public CrawlerModule(String name, String description) {
        super(name, description);
    }

    /** Every subclass should implement this constructor
     *
     * @param name of the module
     */
    public CrawlerModule(String name) {
        super(name, name);
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.datamodel.settings.ComplexType#addElement(org.archive.crawler.datamodel.settings.CrawlerSettings, org.archive.crawler.datamodel.settings.Type)
     */
    public Type addElement(CrawlerSettings settings, Type type)
            throws InvalidAttributeValueException {
        if (isInitialized()) {
            String scope = settings.getScope() == null ? "global" : settings
                    .getScope();
            throw new IllegalStateException(
                    "Not allowed to add elements to modules after"
                            + " initialization. (Module: " + getName()
                            + ", Element: " + type.getName() + ", Settings: "
                            + settings.getName() + " (" + settings.getScope()
                            + ")");
        }
        return super.addElement(settings, type);
    }
}
