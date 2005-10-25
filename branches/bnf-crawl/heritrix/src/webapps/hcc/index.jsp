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
<%!
    // This section has code that needs to be moved out to java classes.
    //
    /**
     * @return The heritrix jndi subcontext (Be sure to close it when done).
     */
    private Context getHeritrixContext() throws NamingException {
        // Crawlers are always in the 'org.archive.crawler' context.
		final String subContext = "org.archive.crawler";
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
     * Parse of jndi name.
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

        public String getName() {
            return this.name;
        }

        public String getJmxport() {
            return this.jmxport;
        }

        public String getHost() {
            return this.host;
        }
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
%>
<%
    // Get all registered instances.  Keep around the list. Also keep around
    // environment and name in namespace.
    Hashtable instances = null;
    String nameInNamespace = null;
    String jndiEnv = null;
    Context context = null;
    try {
        context = getHeritrixContext();
        instances = getInstances(context);
        nameInNamespace = context.getNameInNamespace();
        jndiEnv = context.getEnvironment().toString();
    } finally {
        if (context != null) {
            context.close();
        }
    }

    // Set defaults or set to result of form handling.
    String clusterLogin = (String)session.getValue("clusterLogin");
    if (clusterLogin == null) {
        clusterLogin = "";
    }
    String clusterPassword = (String)session.getValue("clusterPassword");
%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
	<head>
		<title>Heritrix Cluster Controller</title>
		<meta name="author" content="Stack" />
		<meta name="description" content="Heritrix Cluster Controller Home" />
		<meta http-equiv="content-type" content="text/html; charset=UTF-8" />
		<meta http-equiv="Content-Script-Type" content="text/javascript" />
		<meta http-equiv="Content-Style-Type" content="text/css" />
	</head>
	<body>
        <p><img src="./images/logo.jpg" width="80" height="45"
            alt="Heritrix Cluster Controller Logo" />Heritrix instances
            <b>registered</b> on the subcontext <i><%=nameInNamespace%></i>.
            JNDI service provider detail: <i><%=jndiEnv%></i>.</p>

        <%
            // Enumerate crawlers.
            if (instances.size() <= 0) {
                %> <p>No instances registered.</p> <%
            } else {
                // Draw list opener.
                %> <ol> <%
                for (final Iterator i = instances.keySet().iterator();
                            i.hasNext();) {
                    Object key = i.next();
                    HeritrixJndiNameParse p =
                        (HeritrixJndiNameParse)instances.get(key);
                    %> <li> <%
                        // Only surround with href is we have an url. Otherwise
                        // just draw name of instance.
                        // TODO: If multiple hertrice in one JVM instance, need
                        // to add something that will trigger the UI to point
                        // at the selected instance (i.e. go via
                        // local-instances.jsp).
                        if (p.getUrl() != null) {
                            %><a href="<%=p.getUrl()%>"><%=p.getName()%></a><%
                        } else {
                            out.print(p.getName());
                        }
                    %> </li><%
                }
                // Close out the list.
                %> </ol> <%
            }
	    %>
        <p><a href="about.html">About HCC</a>.</p> 
        <hr />
        <p>Provide a <b>login</b> for the cluster. Assumption is that all
        cluster members share the same login and password. Submitting
        writes the login and password into your session.</p>
        <form method="post" action="form-handler.jsp"
                enctype="application/x-www-form-urlencoded">
            <p>Cluster login: <input name="login" maxlength="32" type="text"
                value="<%=clusterLogin%>" />
            Cluster password: <input name="password" maxlength="32"
                type="password" value="<%=clusterPassword%>"/>
            <input name="cluster-login-submit" type="submit" value="Login" />
            </p>
        </form>
        <hr />
	</body>
</html>
