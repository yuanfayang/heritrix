package org.archive.ws.jmxadaptor;

import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;
import javax.management.openmbean.TabularType;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

/**
 * An XStream converter for open MBean TabularData types.
 */
public class TabularDataConverter implements Converter {

    public boolean canConvert(Class clazz) {
        return TabularData.class.isAssignableFrom(clazz) || TabularDataSupport.class.equals(clazz);
    }

    public void marshal(Object object, HierarchicalStreamWriter writer,
            MarshallingContext context) {
        TabularData data = (TabularData) object;
        TabularType type = data.getTabularType();
        
        writer.addAttribute("type", type.getTypeName());

        for (Object value : data.values()) {
            writer.startNode("row");
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
