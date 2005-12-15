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

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.InvalidAttributeValueException;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanRegistration;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.MBeanServerFactory;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import javax.management.NotificationEmitter;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.openmbean.ArrayType;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenMBeanAttributeInfo;
import javax.management.openmbean.OpenMBeanAttributeInfoSupport;
import javax.management.openmbean.OpenMBeanConstructorInfo;
import javax.management.openmbean.OpenMBeanInfoSupport;
import javax.management.openmbean.OpenMBeanOperationInfo;
import javax.management.openmbean.OpenMBeanOperationInfoSupport;
import javax.management.openmbean.OpenMBeanParameterInfo;
import javax.management.openmbean.OpenMBeanParameterInfoSupport;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularData;
import javax.naming.Context;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

import org.archive.hcc.util.ClusterControllerNotification;
import org.archive.hcc.util.NotificationDelegator;
import org.archive.hcc.util.Delegator.DelegatorPolicy;
import org.archive.hcc.util.jmx.MBeanFutureTask;
import org.archive.hcc.util.jmx.MBeanOperation;
import org.archive.hcc.util.jmx.MBeanServerConnectionFactory;
import org.archive.hcc.util.jmx.OpenMBeanInvocationManager;
import org.archive.hcc.util.jmx.RegistrationNotificationHandler;
import org.archive.hcc.util.jmx.SimpleReflectingMBeanOperation;
import org.archive.util.JmxUtils;
import org.archive.util.JndiUtils;

/**
 * As the main workhorse of the package, the <code>ClusterControllerBean</code>
 * provides a unified view of any number of Heritrix instances and all related
 * objects within a JNDI scope.
 * 
 * @author Daniel Bernstein (dbernstein@archive.org)
 */
public class ClusterControllerBean implements
        DynamicMBean,
        NotificationEmitter,
        MBeanRegistration {

    /**
     * The Jndi context
     */
    private Context context;

    /**
     * logger
     */
    private static Logger log = Logger.getLogger(ClusterControllerBean.class
            .getName());

    /**
     * A timer thread that polls the jndi for new containers.
     */
    private Timer jndiPoller;

    /**
     * A single instance of notification listener that simply forwards messages
     * from source beans to listeners.
     */
    private NotificationDelegator remoteNotificationDelegator;

    /**
     * poll period in seconds.
     */
    private static final int JNDI_POLL_PERIOD_IN_SECONDS = 60;

    /**
     * A map of mbean server connections mapped by address. TODO make
     * configuratable
     */
    private Map<InetSocketAddress, MBeanServerConnection> connections =
        new HashMap<InetSocketAddress, MBeanServerConnection>();

    /**
     * A list of remote container references
     */
    private Map<ObjectName, Container> containers;

    private NotificationBroadcasterSupport broadCaster;

    /**
     * Manages and delegates OpenMBean invocations
     */
    private OpenMBeanInvocationManager invocationManager;

    /**
     * Defines the open mbean interface for the bean.
     */
    private MBeanInfo info;

    /**
     * A local mbean server
     */
    private MBeanServer mbeanServer;

    /**
     * The MBean name of the controller.
     */
    private ObjectName name;

    /**
     * A map of the remote crawler handles to the local Crawler dynamic proxy
     * instances.
     */
    private Map<ObjectName, Crawler> remoteNameToCrawlerMap =
        new HashMap<ObjectName, Crawler>();

    /**
     * Watches all traffic (invocations and notifications) moving between the
     * remote Heritrix instances and the heritrix cluster controller.
     */
    private NotificationListener spyListener;

    /**
     * Creates a cluster controller bean. This object uses a two step
     * initialization pattern. Therefore you must call init() before invoking
     * any options.
     */
    public ClusterControllerBean() {
        this.remoteNotificationDelegator = buildRemoteNotificationDelegator();
        jndiPoller = new Timer();
        this.broadCaster = new NotificationBroadcasterSupport();
        this.invocationManager = new OpenMBeanInvocationManager();
        this.info = buildOpenMBeanInfo();
        this.mbeanServer = createMBeanServer();
        this.containers = new HashMap<ObjectName, Container>();
        this.spyListener = new NotificationListener() {
            public void handleNotification(
                    Notification notification,
                    Object handback) {

                if (log.isLoggable(Level.FINER)) {
                    log.finer(">>>>>>>>>spyListener: notification="
                            + notification);
                }

                fireNotificationEvent(notification);
            }
        };
    }

    private static MBeanServer createMBeanServer() {
        MBeanServer result = null;
        List servers = MBeanServerFactory.findMBeanServer(null);
        if (servers == null) {
            return result;
        }
        for (Iterator i = servers.iterator(); i.hasNext();) {
            MBeanServer server = (MBeanServer) i.next();
            if (server == null) {
                continue;
            }
            result = server;
            break;
        }
        return result;
    }

    /**
     * @return Returns the total count of all crawlers within the cluster.
     */
    public Integer getTotalCrawlerCount() {
        return this.remoteNameToCrawlerMap.size();
    }

    private OpenMBeanInfoSupport buildOpenMBeanInfo() {
       return new OpenMBeanInfoSupport(
                ClusterControllerBean.class.getName(),
                "A controller for a pool of Heritrix "
                        + "containers and their contents.",
                buildAttributes(),
                buildConstructors(),
                buildOperations(),
                buildNotifications());
    }

    private OpenMBeanAttributeInfo[] buildAttributes() {
        try {
            return new OpenMBeanAttributeInfo[] {
                new OpenMBeanAttributeInfoSupport(
                    "TotalCrawlerCount",
                    "Total number of crawlers that are "
                            + "currently initialized in the system.",
                    SimpleType.INTEGER,
                    true,
                    false,
                    false,
                    new Integer(0)) };
        } catch (OpenDataException e) {
            throw new RuntimeException(e);
        }
    }

    private OpenMBeanConstructorInfo[] buildConstructors() {
        return new OpenMBeanConstructorInfo[0];
    }

    private boolean uidMatches(TabularData td, String jobUid) {
        if (jobUid == null) {
            throw new NullPointerException("jobUid=" + jobUid);
        }

        if (td == null) {
            return false;
        }

        for (Object o : td.values()) {
            CompositeData cd = (CompositeData) o;
            if (jobUid.equals(cd.get("uid"))) {
                return true;
            }
        }

        return false;
    }

    public ObjectName findCrawlServiceJobParent(
            String jobUid,
            String host,
            Integer port) {

        InetSocketAddress remoteAddress = new InetSocketAddress(host, port);
        Container container = getContainerOn(remoteAddress);

        if (container == null) {
            return null;
        }

        for (Crawler crawler : container.getCrawlers()) {
            DynamicMBean p = crawler.getCrawlServiceProxy();
            try {

                // check if currently running
                ObjectName cjp = crawler.getCrawlJobProxyObjectName();
                ObjectName csp = crawler.getCrawlServiceProxyObjectName();

                if (cjp != null) {
                    if (JmxUtils.getUid(cjp).equals(jobUid)) {
                        return csp;
                    }
                }

                // check completed
                TabularData td = (TabularData) p.invoke(
                        "completedJobs",
                        new Object[0],
                        new String[0]);

                if (uidMatches(td, jobUid)) {
                    return csp;
                }

                // check pending
                td = (TabularData) p.invoke(
                        "pendingJobs",
                        new Object[0],
                        new String[0]);

                if (uidMatches(td, jobUid)) {
                    return csp;
                }
            } catch (MBeanException e) {
                e.printStackTrace();
            } catch (ReflectionException e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    /**
     * Creates a new crawler on the least loaded machine on the cluster.
     * 
     * @return ObjectName of created crawler.
     * @throws MBeanException
     */
    public ObjectName createCrawler() throws MBeanException {
        Container c = resolveLeastLoadedContainer();

        if (c == null) {
            throw new MBeanException(new Exception(
                    "No space available in remote"
                            + " containers for new crawler " + "instances."));
        }

        try {
            return createCrawlerIn(c);
        } catch (Exception e) {
            e.printStackTrace();
            throw new MBeanException(e);
        }
    }

    /**
     * @return Currently returns the least loaded going by a dumb count.
     */
    protected Container resolveLeastLoadedContainer() {

        Container leastLoaded = null;

        for (Container n : this.containers.values()) {
            if (leastLoaded == null) {
                leastLoaded = n;
            }

            if (n.getCrawlers().size() < leastLoaded.getCrawlers().size()) {
                leastLoaded = n;
            }
        }

        return leastLoaded;
    }

    private OpenMBeanOperationInfo[] buildOperations() {
        try {

            addOperation(new SimpleReflectingMBeanOperation(
                    ClusterControllerBean.this,
                    new OpenMBeanOperationInfoSupport(
                            "createCrawler",
                            "creates a new crawler on the cluster",
                            null,
                            SimpleType.OBJECTNAME,
                            OpenMBeanOperationInfoSupport.ACTION_INFO)));

            addOperation(new SimpleReflectingMBeanOperation(
                    ClusterControllerBean.this,
                    new OpenMBeanOperationInfoSupport(
                            "destroy",
                            "Effectively \"detaches\" the bean from the " +
                                "containers, crawl services and jobs that it " +
                                "is managing. The remote objects are not " +
                                "affected. ",
                            null,
                            SimpleType.VOID,
                            OpenMBeanOperationInfoSupport.ACTION)));

            addOperation(new SimpleReflectingMBeanOperation(
                    ClusterControllerBean.this,
                    new OpenMBeanOperationInfoSupport(
                            "listCrawlers",
                            "lists crawlers associated with the cluster",
                            null,
                            new ArrayType(1, SimpleType.OBJECTNAME),
                            OpenMBeanOperationInfoSupport.INFO)));

            addOperation(new SimpleReflectingMBeanOperation(
                    ClusterControllerBean.this,
                    new OpenMBeanOperationInfoSupport(
                            "findCrawlServiceJobParent", "returns the parent" +
                                "name of the specified crawl job.",
                            new OpenMBeanParameterInfo[] {
                                    new OpenMBeanParameterInfoSupport(
                                            "uid",
                                            "The job's uid",
                                            SimpleType.STRING),
                                    new OpenMBeanParameterInfoSupport(
                                            "remoteHost",
                                            "The remote host name",
                                            SimpleType.STRING),
                                    new OpenMBeanParameterInfoSupport(
                                            "remotePort",
                                            "The remote port",
                                            SimpleType.INTEGER) },
                            SimpleType.OBJECTNAME,
                            OpenMBeanOperationInfoSupport.INFO)));
        } catch (OpenDataException e) {
            e.printStackTrace();
        }

        return this.invocationManager.getInfo();
    }

    /**
     * @return Returns a list of all crawler instances within the hcc's jndi
     * scope.
     */
    public ObjectName[] listCrawlers() {
        int size = this.remoteNameToCrawlerMap.keySet().size();
        ObjectName[] crawlers = new ObjectName[size];
        int count = 0;
        for (Crawler c : this.remoteNameToCrawlerMap.values()) {
            crawlers[count] = c.getCrawlServiceProxyObjectName();
            count++;
        }
        return crawlers;
    }

    private void addOperation(MBeanOperation operation) {
        this.invocationManager.addMBeanOperation(operation);
    }

    private MBeanNotificationInfo[] buildNotifications() {
        List<MBeanNotificationInfo> info =
            new LinkedList<MBeanNotificationInfo>();

        info.add(new MBeanNotificationInfo(
            new String[] {ClusterControllerNotification.
                    CRAWL_SERVICE_CREATED_NOTIFICATION.getKey() },
                ClusterControllerBean.class.getName(),
                "Notifies when a new instance of the crawl service comes up"));

        info.add(new MBeanNotificationInfo(
            new String[] { ClusterControllerNotification.
                    CRAWL_SERVICE_DESTROYED_NOTIFICATION.getKey() },
                ClusterControllerBean.class.getName(),
                "Notifies when an instance of the crawl service goes away"));

        info.add(new MBeanNotificationInfo(
            new String[] { ClusterControllerNotification.
                    CRAWL_SERVICE_JOB_STARTED_NOTIFICATION.getKey() },
                ClusterControllerBean.class.getName(),
                "Notifies when a new crawl service job starts"));

        info.add(new MBeanNotificationInfo(
            new String[] { ClusterControllerNotification.
                    CRAWL_SERVICE_JOB_COMPLETED_NOTIFICATION.getKey() },
                ClusterControllerBean.class.getName(),
                "Notifies when a new instance of the crawl service job " +
                "completes"));

        return info.toArray(new MBeanNotificationInfo[0]);
    }

    /**
     * Initializes the cluster controller.
     */
    public void init() {
        try {
            context = JndiUtils.getSubContext("org.archive.crawler");
            this.name = new ObjectName("org.archive.hcc:"
                    + "type=ClusterControllerBean"
                    + ",host="
                    + System.getenv("HOSTNAME")
                    + ",jmxport="
                    + System.getProperty(
                            "com.sun.management.jmxremote.port",
                            "8849"));

            refreshRegistry();

            initializeJndiPoller();

            registerMBean();

        } catch (MalformedObjectNameException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (NullPointerException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (NamingException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private void registerMBean() {
        try {
            this.mbeanServer.registerMBean(this, this.name);
        } catch (InstanceAlreadyExistsException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (MBeanRegistrationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (NotCompliantMBeanException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Disconnects the cluster controller from the network. It unhooks the
     * controller without acting on the remote instances.
     */
    public void destroy() {
        try {
            if (log.isLoggable(Level.INFO)) {
                log.info("destroying cluster controller.");
            }

            jndiPoller.cancel();

            if (log.isLoggable(Level.INFO)) {
                log.info("cancelled jndi poller");
            }

            jndiPoller = null;

            this.broadCaster = null;

            List<Container> containers = new LinkedList<Container>(
                    this.containers.values());
            for (Container container : containers) {
                dereferenceContainer(container);
            }

            context.close();
        } catch (NamingException e) {
            e.printStackTrace();
        } finally {
            try {
                this.mbeanServer.unregisterMBean(this.name);
            } catch (InstanceNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (MBeanRegistrationException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    private void dereferenceCrawler(Crawler crawler) {

        crawler.removeFromParent();
        this.remoteNameToCrawlerMap.remove(crawler
                .getCrawlJobRemoteObjectName());

        try {
            this.mbeanServer.unregisterMBean(crawler
                    .getCrawlServiceProxyObjectName());
        } catch (InstanceNotFoundException e) {
            if (log.isLoggable(Level.WARNING)) {
                log.warning(e.getMessage());
            }

            e.printStackTrace();

        } catch (MBeanRegistrationException e) {
            if (log.isLoggable(Level.WARNING)) {
                log.warning(e.getMessage());
            }

            e.printStackTrace();
        }

        try {
            ObjectName crawlJob = crawler.getCrawlJobProxyObjectName();
            if (crawlJob != null) {
                this.mbeanServer.unregisterMBean(crawlJob);
            }
        } catch (InstanceNotFoundException e) {
            if (log.isLoggable(Level.WARNING)) {
                log.warning(e.getMessage());
            }

        } catch (MBeanRegistrationException e) {
            if (log.isLoggable(Level.WARNING)) {
                log.warning(e.getMessage());
            }

            e.printStackTrace();
        }
    }

    /**
     * Initializes the notification forwarder.
     * @return Returns notification delegator.
     */
    private NotificationDelegator buildRemoteNotificationDelegator() {
        NotificationDelegator d = new NotificationDelegator(
                DelegatorPolicy.ACCEPT_FIRST) {
            protected boolean delegate(Notification n, Object handback) {
                return super.delegate(n, handback);
            }
        };

        d.addDelegatable(new CrawlJobLifecycleNotificationHandler());
        d.addDelegatable(new CrawlerLifecycleNotificationHandler());
        d.addDelegatable(new ContainerLifecycleNotificationHandler());

        return d;
    }

    private void fireNotificationEvent(Notification n) {
        this.broadCaster.sendNotification(n);
    }

    private class CrawlerLifecycleNotificationHandler
            extends
                RegistrationNotificationHandler {
        @Override
        protected String getType() {
            return JmxUtils.SERVICE;
        }

        @Override
        protected void handleRegistered(ObjectName name) {
            handleCrawlerCreated(name);
        }

        @Override
        protected void handleUnregistered(ObjectName name) {
            handleCrawlerRemoved(name);
        }
    }

    private class ContainerLifecycleNotificationHandler
            extends
                RegistrationNotificationHandler {
        @Override
        protected String getType() {
            return "container";
        }

        @Override
        protected void handleRegistered(ObjectName name) {
            // Empty.
        }

        @Override
        protected void handleUnregistered(ObjectName name) {
            handleContainerRemoved(name);
        }
    }

    private class CrawlJobLifecycleNotificationHandler
            extends
                RegistrationNotificationHandler {
        @Override
        protected String getType() {
            return JmxUtils.JOB;
        }

        @Override
        protected void handleRegistered(ObjectName name) {
            handleJobAdded(name);
        }

        @Override
        protected void handleUnregistered(ObjectName name) {
            handleJobRemoved(name);
        }
    }

    protected void handleJobRemoved(ObjectName job) {
        // locate crawler
        Crawler c = getJobContext(job);
        // remove job reference from crawler
        if (c != null) {
            // unregister job proxy.
            ObjectName jobProxy = c.getCrawlJobProxyObjectName();

            if (jobProxy != null) {
                try {
                    this.mbeanServer.unregisterMBean(jobProxy);
                } catch (InstanceNotFoundException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (MBeanRegistrationException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }

            c.setCrawlJobProxy(null);
            fireNotification(
                    jobProxy,
                    ClusterControllerNotification.
                        CRAWL_SERVICE_JOB_COMPLETED_NOTIFICATION.getKey());
        }
    }

    private boolean isJobOnCrawler(ObjectName job, ObjectName crawler) {
        return equals(job, crawler, JmxUtils.JMX_PORT)
                && equals(job, crawler, JmxUtils.HOST)
                && job.getKeyProperty(JmxUtils.MOTHER).equals(
                        crawler.getKeyProperty(JmxUtils.NAME));
    }

    private Crawler getJobContext(ObjectName job) {
        for (ObjectName n : this.remoteNameToCrawlerMap.keySet()) {
            if (isJobOnCrawler(job, n)) {
                return this.remoteNameToCrawlerMap.get(n);
            }
        }

        throw new NullPointerException(
                "shouldn't happen: no crawler found for job: " + job);
    }

    protected void handleJobAdded(ObjectName job) {
        try {
            ObjectName proxyName = addCrawlJob(job);
            // fire event
            fireNotification(
                    proxyName,
                    ClusterControllerNotification.
                        CRAWL_SERVICE_JOB_STARTED_NOTIFICATION.getKey());
        } catch (RuntimeException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private ObjectName addCrawlJob(ObjectName job) {
        InetSocketAddress address = JmxUtils.extractAddress(job);
        Crawler crawler = getJobContext(job);
        ObjectName proxyName = createClientProxyName(job);
        DynamicMBean proxy = (DynamicMBean) Proxy.newProxyInstance(
                DynamicMBean.class.getClassLoader(),
                new Class[] { DynamicMBean.class, NotificationEmitter.class },
                new RemoteMBeanInvocationHandler(
                        job,
                        proxyName,
                        this.connections.get(address),
                        spyListener));

        crawler.setCrawlJobProxy(proxy);
        try {
            this.mbeanServer.registerMBean(proxy, proxyName);
        } catch (InstanceAlreadyExistsException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (MBeanRegistrationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (NotCompliantMBeanException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        ((NotificationEmitter) proxy).addNotificationListener(
                this.remoteNotificationDelegator,
                null,
                new Object());

        return proxyName;
    }

    protected boolean equals(ObjectName a, ObjectName b, String key) {
        Object va = a.getKeyProperty(key);
        Object vb = b.getKeyProperty(key);

        if (!(va != null && vb != null)) {
            return false;
        }

        return va.equals(vb);
    }

    protected void handleContainerRemoved(ObjectName name) {

        for (Container c : new LinkedList<Container>(containers.values())) {
            if (c.getName().equals(name)) {
                for (Crawler crawler : c.getCrawlers()) {
                    removeCrawlerAndNotify(crawler);
                }
                dereferenceContainer(c);
                break;
            }
        }
    }

    private void removeCrawlerAndNotify(Crawler crawler) {
        dereferenceCrawler(crawler);
        fireCrawlerDestroyed(crawler.getCrawlServiceProxyObjectName());
    }

    protected void handleCrawlerRemoved(ObjectName name) {
        Crawler c = this.remoteNameToCrawlerMap.remove(name);
        if (c != null) {
            removeCrawlerAndNotify(c);
        }
    }

    /**
     * @return Returns a list of containers registered with the jndi service.
     */
    protected final List<ObjectName> retrieveContainerListFromJndi() {
        List<ObjectName> list = new LinkedList<ObjectName>();

        try {
            NamingEnumeration<NameClassPair> e = context.list("");
            while (e.hasMore()) {
                NameClassPair ncp = e.next();
                String jndiName = ncp.getName();

                try {
                    ObjectName on = new ObjectName(":" + jndiName);
                    Hashtable ht = on.getKeyPropertyList();
                    if (ht.get("type").equals("container")) {
                        list.add(on);
                    }

                } catch (MalformedObjectNameException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
            }

        } catch (NamingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return list;
    }
    
    protected final void refreshRegistry() {
        List<ObjectName> containerNameList = this
                .retrieveContainerListFromJndi();
        this.containers = synchronizeContainers(
                this.containers,
                containerNameList);
    }

    /**
     * Synchronizes the container list with the fresh list (fresh meaning, last
     * polled from jndi), removing those that went away, and adding any newly
     * discovered containers.
     * 
     * @param containers
     * @param freshContainers
     * @return Map of container object names.
     */
    protected final Map<ObjectName, Container> synchronizeContainers(
            Map<ObjectName, Container> containers,
            List<ObjectName> freshContainers) {

        Map<ObjectName, Container> staleContainers = new HashMap<ObjectName, Container>(
                containers);

        // remove and destroy all containers not in the new list.
        for (ObjectName n : staleContainers.keySet()) {
            if (!freshContainers.contains(n)) {
                handleContainerRemoved(n);
            }
        }

        // add new containers not in the old list.
        for (ObjectName n : freshContainers) {
            if (!containers.keySet().contains(n)) {
                try {
                    InetSocketAddress address = JmxUtils.extractAddress(n);
                    registerAddress(address);
                    synchronizeContainer(n);
                    attachMBeanServerDelegateNotificationListener(
                            address,
                            this.remoteNotificationDelegator);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return containers;
    }

    /**
     * Attaches a notification listener to the remote mbean server delegate at
     * the specified address..
     * @param address 
     * @param listener
     */
    protected void attachMBeanServerDelegateNotificationListener(
            InetSocketAddress address,
            NotificationListener listener) {
        // attach container removed listener
        MBeanServerConnection mbc = this.connections.get(address);

        if (mbc == null) {
            throw new NullPointerException(
                    "no mbean server connection found on " + address);
        }

        try {
            mbc.addNotificationListener(
                    JmxUtils.MBEAN_SERVER_DELEGATE,
                    listener,
                    null,
                    null);
        } catch (InstanceNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    protected void registerAddress(InetSocketAddress isa) throws IOException {
        if (!this.connections.keySet().contains(isa)) {
            // create connection.
            MBeanServerConnection mbc = MBeanServerConnectionFactory.createConnection(isa);
            this.connections.put(isa, mbc);
        }
    }

    /**
     * Unhooks container from the bus.
     * @param c Container to remove.
     */
    protected void dereferenceContainer(Container c) {
        Container removed = containers.remove(c.getName());
        if (removed != null) {
            InetSocketAddress address = JmxUtils.extractAddress(c.getName());
            for (Crawler b : new LinkedList<Crawler>(c.getCrawlers())) {
                dereferenceCrawler(b);
            }

            removeMBeanServerNotificationListener(address);
            this.connections.remove(address);
        }
    }

    protected void removeMBeanServerNotificationListener(
            InetSocketAddress address) {
        // attach container removed listener
        MBeanServerConnection mbc = this.connections.get(address);

        if (mbc == null) {
            throw new NullPointerException(
                    "no mbean server connection found on " + address);
        }

        try {
            mbc.removeNotificationListener(
                    JmxUtils.MBEAN_SERVER_DELEGATE,
                    this.remoteNotificationDelegator);
        } catch (InstanceNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ListenerNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Synchronizes the state of an individual container. This means that the
     * container is polled for instances of mbeans, listeners are attached, to
     * the remote mbeans,
     * 
     * @param c Container to check.
     */
    protected void synchronizeContainer(ObjectName c) {
        InetSocketAddress address = JmxUtils.extractAddress(c);
        MBeanServerConnection mbc = this.connections.get(address);

        if (mbc == null) {
            throw new NullPointerException(
                    "no mbean server connection found on " + address);
        }
        Container container = new Container(c);

        this.containers.put(c, container);
        try {
            // TODO - this should be filtering crawlers and current jobs.
            Set<ObjectName> names = mbc.queryNames(null, null);
            for (ObjectName n : names) {
                synchronizeMBean(n);
            }

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * synchronizes state of beans according to their types. E.g. Heritrix
     * Service beans are added if it hasn't already been created.
     * 
     * @param name
     */
    protected void synchronizeMBean(ObjectName name) {
        String theType = name.getKeyProperty(JmxUtils.TYPE);
        if (JmxUtils.SERVICE.equals(theType)) {
            handleCrawlerCreated(name);
        }
    }

    /**
     * defines and starts the jndi poller timer task
     */
    private void initializeJndiPoller() {
        this.jndiPoller.schedule(
        // define the timer task
                new TimerTask() {
                    public void run() {
                        if (log.isLoggable(Level.INFO)) {
                            log.info("running poll task...");
                        }

                        if (jndiPoller == null) {
                            return;
                        }

                        refreshRegistry();
                        if (log.isLoggable(Level.INFO)) {
                            log.info("poll task done.");
                        }
                    }

                },

                new Date(), // start it now
                JNDI_POLL_PERIOD_IN_SECONDS * 1000); // poll interval
    }

    public Object getAttribute(String attribute)
            throws AttributeNotFoundException,
            MBeanException,
            ReflectionException {
        // TODO Auto-generated method stub
        return null;
    }

    public AttributeList getAttributes(String[] attributes) {
        // TODO Auto-generated method stub
        return null;
    }

    public MBeanInfo getMBeanInfo() {
        return this.info;
    }

    public Object invoke(String actionName, Object[] params, String[] signature)
            throws MBeanException,
            ReflectionException {
        return this.invocationManager.invoke(actionName, params, signature);
    }

    public void setAttribute(Attribute attribute)
            throws AttributeNotFoundException,
            InvalidAttributeValueException,
            MBeanException,
            ReflectionException {
        // TODO Auto-generated method stub

    }

    public AttributeList setAttributes(AttributeList attributes) {
        // TODO Auto-generated method stub
        return null;
    }

    public void addNotificationListener(
            NotificationListener listener,
            NotificationFilter filter,
            Object handback) throws IllegalArgumentException {
        this.broadCaster.addNotificationListener(listener, filter, handback);
    }

    public MBeanNotificationInfo[] getNotificationInfo() {
        return this.broadCaster.getNotificationInfo();
    }

    public void removeNotificationListener(
            NotificationListener listener,
            NotificationFilter filter,
            Object handback) throws ListenerNotFoundException {
        this.broadCaster.removeNotificationListener(listener, filter, handback);
    }

    public void removeNotificationListener(NotificationListener listener)
            throws ListenerNotFoundException {
        this.broadCaster.removeNotificationListener(listener);
    }

    public void postDeregister() {
        // TODO Auto-generated method stub

    }

    public void postRegister(Boolean registrationDone) {
        // TODO Auto-generated method stub

    }

    public void preDeregister() throws Exception {
        // TODO Auto-generated method stub

    }

    public ObjectName preRegister(MBeanServer server, ObjectName name)
            throws Exception {
        return name;
    }

    private ObjectName createServiceBeanName(
            String name,
            InetSocketAddress address,
            String guiPort) {
        try {
            Hashtable<String, String> ht = new Hashtable<String, String>();

            ht.put(JmxUtils.HOST, address.getHostName());
            ht.put(JmxUtils.TYPE, JmxUtils.SERVICE);
            ht.put(JmxUtils.NAME, name);
            ht.put(JmxUtils.JMX_PORT, String.valueOf(address.getPort()));
            ht.put(JmxUtils.GUI_PORT, guiPort);

            return new ObjectName("org.archive.crawler", ht);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private ObjectName createCrawlerIn(Container container) throws Exception {
        InetSocketAddress address = JmxUtils
                .extractAddress(container.getName());
        MBeanServerConnection c = this.connections.get(address);
        if (c == null) {
            throw new Exception("No connection found on " + address);
        }

        MBeanFutureTask t = null;
        try {
            String newBeanName = "h" + System.currentTimeMillis();
            final ObjectName beanName = createServiceBeanName(
                    newBeanName,
                    address,
                    container.getName().getKeyProperty(JmxUtils.GUI_PORT));

            t = new MBeanFutureTask("create crawler:" + address) {
                public boolean isNotificationEnabled(Notification notification) {
                    if (notification
                            .getType()
                            .equals(ClusterControllerNotification.
                                CRAWL_SERVICE_CREATED_NOTIFICATION.getKey())) {
                        Crawler c = remoteNameToCrawlerMap.get(beanName);
                        ObjectName proxy = c.getCrawlServiceProxyObjectName();
                        return (proxy != null && notification
                                .getUserData()
                                .equals(proxy));
                    }
                    return false;
                }
            };

            this.broadCaster.addNotificationListener(t, t, new Object());

            c.createMBean("org.archive.crawler.Heritrix", beanName);

            return (ObjectName) t.get(30 * 1000, TimeUnit.MILLISECONDS);

        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception(e);
        } finally {
            if (t != null) {
                this.broadCaster.removeNotificationListener(t);
            }
        }
    }

    private Container getContainerOn(InetSocketAddress address) {
        for (ObjectName n : this.containers.keySet()) {
            if (address.equals(JmxUtils.extractAddress(n))) {
                return this.containers.get(n);
            }
        }
        return null;
    }

    protected ObjectName addCrawler(ObjectName newCrawler) {
        try {
            // add the crawler id to the list
            boolean contains = (remoteNameToCrawlerMap.containsKey(newCrawler));
            if (!contains) {
                ObjectName proxyName = createClientProxyName(newCrawler);

                InetSocketAddress address = JmxUtils.extractAddress(newCrawler);
                MBeanServerConnection c = this.connections.get(address);
                if (c == null) {
                    throw new RuntimeException("No connection found on "
                            + address + "- this should never happen");
                }

                // attach listener to remote mbean
                // this.connections.get(address).addNotificationListener(newCrawler,
                // this.mbeanServerDelegator, null, new Object());

                DynamicMBean proxy = (DynamicMBean) Proxy.newProxyInstance(
                        DynamicMBean.class.getClassLoader(),
                        new Class[] { DynamicMBean.class },
                        new RemoteMBeanInvocationHandler(
                                newCrawler,
                                proxyName,
                                c,
                                spyListener));

                Container container = getContainerOn(address);

                if (container == null) {
                    throw new RuntimeException(
                            "Container should never be null: " + address);
                }

                Crawler crawler = new Crawler(proxy, null, container);
                container.addCrawler(crawler);
                remoteNameToCrawlerMap.put(newCrawler, crawler);
                this.mbeanServer.registerMBean(proxy, proxyName);

                Hashtable props = new Hashtable();
                props.put(JmxUtils.TYPE, JmxUtils.JOB);
                props.put(JmxUtils.MOTHER, newCrawler
                        .getKeyProperty(JmxUtils.NAME));

                try {
                    Set<ObjectName> jobs = c.queryNames(new ObjectName(
                            newCrawler.getDomain(),
                            props), null);
                    if (jobs.size() > 0) {
                        addCrawlJob(jobs.iterator().next());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return proxyName;
            }
        } catch (InstanceAlreadyExistsException e) {
            e.printStackTrace();
        } catch (MBeanRegistrationException e) {
            e.printStackTrace();
        } catch (NotCompliantMBeanException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        return null;
    }

    protected void handleCrawlerCreated(ObjectName newCrawler) {
        ObjectName proxyName = addCrawler(newCrawler);
        if (proxyName != null) {
            fireCrawlerCreated(proxyName);
        }
    }

    private void fireCrawlerDestroyed(ObjectName proxyName) {
        fireNotification(
            proxyName,
            ClusterControllerNotification.CRAWL_SERVICE_DESTROYED_NOTIFICATION.getKey());
    }

    private void fireNotification(ObjectName name, String type) {
        Notification n = new Notification(type, this.name, System
                .currentTimeMillis());
        n.setUserData(name);
        fireNotificationEvent(n);
        if (log.isLoggable(Level.FINE)) {
            log.info("name=" + name + "; type=" + type);
        }
    }

    private void fireCrawlerCreated(ObjectName proxyName) {
        fireNotification(
            proxyName,
            ClusterControllerNotification.
                CRAWL_SERVICE_CREATED_NOTIFICATION.getKey());
    }

    private ObjectName createClientProxyName(ObjectName remote) {
        try {

            Hashtable lp = this.name.getKeyPropertyList();
            Hashtable cp = new Hashtable();
            Hashtable rp = remote.getKeyPropertyList();
            cp.put(JmxUtils.TYPE, rp.get(JmxUtils.TYPE));
            cp.put(JmxUtils.NAME, rp.get(JmxUtils.NAME));
            cp.put(JmxUtils.HOST, lp.get(JmxUtils.HOST));
            cp.put(JmxUtils.JMX_PORT, lp.get(JmxUtils.JMX_PORT));
            cp.put("remoteHost", rp.get(JmxUtils.HOST));
            cp.put("remoteJmxPort", rp.get(JmxUtils.JMX_PORT));

            return new ObjectName(this.name.getDomain(), cp);
        } catch (MalformedObjectNameException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } catch (NullPointerException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

    }

    public static void main(String[] args) {
        ClusterControllerBean b = new ClusterControllerBean();
        b.init();
    }
}
