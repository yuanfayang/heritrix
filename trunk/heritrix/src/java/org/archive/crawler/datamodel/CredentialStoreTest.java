/* CredentialStoreTest
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

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.logging.Logger;

import javax.management.AttributeNotFoundException;
import javax.management.InvalidAttributeValueException;

import org.archive.crawler.datamodel.credential.Credential;
import org.archive.crawler.datamodel.credential.CredentialDomain;
import org.archive.crawler.datamodel.settings.ListType;
import org.archive.crawler.datamodel.settings.XMLSettingsHandler;
import org.archive.util.TmpDirTestCase;

/**
 * Test add, edit, delete from credential store.
 * 
 * @author stack
 * @version $Revision$, $Date$
 */
public class CredentialStoreTest extends TmpDirTestCase {
    
    protected static Logger logger =
        Logger.getLogger("org.archive.crawler.datamodel.CredentialStoreTest");
    
    private static final String TMP_FILE_PREFIX = "CREDENTIALS_";
    private XMLSettingsHandler settingsHandler = null;
    private CredentialStore store = null;
    
    protected void setUp() throws Exception {
        super.setUp();
        File orderFile = new File(getTmpDir(), TMP_FILE_PREFIX + "order.xml");
        this.settingsHandler = new XMLSettingsHandler(orderFile);
        this.settingsHandler.initialize();
        this.store = (CredentialStore)this.settingsHandler.
            getModule(CredentialStore.ATTR_NAME);
        assertTrue("Store is null", this.store != null);
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        cleanUpOldFiles(TMP_FILE_PREFIX);
    }
    
    final public void testDomainCredential()
        throws AttributeNotFoundException, InvalidAttributeValueException {
        
        final String DOMAIN1 = "one.two.three";
        final String DOMAIN2 = "four.five.six";
        assertFalse("Has domain1.", 
            this.store.hasCredentialDomain(DOMAIN1, null));
        assertFalse("Has domain2.", 
            this.store.hasCredentialDomain(DOMAIN2, null));
        CredentialDomain d1 =
            this.store.createCredentialDomain(DOMAIN1, null);
        assertTrue("Hasn't domain1.", 
            this.store.hasCredentialDomain(DOMAIN1, null));
        CredentialDomain d2 =
            this.store.createCredentialDomain(DOMAIN2, null);
        assertTrue("Hasn't domain2.", 
            this.store.hasCredentialDomain(DOMAIN2, null));       
        CredentialDomain d1get = this.store.getCredentialDomain(DOMAIN1, null);
        assertNotNull("Domain1 is null", d1get);
        assertEquals("Domains1 not equal", d1.getName(), d1get.getName());
        assertTrue("Domain1 name is wrong", d1get.getName().equals(DOMAIN1));
        CredentialDomain d2get = this.store.getCredentialDomain(DOMAIN2, null);
        assertNotNull("Domain2 is null", d2get);
        assertEquals("Domains2 not equal", d2.getName(), d2get.getName());
        assertTrue("Domain2 name is wrong", d2get.getName().equals(DOMAIN2));
        this.store.deleteCredentialDomain(DOMAIN1, null);
        assertFalse("Has domain1. Wasn't removed.", 
           this.store.hasCredentialDomain(DOMAIN1, null));
        this.store.deleteCredentialDomain(DOMAIN2, null);
        assertFalse("Has domain2. Wasn't removed.", 
           this.store.hasCredentialDomain(DOMAIN2, null));
        assertFalse("Has domain1 end.", 
            this.store.hasCredentialDomain(DOMAIN1, null));
        assertFalse("Has domain2 end.", 
                this.store.hasCredentialDomain(DOMAIN2, null));
        
    }
    
    final public void testCredentials()
        throws InvalidAttributeValueException, AttributeNotFoundException,
            IllegalArgumentException, InstantiationException,
            IllegalAccessException, InvocationTargetException {
        final String DOMAIN = "one.two.three";
        CredentialDomain d = this.store.createCredentialDomain(DOMAIN, null);
        List types = d.getCredentialTypes();
        List credentials = new ArrayList();
        assertTrue("Empty credentials", credentials.size() == 0);
        for (Iterator i = types.iterator(); i.hasNext();) {
            Class cl = (Class)i.next();
            Credential c = d.createCredential(cl);
            assertNotNull("Failed create of " + cl, c);
            logger.info("Created " + c.getName());
            credentials.add(c);
        }
        assertTrue("Types not equal to credentials.",
            credentials.size() == types.size());
        for (Iterator i = credentials.iterator(); i.hasNext();) {
            d.addCredential(null, (Credential)i.next());
        }
        ListType all = d.getCredentials(null);
        assertTrue("Types not equal to all.",
            credentials.size() == all.size());
        // Should now be able to find at least one credential per type.
        for (Iterator i = types.iterator(); i.hasNext();) {
            Class cl = (Class)i.next();
            assertTrue("Failed to get credential for type " + cl,
                d.getCredentials(null, (Class)i.next()).size() > 0);
        }
        assertTrue("Creds are empty", all.size() > 0);
        logger.info("Size of all " + all.size());
        for (ListIterator i = all.listIterator(); i.hasNext();) {
            i.next();
            i.remove();
        }
        all = d.getCredentials(null);
        logger.info("Size of all " + all.size());
        assertTrue("Creds not empty", all.size() == 0);
    }
    
}
