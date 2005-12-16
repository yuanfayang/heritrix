/* InsufficientCrawlingResourcesException
 * 
 * $Id$
 * 
 * Created on Dec 15, 2005
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
package org.archive.hcc.client;
public class InsufficientCrawlingResourcesException
        extends
            ClusterException {

    /**
     * 
     */
    public InsufficientCrawlingResourcesException() {
        super();
        // TODO Auto-generated constructor stub
    }

    /**
     * @param message
     */
    public InsufficientCrawlingResourcesException(String message) {
        super(message);
        // TODO Auto-generated constructor stub
    }

    /**
     * @param message
     * @param cause
     */
    public InsufficientCrawlingResourcesException(
            String message,
            Throwable cause) {
        super(message, cause);
        // TODO Auto-generated constructor stub
    }

    /**
     * @param cause
     */
    public InsufficientCrawlingResourcesException(Throwable cause) {
        super(cause);
        // TODO Auto-generated constructor stub
    }

}
