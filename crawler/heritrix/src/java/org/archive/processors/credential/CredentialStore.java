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
package org.archive.processors.credential;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.archive.crawler.datamodel.CrawlURI;
import org.archive.processors.ProcessorURI;
import org.archive.state.Key;
import org.archive.state.KeyManager;
import org.archive.state.Module;
import org.archive.state.StateProvider;


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
public class CredentialStore implements Module, Serializable {

    private static final long serialVersionUID = 3L;

    private static Logger logger = Logger.getLogger(
        "org.archive.crawler.datamodel.CredentialStore");


    /**
     * Credentials used by heritrix authenticating. See
     * http://crawler.archive.org/proposals/auth/ for background.
     * 
     * @see http://crawler.archive.org/proposals/auth/
     */
    public static final Key<Map<String,Credential>> CREDENTIALS
     = Key.makeMap(Credential.class);

    /**
     * List of possible credential types as a List.
     *
     * This types are inner classes of this credential type so they cannot
     * be created without their being associated with a credential list.
     */
    private static final List<Class> credentialTypes;
    // Initialize the credentialType data member.
    static {
        // Array of all known credential types.
        Class [] tmp = {HtmlFormCredential.class, Rfc2617Credential.class};
        credentialTypes = Collections.unmodifiableList(Arrays.asList(tmp));
        KeyManager.addKeys(CredentialStore.class);
    }

    /**
     * Constructor.
     */
    public CredentialStore() {
    }

    /**
     * @return Unmodifable list of credential types.
     */
    public static List<Class> getCredentialTypes() {
        return CredentialStore.credentialTypes;
    }


    /**
     * @param context Pass a CrawlURI, CrawlerSettings or UURI.  Used to set
     * context.  If null, we use global context.
     * @return An iterator or null.
     */
    public Collection<Credential> getAll(StateProvider context) {
        Map<String,Credential> map = context.get(this, CREDENTIALS);
        return map.values();
    }

    /**
     * @param context Pass a CrawlURI, CrawlerSettings or UURI.  Used to set
     * context.  If null, we use global context.
     * @param name Name to give the manufactured credential.  Should be unique
     * else the add of the credential to the list of credentials will fail.
     * @return Returns <code>name</code>'d credential.
     * @throws AttributeNotFoundException
     * @throws MBeanException
     * @throws ReflectionException
     */
    public Credential get(StateProvider context, String name) {
        return context.get(this, CREDENTIALS).get(name);
    }


    /**
     * Return set made up of all credentials of the passed
     * <code>type</code>.
     *
     * @param context Pass a CrawlURI or a CrawlerSettings.  Used to set
     * context.  If null, we use global context.
     * @param type Type of the list to return.  Type is some superclass of
     * credentials.
     * @return Unmodifable sublist of all elements of passed type.
     */
    public Set subset(CrawlURI context, Class type) {
        return subset(context, type, null);
    }

    /**
     * Return set made up of all credentials of the passed
     * <code>type</code>.
     *
     * @param context Pass a CrawlURI or a CrawlerSettings.  Used to set
     * context.  If null, we use global context.
     * @param type Type of the list to return.  Type is some superclass of
     * credentials.
     * @param rootUri RootUri to match.  May be null.  In this case we return
     * all.  Currently we expect the CrawlServer name to equate to root Uri.
     * Its not.  Currently it doesn't distingush between servers of same name
     * but different ports (e.g. http and https).
     * @return Unmodifable sublist of all elements of passed type.
     */
    public Set<Credential> subset(ProcessorURI context, Class type, String rootUri) {
        Set<Credential> result = null;
        for (Credential c: getAll(context)) {
            if (!type.isInstance(c)) {
                continue;
            }
            if (rootUri != null) {
                String cd = c.getCredentialDomain(context);
                if (cd == null) {
                    continue;
                }
                if (!rootUri.equalsIgnoreCase(cd)) {
                    continue;
                }
            }
            if (result == null) {
                result = new HashSet<Credential>();
            }
            result.add(c);
        }
        return result;
    }
    
    
    public Credential getCredential(ProcessorURI curi, CredentialAvatar ca) {
        Credential result = null;

        Collection<Credential> all = getAll(curi);
        if (all == null) {
            logger.severe("Have CredentialAvatar " + toString() +
                " but no collection: " + curi);
            return result;
        }

        for (Credential c: all) {
            if (!ca.getType().isInstance(c)) {
                continue;
            }
            String credKey = c.getKey(curi);
            if (credKey != null && credKey.equals(ca.getKey())) {
                result = c;
                break;
            }
        }

        if (result == null) {
            logger.severe("Have CredentialAvatar " + toString() +
                " but no corresponding credential: " + curi);
        }

        return result;

    }
}
