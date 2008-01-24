package org.archive.ws.jmxadaptor;

import java.util.Set;
import java.util.TreeSet;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import mx4j.tools.adaptor.http.CommandProcessorUtil;
import mx4j.tools.adaptor.http.HttpInputStream;

/**
 * Convenience facade for querying an MBeanServer for object names.
 */
public class MBeanNameQuerier {
    protected MBeanServer server;
    
    public String patternStringForType(String type, String job) {
        String pattern = "*:*";
        
        if (type != null) {
            pattern += ",type=" + type;
        }
        
        if (job != null) {
            pattern += ",name=" + job;
        }
        return pattern;
    }
    
    public ObjectName patternByHttpVars(HttpInputStream in) throws MalformedObjectNameException, NullPointerException {
        String objName = in.getVariable("objectname");
        if (objName != null && !objName.equals("")) {
            return new ObjectName(objName);
        }
        
        objName = patternStringForType(in.getVariable("type"), in.getVariable("job"));
        return new ObjectName(objName);
    }
    
    public Set<ObjectName> namesByHttpVars(HttpInputStream in) throws MalformedObjectNameException {
        ObjectName objectName = patternByHttpVars(in);
        Set<ObjectName> names = new TreeSet<ObjectName>(CommandProcessorUtil.createObjectNameComparator());
        names.addAll(server.queryNames(objectName, null));
        return names;
    }

    public MBeanNameQuerier(MBeanServer server) {
        this.server = server;
    }

    /**
     * Set the target MBean server.
     */
    public void setMBeanServer(MBeanServer server) {
        this.server = server;
    }

}
