/* AcceptRule
*
* $Id$
*
* Created on Apr 1, 2005
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

import javax.management.Attribute;
import javax.management.AttributeNotFoundException;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanException;
import javax.management.ReflectionException;

import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.settings.SimpleType;



/**
 * Rule applies configured decision (typically REJECT) to any CrawlURIs 
 * whose total number of hops (length of the hopsPath string) is over a 
 * threshold.
 *
 * @author gojomo
 */
public class TooManyHopsDecideRule extends PredicatedDecideRule {

    private static final String ATTR_MAX_HOPS = "max-hops";
    private static final Integer DEFAULT_MAX_HOPS = new Integer(20);

    /**
     * Usual constructor. 
     * @param name
     */
    public TooManyHopsDecideRule(String name) {
        super(name);
        setDescription("TooManyHopsDecideRule: Applies the configured" +
                "decision to URIs discovered after too many hops (followed " +
                "links of any type) from seed.");
        addElementToDefinition(new SimpleType(ATTR_MAX_HOPS, "Max path" +
                " depth for which this filter will match", DEFAULT_MAX_HOPS));
        // make default REJECT
        try {
            setAttribute(new Attribute(ConfiguredDecideRule.ATTR_DECISION, REJECT));
        } catch (AttributeNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InvalidAttributeValueException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (MBeanException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ReflectionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Evaluate whether given object is over the threshold number of
     * hops.
     * 
     * @param object
     * @return
     */
    protected boolean evaluate(Object object) {
        try {
            CrawlURI curi = (CrawlURI)object;
            return curi.getPathFromSeed().length() > getThresholdHops(object);
        } catch (ClassCastException e) {
            // if not CrawlURI, always disregard
            return false; 
        }
    }

    /**
     * @return hops cutoff threshold
     */
    private int getThresholdHops(Object obj) {
        return ((Integer)getUncheckedAttribute(obj,ATTR_MAX_HOPS)).intValue();
    }

}
