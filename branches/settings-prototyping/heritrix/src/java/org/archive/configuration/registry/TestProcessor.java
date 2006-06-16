package org.archive.configuration.registry;

import javax.management.Attribute;
import javax.management.AttributeNotFoundException;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanException;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenMBeanAttributeInfoSupport;
import javax.management.openmbean.SimpleType;

import org.archive.configuration.Configurable;
import org.archive.configuration.Configuration;
import org.archive.configuration.ConfigurationException;
import org.archive.configuration.Pointer;
import org.archive.configuration.Registry;

/**
 * Empty processor.
 * Used by tests.
 * @author stack
 */
public class TestProcessor implements Configurable {
    public static final String BOOLEAN_ATTRIBUTE_NAME = "george";
    public static final String ARRAY_ATTRIBUTE_NAME = "deseree";
    public static final String PROCESSOR_PTR_ATTRIBUTE_NAME = "ptrs";
    private final String name;
    
    public TestProcessor(final String n) {
        this.name = n;
    }
    
    public Configurable initialize(Registry r)
    throws ConfigurationException {
        return this;
    }

     public synchronized Configuration getConfiguration()
            throws ConfigurationException {
        Configuration configuration = new Configuration("Test processor.") {
            protected java.util.List<javax.management.openmbean.OpenMBeanAttributeInfo> addAttributeInfos(
                    java.util.List<javax.management.openmbean.OpenMBeanAttributeInfo> infos)
                    throws javax.management.openmbean.OpenDataException {
                infos = super.addAttributeInfos(infos);
                infos.add(new OpenMBeanAttributeInfoSupport(BOOLEAN_ATTRIBUTE_NAME,
                    "Enabled if true xxxxxxxxxxx",
                    SimpleType.BOOLEAN, true, true, true,
                    Boolean.TRUE, TRUE_FALSE_LEGAL_VALUES));
                infos.add(new OpenMBeanAttributeInfoSupport(ARRAY_ATTRIBUTE_NAME,
                    "Test adding an array", Configuration.STR_ARRAY_TYPE,
                    true, true, false));
                infos.add(new OpenMBeanAttributeInfoSupport(
                    PROCESSOR_PTR_ATTRIBUTE_NAME,
                    "Test adding an array of ptrs",
                    Configuration.PTR_ARRAY_TYPE, true, true, false));
                return infos;
            }
        };

        // Set array and processor values.

        try {
            // Looks like arrays need to have values. TODO.
            configuration.setAttribute(new Attribute(ARRAY_ATTRIBUTE_NAME,
                new String[] {"HELLO", "GOODBYE"}));
            configuration.setAttribute(new Attribute(PROCESSOR_PTR_ATTRIBUTE_NAME,
                new Object[] {
                    new Pointer(new ObjectName(JmxRegistryTest.BASE_DOMAIN
                            + ":name=p1,type=" + this.getClass().getName())).getCompositeData(),
                    new Pointer(new ObjectName(JmxRegistryTest.BASE_DOMAIN
                            + ":name=p2,type=" + this.getClass().getName())).getCompositeData(),
                    new Pointer(new ObjectName(JmxRegistryTest.BASE_DOMAIN
                            + ":name=p3,type=" + this.getClass().getName())).getCompositeData() }));
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
        }
        return configuration;
    }

    public String getName() {
        // TODO Auto-generated method stub
        return null;
    }
}
