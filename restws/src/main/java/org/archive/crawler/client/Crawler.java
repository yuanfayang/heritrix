package org.archive.crawler.client;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.archive.crawler.framework.Engine;
import org.archive.openmbeans.annotations.BeanProxy;

/**
 * A local or remote Heritrix crawl engine available via JMX.
 * 
 * @author ato
 * 
 */
public class Crawler {
	final private static Logger LOGGER = Logger.getLogger(Crawler.class
			.getName());

	/**
	 * Hostname of the crawler.
	 */
	private String host = "localhost";

	/**
	 * JMX port of the crawler. (-1 for local).
	 */
	private int port = -1;

	/**
	 * An identifier used to distinguish multiple crawlers running in the one
	 * JVM.
	 */
	private int identityHashCode;
	
	/**
	 * A more user-friendly alternative to the identityHashCode.  Increments
	 * from zero as crawlers are discovered from a particular MBeanServer.
	 */
	private int instanceNo;

	/**
	 * JMX username to login with.
	 */
	private String username;

	/**
	 * JMX password to login with.
	 */
	private String password;
	
	/**
	 * JMX object name of the Engine.
	 */
	private ObjectName objectName;

	/**
	 * JMX connection.
	 */
	private MBeanServerConnection conn;
	
	/**
	 * JMX connector for remote connections.
	 */
	private JMXConnector jmxc;
	
	/**
	 * Construct a Crawler from the given JMX ObjectName.
	 */
	public Crawler(ObjectName oname) {
		setHost(oname.getKeyProperty("host"));
		setPort(Integer.parseInt(oname.getKeyProperty("jmxport")));
		setIdentityHashCode(Integer.parseInt(oname.getKeyProperty("instance")));
		setObjectName(oname);
	}

	public static JMXConnector jmxConnect(String host, int port,
			String username, String password) throws IOException {

		String hp = host + ":" + port;
		String s = "service:jmx:rmi://" + hp + "/jndi/rmi://" + hp + "/jmxrmi";
		JMXServiceURL url = new JMXServiceURL(s);
		Map<String, Object> env = new HashMap<String, Object>(1);
		String[] creds = new String[] { username, password };
		env.put(JMXConnector.CREDENTIALS, creds);

		JMXConnector jmxc = JMXConnectorFactory.connect(url, env);
		return jmxc;
	}

	private static boolean isLocal(String host, int port, String username,
			String password) {
		return port == -1;
	}

	/**
	 * Discover all crawlers in the local JVM.
	 * @return a Crawler object for each Heritrix engine
	 */
	public static Collection<Crawler> discoverLocal() {
		try {
			return discover(null, -1, null, null);
		} catch (IOException e) { // shouldn't happen for local crawlers.
			throw new RuntimeException("Error communicating with local JMX", e);
		}
	}
	
	/**
	 * Discover all the crawlers in a given JMX server.
	 * 
	 * @param host hostname of JMX RMI server
	 * @param port port number of JMX RMI server
	 * @param username JMX auth username
	 * @param password JMX auth password
	 * @return a Crawler object for each Heritrix engine
	 */
	public static Collection<Crawler> discover(String host, int port,
			String username, String password) throws IOException {

		JMXConnector jmxc = null;
		try {
			/* Connect to the MBean server */
			MBeanServerConnection conn;
			if (isLocal(host, port, username, password)) {
				conn = ManagementFactory.getPlatformMBeanServer();
			} else {
				jmxc = jmxConnect(host, port, username, password);
				conn = jmxc.getMBeanServerConnection();
			}

			/* Grab a list of engines and covert them into crawlers*/
			Set<ObjectName> set = conn.queryNames(null, new ObjectName(
					"org.archive.crawler:*,name=Engine"));
			
			Collection<Crawler> result = new ArrayList<Crawler>();
			for (ObjectName oname : set) {
				Crawler c = new Crawler(oname);
				c.setUsername(username);
				c.setPassword(password);
				result.add(c);
			}

			return result;
		} catch (MalformedObjectNameException e) {
			throw new RuntimeException("Bug in Crawler.discover(...)", e);
		} finally {
			if (jmxc != null)
				try {
					jmxc.close();
				} catch (IOException e) {
					LOGGER.log(Level.WARNING,
							"Could not close jmx connection.", e);
				}
		}
	}
	
	public String getId() {
		return getHost() + ":" + getPort() + "." + getInstanceNo();
	}
	public String toString() {
		return getId();
	}
	
	/**
	 * Return the remote JMX object associated with this crawler.
	 */
	protected Engine getEngine()  {
		return getObject(getObjectName(), Engine.class);
	}
	
	/**
	 * Get a remote JMX object by ObjectName.
	 */
	public <T> T getObject(ObjectName oname, Class<T> cls) {
		try {
			return BeanProxy.proxy(getConnection(), oname, cls);
		} catch (Exception e) {
			// FIXME: this is nasty, figure out a better way to handle all the 
			//        crazy exceptions we can get.
			throw new RuntimeException("JMX error", e);
		}		
	}
	
	private MBeanServerConnection getConnection() throws IOException {
		if (conn == null) {
			if (isLocal(getHost(), getPort(), getUsername(), getPassword())) {
				conn = ManagementFactory.getPlatformMBeanServer();
			} else {
				jmxc = jmxConnect(host, port, username, password);
				conn = jmxc.getMBeanServerConnection();
			}
		}
		return conn;
	}

	/**
	 * Get all the jobs this crawler has.
	 * @return
	 */
	public Collection<Job> getJobs() {
		Vector<Job> jobs = new Vector<Job>();
		for (String name : getEngine().listJobs()) {
			jobs.add(new Job(this, name)); 
		}
		return jobs;
	}

	/**
	 * Find a job with the given name.
	 * @param jobName
	 * @return
	 */
	public Job getJob(String jobName) {
		for (Job job: getJobs()) {
			if (job.getName().equals(jobName)) {
				return job;
			}
		}
		return null;
	}

	/* Getters/setters */

	public int getInstanceNo() {
		return instanceNo;
	}

	public void setInstanceNo(int instanceNo) {
		this.instanceNo = instanceNo;
	}
	
	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public int getIdentityHashCode() {
		return identityHashCode;
	}

	public void setIdentityHashCode(int identityHashCode) {
		this.identityHashCode = identityHashCode;
	}
	protected ObjectName getObjectName() {
		return objectName;
	}

	protected void setObjectName(ObjectName objectName) {
		this.objectName = objectName;
	}
}
