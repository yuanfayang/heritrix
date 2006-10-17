package org.archive.configuration.registry;

import java.io.IOException;
import java.util.HashMap;

import javax.management.AttributeNotFoundException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;

import org.archive.configuration.Configuration;
import org.archive.configuration.ConfigurationException;
import org.archive.configuration.Registry;

import junit.framework.TestCase;

public class PTest extends TestCase {
    private Registry r = null;
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        
        // Try and do the most basic possible registry.  Do a Registry
        // that wraps a HashMap.
        this.r = new Registry() {
            HashMap<ObjectName, Configuration> m =
                new HashMap<ObjectName, Configuration>();

            public Object get(String attributeName, String componentName)
            throws AttributeNotFoundException {
                return get(attributeName, componentName);
            }

            public Object get(String attributeName, String componentName,
                    Class<?> componentType) throws AttributeNotFoundException {
                return get(attributeName, componentName, componentType, null);
            }

            public Object get(String attributeName, String componentName,
                    Class<?> componentType, String domain)
            throws AttributeNotFoundException {
                return null; // TODO
            }

            public boolean isRegistered(String componentName,
                    Class<?> componentType) {
                return isRegistered(componentName, componentType, null);
            }

            public boolean isRegistered(String componentName,
                    Class<?> componentType, String domain) {
                // TODO
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

            public void load(String domain) throws IOException {
                // There is no load in this case.
            }
            
            public void save(String domain) throws IOException {
                // There is no save in this case.
            }
            
            public void deRegister(Object obj) {
                ObjectName on = ((ObjectInstance)obj).getObjectName();
                if (this.m.containsKey(on)) {
                    this.m.remove(on);
                }
            }
            
            public void deRegister(String componentName,
                    Class<?> componentType) {
                deRegister(componentName, componentType, null);
            }

            public void deRegister(String componentName, Class<?> componentType,
                    String domain) {
                
            }
        };
    }
    
    public void testWhatHappens() throws Exception {
        P p = new P("P");
        p.initialize(this.r);
        Configuration c = p.getConfiguration();
    }
}
