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
import java.util.List;
import java.util.logging.Logger;

import javax.management.AttributeNotFoundException;
import javax.management.InvalidAttributeValueException;

import org.archive.crawler.datamodel.credential.*;
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
            ClassNotFoundException {
        final String DOMAIN = "one.two.three";
        final String REALM = "allover";
        final String REALM_PATTERN = DOMAIN + "/login/realm";
        CredentialDomain d = this.store.createCredentialDomain(DOMAIN, null);
        Credential rfc2617 = d.createRfc2617Credential(null, REALM);
        List list = d.getRfc2617Credentials(null);
        assertTrue("No rfc2617", list.size() == 1);
        assertTrue("Not equal realms.", rfc2617.getName().
            equals(((Credential)list.get(0)).getName()));
        Credential htmlForm = d.createHtmlFormCredential(null, REALM_PATTERN);
        list = d.getHtmlFormCredentials(null);
        assertTrue("No htmlform", list.size() == 1);
        assertTrue("Not equal realmpatterns.", htmlForm.getName().
            equals(((Credential)list.get(0)).getName()));
        d.removeCredential(null, rfc2617);
        d.removeCredential(null, htmlForm);
        ListType lt = d.getCredentials(null);
        assertTrue("Not empty", lt.size() == 0);
    }
    
}
