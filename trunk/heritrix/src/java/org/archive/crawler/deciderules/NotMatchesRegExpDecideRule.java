/* NotMatchesRegExpDecideRule
*
* $Id$
*
* Created on Apr 4, 2005
*
* Copyright (C) 2005 Internet Archive.
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
package org.archive.crawler.deciderules;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.AttributeNotFoundException;

import org.archive.crawler.datamodel.CandidateURI;
import org.archive.crawler.settings.SimpleType;
import org.archive.util.TextUtils;



/**
 * Rule applies configured decision to any CrawlURIs whose String URI
 * matches the supplied regexp.
 *
 * @author gojomo
 */
public class NotMatchesRegExpDecideRule extends MatchesRegExpDecideRule {
    private static final Logger logger =
        Logger.getLogger(NotMatchesRegExpDecideRule.class.getName());


    /**
     * Usual constructor. 
     * @param name
     */
    public NotMatchesRegExpDecideRule(String name) {
        super(name);
    }

    /**
     * Evaluate whether given object's string version does not match 
     * configured regexp (by reversing the superclass's answer).
     * 
     * @param object
     * @return true if the regexp is not matched
     */
    protected boolean evaluate(Object object) {
        return ! super.evaluate(object);
    }
}
