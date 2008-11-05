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
package org.archive.hcc;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.LinkedList;

import javax.management.ObjectName;

/**
 * Represents the relationship between a container and the crawlers "container"
 * by it.
 * @author Daniel Bernstein (dbernstein@archive.org)
 */
class Container {
//    private ObjectName name;

    private Collection<Crawler> crawlers = new LinkedList<Crawler>();
    
    private int maxInstances = 1;
    private InetSocketAddress address = null;
    public Container(InetSocketAddress address, Integer maxInstances){
    	this.address = address;
    }
    
//    public Container(ObjectName name, int maxInstances) {
//        super();
//        this.name = name;
//        this.maxInstances = maxInstances;
//    }

    public Collection<Crawler> getCrawlers() {
        return crawlers;
    }

//    public ObjectName getName() {
//        return name;
//    }

    public void addCrawler(Crawler crawler) {
        crawler.removeFromParent();
        this.crawlers.add(crawler);
        crawler.setParent(this);
    }

	public int getMaxInstances() {
		return maxInstances;
	}

	public void setMaxInstances(int maxInstances) {
		this.maxInstances = maxInstances;
	}
	
	public InetSocketAddress getAddress(){
		return this.address;
	}
	
	public boolean equals(Object o){
		if(!(o instanceof Container)){
			return false;
		}
		
		Container c = (Container)o;
		
		return c.address.getHostName().equals(address.getHostName())
				&& ((Integer)c.address.getPort()).equals(address.getPort());
	}
}