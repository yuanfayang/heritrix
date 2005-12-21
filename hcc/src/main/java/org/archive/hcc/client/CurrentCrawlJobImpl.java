/* $Id$
 *
 * Created on Dec 12, 2005
 *
 * Copyright (C) 2005 Internet Archive.
 *  
 * This file is part of the Heritrix Cluster Controller (crawler.archive.org).
 *  
 * HCC is free software; you can redistribute it and/or modify
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

import java.net.InetSocketAddress;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import org.archive.util.JmxUtils;


public class CurrentCrawlJobImpl extends CrawlJobBase
implements CurrentCrawlJob {
    
    private ObjectName name;

    public CurrentCrawlJobImpl(
            ObjectName name,
            CrawlerImpl mother,
            MBeanServerConnection connection) {
        super(new Long(JmxUtils.getUid(name)),extractSeedCollectionName(name), mother, connection);
        this.name = name;
    }
    
    static String extractSeedCollectionName(ObjectName on){
        String name = on.getKeyProperty(JmxUtils.NAME);
        return name.substring(0,name.indexOf("-"));
    }

    public ObjectName getName() {
        return this.name;
    }


    public InetSocketAddress getRemoteAddress() {
        // TODO Auto-generated method stub
        return new InetSocketAddress(
                getName().getKeyProperty("remoteHost"),
                new Integer(getName().getKeyProperty("remoteJmxPort")));
    }

    public void pause() {
        try {
            this.connection.invoke(
                    this.name,
                    "pause",
                    new Object[0],
                    new String[0]);
        } catch (Exception e) {
            e.printStackTrace();
            throw new IllegalStateException(e);
        }

    }

    public void resume() {
        try {
            this.connection.invoke(
                    this.name,
                    "resume",
                    new Object[0],
                    new String[0]);
        } catch (Exception e) {
            e.printStackTrace();
            throw new IllegalStateException(e);
        }
    }

    public String getCrawlStatus() {
        try {
            return this.connection.getAttribute(this.name, "Status").toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public Crawler getMother() {
        return this.mother;
    }
    
    public boolean equals(Object o) {
        return ((CurrentCrawlJob) o).getName().equals(this.name);
    }

    public int hashCode() {
        return this.name.hashCode();
    }
}