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

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.management.AttributeNotFoundException;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanException;
import javax.management.ReflectionException;

import org.archive.crawler.datamodel.credential.*;
import org.archive.crawler.settings.CrawlerSettings;
import org.archive.crawler.settings.MapType;
import org.archive.crawler.settings.ModuleType;
import org.archive.crawler.settings.SettingsHandler;
import org.archive.crawler.settings.Type;


/**
 * Front door to the credential store.
 * 
 * Come here to get at credentials.
 * 
 * <p>See <a 
 * href="http://crawler.archive.org/proposals/auth/#credentialstoredesign">Credential
 * Store Design</a>.
 * 
 * @author stack
 * @version $Revision$, $Date$
 */
public class CredentialStore extends ModuleType {
    
    private static Logger logger = Logger.getLogger(
        "org.archive.crawler.datamodel.CredentialStore");
    
    public static final String ATTR_NAME = "credential-store";
    
    /**
     * Name of the contained credentials map type.
     */
    public static final String ATTR_CREDENTIALS = "credentials";
    
    /**
     * List of possible credential types as a List.
     * 
     * This types are inner classes of this credential type so they cannot
     * be created without their being associated with a credential list.
     */
    private static final List credentialTypes;
    // Initialize the credentialType data member.
    static {
        // Array of all known credential types.
        Class [] tmp = {HtmlFormCredential.class, Rfc2617Credential.class};
        credentialTypes = Collections.unmodifiableList(Arrays.asList(tmp));
    }
    
    /**
     * Constructor.
     * 
     * @param name for this credential store.
     */
    public CredentialStore(String name)
    {
        super(name, "Credentials used by heritrix" +
            " authenticating.\nSee http://crawler.archive.org/proposals/auth/" +
            " for background.");
        
        Type t = addElementToDefinition(new MapType(ATTR_CREDENTIALS,
            "Map of credentials.", Credential.class));
        t.setOverrideable(true);
        t.setExpertSetting(true);
    }
    
    /**
     * @return Unmodifable list of credential types.
     */
    public static List getCredentialTypes() {
        return CredentialStore.credentialTypes;
    }
    
    /**
     * @param context Pass a CrawlURI, CrawlerSettings or UURI.  Used to set
     * context.  If null, we use global context.
     * @return A map of all credentials from passed context.
     */
    protected MapType get(Object context)
        throws AttributeNotFoundException {
        
        return (MapType)getAttribute(context, ATTR_CREDENTIALS);
    }
    
    /**
     * @param context Pass a CrawlURI, CrawlerSettings or UURI.  Used to set
     * context.  If null, we use global context.
     * @param name Name to give the manufactured credential.  Should be unique
     * else the add of the credential to the list of credentials will fail.
     * @return Returns <code>name</code>'d credential.
     */
    public Credential get(Object context, String name)
        throws AttributeNotFoundException, MBeanException, ReflectionException {
        
        return (Credential)get(context).getAttribute(name);
    }
    
    /**
     * Create and add to the list a credential of the passed <code>type</code>
     * giving the credential the passed <code>name</code>.
     * 
     * @param context Pass a CrawlerSettings.  Used to set
     * context.  If null, we use global context.
     * @param name Name to give the manufactured credential.  Should be unique
     * else the add of the credential to the list of credentials will fail.
     * @param type Type of credentials to get.
     * @return The credential created and added to the list of credentials.
     */
    public Credential create(CrawlerSettings context, String name, Class type)
        throws IllegalArgumentException, InvocationTargetException,
        InvalidAttributeValueException, AttributeNotFoundException {
        
        Credential result = (Credential)SettingsHandler.
            instantiateModuleTypeFromClassName(name, type.getName());
        // Now add the just-created credential to the list.
        get(context).addElement(context, result);
        return result;
    }
    
    /**
     * Delete the credential <code>name</code>.
     * 
     * @param context Pass a CrawlerSettings.  Used to set
     * context.  If null, we use global context.
     * @param credential Credential to delete.
     */
    public void remove(CrawlerSettings context, Credential credential)
        throws AttributeNotFoundException, IllegalArgumentException {
        
        remove(context, credential.getName());
    }   
    
    /**
     * Delete the credential <code>name</code>.
     * 
     * @param context Pass a CrawlerSettings.  Used to set
     * context.  If null, we use global context.
     * @param name Name of credential to delete.
     */
    public void remove(CrawlerSettings context, String name)
        throws IllegalArgumentException, AttributeNotFoundException {
        
        get(context).removeElement(context, name);
    }
    
    /**
     * @return Iterator over the credentials in this CredentialStore.
     */
    public Iterator iterator(Object context) {
        
        Iterator result =  null;
        try
        {
            result = get(context).iterator(context);
        } catch (AttributeNotFoundException e) {
            logger.severe("Failed getting iterator: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Return sublist made up of all credentials of the passed
     * <code>type</code>.
     * 
     * @param context Pass a CrawlURI or a CrawlerSettings.  Used to set
     * context.  If null, we use global context.
     * @param type Type of the list to return.  Type is some superclass of
     * credentials.
     * @return Unmodifable sublist of all elements of passed type.
     */
    public List sublist(Object context, Class type) {
        
        List result = new ArrayList();
        for (Iterator i = iterator(context); i.hasNext();) {
            Credential c = (Credential)i.next();
            if (!type.isInstance(c)) {
                continue;
            }
            result.add(c);
        }
        return result;
    }
}
