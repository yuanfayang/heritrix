/* CredentialStore
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
package org.archive.crawler.datamodel;

import javax.management.AttributeNotFoundException;
import javax.management.InvalidAttributeValueException;

import org.archive.crawler.datamodel.credential.*;
import org.archive.crawler.datamodel.settings.MapType;
import org.archive.crawler.datamodel.settings.ModuleType;
import org.archive.crawler.datamodel.settings.Type;

/**
 * Front door to the credential store.
 * 
 * See <a 
 * href="http://crawler.archive.org/proposals/auth/#credentialstoredesign">Credential
 * Store Design</a>.
 * 
 * @author stack
 * @version $Revision$, $Date$
 */
public class CredentialStore extends ModuleType {
    
    public static final String ATTR_NAME = "credential-store";
    private static final String ATTR_CREDENTIAL_DOMAINS = "credential-domains";

    /**
     * Constructor.
     */
    public CredentialStore()
    {
        super(ATTR_NAME, "Credentials used by heritrix" +
            " authenticating.\nSee http://crawler.archive.org/proposals/auth/" +
            " for background.");
        Type t = addElementToDefinition(
            new MapType(ATTR_CREDENTIAL_DOMAINS,
            "Map of credential domains keyed by domain name.",
            CredentialDomain.class));
        t.setOverrideable(true);
        t.setExpertSetting(true);
    }
    
    /**
     * @param curi A crawl uri.  <code>curi</code> sets context for the 
     * getting of parameters.  If null, we use global context.
     * @return Returns the credentialDomains.
     */
    public MapType getCredentialDomains(CrawlURI curi)
        throws AttributeNotFoundException
    {
        return (MapType)getAttribute(ATTR_CREDENTIAL_DOMAINS, curi);
    }
    
    /**
     * @param credentialDomainName Name of domain to get.
     * @param curi A crawl uri.  <code>curi</code> sets context for the 
     * getting of parameters.  If null, we use global context.
     * @return A CredentialDomain.
     */
    public CredentialDomain getCredentialDomain(String credentialDomainName,
            CrawlURI curi) throws AttributeNotFoundException {
        return (CredentialDomain)getCredentialDomains(curi).
            getAttribute(credentialDomainName, curi);
    }
    
    /**
     * @param credentialDomainName Name of domain to get.
     * @param curi A crawl uri.  <code>curi</code> sets context for the 
     * getting of parameters.  If null, we use global context.
     * @return A CredentialDomain.
     */
    public boolean hasCredentialDomain(String credentialDomainName,
            CrawlURI curi) {
        
        boolean result = true;
        try
        {
            getCredentialDomain(credentialDomainName, curi);
        }
        catch (AttributeNotFoundException e)
        {
            result = false;
        }
        return result;
    }
    
    /**
     * Creates and adds to the list of credential domains a new
     * CredentialDomain of the passed name.
     * 
     * @param credentialDomainName Name of domain to create.
     * @param curi A crawl uri.  <code>curi</code> sets context for the 
     * getting of parameters.  If null, we use global context.
     * @return Created CredentialDomain.
     */
    public synchronized CredentialDomain createCredentialDomain(
            String credentialDomainName, CrawlURI curi)
        throws AttributeNotFoundException, InvalidAttributeValueException {
       
        MapType domains = getCredentialDomains(curi);
        CredentialDomain anonymousSubClass =
            new CredentialDomain(credentialDomainName) {
                // Anonymous subsclass done so class can't be instantiated
                // directly -- you have to go via this create method.
            };
        return (CredentialDomain)domains.addElement(getSettingsFromObject(curi),
            anonymousSubClass);
    }
    
    /**
     * Delete the named CredentialDomain for the list of credential domains.
     *
     * @param credentialDomainName Name of domain to delete.
     * @param curi A crawl uri.  <code>curi</code> sets context for the 
     * getting of parameters.  If null, we use global context.
     * @return Deleted CredentialDomain.
     */
    public synchronized CredentialDomain deleteCredentialDomain(
        String credentialDomainName, CrawlURI curi)
        throws AttributeNotFoundException {
        
        MapType domains = getCredentialDomains(curi);
        return (CredentialDomain)domains.removeElement(
            getSettingsFromObject(curi), credentialDomainName);
    }
}
