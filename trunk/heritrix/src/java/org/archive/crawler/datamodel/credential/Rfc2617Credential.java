/* Rfc2617Credential
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

import org.archive.crawler.datamodel.settings.SimpleType;
import org.archive.crawler.datamodel.settings.Type;


/**
 * A Basic/Digest auth RFC2617 credential.
 * 
 * @author stack
 * @version $Revision$, $Date$
 */
public class Rfc2617Credential extends Credential {

    protected Rfc2617Credential(String realm, boolean preemptive, String login,
        String password)
    {
        super(realm, "Basic/Digest Auth type credential.");
        
        Type t = addElementToDefinition(new SimpleType("preempt",
            "Preemptively offer this credential in advance of 401 challenge.",
            new Boolean(preemptive)));
        t.setOverrideable(true);
        t.setExpertSetting(true);
        
        t = addElementToDefinition(new SimpleType("login", "Login.",
            login));
        t.setOverrideable(true);
        t.setExpertSetting(true);
        
        t = addElementToDefinition(new SimpleType("password", "Password.",
            password));
        t.setOverrideable(true);
        t.setExpertSetting(true);
    }
}
