/* Rfc2617Credential
 * 
 * Created on Apr 7, 2004
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


import java.util.Iterator;
import java.util.Set;
import java.util.logging.Logger;

import javax.management.AttributeNotFoundException;

import org.apache.commons.httpclient.auth.AuthScheme;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.settings.SimpleType;
import org.archive.crawler.settings.Type;



/**
 * A Basic/Digest auth RFC2617 credential.
 * 
 * @author stack
 * @version $Revision$, $Date$
 */
public class Rfc2617Credential extends Credential {
    
    private static Logger logger = Logger.getLogger(
        "org.archive.crawler.datamodel.credential.Rfc2617Credential");

    private static final String ATTR_REALM = "realm";
    private static final String ATTR_LOGIN = "login";
    private static final String ATTR_PASSWORD = "password";
    
    /**
     * Cache an auth scheme here.
     * 
     * Needed when we come back around after a 401 only this time we want
     * to run the authentication.
     */
    private AuthScheme authScheme = null;
    
    /**
     * Constructor.
     * 
     * A constructor that takes name of the credential is required by settings
     * framework.
     * 
     * @param name Name of this credential.
     */
    public Rfc2617Credential(String name) {
        super(name, "Basic/Digest Auth type credential.");
        
        Type t = addElementToDefinition(new SimpleType(ATTR_REALM,
            "Basic/Digest Auth realm.", "Realm"));
        t.setOverrideable(false);
        t.setExpertSetting(true);
        
        t = addElementToDefinition(new SimpleType(ATTR_LOGIN, "Login.",
            "login"));
        t.setOverrideable(false);
        t.setExpertSetting(true);
        
        t = addElementToDefinition(new SimpleType(ATTR_PASSWORD, "Password.",
            "password"));
        t.setOverrideable(false);
        t.setExpertSetting(true);
    }
    
    /**
     * @param context Context to use when searching the realm.
     * @return Realm using set context.
     */
    public String getRealm(CrawlURI context) throws AttributeNotFoundException {
        return (String)getAttribute(ATTR_REALM, context);
    }
    
    /**
     * @param context Context to use when searching the realm.
     * @return login to use doing credential.
     */
    public String getLogin(CrawlURI context)
            throws AttributeNotFoundException {
        return (String)getAttribute(ATTR_LOGIN, context);
    }
    
    /**
     * @param context Context to use when searching the realm.
     * @return Password to use doing credential.
     */
    public String getPassword(CrawlURI context)
            throws AttributeNotFoundException {
        return (String)getAttribute(ATTR_PASSWORD, context);  
    }
    
    /**
     * Convenience method that does look up on passed set using realm for key.
     * 
     * @param rfc2617Credentials Set of Rfc2617 credentials.  If passed set is
     * not pure Rfc2617Credentials then will be ClassCastExceptions.
     * @param realm Realm to find in passed set.
     * @param context Context to use when searching the realm.
     * @return Credential of passed realm name else null.  If more than one 
     * credential w/ passed realm name, and there shouldn't be, we return first
     * found.
     */
    public static Rfc2617Credential getByRealm(Set rfc2617Credentials,
            String realm, CrawlURI context) {
        
        Rfc2617Credential result = null;
        if (rfc2617Credentials != null && rfc2617Credentials.size() > 0) {
            for (Iterator i = rfc2617Credentials.iterator(); i.hasNext();) {
                Rfc2617Credential c = (Rfc2617Credential)i.next();
                try {
                    if (c.getRealm(context).equals(realm)) {
                        result = c;
                        break;
                    }
                } catch (AttributeNotFoundException e) {
                    logger.severe("Failed look up by realm " + realm + " " + e);
                }
            }
        }
        return result;
    }
    
    /**
     * @return Returns the currently cached authScheme.
     */
    public AuthScheme getAuthScheme()
    {
        return this.authScheme;
    }
    
    /**
     * @param authScheme The authScheme to cache.
     */
    public void setAuthScheme(AuthScheme authScheme)
    {
        this.authScheme = authScheme;
    }
}