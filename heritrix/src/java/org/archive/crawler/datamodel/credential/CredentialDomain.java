/* CredentialDomain
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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.management.AttributeNotFoundException;

import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.settings.ListType;
import org.archive.crawler.datamodel.settings.MapType;
import org.archive.crawler.datamodel.settings.ModuleType;
import org.archive.crawler.datamodel.settings.SimpleType;
import org.archive.crawler.datamodel.settings.Type;

/**
 * A credential domain.
 * 
 * Has an awkward moniker so we don't clash w/ other notions around domains.
 * This domain comprises the 'URI canonical root URL' as per RFC2617;
 * effectively, the url's domain + port number.  This name should map to a
 * CrawlServer name so we can hang a CredentialDomain and its credential 
 * contents on a CrawlServer.
 * 
 * <p>Implements JAAS principal (TODO: May have to implement serializable).
 * 
 * @author stack
 * @version $Revision$, $Date$
 */
public class CredentialDomain
    extends ModuleType implements java.security.Principal {
    
    /**
     * Local copy of the name of this object kept around for easy access.
     */
    private String credentialDomainName = null;
    
    static String xxx = null;
    
    
    /**
     * Constructor.
     * 
     * Protected so can only be created by the CredentialStore.
     * 
     * @param credentialDomainName A 'URI canonical root URL'.
     */
    protected CredentialDomain(String credentialDomainName) {
        super(credentialDomainName, "Holds credentials for named domain.");
        this.credentialDomainName = credentialDomainName;
        Type t = addElementToDefinition(new CredentialListType());
        t.setOverrideable(true);
        t.setExpertSetting(true);
    }
    
    /**
     * Convenience method used by inner classes to get at name for this class.
     * @return Returns the credentialDomainName.
     */
    protected String getCredentialDomainName()
    {
        return this.credentialDomainName;
    }
    
    /**
     * @param curi A crawl uri.  <code>curi</code> sets context for the 
     * getting of parameters.  If null, we use global context.
     * @return Returns the list of credentials.
     */
    public ListType getCredentials(CrawlURI curi)
        throws AttributeNotFoundException {
        
        return (ListType)getAttribute(CredentialListType.ATTR_NAME, curi);
    }
   
    /**
     * @param curi A crawl uri.  <code>curi</code> sets context for the 
     * getting of parameters.  If null, we use global context.
     * @param type Type of credentials to get.
     * @return Returns the list of rfc2617 credentials.
     */
    public List getCredentials(CrawlURI curi, Class type)
        throws AttributeNotFoundException {
        
        Object obj = getAttribute(CredentialListType.ATTR_NAME, curi);
        return (((CredentialListType)obj)).
            sublist(curi, type);
    }
    
    /**
     * Create an unassociated credential. 
     * 
     * An unassociated credential does not belong to any domain and will remain
     * so until explicitly added to a domain.
     * 
     * @param type Return a credential of the passed type.
     * @return Returns a new credential unattached to any CredentialDomain.
     */
    public Credential createCredential(Class type)
        throws IllegalArgumentException, InstantiationException,
            IllegalAccessException, InvocationTargetException {
        
        Constructor [] cs = type.getConstructors();
        if (cs.length != 1) {
            throw new IllegalArgumentException("More than or less than 1" +
                " constructor(s) when expected one only: " + cs.toString());
        }
        Object [] objs = {this};
        return (Credential)cs[0].newInstance(objs);
    }
    
    /**
     * @return List of credential types.
     */
    public List getCredentialTypes() throws AttributeNotFoundException {    
        return ((CredentialListType)getCredentials(null)).getCredentialTypes();
    }
    
    /**
     * Add a credential to this domain.
     * 
     * 
     * @param curi A crawl uri.  <code>curi</code> sets context for the 
     * getting of parameters.  If null, we use global context.
     * @param credential Credential to add.
     * @return Returns the created credential.
     */
    public Credential addCredential(CrawlURI curi, Credential credential)
        throws AttributeNotFoundException {
        
        CredentialListType credentials =
            (CredentialListType)getCredentials(curi);
        // I used to check if credential of the passed type already existed
        // but thats kinda tough now I'm taking a perspective whereby I don't
        // look at the type.
        credentials.add(credential);
        return credential;
    }
    
    /**
     * Remove passed credential from list of credentials.
     * 
     * @param curi A crawl uri.  <code>curi</code> sets context for the 
     * getting of parameters.  If null, we use global context.
     * @param credential Credential to remove.
     * @return Returns the removed credential.
     */
    public Credential removeCredential(CrawlURI curi, Credential credential)
        throws AttributeNotFoundException {
        
        CredentialListType credentials =
            (CredentialListType)getCredentials(curi);
        credentials.remove(credential);
        return credential;
    }  
    
    /**
     * A ListType that holds credentials.
     * 
     * @author stack
     * @version $Revision$, $Date$
     */
    private class CredentialListType extends ListType
    {
        public static final String ATTR_NAME = "credentials";
        
        /**
         * List of possible credential types as an array.
         */
        private final Class [] _credentialTypes =
            {HtmlFormCredential.class, Rfc2617Credential.class};
        
        /**
         * List of possible credential types as a List.
         */
        private final List credentialTypes =
            Arrays.asList(this._credentialTypes);
        
        /**
         * Constructor.
         */
        public CredentialListType()
        {
            super(ATTR_NAME, "List of credentials.");
        }
        
        public Object checkType(Object element) throws ClassCastException
        {
            // Just try and cast it.  If fails, then ClassCastException.
            return (Credential)element;
        }
        
        /**
         * 
         * @param curi A crawl uri.  <code>curi</code> sets context for the 
         * getting of parameters.  If null, we use global context.
         * @param type Type of the list to return.  Type is some superclass of
         * credentials.
         * @return Sublist of all elements of passed type.
         */
        public List sublist(CrawlURI curi, Class type)
            throws AttributeNotFoundException {
            CredentialListType credentials =
                (CredentialListType)getCredentials(curi);
            List result = new ArrayList();
            for (Iterator i = credentials.iterator(); i.hasNext();) {
                Credential c = (Credential)i.next();
                if (!type.isInstance(c)) {
                    continue;
                }
                result.add(c);
            }
            return result;
        }
        
        /**
         * @return Returns the credentialTypes.
         */
        public List getCredentialTypes()
        {
            return this.credentialTypes;
        }
    }
    
    /**
     * Credential that holds all needed to do a GET/POST to a HTML form.
     * 
     * @author stack
     * @version $Revision$, $Date$
     */
    private class HtmlFormCredential extends Credential {
        
        public HtmlFormCredential()
        {
            super("HtmlFormCredential",
                "Credential that has all necessary" +
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

    /**
     * A Basic/Digest auth RFC2617 credential.
     * 
     * @author stack
     * @version $Revision$, $Date$
     */
    private class Rfc2617Credential extends Credential {

        public Rfc2617Credential()
        {
            super("Rfc2617Credential", "Basic/Digest Auth type credential.");
            
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
}
