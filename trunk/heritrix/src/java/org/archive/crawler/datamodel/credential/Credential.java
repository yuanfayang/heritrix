/* Credential
 * 
 * Created on Apr 1, 2004
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
package org.archive.crawler.datamodel.credential;

import org.archive.crawler.datamodel.settings.ModuleType;
import org.archive.crawler.datamodel.settings.SimpleType;
import org.archive.crawler.datamodel.settings.Type;



/**
 * Credential type.
 * 
 * Let this be also a credential in the JAAS sense to in that this is what
 * gets added to a subject on successful authentication since it contains
 * data needed to authenticate (realm, login, password, etc.).
 * 
 * <p>Settings system assumes that subclasses implement a constructor that
 * takes a name only.
 * 
 * @author stack
 * @version $Revision$, $Date$
 */
public abstract class Credential extends ModuleType {
    
    /**
     * Constructor.
     * 
     * @param name Name of this credential.
     * @param description Descrtiption of this particular credential.
     */
    public Credential(String name, String description)
    {
        super(name, description);
        Type t = addElementToDefinition(new SimpleType("credential-domain",
                "The root URI this credential goes against.", ""));
            t.setOverrideable(false);
            t.setExpertSetting(true);
    }
}
