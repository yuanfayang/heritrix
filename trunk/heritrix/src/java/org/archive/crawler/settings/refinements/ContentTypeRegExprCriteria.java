/* ContentTypeRegExprCriteria
 * 
 * $Id$
 * 
 * Created on 29.11.2004
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
package org.archive.crawler.settings.refinements;

import org.archive.crawler.datamodel.CrawlURI;
import org.archive.util.TextUtils;

/**
 * A refinement criteria that tests if a URIs mime type matches a regular
 * expression. If no mime type, then URI does not match regardless of the 
 * regular expression. Consequentially, the regular expression '.*' will match
 * all URIs that <i>have</i> a mime type (have been fetched).
 *
 * @author Kristinn Sigurdsson
 */
public class ContentTypeRegExprCriteria implements Criteria {
    private String regexp = "";

    /**
     * Create a new instance of ContentTypeRegExprCriteria.
     */
    public ContentTypeRegExprCriteria() {
        super();
    }

    /**
     * Create a new instance of ContentTypeRegExprCriteria initializing it with
     * a regular expression.
     *
     * @param regexp the regular expression for this criteria.
     */
    public ContentTypeRegExprCriteria(String regexp) {
        setRegexp(regexp);
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.settings.refinements.Criteria#isWithinRefinementBounds(org.archive.crawler.datamodel.UURI, int)
     */
    public boolean isWithinRefinementBounds(CrawlURI uri) {
        String content_type = uri.getContentType();
        // Undefined content type => not within refinement bounds.
        return (content_type == null || regexp == null)?
            false: TextUtils.matches(regexp, content_type);

    }

    /**
     * Get the regular expression to be matched against a URI's mime type.
     *
     * @return Returns the regexp.
     */
    public String getRegexp() {
        return regexp;
    }
    /**
     * Set the regular expression to be matched against a URI's mime type.
     *
     * @param regexp The regexp to set.
     */
    public void setRegexp(String regexp) {
        this.regexp = regexp;
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.settings.refinements.Criteria#getName()
     */
    public String getName() {
        return "Content type regular expression criteria";
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.settings.refinements.Criteria#getDescription()
     */
    public String getDescription() {
        return "Accept URIs whose mime type matchs the following regular " +
                "expression: " + getRegexp();
    }
}
