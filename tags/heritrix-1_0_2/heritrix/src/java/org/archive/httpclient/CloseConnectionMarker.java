/* CloseConnectionMarker
 *
 * Created on August 22, 2004
 *
 * Copyright (C) 2003 Internet Archive.
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
package org.archive.httpclient;

/**
 * Market interface to designate HttpMethod instances that support
 * close connection.
 * 
 * @author stack
 * @version $Revision$, $Date$
 */
public interface CloseConnectionMarker {
    /**
     * Force close of the connection.
     * 
     * Use this method when we've read a time or size limit.
     * 
     * <p>This method goes against all the work that httpclient
     * does to hide the connection from clients.
     */
    public void closeConnection();
}
