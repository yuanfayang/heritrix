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
package org.archive.modules.credential;


import java.util.Iterator;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.archive.modules.ProcessorURI;

/**
 * A Basic/Digest auth RFC2617 credential.
 *
 * @author stack
 * @version $Revision$, $Date$
 */
public class Rfc2617Credential extends Credential {

    private static final long serialVersionUID = 3L;

    private static Logger logger =
        Logger.getLogger(Rfc2617Credential.class.getName());


    /** Basic/Digest Auth realm. */
    String realm = "Realm";
    public String getRealm() {
        return this.realm;
    }
    public void setRealm(String realm) {
        this.realm = realm;
    }

    /** Login. */
    String login = "login";
    public String getLogin() {
        return this.login;
    }
    public void setLogin(String login) {
        this.login = login;
    }

    /** Password. */
    String password = "password";
    public String getPassword() {
        return this.password;
    }
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Constructor.
     */
    public Rfc2617Credential() {
    }

    public boolean isPrerequisite(ProcessorURI curi) {
        // Return false.  Later when we implement preemptive
        // rfc2617, this will change.
        return false;
    }

    public boolean hasPrerequisite(ProcessorURI curi) {
        // Return false.  Later when we implement preemptive
        // rfc2617, this will change.
        return false;
    }

    public String getPrerequisite(ProcessorURI curi) {
        // Return null.  Later when we implement preemptive
        // rfc2617, this will change.
        return null;
    }

    public String getKey() {
        return getRealm();
    }

    public boolean isEveryTime() {
        return true;
    }

    public boolean populate(ProcessorURI curi, HttpClient http, HttpMethod method,
            String payload) {
        boolean result = false;
        String authRealm = payload;
        if (authRealm == null) {
            logger.severe("No authscheme though creds: " + curi);
            return result;
        }

        // Always add the credential to HttpState. Doing this because no way of
        // removing the credential once added AND there is a bug in the
        // credentials management system in that it always sets URI root to
        // null: it means the key used to find a credential is NOT realm + root
        // URI but just the realm. Unless I set it everytime, there is
        // possibility that as this thread progresses, it might come across a
        // realm already loaded but the login and password are from another
        // server. We'll get a failed authentication that'd be difficult to
        // explain.
        //
        // Have to make a UsernamePasswordCredentials. The httpclient auth code
        // does an instanceof down in its guts.
        UsernamePasswordCredentials upc = null;
        try {
        	upc = new UsernamePasswordCredentials(getLogin(),
        	    getPassword());
        	http.getState().setCredentials(new AuthScope(curi.getUURI().getHost(),
        	    curi.getUURI().getPort(), authRealm), upc);
        	logger.fine("Credentials for realm " + authRealm +
        	    " for CrawlURI " + curi.toString() + " added to request: " +
				result);
        	result = true;
        } catch (URIException e) {
        	logger.severe("Failed to parse host from " + curi + ": " +
        			e.getMessage());
        }
        
        return result;
    }

    public boolean isPost() {
        // Return false.  This credential type doesn't care whether posted or
        // get'd.
        return false;
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
            String realm, ProcessorURI context) {

        Rfc2617Credential result = null;
        if (rfc2617Credentials == null || rfc2617Credentials.size() <= 0) {
            return result;
        }
        if (rfc2617Credentials != null && rfc2617Credentials.size() > 0) {
            for (Iterator i = rfc2617Credentials.iterator(); i.hasNext();) {
                Rfc2617Credential c = (Rfc2617Credential)i.next();
                    if (c.getRealm().equals(realm)) {
                        result = c;
                        break;
                    }
            }
        }
        return result;
    }
}
