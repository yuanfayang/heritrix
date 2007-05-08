/* 
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
 *
 * Actions.java
 *
 * Created on May 7, 2007
 *
 * $Id:$
 */

package org.archive.crawler.webui;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 * Static utility methods for action code.
 * 
 * @author pjack
 *
 */
public class Misc {

    
    public static JMXConnector connect(
            String host, 
            int port, 
            String username, 
            String password) throws IOException {     
        String hp = host + ":" + port;
        String s = "service:jmx:rmi://" + hp + "/jndi/rmi://" + hp + "/jmxrmi";
        JMXServiceURL url = new JMXServiceURL(s);
        Map<String,Object> env = new HashMap<String,Object>(1);
        String[] creds = new String[] { username, password };
        env.put(JMXConnector.CREDENTIALS, creds);

        JMXConnector jmxc = JMXConnectorFactory.connect(url, env);
        return jmxc;
    }

    
    
    public static void forward(HttpServletRequest request, 
            HttpServletResponse response, String path) {
        try {
            request.getRequestDispatcher(path).forward(request, response);
        } catch (ServletException e) {
            throw new IllegalStateException(e);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

}
