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

import java.lang.reflect.Proxy;

import javax.management.DynamicMBean;
import javax.management.ObjectName;
/**
 * Represents the relationship between a crawler, it's container and it's transient 
 * child crawl job bean.
 * 
 * @author Daniel Bernstein (dbernstein@archive.org)
 */
class Crawler {
    private DynamicMBean crawlServiceProxy;

    private DynamicMBean crawlJobProxy;

    private Container parent;

    public DynamicMBean getCrawlJobProxy() {
        return crawlJobProxy;
    }

    public DynamicMBean getCrawlServiceProxy() {
        return crawlServiceProxy;
    }

    public Crawler(
            DynamicMBean crawlServiceProxy,
            DynamicMBean crawlJobProxy,
            Container parent) {
        super();
        if (parent == null) {
            throw new NullPointerException("Parent is null");
        }
        this.crawlServiceProxy = crawlServiceProxy;
        this.crawlJobProxy = crawlJobProxy;
        this.parent = parent;
    }

    public void removeFromParent() {
        if (this.parent != null) {
            this.parent.getCrawlers().remove(this);
            this.parent = null;
        }
    }

    public ObjectName getCrawlServiceProxyObjectName() {
        return getProxyObjectName(this.crawlServiceProxy);
    }

    public ObjectName getCrawlServiceRemoteObjectName() {
        return getRemoteObjectName(this.crawlServiceProxy);
    }

    public ObjectName getCrawlJobRemoteObjectName() {
        if (this.crawlJobProxy != null) {
            return getRemoteObjectName(this.crawlJobProxy);
        }
        return null;
    }

    public ObjectName getCrawlJobProxyObjectName() {
        if (this.crawlJobProxy != null) {
            return getProxyObjectName(this.crawlJobProxy);
        }

        return null;
    }

    private ObjectName getProxyObjectName(Object proxy) {
        return ((RemoteMBeanInvocationHandler) Proxy
                .getInvocationHandler(proxy)).getProxyObjectName();
    }

    private ObjectName getRemoteObjectName(Object proxy) {
        return ((RemoteMBeanInvocationHandler) Proxy
                .getInvocationHandler(proxy)).getRemoteObjectName();
    }

    public Container getParent() {
        return parent;
    }

    public void setParent(Container parent) {
        this.parent = parent;
    }

    public void setCrawlJobProxy(DynamicMBean crawlJobProxy) {
        this.crawlJobProxy = crawlJobProxy;
    }
}