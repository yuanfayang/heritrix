package org.archive.configuration.registry;

import java.util.List;

import javax.management.Attribute;
import javax.management.AttributeNotFoundException;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.ReflectionException;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenMBeanAttributeInfo;
import javax.management.openmbean.OpenMBeanAttributeInfoSupport;
import javax.management.openmbean.SimpleType;

import org.archive.configuration.Configurable;
import org.archive.configuration.Configuration;
import org.archive.configuration.ConfigurationException;
import org.archive.configuration.Registry;

public class CrawlOrder implements Configurable {
    private final String name;
    
    public static final String ATTR_SETTINGS_DIRECTORY =
        "settings-directory";
    
    private CrawlOrder() {
        this(null);
    }
    
    public CrawlOrder(final String n) {
        super();
        this.name = n;
    }

    public Configurable initialize(final Registry r)
    throws ConfigurationException {
        return null;
    }

    public synchronized Configuration getConfiguration()
    throws ConfigurationException {
        return new CrawlOrderConfiguration("Heritrix crawl order. " +
            "This forms the root of the settings framework.");
    }
    
    public String getName() {
        return this.name;
    }
    
    @SuppressWarnings("serial")
    private static class CrawlOrderConfiguration extends Configuration {
        public CrawlOrderConfiguration(String description)
        throws ConfigurationException {
            super(description);
        }
        
        protected void initialize()
        throws AttributeNotFoundException, InvalidAttributeValueException,
                MBeanException, ReflectionException {
            
            setAttribute(new Attribute(ATTR_EXPERT,
                new String[] {ATTR_SETTINGS_DIRECTORY}));
            setAttribute(new Attribute(ATTR_NO_OVERRIDE,
                new String[] {ATTR_SETTINGS_DIRECTORY}));
        }
        
        @Override
        protected List<OpenMBeanAttributeInfo> addAttributeInfos(
                List<OpenMBeanAttributeInfo> infos)
        throws OpenDataException {
            infos = super.addAttributeInfos(infos);
            infos.add(new OpenMBeanAttributeInfoSupport(
                ATTR_SETTINGS_DIRECTORY,
                "Directory where override settings are kept. The settings " +
                "for many modules can be overridden based on the domain or " +
                "subdomain of the URI being processed. This setting specifies" +
                " a file level directory to store those settings. The path" +
                " is relative to 'disk-path' unless" +
                " an absolute path is provided.",
                SimpleType.STRING, true, true, false, "settings"));
            return infos;
        }
    }
}
