/* HtmlFormCredential
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

import org.archive.crawler.settings.MapType;
import org.archive.crawler.settings.SimpleType;
import org.archive.crawler.settings.Type;



/**
 * Credential that holds all needed to do a GET/POST to a HTML form.
 * 
 * @author stack
 * @version $Revision$, $Date$
 */
public class HtmlFormCredential extends Credential {
    
    /**
     * Constructor.
     * 
     * A constructor that takes name of the credential is required by settings
     * framework.
     * 
     * @param name Name of this credential.
     */
    public HtmlFormCredential(String name)
    {
        super(name, "Credential that has all necessary" +
            " for running a POST/GET to an HTML login form.");
        
        Type t = addElementToDefinition(new SimpleType("login-uri",
            "URI of page that contains the HTML login form we're to apply" +
            " these credentials too.", ""));
        t.setOverrideable(false);
        t.setExpertSetting(true);

        final String [] METHODS = {"POST", "GET"};
        t = addElementToDefinition(new SimpleType("http-method",
            "GET or POST", METHODS[0], METHODS));
        t.setOverrideable(false);
        t.setExpertSetting(true);
        
        t = addElementToDefinition(new MapType("form-items", "Form items.",
            String.class));
        t.setOverrideable(false);
        t.setExpertSetting(true);       
    }
}