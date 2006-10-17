package org.archive.configuration.registry;

import java.io.IOException;
import java.util.Hashtable;
import java.util.Map;

import javax.management.AttributeNotFoundException;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.archive.configuration.Configuration;
import org.archive.configuration.ConfigurationException;
import org.archive.configuration.Registry;

/**
 * Registry implementation based on Map.
 * Pass Map-to-wrap into Constructor.  Passed Map is keyed by Component
 * name.  Value is a Map that has in it all attributes for a particular
 * Component.
 * @author stack
 */
public class MapRegistry implements Registry {
    private final Map<ObjectName, Map> m;
    private final static String DEFAULT_DOMAIN = "";
    
    public MapRegistry(final Map<ObjectName, Map> m) {
        this.m = m;
    }
    
    public Object get(String attributeName, String componentName)
            throws AttributeNotFoundException {
        return get(attributeName, componentName, null);
    }

    public Object get(String attributeName, String componentName,
            Class<?> componentType) throws AttributeNotFoundException {
        return get(attributeName, componentName, componentType, null);
    }

    public Object get(String attributeName, String componentName,
            Class<?> componentType, String domain)
    throws AttributeNotFoundException {
        ObjectName on = getObjectName(componentName, componentType, domain);
        if (on == null) {
            throw new AttributeNotFoundException("Failed get of ObjectName " +
                "for: " + attributeName + "...etc");
        }
        return this.m.get(on).get(attributeName);
    }
    
    private ObjectName getObjectName(String name,
            Class<?> componentType, String domain) {
        Hashtable<String, String> ht = new Hashtable<String, String>(2);
        ht.put(Configuration.NAME_KEY, name);
        if (componentType != null) {
            ht.put(Configuration.TYPE_KEY, componentType.getName());
        }
        ObjectName on = null;
        try {
            on = new ObjectName(domain == null? DEFAULT_DOMAIN: domain, ht);
        } catch (MalformedObjectNameException e) {
            e.printStackTrace();
        }
        return on;
    }
    

    public boolean isRegistered(String componentName, Class<?> componentType) {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isRegistered(String componentName, Class<?> componentType,
            String domain) {
        // TODO Auto-generated method stub
        return false;
    }

    public Object register(String componentName, Class<?> componentType,
            Configuration instance) throws ConfigurationException {
        // TODO Auto-generated method stub
        return null;
    }

    public Object register(String componentName, Class<?> componentType,
            String domain, Configuration instance)
            throws ConfigurationException {
        // TODO Auto-generated method stub
        return null;
    }

    public void deRegister(Object obj) {
        // TODO Auto-generated method stub

    }

    public void deRegister(String componentName, Class<?> componentType) {
        // TODO Auto-generated method stub

    }

    public void deRegister(String componentName, Class<?> componentType,
            String domain) {
        // TODO Auto-generated method stub

    }
    
    public void load(String domain) throws IOException {
        // TODO Auto-generated method stub

    }
    
    public void save(String domain) throws IOException {
        // TODO Auto-generated method stub

    }
}