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

/**
 * Credential.
 * 
 * Let this be also a credential in the JAAS sense also.
 * 
 * <p>Abstract.
 * 
 * @author stack
 * @version $Revision$, $Date$
 */
public abstract class Credential extends ModuleType {
    
    /**
     * Constructor.
     * 
     * @param name Name is unique within a credential type.  For example, if
     * the credential is rfc2617, then name should be the realm: i.e. there 
     * is only one credential for a particular realm.
     * @param description Descrtiption of this particular credential.
     */
    protected Credential(String name, String description)
    {
        super(name, description);
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object arg0)
    {
        return this.hashCode() == arg0.hashCode();
    }
}
