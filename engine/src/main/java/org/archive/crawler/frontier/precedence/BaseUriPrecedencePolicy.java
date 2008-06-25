/* BaseQueuePrecedencePolicy
*
* $Id: CostAssignmentPolicy.java 4981 2007-03-12 07:06:01Z paul_jack $
*
* Created on Nov 20, 2007
*
* Copyright (C) 2007 Internet Archive.
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
package org.archive.crawler.frontier.precedence;

import org.archive.crawler.datamodel.CrawlURI;
import org.archive.spring.HasKeyedProperties;
import org.archive.spring.KeyedProperties;

/**
 * UriPrecedencePolicy which assigns URIs a set value (perhaps a overridden
 * for different URIs). 
 */
public class BaseUriPrecedencePolicy extends UriPrecedencePolicy 
implements HasKeyedProperties {
    private static final long serialVersionUID = -8247330811715982746L;
    
    protected KeyedProperties kp = new KeyedProperties();
    public KeyedProperties getKeyedProperties() {
        return kp;
    }
    
    /** constant precedence to assign; default is 1 */
    {
        setBasePrecedence(1);
    }
    public int getBasePrecedence() {
        return (Integer) kp.get("basePrecedence");
    }
    public void setBasePrecedence(int precedence) {
        kp.put("basePrecedence", precedence);
    }
    
    /* (non-Javadoc)
     * @see org.archive.crawler.frontier.precedence.UriPrecedencePolicy#uriScheduled(org.archive.crawler.datamodel.CrawlURI)
     */
    @Override
    public void uriScheduled(CrawlURI curi) {
        curi.setPrecedence(calculatePrecedence(curi)); 
    }

    /**
     * Calculate the precedence value for the given URI. 
     * @param curi CrawlURI to evaluate
     * @return int precedence for URI
     */
    protected int calculatePrecedence(CrawlURI curi) {
        return getBasePrecedence();
    }
}
