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
package org.archive.crawler.datamodel.settings;



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
     * The root URI that this credential is to go against.
     */
    private String credentialDomain = null;
    
    /**
     * Constructor.
     * 
     * @param name Name of this credential.
     * @param description Descrtiption of this particular credential.
     */
    public Credential(String name, String description)
    {
        super(name, description);
    }
    
    /**
     * @return Returns the credentialDomain.
     */
    public String getCredentialDomain()
    {
        return this.credentialDomain;
    }
    
    /**
     * @param credentialDomain The credentialDomain to set.
     */
    public void setCredentialDomain(String credentialDomain)
    {
        this.credentialDomain = credentialDomain;
    }
}
