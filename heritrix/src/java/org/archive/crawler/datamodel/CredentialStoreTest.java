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
import java.util.logging.Logger;

import javax.management.AttributeNotFoundException;
import javax.management.InvalidAttributeValueException;

import org.archive.crawler.datamodel.settings.Credential;
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
    
    final public void testCredentials()
        throws InvalidAttributeValueException, AttributeNotFoundException,
            IllegalArgumentException, InstantiationException,
            IllegalAccessException, InvocationTargetException {
        List types = CredentialStore.getCredentialTypes();
        for (Iterator i = types.iterator(); i.hasNext();) {
            Class cl = (Class)i.next();
            Credential c = this.store.create(null, cl.getName(), cl);
            assertNotNull("Failed create of " + cl, c);
            logger.info("Created " + c.getName());
        }
        List names = new ArrayList(types.size());
        for (Iterator i = this.store.iterator(null); i.hasNext();) {
            names.add(((Credential)i.next()).getName());
        }
        assertTrue("Creds are empty", names.size() > 0);
        assertTrue("Creds and types don't match", names.size() == types.size());
        for (Iterator i = names.iterator(); i.hasNext();) {
            this.store.remove(null, (String)i.next());
        }
        int count = 0;
        for (Iterator i = this.store.iterator(null); i.hasNext(); count++) {
            i.next();
        }
        assertTrue("Creds not empty", count == 0);
    }
}
