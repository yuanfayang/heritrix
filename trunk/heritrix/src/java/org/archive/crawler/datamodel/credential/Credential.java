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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Logger;

import javax.management.Attribute;
import javax.management.AttributeNotFoundException;
import javax.management.InvalidAttributeValueException;

import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.settings.CrawlerSettings;
import org.archive.crawler.settings.ModuleType;
import org.archive.crawler.settings.SimpleType;
import org.archive.crawler.settings.Type;



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
    
    private static Logger logger = Logger.getLogger(
        "org.archive.crawler.datamodel.credential.Credential");
    
    private static final String ATTR_CREDENTIAL_DOMAIN = "credential-domain";
    
    /**
     * Constructor.
     * 
     * @param name Name of this credential.
     * @param description Descrtiption of this particular credential.
     */
    public Credential(String name, String description)
    {
        super(name, description);
        Type t = addElementToDefinition(new SimpleType(ATTR_CREDENTIAL_DOMAIN,
                "The root URI this credential goes against.", ""));
            t.setOverrideable(false);
            t.setExpertSetting(true);
    }
    
    /**
     * @param context Context to use when searching for credential domain.
     * @return The domain/root URI this credential is to go against.
     */
    public String getCredentialDomain(CrawlURI context)
            throws AttributeNotFoundException {
        return (String)getAttribute(ATTR_CREDENTIAL_DOMAIN, context);  
    }
    
    /**
     * @param context Context to use when searching for credential domain.
     * @param domain New domain.
     */
    public void setCredentialDomain(CrawlerSettings context, String domain)
            throws InvalidAttributeValueException, AttributeNotFoundException {
        setAttribute(context, new Attribute(ATTR_CREDENTIAL_DOMAIN, domain));
    }
    
    /**
     * Convenience method for pulling credentials of a single type from passed
     * set of mixed credential types.
     * 
     * @param credentials Set of credentials of mixed type.
     * @param type Type to strain the list of passed credentials with.
     * 
     * @return Credentials of the passed type set for this server.  Returns
     * null if no credentials of passed type associated with this server.
     */
    public static Set filterCredentials(Set credentials, Class type) {
        Set result = null;
        if (credentials != null && credentials.size() > 0) {
            result = filterCredentials(credentials.iterator(), type);
        }
        return result;    
    }
    
    /**
     * Convenience method for pulling credentials of a single type from passed
     * set of mixed credential types.
     * 
     * @param credentials Set of credentials of mixed type.
     * @param type Type to strain the list of passed credentials with.
     * @param context Context to use when searching for credential domain.
     * @param credentialDomain Name of server to use filtering credentials.  Can
     * have a port appended (i.e. its the root URI as per RFC2617).  If null,
     * we return all credentials w/o filtering on credentialDomain.
     * 
     * @return Credentials of the passed type set for this server.  Returns
     * null if no credentials of passed type associated with this server.
     */
    public static Set filterCredentials(Set credentials, Class type,
            CrawlURI context, String credentialDomain) {
        Set result = null;
        if (credentials != null && credentials.size() > 0) {
            result = filterCredentials(credentials.iterator(), type, context,
                credentialDomain);
        }
        return result;    
    }
    
    /**
     * Convenience method for pulling credentials of a single type from passed
     * set of mixed credential types.
     * 
     * @param iterator Iterator over a set credentials of mixed type.
     * @param type Type to strain the list of passed credentials with.
     * 
     * @return New set of credentials of the passed type set for this server. 
     * Returns null if no credentials of passed type associated with this
     * server.
     */
    public static Set filterCredentials(Iterator iterator, Class type) {

        return filterCredentials(iterator, type, null, null);
    }
    
    /**
     * Convenience method for pulling credentials of a single type from passed
     * set of mixed credential types.
     * 
     * If non-null context and credentialDomain, will filter on type AND
     * credential domain.
     * 
     * @param iterator Iterator over a set credentials of mixed type.
     * @param type Type to strain the list of passed credentials with.
     * @param context Context to use when searching for credential domain.
     * @param credentialDomain Name of server to use filtering credentials.
     * This is the credential domain we're looking to match. Can
     * have a port appended (i.e. its the root URI as per RFC2617).  If null,
     * we return all credentials w/o filtering on credentialDomain.
     * 
     * @return New set of credentials of the passed type set for this server. 
     * Returns null if no credentials of passed type associated with this
     * server.
     */
    public static Set filterCredentials(Iterator iterator, Class type,
            CrawlURI context, String credentialDomain) {
        
        Set result = null;
        if (iterator == null) {
            return result;
        }
        while (iterator.hasNext()) {
            
            Credential c = (Credential)iterator.next();
            if (!type.isInstance(c)) {
                continue;
            }
            
            if (credentialDomain != null && credentialDomain.length() > 0 &&
                    context !=  null) {
                String cd = null;
                try
                {
                    cd = c.getCredentialDomain(context);
                } catch (AttributeNotFoundException e) {
                    logger.severe("Failed get of credential domain: " +
                        c + " " + context.toString());
                    continue;
                }
                
                if (!cd.equals(credentialDomain)) {
                    // If credentials don't match, skip..continue.
                    continue;
                }
            }
            
            if (result == null) {
                result = new HashSet();
            }
            result.add(c);
        }
        return result;    
    }
}
