<%--This include sets up context getting login and password if in session and
making up a Map of the cluster members.--%>
<%@ page import="javax.naming.Context" %>
<%@ page import="javax.naming.InitialContext" %>
<%@ page import="javax.naming.NameClassPair" %>
<%@ page import="javax.naming.NameNotFoundException" %>
<%@ page import="javax.naming.NamingEnumeration" %>
<%@ page import="javax.naming.NamingException" %>
<%@ page import="javax.management.ObjectName" %>
<%@ page import="javax.management.MalformedObjectNameException" %>
<%@ page import="java.util.Hashtable" %>
<%@ page import="java.util.Iterator" %>
<%@ page import="org.archive.jmx.Client" %>
<%!
    // This section has code that needs to be moved out to java classes.
    //
    /**
     * JNDI subcontext/domain for heritirx instances.
     */
    private final String DOMAIN = "org.archive.crawler";

    /**
     * @return The heritrix jndi subcontext (Be sure to close it when done).
     */
    private Context getHeritrixContext() throws NamingException {
        // Crawlers are always in the 'org.archive.crawler' context.
		final String subContext = DOMAIN;
        // TODO: Below duplicate of method from JndiUtils.
		Context context = new InitialContext();
        try {
		    context = (Context)context.lookup(subContext);
        } catch (NameNotFoundException e) {
            context = context.createSubcontext(subContext); 
        }
        if (context == null) {
            throw new NullPointerException("Context is null");
        }
        return context;
    }

    /**
     * @return Hashtable of registered instances.  Value is a data structure
     * that gives access to host and port for access to remote instance.
     */
    private Hashtable getInstances(final Context context)
    throws MalformedObjectNameException, NamingException {
        Hashtable result = new Hashtable();
        for (final NamingEnumeration i = context.list("");
                i.hasMoreElements();) {
            NameClassPair ncp = (NameClassPair)i.next();
            String jndiName = ncp.getName();
            HeritrixJndiNameParse p = new HeritrixJndiNameParse(jndiName);
            result.put(jndiName, p);
        }
        return result;
    }

    /**
     * Parse of jndi name.
     * 
     * Use accessors to get at host, port, gui url, etc.
     */
    private class HeritrixJndiNameParse {
        private final String url;
        private final String name;
        private final String host;
        private final String jmxport;

        public HeritrixJndiNameParse(final String jndiName) 
        throws MalformedObjectNameException {
            // Use JMX ObjectName for parsing the jndi name (The jndi name is
            // purposefully derivative of Heritrix mbean name). Note, the
            // field names used in here need to agree with those Heritrix uses
            // writing the mbean name (TODO: Definitions are in JmxUtils. Make
            // a archive-commons.jar with JmxUtils and JndiUtils so can import
            // here).
            ObjectName on = new ObjectName(":" + jndiName);
            Hashtable ht = on.getKeyPropertyList();
            if (ht.containsKey("guiport")) {
                this.url = "http://" + ht.get("host") + ":" + ht.get("guiport");
            } else {
                this.url = null;
            }
            this.name = jndiName; 
            this.jmxport = (String)ht.get("jmxport");
            if (this.jmxport == null) {
                throw new NullPointerException("No jmx port in " + jndiName);
            }
            this.host = (String)ht.get("host");
            if (this.host == null) {
                throw new NullPointerException("No host in " + jndiName);
            }
        }

        public String getUrl() {
            return this.url;
        }

        public String getMBeanName() {
            return DOMAIN + ":" + this.name;
        }

        public String getJmxport() {
            return this.jmxport;
        }

        public String getHost() {
            return this.host;
        }

        public String getJmxHostPort() {
            return getHost() + ":" + getJmxport();
        }
    }
%>
<%
    // Get all registered instances.  Keep around a hashtable. Also keep around
    // jndi environment and jndi name in namespace.
    Hashtable clusterInstances = null;
    String jndiNamespace = null;
    String jndiEnv = null;
    Context context = null;
    try {
        context = getHeritrixContext();
        clusterInstances = getInstances(context);
        jndiNamespace = context.getNameInNamespace();
        jndiEnv = context.getEnvironment().toString();
    } finally {
        if (context != null) {
            context.close();
        }
    }

    // Set defaults or set to result of any form handling.
    String clusterLogin = (String)session.getValue("clusterLogin");
    if (clusterLogin == null) {
        clusterLogin = "";
    }
    String clusterPassword = (String)session.getValue("clusterPassword");

    // Get a jmx client instance.
    Client client = (Client)application.getAttribute("jmxclient");
    if (client == null) {
        client = new Client();
    }
%>
