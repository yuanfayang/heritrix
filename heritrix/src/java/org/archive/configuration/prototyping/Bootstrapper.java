package org.archive.configuration.prototyping;

import java.io.IOException;

import javax.management.Attribute;
import javax.management.AttributeNotFoundException;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanException;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenMBeanAttributeInfoSupport;
import javax.management.openmbean.SimpleType;

import org.archive.configuration.Configurable;
import org.archive.configuration.Configuration;
import org.archive.configuration.ConfigurationException;
import org.archive.configuration.Pointer;
import org.archive.configuration.Registry;


@SuppressWarnings("serial")
public class Bootstrapper implements Configurable {
    /**
     * Registry implementation.  Read from config. file.
     */
    private static final String REGISTRY = JmxRegistry.class.getName();
    
    private static final String BASE_DOMAIN = "org.archive";
    
    private Registry registry = null;
    
    private final String name;
    
    public Bootstrapper(final String n) {
        super();
        this.name = n;
    }
    
    public Configurable initialize(final Registry r)
    throws ConfigurationException {
        this.registry = r;
        if (!this.registry.isRegistered(this.name, this.getClass().getName())) {
            this.registry.register(this.name, this.getClass().getName(),
            		getInitialConfiguration());
        } else {
        	// Its registered already. Get all config. now or
        	// just do get as I need config. during processing?
        	
        Object[] objs = null;
		try {
			objs = (Object[])this.registry.get("processors", this.name);
		} catch (AttributeNotFoundException e) {
			throw new ConfigurationException(e);
		}
        for (int i = 0; i < objs.length; i++) {
        	if (Pointer.isPointer((CompositeData)objs[i])) {
        		
        	}
        }
        }
        return this;
    }
    
    public Configuration getInitialConfiguration()
    throws ConfigurationException {
        Configuration c = new Configuration() {
        protected java.util.List<javax.management.openmbean.OpenMBeanAttributeInfo> addAttributeInfos(java.util.List<javax.management.openmbean.OpenMBeanAttributeInfo> infos)
        throws javax.management.openmbean.OpenDataException {
        	infos = super.addAttributeInfos(infos);
        	infos.add(new OpenMBeanAttributeInfoSupport("Two",
                "Enabled if true xxxxxxxxxxx", SimpleType.BOOLEAN,
                true, true, true, Boolean.TRUE, TRUE_FALSE_LEGAL_VALUES));
        	infos.add(new OpenMBeanAttributeInfoSupport("array",
        		"Test adding an array", Configuration.STR_ARRAY_TYPE,
        		true, true, false));
        	infos.add(new OpenMBeanAttributeInfoSupport("processors",
            		"Test adding an array of ptrs", Configuration.PTR_ARRAY_TYPE,
            		true, true, false));
            return infos;
        }
        };
        try {
        	// Looks like arrays need to have values.  TODO.
			c.setAttribute(new Attribute("array", new String [] {"HELLO"}));
			c.setAttribute(new Attribute("processors",
				new Object[] {
                new Pointer(new ObjectName(BASE_DOMAIN + ":name=p1")).getCompositeData(),
                new Pointer(new ObjectName(BASE_DOMAIN + ":name=p2")).getCompositeData(),
                new Pointer(new ObjectName(BASE_DOMAIN + ":name=p3")).getCompositeData()}));
		} catch (AttributeNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidAttributeValueException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MBeanException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ReflectionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MalformedObjectNameException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (OpenDataException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NullPointerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	return c;
    }
    
    public boolean isEnabled() throws AttributeNotFoundException {
        return ((Boolean)this.registry.get("Enabled", this.name)).
            booleanValue();
    }
    
    public boolean isNew() throws AttributeNotFoundException {
        return ((Boolean)this.registry.get("Two", this.name)).
            booleanValue();
    }
    
    public static void main(String[] args)
    throws InstantiationException, IllegalAccessException,
            ClassNotFoundException, IOException, ConfigurationException,
            AttributeNotFoundException {
        Registry r = (Registry)Class.forName(REGISTRY).newInstance();
        r.load(BASE_DOMAIN);
        Bootstrapper b = new Bootstrapper("Bootstrapper");
        b.initialize(r);
        boolean x = b.isEnabled();
        x = b.isNew();
        r.save(BASE_DOMAIN);
    }
}