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
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

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
import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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

/**
 * As the main workhorse of the package, the <code>ClusterControllerBean</code>
 * provides a unified view of any number of Heritrix instances and all related
 * objects defined within the hcc-configuration.xml file.
 * 
 * @author Daniel Bernstein (dbernstein@archive.org)
 */
public class ClusterControllerBean implements
        DynamicMBean,
        NotificationEmitter,
        MBeanRegistration {

//    /**
//     * The Jndi context
//     */
//    private Context context;

    /**
     * logger
     */
	private static Log log = LogFactory.getLog(ClusterControllerBean.class);

    /**
     * A timer thread that polls the nodes for new containers.
     */
    private Timer nodePoller;

    /**
     * A single instance of notification listener that simply forwards messages
     * from source beans to listeners.
     */
    private NotificationDelegator remoteNotificationDelegator;

    /**
     * default poll period in seconds.
     */
    private static final int DEFAULT_POLL_PERIOD_IN_SECONDS = 15;

    /**
     * poll period in seconds.
     */

    private int pollPeriodInSeconds = DEFAULT_POLL_PERIOD_IN_SECONDS;
    /**
     * A map of mbean server connections mapped by address. TODO make
     * configuratable
     */
    private Map<InetSocketAddress, MBeanServerConnection> connections =
        new HashMap<InetSocketAddress, MBeanServerConnection>();

    /**
     * A list of remote container references
     */
    private List<Container> containers;

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
    private Map<RemoteMapKey, Crawler> remoteNameToCrawlerMap =
        new HashMap<RemoteMapKey, Crawler>();

    /**
     * Watches all traffic (invocations and notifications) moving between the
     * remote Heritrix instances and the heritrix cluster controller.
     */
    private NotificationListener spyListener;
    
    /**
     * Upperbound on crawlers per container.
     */
    private int defaultMaxPerContainer = 1;

    /**
     * Creates a cluster controller bean. This object uses a two step
     * initialization pattern. Therefore you must call init() before invoking
     * any options.
     */
    public ClusterControllerBean() {
        this.remoteNotificationDelegator = buildRemoteNotificationDelegator();
        nodePoller = new Timer();
        this.broadCaster = new NotificationBroadcasterSupport();
        this.invocationManager = new OpenMBeanInvocationManager();
        this.info = buildOpenMBeanInfo();
        this.mbeanServer = createMBeanServer();
        this.containers = new LinkedList<Container>();
        this.spyListener = new NotificationListener() {
            public void handleNotification(
                    Notification notification,
                    Object handback) {

            	log.info(">>>>>>>>>spyListener: notification=" + notification);

                fireNotificationEvent(notification);
            }
        };
    }
    
    private class RemoteMapKey {
    	private ObjectName name;
    	private String id;
    	public ObjectName getObjectName(){
    		return name;
    		
    	}
    	
    	public RemoteMapKey(ObjectName on){
    		this.name = on;
    		this.id = extractId(on);
    	}
    	
    	@Override
    	public boolean equals(Object obj) {
    		if(!(obj instanceof RemoteMapKey)){
    			return false;
    		}
    		
    		RemoteMapKey o = (RemoteMapKey)obj;
    		
    		return (o.id.equals(id));
    	}
    	
	    private String extractId(ObjectName name){
	    	StringBuffer b = new StringBuffer();
	    	b.append(name.getKeyProperty(JmxUtils.NAME));
	    	b.append(name.getKeyProperty(JmxUtils.HOST));
	    	b.append(name.getKeyProperty(JmxUtils.JMX_PORT));
	    	b.append(name.getKeyProperty(JmxUtils.TYPE));
	    	return b.toString();
	    }
    	@Override
    	public int hashCode() {
    		return id.hashCode();
    	}
    }

    private static MBeanServer createMBeanServer() {
        List<MBeanServer> servers = MBeanServerFactory.findMBeanServer(null);
        if (servers == null) {
            return null;
        }

        for (MBeanServer server: servers) {
            if (server != null)
                return server;
        }

        return null;
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

        List<Crawler> crawlers = 
        	new LinkedList<Crawler>(container.getCrawlers());
        for (Crawler crawler : crawlers) {
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
            	log.warn(e.toString(), e);
            } catch (ReflectionException e) {
            	log.warn(e.toString(), e);
            }
        }

        return null;
    }

    
    /**
     * Returns the current job object name associated with the specified crawler. 
     * Returns null if no job was running.
     * @param mother
     * @return
     */
    public ObjectName getCurrentCrawlJob(ObjectName mother) {
        InetSocketAddress remoteAddress = org.archive.hcc.util.JmxUtils.extractRemoteAddress(mother);
        Container container = getContainerOn(remoteAddress);

        if (container == null) {
            return null;
        }

        for (Crawler crawler : new LinkedList<Crawler>(container.getCrawlers())) {
            if(crawler.getCrawlServiceProxyObjectName().equals(mother)){
                return  crawler.getCrawlJobProxyObjectName();
            }
        }
        return null;
    }
    
    /**
     * Returns the maximum number of instances allowed for this container.
     * If the container does not exist, -1 is returned.
     * @param hostname
     * @param port
     * @return
     */
    public int getMaxInstances(String hostname, Integer port){
    	Collection<Container> list = 
    		new LinkedList<Container>(this.containers);
    	
    	for(Container c : list){
    		InetSocketAddress a = c.getAddress();
    		
    		if(addressMatches(hostname, port, a)){
    			return c.getMaxInstances();
    		}
    	}
    	
    	return -1;
    }

	private boolean addressMatches(String hostname, Integer port, InetSocketAddress a) {
		return a.getHostName().equals(hostname) && port == a.getPort();
	}

    /**
     * Sets the maximum number of instances that may run on a 
     * specified container defined by a host and port.
     * @param hostname
     * @param port
     * @param maxInstances
     */
    public void setMaxInstances(String hostname, Integer port, Integer maxInstances){
    	Collection<Container> list = 
    		new LinkedList<Container>(this.containers);
    	
    	if(maxInstances < -1){
    		maxInstances = -1;
    	}
    	
    	for(Container c : list){
    		InetSocketAddress a = c.getAddress();
    		if(addressMatches(hostname, port, a)){
    			c.setMaxInstances(maxInstances);
    			break;
    		}
    	}
    }
    
    /**
     * Creates a new crawler on the least loaded machine on the cluster.
     * 
     * @return ObjectName of created crawler.
     * @throws MBeanException
     */
    public ObjectName createCrawler() throws MBeanException {
    	ContainerLoadBalancingStrategy strategy = new ContainerLoadBalancingStrategy();
        List<Container> orderedContainers = strategy.prioritize(this.containers);

        for(Container container : orderedContainers){
            try {
            	log.info("attempting to create crawler on container: " + container.getAddress());
                return createCrawlerIn(container);
            } catch (Exception e) {
            	log.warn("unexpected error!!! failed to create crawler as expected on " + container.getAddress(), e);
            }
        }
        
        if(orderedContainers.size() > 0){
            log.error("unable to start any crawlers on container due to communication " +
    		"failure with all available containers.");
        }

        MBeanException e =  new MBeanException(
                new Exception(
                "No space available in remote"+
                " containers for new crawler " + "instances."),
                "insufficent crawler resources"
        );
        
        throw e;

    }


//  This is the old implementation - didn't take into account the load on a particular host machine
//    /**
//     * @return A list of available (ie not fully loaded) containers sorted from least loaded to most loaded. If no
//     * containers are available, returns null.
//     */
//    protected List<Container> resolveLeastLoadedContainers() {
//
//        List<Container>leastLoaded = null;
//        Container last = null;
//        
//        List<Container> currentContainers = new LinkedList<Container>(this.containers);
//        
//        Collections.sort(currentContainers, new Comparator(){
//        	public int compare(Object o1, Object o2) {
//        		Container c1 = (Container)o1;
//        		Container c2 = (Container)o2;
//        		return new Integer(c1.getCrawlers().size()).compareTo(new Integer(c2.getCrawlers().size()));
//        	}
//        });
//
//        for (Container n : currentContainers) {
//            if (n.getCrawlers().size() >= n.getMaxInstances()) {
//                continue;
//            }
//            
//            if(leastLoaded == null){
//            	leastLoaded = new LinkedList<Container>();
//            }
//           
//            leastLoaded.add(n);
//        }
//
//        return leastLoaded;
//    }


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
                            "destroyAllCrawlers",
                            "destroys all crawlers that are managed by the cluster" +
                            " controller",
                            null,
                            SimpleType.VOID,
                            OpenMBeanOperationInfoSupport.ACTION)));

            
            addOperation(new SimpleReflectingMBeanOperation(
                    ClusterControllerBean.this,
                    new OpenMBeanOperationInfoSupport(
                            "pauseAllJobs",
                            "pauses all jobs that are managed by the cluster" +
                            " controller",
                            null,
                            SimpleType.BOOLEAN,
                            OpenMBeanOperationInfoSupport.ACTION)));
            
            addOperation(new SimpleReflectingMBeanOperation(
                    ClusterControllerBean.this,
                    new OpenMBeanOperationInfoSupport(
                            "resumeAllPausedJobs",
                            "resumes crawling of all paused jobs that are managed by the cluster" +
                            " controller",
                            null,
                            SimpleType.BOOLEAN,
                            OpenMBeanOperationInfoSupport.ACTION)));

            
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
            
            addOperation(new SimpleReflectingMBeanOperation(
                    ClusterControllerBean.this,
                    new OpenMBeanOperationInfoSupport(
                            "getCurrentCrawlJob", "returns the current " +
                                "crawl job name of the crawl job running" +
                                " on the specified crawler Returns null" +
                                " if either the mother is not found or " +
                                "if the crawler does not have a current job.",
                            new OpenMBeanParameterInfo[] {
                                    new OpenMBeanParameterInfoSupport(
                                            "mother",
                                            "The job's mother (CrawlService)",
                                            SimpleType.OBJECTNAME)},
                            SimpleType.OBJECTNAME,
                            OpenMBeanOperationInfoSupport.INFO)));
            

            addOperation(new SimpleReflectingMBeanOperation(
                    ClusterControllerBean.this,
                    new OpenMBeanOperationInfoSupport(
                            "setMaxInstances", "sets the max number of " +
                            		"instances of heritrix that a heritrix " +
                            		"enabled jvm can serve.",
                            new OpenMBeanParameterInfo[] {
                                    new OpenMBeanParameterInfoSupport(
                                            "host",
                                            "The jvm's host",
                                            SimpleType.STRING),
                                    new OpenMBeanParameterInfoSupport(
                                            "port",
                                            "The jvm's jmx port",
                                            SimpleType.INTEGER),
                                    new OpenMBeanParameterInfoSupport(
                                            "maxInstances",
                                            "The max number of instances",
                                            SimpleType.INTEGER) },
                            SimpleType.VOID,
                            OpenMBeanOperationInfoSupport.ACTION)));  
            

            addOperation(new SimpleReflectingMBeanOperation(
                    ClusterControllerBean.this,
                    new OpenMBeanOperationInfoSupport(
                            "getMaxInstances", "returns the max number of " +
                            		"instances of heritrix that a heritrix " +
                            		"enabled jvm can serve.",
                            new OpenMBeanParameterInfo[] {
                                    new OpenMBeanParameterInfoSupport(
                                            "host",
                                            "The jvm's host",
                                            SimpleType.STRING),
                                    new OpenMBeanParameterInfoSupport(
                                            "port",
                                            "The jvm's jmx port",
                                            SimpleType.INTEGER)
                                            },
                            SimpleType.INTEGER,
                            OpenMBeanOperationInfoSupport.INFO)));     
            
        } catch (OpenDataException e) {
        	log.warn(e.toString(), e);
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
    
    public void destroyAllCrawlers(){
    	List<Crawler> list = 
    		new LinkedList<Crawler>(this.remoteNameToCrawlerMap.values());
    	for(Crawler c : list){
    		try {
				c.getCrawlServiceProxy().invoke("destroy", new Object[0], new String[0]);
			} catch (MBeanException e) {
            	log.warn(e.toString(), e);
			} catch (ReflectionException e) {
            	log.warn(e.toString(), e);
			}
    	}
    }


    public boolean pauseAllJobs(){
    	List<Crawler> list = 
    		new LinkedList<Crawler>(this.remoteNameToCrawlerMap.values());
    	
    	boolean success = true;
    	for(Crawler c : list){
    		try {
    			DynamicMBean m = c.getCrawlJobProxy();
    			if(m == null){
    				continue;
    			}
    			String status = (String)m.getAttribute("Status");
    			
    			if("RUNNING".equals(status)){
    				m.invoke("pause",  new Object[0], new String[0]);
    			}
			} catch (AttributeNotFoundException e) {
				log.warn(e.toString(), e);
			} catch (MBeanException e) {
				log.warn(e.toString(), e);
				success = false;
			} catch (ReflectionException e) {
				log.warn(e.toString(), e);
				success = false;
			}
    	}
    	
    	return success;
    }
    
    public boolean resumeAllPausedJobs(){
    	List<Crawler> list = 
    		new LinkedList<Crawler>(this.remoteNameToCrawlerMap.values());
    	
    	boolean success = true;
    	for(Crawler c : list){
    		try {
    			DynamicMBean m = c.getCrawlJobProxy();
    			if(m == null){
    				continue;
    			}

    			String status = (String)m.getAttribute("Status");
    			if("PAUSED".equals(status) || "PAUSING".equals(status)){
    				m.invoke("resume",  new Object[0], new String[0]);
    			}
			} catch (AttributeNotFoundException e) {
				log.warn(e.toString(), e);
			} catch (MBeanException e) {
				log.warn(e.toString(), e);
				success = false;
			} catch (ReflectionException e) {
				log.warn(e.toString(), e);
				success = false;
			}
    	}
    	
    	return success;
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
        	initializeConfig();
            
        	initializeContext();

            refreshRegistry();

            initializePoller();

            registerMBean();

        } catch (MalformedObjectNameException e) {
			log.warn(e.toString(), e);
        } catch (NullPointerException e) {
			log.warn(e.toString(), e);
        } catch (NamingException e) {
			log.error(e.toString(), e);
            throw new RuntimeException(e);
        }catch (UnknownHostException e) {
			log.error(e.toString(), e);
            throw new RuntimeException(e);
        }
    }
    
    private void initializeConfig(){
    	Config.instance();
    }

    
    private void initializeContext() throws MalformedObjectNameException, NamingException, UnknownHostException{
        //context = JndiUtils.getSubContext("org.archive.crawler");
        String hostName = System.getenv("HOSTNAME");
        if(hostName == null){
        		hostName = InetAddress.getLocalHost().getHostName();
        }
    	
    	this.name = new ObjectName("org.archive.hcc:"
                + "type=ClusterControllerBean"
                + ",host="
                + hostName
                + ",jmxport="
                + System.getProperty(
                        "com.sun.management.jmxremote.port",
                        "8849"));

    }
    private void registerMBean() {
        try {
            this.mbeanServer.registerMBean(this, this.name);
        } catch (InstanceAlreadyExistsException e) {
            log.warn(e.toString(), e);
        } catch (MBeanRegistrationException e) {
            log.warn(e.toString(), e);
        } catch (NotCompliantMBeanException e) {
            log.warn(e.toString(), e);
        }
    }

    /**
     * Disconnects the cluster controller from the network. It unhooks the
     * controller without acting on the remote instances.
     */
    public void destroy() {
        try {
        	log.info("destroying cluster controller.");

            nodePoller.cancel();

            log.info("cancelled jndi poller");

            nodePoller = null;

            this.broadCaster = null;

            List<Container> containers = new LinkedList<Container>(
                    this.containers);
            for (Container container : containers) {
                dereferenceContainer(container);
            }

           //context.close();
        } 
//        catch (NamingException e) {
//            e.printStackTrace();
//        } 
        finally {
            try {
                this.mbeanServer.unregisterMBean(this.name);
            } catch (InstanceNotFoundException e) {
                log.warn(e.toString(), e);
            } catch (MBeanRegistrationException e) {
                log.warn(e.toString(), e);
            }
        }
    }

    private void dereferenceCrawler(Crawler crawler) {

        crawler.removeFromParent();
        if(crawler.getCrawlJobRemoteObjectName()!= null){
            this.remoteNameToCrawlerMap.remove(
            		new RemoteMapKey(crawler.getCrawlJobRemoteObjectName()));
        }

        try {
            this.mbeanServer.unregisterMBean(crawler
                    .getCrawlServiceProxyObjectName());
        } catch (InstanceNotFoundException e) {
            log.warn(e.toString(), e);
        } catch (MBeanRegistrationException e) {
            log.warn(e.toString(), e);
        }

        try {
            ObjectName crawlJob = crawler.getCrawlJobProxyObjectName();
            if (crawlJob != null) {
                this.mbeanServer.unregisterMBean(crawlJob);
            }
        } catch (InstanceNotFoundException e) {
        	log.warn(e.getMessage());
        } catch (MBeanRegistrationException e) {
        	log.warn(e.getMessage(), e);
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
    	log.info("entering: job=" + job);
        // locate crawler
        Crawler c = getJobContext(job);
        
        if(c == null){
        	log.warn("no crawler context found for job=" + job);
        	
        	return;
        }
        // remove job reference from crawler
        // unregister job proxy.
        ObjectName jobProxy = c.getCrawlJobProxyObjectName();
        if(jobProxy == null){
        	log.warn("jobProxy was not found on crawler=" + c.getCrawlJobProxyObjectName());
        	return;
        }
        
        try {
            this.mbeanServer.unregisterMBean(jobProxy);
        } catch (InstanceNotFoundException e) {
            log.error("failed to unregister job proxy: " + jobProxy  + "; remote job=" + job + "; error.class=" + e.getClass() + ";  error.message=" + e.getMessage(), e);
        } catch (MBeanRegistrationException e) {
            log.error("failed to unregister job proxy: " + jobProxy  + "; remote job=" + job  + "; error.class=" + e.getClass() + "; message=" + e.getMessage(), e);
        }

        c.setCrawlJobProxy(null);
        fireNotification(
                jobProxy,
                ClusterControllerNotification.
                    CRAWL_SERVICE_JOB_COMPLETED_NOTIFICATION.getKey());
            
        log.info("exitting successfully: job=" + job);
            
    }

    private boolean isJobOnCrawler(ObjectName job, ObjectName crawler) {
        
    	return equals(job, crawler, JmxUtils.JMX_PORT)
                && equals(job, crawler, JmxUtils.HOST)
                && job.getKeyProperty(JmxUtils.MOTHER).equals(
                        crawler.getKeyProperty(JmxUtils.NAME));
    }

    private Crawler getJobContext(ObjectName job) {
        for (RemoteMapKey key : this.remoteNameToCrawlerMap.keySet()) {
            if (isJobOnCrawler(job, key.getObjectName())) {
                return this.remoteNameToCrawlerMap.get(key);
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
            log.warn(e.toString(), e);
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
            log.warn(e.toString(), e);
        } catch (MBeanRegistrationException e) {
            log.warn(e.toString(), e);
        } catch (NotCompliantMBeanException e) {
            log.warn(e.toString(), e);
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

    protected void handleContainerRemoved(Object name) {
        for (Container c : new LinkedList<Container>(containers)) {
            InetSocketAddress a = JmxUtils.extractAddress((ObjectName)name);
        	if (c.getAddress().equals(a)) {
        		handleContainerRemoved(c);
                break;
            }
        }
    }

    protected void handleContainerRemoved(Container c) {
    	List<Crawler> crawlers = new LinkedList<Crawler>(c.getCrawlers());
        for (Crawler crawler : crawlers) {
            removeCrawlerAndNotify(crawler);
        }
        dereferenceContainer(c);
    }
    
    private void removeCrawlerAndNotify(Crawler crawler) {
        dereferenceCrawler(crawler);
        fireCrawlerDestroyed(crawler.getCrawlServiceProxyObjectName());
    }

    protected void handleCrawlerRemoved(ObjectName name) {
        Crawler c = this.remoteNameToCrawlerMap.remove(new RemoteMapKey(name));
        if (c != null) {
            removeCrawlerAndNotify(c);
        }
    }

    
    protected final void refreshRegistry() {
        List<Container> containerNameList = retrieveContainerListFromXml();
        this.containers = synchronizeContainers(
                this.containers,
                containerNameList);
    }

    private List<Container> retrieveContainerListFromXml(){
    	Config c = Config.instance();
    	c.refresh();
    	return Config.instance().getContainers();
    	
    }
    
    private boolean isConnected(Container container){
    	MBeanServerConnection c = (MBeanServerConnection)this.connections.get(container.getAddress());
    	if(c == null){
    		return false;
    	}
    	
    	try {
			c.getMBeanCount();
			return true;
		} catch (IOException e) {
			return false;
		}
		
    }
    

    /**
     * Synchronizes the container list with the fresh list (fresh meaning, last
     * polled from hcc-configuration.xml), removing those that went away, and adding any newly
     * adding containers(jvms).
     * @param containers
     * @param freshContainers
     * @return Map of container object names.
     */
    protected final List<Container> synchronizeContainers(
            List<Container> containers,
            List<Container> freshContainers) {

    	//check that existing containers can communicate with remote container
        List<Container> staleContainers= new LinkedList<Container>(containers);

        // remove and destroy all containers not in the new list.
        for (Container c: staleContainers) {
            if (!freshContainers.contains(c) || !isConnected(c)) {
                handleContainerRemoved(c);
            }
        }

        // add new containers not in the old list.
        for (Container c: freshContainers) {
            if (!containers.contains(c)) {
                InetSocketAddress address = c.getAddress();

            	try {
                    registerAddress(address);
                    synchronizeContainer(c);
                    
                    attachMBeanServerDelegateNotificationListener(
                            address,
                            this.remoteNotificationDelegator);
                } catch (IOException e) {
                	log.warn("unabled to synchronize container on " + 
                			address.getHostName()+":" + address.getPort() + 
                			" - message: " + e.getMessage());
                    
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
            log.warn(e.toString(), e);
        } catch (IOException e) {
            log.warn(e.toString(), e);
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
        boolean removed = containers.remove(c);
        if (removed) {
            InetSocketAddress address = c.getAddress();
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
        	log.warn("MBeanServerConnection.removeNotificationListener() threw exception: " + e, e);
        } catch (ListenerNotFoundException e) {
        	log.warn("MBeanServerConnection.removeNotificationListener() threw exception: " + e, e);
        } catch (IOException e) {
        	log.warn("MBeanServerConnection.removeNotificationListener() threw exception: " + e, e);
        }
    }

    /**
     * Synchronizes the state of an individual container. This means that the
     * container is polled for instances of mbeans, listeners are attached, to
     * the remote mbeans.  If an mbean server connection cannot be created on the remote
     * address that fact is logged, but no action is taken.  This situation might arise if
     * a crawler goes down unexpectedly.   Since the list of available containers is managed
     * manually, it is possible for this list to go out of sync with reality. 
     * 
     * @param c Container to check.
     */
    protected void synchronizeContainer(Container c) {
        log.info("synchonizing container:" + c.toString());
    	
        InetSocketAddress address = c.getAddress();
        MBeanServerConnection mbc = this.connections.get(address);

        if (mbc == null) {
        	log.warn("unable to synchronize container: no mbean server connection found on " + address);
        	return;
        }

        this.containers.add(c);
        try {
            // TODO - this should be filtering crawlers and current jobs.
            Set<ObjectName> names = mbc.queryNames(null, null);
            for (ObjectName n : names) {
                synchronizeMBean(n);
            }

        } catch (IOException e) {
        	log.warn(e.getMessage(), e);
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
           log.info("crawler service found:" + name.toString());
            handleCrawlerCreated(name);
        }
    }

    /**
     * defines and starts the poller timer task
     */
    private void initializePoller() {
        this.nodePoller.schedule(
        // define the timer task
                new TimerTask() {
                    public void run() {
                    	log.debug("running poll task...");

                        if (nodePoller == null) {
                            return;
                        }

                        refreshRegistry();
                        log.debug("poll task done.");
                    }

                },

                new Date(), // start it now
                pollPeriodInSeconds * 1000); // poll interval
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
            InetSocketAddress address
//            ,String guiPort
            ) {
        try {
            Hashtable<String, String> ht = new Hashtable<String, String>();

            ht.put(JmxUtils.HOST, address.getHostName());
            ht.put(JmxUtils.TYPE, JmxUtils.SERVICE);
            ht.put(JmxUtils.NAME, name);
            ht.put(JmxUtils.JMX_PORT, String.valueOf(address.getPort()));
//            if(guiPort != null){
//                ht.put(JmxUtils.GUI_PORT, guiPort);
//            }

            return new ObjectName("org.archive.crawler", ht);
        } catch (Exception e) {
        	log.warn(e.toString(), e);
            throw new RuntimeException(e);
        }
    }


    private ObjectName createCrawlerIn(Container container) throws Exception {
        InetSocketAddress address = container.getAddress();
        MBeanServerConnection c = this.connections.get(address);
        if (c == null) {
            throw new Exception("No connection found on " + address);
        }

        MBeanFutureTask t = null;
        try {
            String newBeanName = "h" + System.currentTimeMillis();
            final ObjectName beanName = createServiceBeanName(
                    newBeanName,
                    address);

            t = new MBeanFutureTask("create crawler:" + address) {
                private static final long serialVersionUID = 1L;
                public boolean isNotificationEnabled(Notification notification) {
                    if (notification
                            .getType()
                            .equals(ClusterControllerNotification.
                                CRAWL_SERVICE_CREATED_NOTIFICATION.getKey())) {
                        Crawler c = remoteNameToCrawlerMap.get(new RemoteMapKey(beanName));
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
        	log.warn(e.toString(), e);
            throw e;
        } finally {
            if (t != null) {
                this.broadCaster.removeNotificationListener(t);
            }
        }
    }

    private Container getContainerOn(InetSocketAddress address) {
        for (Container n : this.containers) {
            if (address.equals(n.getAddress())) {
                return n;
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
                remoteNameToCrawlerMap.put(new RemoteMapKey(newCrawler), crawler);
                this.mbeanServer.registerMBean(proxy, proxyName);

                Hashtable<String,String> props = new Hashtable<String,String>();
                props.put(JmxUtils.TYPE, JmxUtils.JOB);
                props.put(JmxUtils.MOTHER, newCrawler
                        .getKeyProperty(JmxUtils.NAME));

                try {
                	//TODO - design the query to pick up only the one job.
                    Set<ObjectName> jobs = c.queryNames(null,null);
                    for(ObjectName job : jobs){
                        if (JmxUtils.JOB.equals(job.getKeyProperty(JmxUtils.TYPE)) &&
                        		newCrawler
                                .getKeyProperty(JmxUtils.NAME).equals(
                                		job.getKeyProperty(JmxUtils.MOTHER))
                        		
                        	) {
                            addCrawlJob(job);
                            break;
                        }
                    	
                    }
                } catch (Exception e) {
                	log.warn(e.toString(), e);
                }
                return proxyName;
            }
        } catch (InstanceAlreadyExistsException e) {
        	log.warn(e.toString(), e);
        } catch (MBeanRegistrationException e) {
        	log.warn(e.toString(), e);
        } catch (NotCompliantMBeanException e) {
        	log.warn(e.toString(), e);
        } catch (IllegalArgumentException e) {
        	log.warn(e.toString(), e);
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
        log.info("name=" + name + "; type=" + type);
    }

    private void fireCrawlerCreated(ObjectName proxyName) {
        fireNotification(
            proxyName,
            ClusterControllerNotification.
                CRAWL_SERVICE_CREATED_NOTIFICATION.getKey());
    }

    private ObjectName createClientProxyName(ObjectName remote) {
        try {

            Hashtable<String,String> lp = this.name.getKeyPropertyList();
            Hashtable<String,String> cp = new Hashtable<String,String>();
            Hashtable<String,String> rp = remote.getKeyPropertyList();
            cp.put(JmxUtils.TYPE, rp.get(JmxUtils.TYPE));
            cp.put(JmxUtils.NAME, rp.get(JmxUtils.NAME));
            cp.put(JmxUtils.HOST, lp.get(JmxUtils.HOST));
            cp.put(JmxUtils.JMX_PORT, lp.get(JmxUtils.JMX_PORT));
            cp.put("remoteHost", rp.get(JmxUtils.HOST));
            cp.put("remoteJmxPort", rp.get(JmxUtils.JMX_PORT));

            return new ObjectName(this.name.getDomain(), cp);
        } catch (MalformedObjectNameException e) {
        	log.error(e.toString(), e);
            throw new RuntimeException(e);
        } catch (NullPointerException e) {
        	log.error(e.toString(), e);
            throw new RuntimeException(e);
        }

    }

    public static void main(String[] args) {
        ClusterControllerBean b = new ClusterControllerBean();
        b.init();
    }
}
