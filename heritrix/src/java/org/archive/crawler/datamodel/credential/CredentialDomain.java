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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.management.AttributeNotFoundException;
import javax.management.InvalidAttributeValueException;

import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.settings.ListType;
import org.archive.crawler.datamodel.settings.MapType;
import org.archive.crawler.datamodel.settings.ModuleType;
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
     * Constructor.
     * 
     * Protected so can only be created by the CredentialStore.
     * 
     * @param credentialDomainName A 'URI canonical root URL'.
     */
    protected CredentialDomain(String credentialDomainName) {
        super(credentialDomainName, "Holds credentials for named domain.");
        Type t = addElementToDefinition(new CredentialListType());
        t.setOverrideable(true);
        t.setExpertSetting(true);
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
     * @return Returns the list of rfc2617 credentials.
     */
    public List getRfc2617Credentials(CrawlURI curi)
        throws AttributeNotFoundException, ClassNotFoundException {
        
        Object obj = getAttribute(CredentialListType.ATTR_NAME, curi);
        return (((CredentialListType)obj)).
            sublist(curi, Rfc2617Credential.class.getName());
    }
    
    /**
     * @param curi A crawl uri.  <code>curi</code> sets context for the 
     * getting of parameters.  If null, we use global context.
     * @return Returns the list of html form credentials.
     */
    public List getHtmlFormCredentials(CrawlURI curi)
        throws AttributeNotFoundException, ClassNotFoundException {
        
        Object obj = getAttribute(CredentialListType.ATTR_NAME, curi);
        return (((CredentialListType)obj)).
            sublist(curi, HtmlFormCredential.class.getName());
    }
    
    /**
     * Add an RFC2617 credential to this domain.
     * 
     * Ensures one credental per realm only.
     * 
     * @param curi A crawl uri.  <code>curi</code> sets context for the 
     * getting of parameters.  If null, we use global context.
     * @param realm Basic/Digest Auth realm this credential goes up against.
     * @return Returns the created credential.
     */
    public Credential createRfc2617Credential(CrawlURI curi, String realm)
        throws AttributeNotFoundException, ClassNotFoundException {
        
        return createRfc2617Credential(curi, realm, false, null, null);
    }
    
    /**
     * Add an RFC2617 credential to this domain.
     * 
     * Ensures one credental per realm only.
     * 
     * @param curi A crawl uri.  <code>curi</code> sets context for the 
     * getting of parameters.  If null, we use global context.
     * @param realm Basic/Digest Auth realm this credential goes up against.
     * @param preemptive Whether to preemptively offer this credential.
     * @param login Basic/Digest Auth login.
     * @param password Basic/Digest Auth password.
     * @return Returns the created credential.
     */
    public Credential createRfc2617Credential(CrawlURI curi, String realm,
        boolean preemptive, String login, String password)
        throws AttributeNotFoundException, ClassNotFoundException {
        
        CredentialListType credentials =
            (CredentialListType)getCredentials(curi);
        List list = credentials.sublist(curi,
            Rfc2617Credential.class.getName());
        Credential c = null;
        for (Iterator i = list.iterator(); i.hasNext();) {
            c = (Credential)i.next();
            // RFC2617 says the realm compare should be case insensitive.
            if (c.getName().toLowerCase().equals(realm.toLowerCase())) {
                throw new IllegalArgumentException("Already a credential for" +
                    " this realm.");
            }
        }
        
        c = new Rfc2617Credential(realm, preemptive, login, password) {
                // Anonymous subclass. Parent cannot be instantiated directly
                // intentionally.  Forces use of this method getting new
                // instances.
            };
        credentials.add(c);
        return c;
    }
    
    /**
     * Add an HtmlForm credential to this domain.
     * 
     * Ensures one credental per realmPattern only.
     * 
     * @param curi A crawl uri.  <code>curi</code> sets context for the 
     * getting of parameters.  If null, we use global context.
     * @param realmPattern Pattern this credential goes up against.
     * @return Returns the created credential.
     */
    public Credential createHtmlFormCredential(CrawlURI curi,
        String realmPattern) throws AttributeNotFoundException,
            ClassNotFoundException, InvalidAttributeValueException {
        return createHtmlFormCredential(curi, realmPattern, null, null, null,
            null);
    }

    /**
     * Add an HtmlForm credential to this domain.
     * 
     * Ensures one credental per realmPattern only.
     * 
     * @param curi A crawl uri.  <code>curi</code> sets context for the 
     * getting of parameters.  If null, we use global context.
     * @param realmPattern Pattern this credential goes up against.
     * @param loginURI URI of login page that has the html form we're to submit
     * to.
     * @param method POST or GET.
     * @param formItems MapType of form Items.
     * @param cookie Cookie to log before and after on.
     * @return Returns the created credential.
     */
    public Credential createHtmlFormCredential(CrawlURI curi,
        String realmPattern, String loginURI, String method, MapType formItems,
        String cookie) throws AttributeNotFoundException,
            ClassNotFoundException, InvalidAttributeValueException {
        
        CredentialListType credentials =
            (CredentialListType)getCredentials(curi);
        List list = credentials.sublist(curi,
            HtmlFormCredential.class.getName());
        Credential c = null;
        for (Iterator i = list.iterator(); i.hasNext();) {
            c = (Credential)i.next();
            if (c.getName().equals(realmPattern)) {
                throw new IllegalArgumentException("Already a credential for" +
                    " this realm.");
            }
        }
        
        c = new HtmlFormCredential(realmPattern, loginURI, method, cookie) {
            // Anonymous subclass. Parent cannot be instantiated directly
            // intentionally.  Forces use of this method getting new
            // instances.
            };
        credentials.add(c);
        if (formItems != null) {
            c.addElement(getSettingsFromObject(curi), formItems);
        }
        return c;
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
     * A list of credentials.
     * 
     * @author stack
     * @version $Revision$, $Date$
     */
    private class CredentialListType extends ListType
    {
        public static final String ATTR_NAME = "credentials";
        
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
        public List sublist(CrawlURI curi, String type)
            throws AttributeNotFoundException, ClassNotFoundException {
            CredentialListType credentials =
                (CredentialListType)getCredentials(curi);
            List result = new ArrayList();
            Class classType = Class.forName(type);
            for (Iterator i = credentials.iterator(); i.hasNext();) {
                Credential c = (Credential)i.next();
                if (!classType.isInstance(c)) {
                    continue;
                }
                result.add(c);
            }
            return result;
        }
    }
}
