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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.InvalidParameterException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.MBeanServerFactory;
import javax.management.Notification;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.SimpleType;
import javax.swing.event.EventListenerList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.archive.hcc.util.ClusterControllerNotification;
import org.archive.hcc.util.NotificationDelegatableBase;
import org.archive.hcc.util.NotificationDelegator;
import org.archive.hcc.util.jmx.MBeanServerConnectionFactory;
import org.archive.util.JmxUtils;

/**
 * As the workhorse of the cluster controller client, this class is responsible 
 * for connecting to the local or remote <code>ClusterControllerBean</code> via
 * its <code>DynamicMBean</code> interface. It hides all the details of
 * connecting to the remote MBean, ie invocations and notifications. 
 * @author Daniel Bernstein (dbernstein@archive.org)
 */
class ClusterControllerClientImpl implements ClusterControllerClient{

	private static Log log = LogFactory.getLog(ClusterControllerClientImpl.class);

    private MBeanServerConnection connection;

    private ObjectName name;

    private NotificationDelegator notificationDelegator;

    private EventListenerList listenerList;

    /**
     * Constructs a client running on a remote machine.
     * 
     * @param address
     * @throws InstanceNotFoundException
     * @throws IOException
     */
    ClusterControllerClientImpl(InetSocketAddress address, String username, String password)
            throws InstanceNotFoundException,
            IOException {
        init(MBeanServerConnectionFactory.createConnection(address, username, password));
    }

    private void init(MBeanServerConnection connection)
            throws InstanceNotFoundException {
        try {
            this.connection = connection;
            ObjectName query = new ObjectName(
                    "org.archive.hcc:type=ClusterControllerBean,*");
            Set<ObjectName> names = connection.queryNames(query, null);
            if (names.size() < 1) {
                throw new InstanceNotFoundException(
                        "no mbean found matching query:" + query);
            }
            this.name = names.iterator().next();
            this.notificationDelegator = createNotificationDelegator();
            this.connection.addNotificationListener(
                    this.name,
                    this.notificationDelegator,
                    null,
                    new Object());
            this.listenerList = new EventListenerList();
        } catch (Exception e) {
            log.error(e.toString(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Creates a local instance of the ClusterControllerBean and attaches to it.
     */
    ClusterControllerClientImpl() {
        try {
            init(createMBeanServer());
        } catch (InstanceNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public void addCrawlerLifecycleListener(CrawlerLifecycleListener l) {
        this.listenerList.add(CrawlerLifecycleListener.class, l);
    }

    public void removeCrawlerLifecycleListener(CrawlerLifecycleListener l) {
        this.listenerList.remove(CrawlerLifecycleListener.class, l);
    }

    public void addCrawlJobListener(CurrentCrawlJobListener l) {
        this.listenerList.add(CurrentCrawlJobListener.class, l);
    }

    public void removeCrawlJobListener(CurrentCrawlJobListener l) {
        this.listenerList.remove(CurrentCrawlJobListener.class, l);
    }

    private NotificationDelegator createNotificationDelegator() {
        NotificationDelegator d = new NotificationDelegator();
        d.addDelegatable(new NotificationDelegatableBase() {
            protected boolean delegate(Notification n, Object handbac) {
                if (n
                        .getType()
                        .equals(
                                ClusterControllerNotification.
                                    CRAWL_SERVICE_CREATED_NOTIFICATION.getKey())) {
                    
                    log.debug("crawler service created: " + n.getUserData());
                    handleCrawlServiceCreated((ObjectName) n.getUserData());
                    return true;
                }
                return false;
            }
        });

        d.addDelegatable(new NotificationDelegatableBase() {
            protected boolean delegate(Notification n, Object handbac) {
                if (n
                        .getType()
                        .equals(
                                ClusterControllerNotification.
                                    CRAWL_SERVICE_DESTROYED_NOTIFICATION.getKey())) {
                   
                	log.info("crawler service destroyed: " + n.getUserData());

                    handleCrawlServiceDestroyed((ObjectName) n.getUserData());
                    return true;
                }
                return false;
            }
        });

        d.addDelegatable(new NotificationDelegatableBase() {
            protected boolean delegate(Notification n, Object handbac) {
                if (n.getType().equals(
                    ClusterControllerNotification.
                        CRAWL_SERVICE_JOB_STARTED_NOTIFICATION.getKey())){
                    handleCrawlServiceJobStarted((ObjectName) n.getUserData());
                    return true;
                }
                return false;
            }
        });

        d.addDelegatable(new NotificationDelegatableBase() {
            protected boolean delegate(Notification n, Object handbac) {
                if (n.getType().equals(
                    ClusterControllerNotification.
                        CRAWL_SERVICE_JOB_COMPLETED_NOTIFICATION.getKey())){
                    handleCrawlServiceJobCompleted((ObjectName)n.getUserData());
                    return true;
                }
                return false;
            }
        });

        d.addDelegatable(new NotificationDelegatableBase() {
            protected boolean delegate(Notification n, Object handbac) {
                if (n.getType().equals("progressStatistics")) {
                    ObjectName source = (ObjectName) n.getSource();
                    if (source.getKeyProperty(JmxUtils.TYPE).equals(
                            JmxUtils.JOB)) {
                        Map<String,Object> statistics = toMap(n.getUserData());
                        handleCrawlServiceJobAttributesChanged(
                                source,
                                statistics,
                                statistics);
                        return true;
                    }
                }
                return false;
            }
        });

        d.addDelegatable(new NotificationDelegatableBase() {
            protected boolean delegate(Notification n, Object handbac) {
                if (n.getType().equals("crawlResuming")) {
                    ObjectName source = (ObjectName) n.getSource();
                    try {
                        fireCrawlJobResumed(new CurrentCrawlJobImpl(
                                source,
                                findCrawlJobParentInternal(
                                        JmxUtils.getUid(source),
                                        JmxUtils.extractAddress(source)),
                                connection));
                    } catch (ClusterException e) {
                        log.warn(e.toString(), e);
                    }
                    return true;
                }
                return false;
            }
        });

        d.addDelegatable(new NotificationDelegatableBase() {
            protected boolean delegate(Notification n, Object handbac) {
                if (n.getType().equals("crawlPaused")) {
                    ObjectName source = (ObjectName) n.getSource();
                    try {
                        fireCrawlJobPaused(new CurrentCrawlJobImpl(
                                source,
                                findCrawlJobParentInternal(
                                        JmxUtils.getUid(source),
                                        JmxUtils.extractAddress(source)),
                                connection));
                        return true;
                    } catch (ClusterException e) {
                        log.warn(e.toString(), e);
                    }
                }
                return false;
            }
        });

        
        d.addDelegatable(new NotificationDelegatableBase() {
            protected boolean delegate(Notification n, Object handbac) {
                if (n.getType().equals("crawlEnding")) {
                    ObjectName source = (ObjectName) n.getSource();
                    try {
                        fireCrawlJobStopping(createCurrentCrawlJob(source, connection));
                        return true;
                    } catch (ClusterException e) {
                        log.warn(e.toString(), e);
                    }
                }
                return false;
            }
        });
        return d;
    }
    
    private CurrentCrawlJobImpl createCurrentCrawlJob(
                                            ObjectName job, 
                                            MBeanServerConnection connection) 
                                            throws ClusterException {
        return new CurrentCrawlJobImpl(
                job,
                findCrawlJobParentInternal(
                        JmxUtils.getUid(job),
                        org.archive.hcc.util.JmxUtils.extractRemoteAddress(job)),
                        connection);
    }

	@SuppressWarnings("unchecked")
	private static Map<String,Object> toMap(Object object) {
        if (object instanceof Map<?,?>) {
			return (Map<String,Object>) object;
        } else if (object instanceof CompositeData) {
            CompositeData cd = (CompositeData) object;
            String[] keys = new String[] { "deepestUri", "downloadedUriCount",
                    "queuedUriCount", "docsPerSecond", "freeMemory",
                    "downloadFailures", "totalKBPerSec", "totalMemory",
                    "currentKBPerSec", "currentDocsPerSecond", "busyThreads",
                    "averageDepth", "discoveredUriCount", "congestionRatio","totalProcessedBytes" };
            Map<String,Object> map = new HashMap<String,Object>();
            
            Object[] values = cd.getAll(keys);
            for (int i = 0; i < keys.length; i++) {
                map.put(keys[i], values[i]);
            }

            return map;
        } else {
            throw new InvalidParameterException("unable to convert to map: "
                    + object.getClass());
        }
    }

    private void handleCrawlServiceJobAttributesChanged(
            ObjectName source,
            Map<String, Object> oldValue,
            Map<String, Object> newValue) {
        try {
            CurrentCrawlJobImpl cj = createCurrentCrawlJob(
                    source,this.connection);
            CurrentCrawlJobListener[] listener = this.listenerList
                    .getListeners(CurrentCrawlJobListener.class);
            for (int i = 0; i < listener.length; i++) {
                listener[i].statisticsChanged(cj, newValue);
            }
        } catch (ClusterException e) {
            log.warn(e.toString(), e);
        }
    }

    private void handleCrawlServiceCreated(ObjectName crawlerName) {
            Crawler c = new CrawlerImpl(crawlerName, this.connection);
            fireCrawlerCreated(c);
    }

    private void handleCrawlServiceDestroyed(ObjectName crawlerName) {
        Crawler c = new CrawlerImpl(crawlerName, this.connection);
        fireCrawlerDestroyed(c);
    }

    private void handleCrawlServiceJobStarted(ObjectName crawlJob) {
        try {
            CurrentCrawlJob job = createCurrentCrawlJob(
                                    crawlJob,this.connection);
            fireCrawlJobStarted(job);
        } catch (ClusterException e) {
            log.warn(e.toString(), e);
        }
    }
    
    

    private void handleCrawlServiceJobCompleted(ObjectName crawlJob) {
        try {
            CurrentCrawlJob job = createCurrentCrawlJob(
                    crawlJob,this.connection);
            fireCrawlJobCompleted(job);

        } catch (ClusterException e) {
            log.warn(e.toString(), e);
        }
    }

    private void fireCrawlerCreated(Crawler crawler) {
        CrawlerLifecycleListener[] listener = this.listenerList
                .getListeners(CrawlerLifecycleListener.class);
        for (int i = 0; i < listener.length; i++) {
            listener[i].crawlerCreated(crawler);
        }
    }

    private void fireCrawlerDestroyed(Crawler crawler) {
        CrawlerLifecycleListener[] listener = this.listenerList
                .getListeners(CrawlerLifecycleListener.class);
        for (int i = 0; i < listener.length; i++) {
            listener[i].crawlerDestroyed(crawler);
        }
    }

    private void fireCrawlJobStarted(CurrentCrawlJob job) {
        CurrentCrawlJobListener[] listener = this.listenerList
                .getListeners(CurrentCrawlJobListener.class);
        for (int i = 0; i < listener.length; i++) {
            listener[i].crawlJobStarted(job);
        }
    }

    private void fireCrawlJobPaused(CurrentCrawlJob job) {
        CurrentCrawlJobListener[] listener = this.listenerList
                .getListeners(CurrentCrawlJobListener.class);
        for (int i = 0; i < listener.length; i++) {
            listener[i].crawlJobPaused(job);
        }
    }
    
    private void fireCrawlJobStopping(CurrentCrawlJob job) {
        CurrentCrawlJobListener[] listener = this.listenerList
                .getListeners(CurrentCrawlJobListener.class);
        for (int i = 0; i < listener.length; i++) {
            listener[i].crawlJobStopping(job);
        }
    }

    private void fireCrawlJobResumed(CurrentCrawlJob job) {
        CurrentCrawlJobListener[] listener = this.listenerList
                .getListeners(CurrentCrawlJobListener.class);
        for (int i = 0; i < listener.length; i++) {
            listener[i].crawlJobResumed(job);
        }
    }

    private void fireCrawlJobCompleted(CurrentCrawlJob job) {
        CurrentCrawlJobListener[] listener = this.listenerList
                .getListeners(CurrentCrawlJobListener.class);
        CompletedCrawlJobImpl cj = 
            new CompletedCrawlJobImpl(
                    job.getUid(), 
                    job.getJobName(),
                    (CrawlerImpl)job.getMother(), 
                    this.connection);

        for (int i = 0; i < listener.length; i++) {
            listener[i].crawlJobCompleted(cj);
        }
    }
    

    public void destroyAllCrawlers() throws ClusterException{
        try {
        	this.connection.invoke(
                    this.name,
                    "destroyAllCrawlers",
                    new Object[] {},
                    new String[] {});
        } catch (Exception e) {
            log.warn(e.toString(), e);
            throw new ClusterException(e);
        }
    }

    public boolean pauseAllJobs() throws ClusterException {
    	try {
            return (Boolean) this.connection.invoke(
                    this.name,
                    "pauseAllJobs",
                    new Object[] {},
                    new String[] {});

           
        } catch (Exception e) {
            log.warn(e.toString(), e);
            throw new ClusterException(e);
        }
    }
    
    public boolean resumeAllPausedJobs() throws ClusterException {
    	try {
            return (Boolean) this.connection.invoke(
                    this.name,
                    "resumeAllPausedJobs",
                    new Object[] {},
                    new String[] {});

           
        } catch (Exception e) {
            log.warn(e.toString(), e);
            throw new ClusterException(e);
        }
    }
    
    public Crawler findCrawlJobParent(String uid, InetSocketAddress address) 
        throws ClusterException {
        return findCrawlJobParentInternal(uid, address);
    }
    

    public CrawlerImpl findCrawlJobParentInternal(String uid, InetSocketAddress address)
            throws ClusterException {
        try {
            ObjectName parent = (ObjectName) this.connection.invoke(
                    this.name,
                    "findCrawlServiceJobParent",
                    new Object[] { uid, address.getHostName(),
                            new Integer(address.getPort()) },
                    new String[] { "java.lang.String", "java.lang.String",
                            "java.lang.Integer" });

            if (parent != null) {
                return new CrawlerImpl(parent, this.connection);
            }

            log.warn("no parent (crawl service) found for job address=" + address + " uid=" + uid); 
            return null;
        } catch (Exception e) {
            log.warn(e.toString(), e);
            throw new ClusterException(e);
        }

    }

    private MBeanServer createMBeanServer() {
        List<MBeanServer> servers = MBeanServerFactory.findMBeanServer(null);
        if (servers == null) {
            return null;
        }

        for (MBeanServer server: servers) {
            if (server != null) {
                return server;
            }
        }
        
        return null;
    }

    public Crawler createCrawler() throws 
            InsufficientCrawlingResourcesException, ClusterException {
        try {
            ObjectName crawler = (ObjectName) this.connection.invoke(
                    this.name,
                    "createCrawler",
                    new Object[0],
                    new String[0]);
            return new CrawlerImpl(crawler, this.connection);
        }catch (MBeanException e) {
            if(e.getCause() != null && e.getCause().getCause() != null && 
            		e.getCause().getCause().getMessage().equals("insufficent crawler resources")){
                throw new InsufficientCrawlingResourcesException(e);
            }
            log.warn("exception attempting to create crawler: " + e + ": " + e.getTargetException());
            throw new ClusterException(e);
        } 
        catch (Exception e) {
            log.warn(e.toString(), e);
            throw new ClusterException(e);
        }
    }

    public Collection<Crawler> listCrawlers() throws ClusterException {
        try {
            ObjectName[] crawlers = (ObjectName[]) this.connection.invoke(
                    this.name,
                    "listCrawlers",
                    new Object[0],
                    new String[0]);
            Collection<Crawler> crawlerList = new LinkedList<Crawler>();
            for (int i = 0; i < crawlers.length; i++) {
                crawlerList.add(new CrawlerImpl(crawlers[i], this.connection));
            }

            return crawlerList;
        } catch (Exception e) {
            log.warn(e.toString(), e);
            throw new ClusterException(e);
        }

    }

    public void destroy() {
        try {
            this.connection.invoke(
                    this.name,
                    "destroy",
                    new Object[0],
                    new String[0]);
        } catch (InstanceNotFoundException e) {
            log.warn(e.toString(), e);
        } catch (MBeanException e) {
            log.warn(e.toString(), e);
        } catch (ReflectionException e) {
            log.warn(e.toString(), e);
        } catch (IOException e) {
            log.warn(e.toString(), e);
        }
    }
    
    /* (non-Javadoc)
     * @see org.archive.hcc.client.ClusterControllerClient#getCurrentCrawlJob(org.archive.hcc.client.Crawler)
     */
    public CurrentCrawlJob getCurrentCrawlJob(Crawler crawler) throws ClusterException {
        try {
            ObjectName currentCrawlJob = (ObjectName) this.connection.invoke(
                    this.name,
                    "getCurrentCrawlJob",
                    new Object[]{crawler.getName()},
                    new String[]{SimpleType.OBJECTNAME.getClassName()});
            
            if(currentCrawlJob == null){
                return null;
            }

            return new CurrentCrawlJobImpl(currentCrawlJob, (CrawlerImpl)crawler, this.connection);
        }catch (Exception e) {
            log.warn(e.toString(), e);
            throw new ClusterException(e);
        }
    }
    
    /**
     * Returns the maximum number of instances allowed for this container.
     * If the container does not exist, -1 is returned.
     * @param hostname
     * @param port
     * @return
     */
    public int getMaxInstances(String hostname, int port) throws ClusterException{
        try {
            Integer maxInstances = (Integer) this.connection.invoke(
                    this.name,
                    "getMaxInstances",
                    new Object[]{hostname, new Integer(port)},
                    new String[]{SimpleType.STRING.getClassName(), 
                    				SimpleType.INTEGER.getClassName()});
            return maxInstances;
           
        }catch (Exception e) {
            log.warn(e.toString(), e);
            throw new ClusterException(e);
        }
    }
    
    /**
     * Sets the maximum number of instances that may run on a 
     * specified container defined by a host and port.
     * @param hostname
     * @param port
     * @param maxInstances
     */
    public void setMaxInstances(String hostname, int port, int maxInstances) 
    	throws ClusterException{
        try {
            this.connection.invoke(
                    this.name,
                    "setMaxInstances",
                    new Object[]{
                    			hostname, 
                    			new Integer(port),
                    			new Integer(maxInstances)},
                    new String[]{
                    			SimpleType.STRING.getClassName(),
                    			SimpleType.INTEGER.getClassName(),
                    			SimpleType.INTEGER.getClassName()});
            
           
        }catch (Exception e) {
            log.warn(e.toString(), e);
            throw new ClusterException(e);
        }
    }
}
