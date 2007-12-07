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
package org.archive.modules.credential;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.archive.modules.credential.Credential;
import org.archive.modules.credential.CredentialStore;
import org.archive.settings.MemorySheetManager;
import org.archive.settings.SettingsMap;
import org.archive.settings.Sheet;
import org.archive.settings.SheetBundle;
import org.archive.settings.SingleSheet;
import org.archive.state.Key;
import org.archive.state.ModuleTestBase;



/**
 * Test add, edit, delete from credential store.
 *
 * @author stack
 * @version $Revision$, $Date$
 */
public class CredentialStoreTest extends ModuleTestBase {

    protected static Logger logger =
        Logger.getLogger("org.archive.crawler.datamodel.CredentialTest");

    @Override
    protected Class getModuleClass() {
        return CredentialStore.class;
    }

    @Override
    protected Object makeModule() throws Exception {
        return new CredentialStore();
    }

    final public void testCredentials() throws Exception {
        MemorySheetManager manager = new MemorySheetManager();
        SingleSheet global = manager.getGlobalSheet();
        SingleSheet domain = manager.addSingleSheet("domain");
        SingleSheet hostSingle = manager.addSingleSheet("hostSingle");
        SheetBundle host = manager.addSheetBundle("host", 
                Arrays.asList(new Sheet[] { hostSingle, domain }));
        
        CredentialStore store = new CredentialStore();
        global.set(store, CredentialStore.CREDENTIALS, 
                new SettingsMap<Credential>(global, Credential.class));
        domain.set(store, CredentialStore.CREDENTIALS, 
                new SettingsMap<Credential>(domain, Credential.class));
        hostSingle.set(store, CredentialStore.CREDENTIALS, 
                new SettingsMap<Credential>(hostSingle, Credential.class));
        
        writeCrendentials(store, global, "global");
        writeCrendentials(store, domain, "domain");
        writeCrendentials(store, hostSingle, "host");
        List types = CredentialStore.getCredentialTypes();
        
        
        
        List<String> globalNames = checkContextNames(store, global, types.size());
        checkContextNames(store, domain, types.size() * 2); // should be global + domain
        checkContextNames(store, host, types.size() * 3); // should be global + domain + host

        Key<Map<String,Credential>> k = CredentialStore.CREDENTIALS;
        Map<String,Credential> defMap = global.resolveEditableMap(store, k);
        for (String name: globalNames) {
            defMap.remove(name);
        }
        // Should be only host and domain objects at deepest scope.
        checkContextNames(store, host, types.size() * 2);
    }

    private List<String> checkContextNames(CredentialStore store, 
            Sheet sheet, int size) {
        Map<String,Credential> map = sheet.get(store, 
                CredentialStore.CREDENTIALS);

        List<String> names = new ArrayList<String>(size);
        names.addAll(map.keySet());
        assertEquals("Not enough names", size, map.size());
        return names;
    }

    private void writeCrendentials(CredentialStore store, SingleSheet context,
                String prefix) throws Exception {
        Map<String,Credential> map = new SettingsMap<Credential>(context, 
                Credential.class);
        context.set(store, CredentialStore.CREDENTIALS, map);
        
        List<Class> types = CredentialStore.getCredentialTypes();
        for (Class cl: types) {
            Credential c = (Credential)cl.newInstance();
            map.put(prefix + "." + cl.getName(), c);
            assertNotNull("Failed create of " + cl, c);
            logger.info("Created " + cl.getName());
        }
    }
}
