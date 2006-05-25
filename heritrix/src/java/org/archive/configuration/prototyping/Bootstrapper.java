package org.archive.configuration.prototyping;

import java.io.IOException;

import javax.management.openmbean.OpenMBeanAttributeInfoSupport;
import javax.management.openmbean.SimpleType;

import org.archive.configuration.Configurable;
import org.archive.configuration.Configuration;
import org.archive.configuration.ConfigurationException;
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
    
    /**
     * Shutdown constructor.
     */
    private Bootstrapper() {
        this(null);
    }
    
    public Bootstrapper(final String n) {
        super();
        this.name = n;
    }
    
    public void configure(final Registry r) throws ConfigurationException {
        this.registry = r;
        if (!this.registry.isRegistered(this.name)) {
            this.registry.register(this.name, getDefaultConfiguration());
        }
    }
    
    protected Configuration getDefaultConfiguration()
    throws ConfigurationException {
        return new Configuration() {
        protected java.util.List<javax.management.openmbean.OpenMBeanAttributeInfo> addAttributeInfos(java.util.List<javax.management.openmbean.OpenMBeanAttributeInfo> infos)
        throws javax.management.openmbean.OpenDataException {
        	infos = super.addAttributeInfos(infos);
        	infos.add(new OpenMBeanAttributeInfoSupport("Two",
                "Enabled if true xxxxxxxxxxx", SimpleType.BOOLEAN,
                true, true, true, Boolean.TRUE, TRUE_FALSE_LEGAL_VALUES));
            return infos;
        }
    };
    }
    
    public boolean isEnabled() {
        return ((Boolean)this.registry.get("Enabled", this.name)).
            booleanValue();
    }
    
    public boolean isNew() {
        return ((Boolean)this.registry.get("Two", this.name)).
            booleanValue();
    }
    
    public static void main(String[] args)
    throws InstantiationException, IllegalAccessException,
            ClassNotFoundException, IOException, ConfigurationException {
        Registry r = (Registry)Class.forName(REGISTRY).newInstance();
        r.load(BASE_DOMAIN);
        Bootstrapper b = new Bootstrapper("Bootstrapper");
        b.configure(r);
        boolean x = b.isEnabled();
        x = b.isNew();
        r.save(BASE_DOMAIN);
    }
}
