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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.management.AttributeNotFoundException;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanException;
import javax.management.ReflectionException;

import org.archive.crawler.datamodel.settings.Credential;
import org.archive.crawler.datamodel.settings.MapType;
import org.archive.crawler.datamodel.settings.ModuleType;
import org.archive.crawler.datamodel.settings.SimpleType;
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

    /**
     * Name of the contained credentials map type.
     */
    public static final String CREDENTIALS = "credentials";
    
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
     */
    public CredentialStore()
    {
        super(ATTR_NAME, "Credentials used by heritrix" +
            " authenticating.\nSee http://crawler.archive.org/proposals/auth/" +
            " for background.");
        
        Type t = addElementToDefinition(new MapType(CREDENTIALS,
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
     * @param curi A crawl uri.  <code>curi</code> sets context for the 
     * getting of parameters.  If null, we use global context.
     * @return A map of all credentials.
     */
    protected MapType get(CrawlURI curi)
        throws AttributeNotFoundException {
        
        return (MapType)getAttribute(CREDENTIALS, curi);
    }
    
    /**
     * @param curi A crawl uri.  <code>curi</code> sets context for the 
     * getting of parameters.  If null, we use global context.
     * @return An interator over all credentials
     */
    protected Iterator iterator(CrawlURI curi)
        throws AttributeNotFoundException {
        
        MapType cmt = get(curi);
        return cmt.iterator(cmt.getSettingsFromObject(curi));
    }   
    
    /**
     * @param curi A crawl uri.  <code>curi</code> sets context for the 
     * getting of parameters.  If null, we use global context.
     * @param name Name to give the manufactured credential.  Should be unique
     * else the add of the credential to the list of credentials will fail.
     * @return Returns <code>name</code>'d credential.
     */
    public Credential get(CrawlURI curi, String name)
        throws AttributeNotFoundException, MBeanException, ReflectionException {
        
        return (Credential)get(curi).getAttribute(name);
    }
    
    /**
     * Create and add to the list a credential of the passed <code>type</code>
     * giving the credential the passed <code>name</code>.
     * 
     * @param curi A crawl uri.  <code>curi</code> sets context for the 
     * getting of parameters.  If null, we use global context.
     * @param name Name to give the manufactured credential.  Should be unique
     * else the add of the credential to the list of credentials will fail.
     * @param type Type of credentials to get.
     * @return The credential created and added to the list of credentials.
     */
    public Credential create(CrawlURI curi, String name, Class type)
        throws IllegalArgumentException, InstantiationException,
        IllegalAccessException, InvocationTargetException,
        InvalidAttributeValueException, AttributeNotFoundException {
        
        Constructor [] constructors = type.getConstructors();
        if (constructors.length != 1) {
            throw new IllegalArgumentException("More than or less than 1" +
                " constructor(s) when expected one only: " +
                constructors.toString());
        }
        Object [] objs = {this, name};
        final Credential result = (Credential)constructors[0].newInstance(objs);
        
        // Now add the just-created credential to the list.
        get(curi).addElement(getSettingsFromObject(curi), result);
        
        return result;
    }
    
    /**
     * Delete the credential <code>name</code>.
     * 
     * @param curi A crawl uri.  <code>curi</code> sets context for the 
     * getting of parameters.  If null, we use global context.
     * @param credential Credential to delete.
     */
    public void remove(CrawlURI curi, Credential credential)
        throws IllegalArgumentException, AttributeNotFoundException {
        
        remove(curi, credential.getName());
    }   
    
    /**
     * Delete the credential <code>name</code>.
     * 
     * @param curi A crawl uri.  <code>curi</code> sets context for the 
     * getting of parameters.  If null, we use global context.
     * @param name Name of credential to delete.
     */
    public void remove(CrawlURI curi, String name)
        throws IllegalArgumentException, AttributeNotFoundException {
        
        get(curi).removeElement(getSettingsFromObject(curi), name);
    }
    
    /**
     * Return sublist made up of all credentials of the passed
     * <code>type</code>.
     * 
     * @param curi A crawl uri.  <code>curi</code> sets context for the 
     * getting of parameters.  If null, we use global context.
     * @param type Type of the list to return.  Type is some superclass of
     * credentials.
     * @return Unmodifable sublist of all elements of passed type.
     */
    public List sublist(CrawlURI curi, Class type)
        throws AttributeNotFoundException {
        
        List result = new ArrayList();
        for (Iterator i = iterator(curi); i.hasNext();) {
            Credential c = (Credential)i.next();
            if (!type.isInstance(c)) {
                continue;
            }
            result.add(c);
        }
        return result;
    }
    
    /**
     * A Basic/Digest auth RFC2617 credential.
     * 
     * @author stack
     * @version $Revision$, $Date$
     */
    private class Rfc2617Credential extends Credential {

        public Rfc2617Credential(String name)
        {
            super(name, "Basic/Digest Auth type credential.");
            
            Type t = addElementToDefinition(new SimpleType("realm",
                "Basic/Digest Auth realm.", "Realm"));
            t.setOverrideable(true);
            t.setExpertSetting(true);
        
            t = addElementToDefinition(new SimpleType("preempt",
                "Preemptively offer credential in advance of 401 challenge.",
                Boolean.FALSE));
            t.setOverrideable(true);
            t.setExpertSetting(true);
            
            t = addElementToDefinition(new SimpleType("login", "Login.",
                "login"));
            t.setOverrideable(true);
            t.setExpertSetting(true);
            
            t = addElementToDefinition(new SimpleType("password", "Password.",
                "password"));
            t.setOverrideable(true);
            t.setExpertSetting(true);
        }
    }
    
    /**
     * Credential that holds all needed to do a GET/POST to a HTML form.
     * 
     * @author stack
     * @version $Revision$, $Date$
     */
    private class HtmlFormCredential extends Credential {
        
        public HtmlFormCredential(String name)
        {
            super(name, "Credential that has all necessary" +
                " for running a POST/GET to an HTML login form.");
            
            Type t = addElementToDefinition(new SimpleType("url-pattern",
                    "Pattern that defines the realm this login covers.", ""));
                t.setOverrideable(true);
                t.setExpertSetting(true);
            
            t = addElementToDefinition(new SimpleType("login-uri",
                "URI of page that contains the HTML login form we're to apply" +
                " these credentials too.", ""));
            t.setOverrideable(true);
            t.setExpertSetting(true);

            final String [] METHODS = {"POST", "GET"};
            t = addElementToDefinition(new SimpleType("http-method",
                "GET or POST", METHODS[0], METHODS));
            t.setOverrideable(true);
            t.setExpertSetting(true);
            
            t = addElementToDefinition(new MapType("form-items", "Form items.",
                String.class));
            t.setOverrideable(true);
            t.setExpertSetting(true);       
            
            t = addElementToDefinition(new SimpleType("cookie-name",
                "Name of cookie that pertains to this authentication.\n" +
                "This field will be logged before, if present, and after" +
                " authentication attempt.  To aid debugging only.", ""));
            t.setOverrideable(true);
            t.setExpertSetting(true);
        }
    }
}
