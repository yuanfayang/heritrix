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
 * WebUIConfig.java
 *
 * Created on Jun 29, 2007
 *
 * $Id:$
 */

package org.archive.crawler;

import java.util.HashSet;
import java.util.Set;

/**
 * @author pjack
 *
 */
public class WebUIConfig {

    
    private Set<String> hosts = new HashSet<String>();
    private int port = 8080;
    private String pathToWAR = null;
    private String uiPassword = null;
    
    public WebUIConfig() {
    }


    public Set<String> getHosts() {
        return hosts;
    }


    public void setHosts(Set<String> hosts) {
        this.hosts = hosts;
    }


    public String getPathToWAR() {
        return pathToWAR;
    }


    public void setPathToWAR(String pathToWAR) {
        this.pathToWAR = pathToWAR;
    }


    public int getPort() {
        return port;
    }


    public void setPort(int port) {
        this.port = port;
    }


    public String getUiPassword() {
        return uiPassword;
    }


    public void setUiPassword(String uiPassword) {
        this.uiPassword = uiPassword;
    }
}
