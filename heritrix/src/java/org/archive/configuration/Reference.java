/* Reference
 * 
 * $Id$
 * 
 * Created on Jan 4, 2006
 *
 * Copyright (C) 2006 Internet Archive.
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

import java.util.Map;

import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;

/**
 * Utility class to build References.
 * Creates pointers back into the registry.
 * Tried to do as subclass of CompositeDataSupport but then in remote client,
 * the resultant Reference composite is unrecognizable.
 * @author stack
 * @version $Date$ $Revision$
 */
public class Reference  {
    private static final long serialVersionUID = -4313585767623925356L;
    private static final String DOMAIN_KEY = "domain";
    private static final String LIST_STR_KEY = "keyPropertyListString";
    private static final String [] KEYS =
        new String [] {DOMAIN_KEY, LIST_STR_KEY};
    static CompositeType COMPOSITE_TYPE;
    static {
        try {
            COMPOSITE_TYPE = new CompositeType(Reference.class.getName(),
                "ObjectName as CompositeType Reference to a " +
                    "Configuration in registry", KEYS,
                new String [] {"ObjectName domain",
                    "ObjectName#getCanonicalKeyPropertyListString() output"},
                new OpenType [] {SimpleType.STRING, SimpleType.STRING});
        } catch (OpenDataException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Shutdown constructor so no accidental instantiation of Reference.
     */
    private Reference() {
        super();
    }
    
    public static CompositeData get(final ObjectName on)
    throws OpenDataException {
        return new CompositeDataSupport(COMPOSITE_TYPE, KEYS,
            new String [] {on.getDomain(),
                on.getCanonicalKeyPropertyListString()});
    }
}
