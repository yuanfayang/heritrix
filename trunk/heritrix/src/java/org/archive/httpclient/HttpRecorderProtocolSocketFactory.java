/* HttpRecorderProtocolSocketFactory
 * 
 * Created on Feb 18, 2004
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
package org.archive.httpclient;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.net.SocketFactory;

import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;


/**
 * Implementation of the commons-httpclient ProtocolSocketFactory so we 
 * can insert our own HttpRecorderSocketFactory. 
 *
 * <p>Should be only one instance of this factory per JVM.
 * 
 * @author stack
 * @version $Id$
 */
public class HttpRecorderProtocolSocketFactory
    implements ProtocolSocketFactory
{   
    private static SocketFactory factory =
        HttpRecorderSocketFactory.getDefault();
 
    /**
     * Constructor.
     */
    public HttpRecorderProtocolSocketFactory()
    {
        super();
    }

    public Socket createSocket(String host, int port, InetAddress clientHost,
            int clientPort)
        throws IOException, UnknownHostException
    {
        return HttpRecorderProtocolSocketFactory.factory.
                createSocket(host, port, clientHost, clientPort);
    }

    public Socket createSocket(String host, int port)
        throws IOException, UnknownHostException
    {
        return HttpRecorderProtocolSocketFactory.factory.
                createSocket(host, port);
    }
}
