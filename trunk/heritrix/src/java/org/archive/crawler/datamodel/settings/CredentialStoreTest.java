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
package org.archive.crawler.datamodel.settings;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.management.AttributeNotFoundException;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanException;
import javax.management.ReflectionException;


/**
 * Test add, edit, delete from credential store.
 * 
 * @author stack
 * @version $Revision$, $Date$
 */
public class CredentialStoreTest extends SettingsFrameworkTestCase {

    protected static Logger logger =
        Logger.getLogger("org.archive.crawler.datamodel.CredentialTest");
    
    final public void testCredentials()
        throws InvalidAttributeValueException, IllegalArgumentException, 
        InvocationTargetException, AttributeNotFoundException, MBeanException,
        ReflectionException {
        
        CredentialStore store = (CredentialStore)this.settingsHandler.
            getOrder().getAttribute(CredentialStore.ATTR_NAME);
        List types = CredentialStore.getCredentialTypes();
        for (Iterator i = types.iterator(); i.hasNext();) {
            Class cl = (Class)i.next();
            Credential c = store.create(null, cl.getName(), cl);
            assertNotNull("Failed create of " + cl, c);
            logger.info("Created " + c.getName());
        }
        List names = new ArrayList(types.size());
        for (Iterator i = store.iterator(null); i.hasNext();) {
            names.add(((Credential)i.next()).getName());
        }
        
        // Write out the file so can get a look at it.
        getSettingsHandler().writeSettingsObject(getGlobalSettings());
        
        assertTrue("Creds are empty", names.size() > 0);
        assertTrue("Creds and types don't match", names.size() == types.size());
        for (Iterator i = names.iterator(); i.hasNext();) {
            store.remove(null, (String)i.next());
        }
        int count = 0;
        for (Iterator i = store.iterator(null); i.hasNext(); count++) {
            i.next();
        }
        assertTrue("Creds not empty", count == 0);
    }
}
