package org.archive.ws.jmxadaptor;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import mx4j.tools.adaptor.http.HttpAdaptor;

/**
 * Web service adaptor for JMX to allow low-level control.  We extend MX4J's 
 * HTTP adaptor to add open mbean serialization and some convenience methods.
 */
public class JmxAdaptor extends HttpAdaptor {

    @Override
    public ObjectName preRegister(MBeanServer server, ObjectName name)
            throws Exception {
        ObjectName result = super.preRegister(server, name);
        
        // Register our replacement command processors.
        removeCommandProcessor("mbean");
        addCommandProcessor("mbean", "org.archive.ws.jmxadaptor.MBeanCommandProcessor");
        return result;
    }

    public JmxAdaptor() {
        super();
    }

    public JmxAdaptor(int port, String host) {
        super(port, host);
    }

    public JmxAdaptor(int port) {
        super(port);
    }

    public JmxAdaptor(String host) {
        super(host);
    }

}
