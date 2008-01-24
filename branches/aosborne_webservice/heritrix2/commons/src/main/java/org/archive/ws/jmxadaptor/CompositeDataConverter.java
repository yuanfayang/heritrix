package org.archive.ws.jmxadaptor;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

/**
 * An XStream converter for CompositeData open MBean types.
 */
public class CompositeDataConverter implements Converter {
    public boolean canConvert(Class clazz) {
        return CompositeData.class.isAssignableFrom(clazz)
                || CompositeDataSupport.class.equals(clazz);
    }

    public void marshal(Object object, HierarchicalStreamWriter writer,
            MarshallingContext context) {
        CompositeData data = (CompositeData) object;
        writer.addAttribute("type", data.getCompositeType().getTypeName());

        for (Object keyObj : data.getCompositeType().keySet()) {
            String key = (String) keyObj;
            Object value = data.get(key);
            writer.startNode(key);
            if (value == null) {
                writer.startNode("null");
                writer.endNode();
            } else {
                context.convertAnother(value);
            }
            writer.endNode();
        }
    }

    public Object unmarshal(HierarchicalStreamReader reader,
            UnmarshallingContext context) {
        // TODO
        return null;
    }

}
