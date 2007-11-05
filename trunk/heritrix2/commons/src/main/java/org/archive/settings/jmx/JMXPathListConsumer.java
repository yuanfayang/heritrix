package org.archive.settings.jmx;

import java.util.ArrayList;
import java.util.List;

import javax.management.openmbean.CompositeData;

import org.archive.settings.path.StringPathListConsumer;

public class JMXPathListConsumer extends StringPathListConsumer {

    final private List<CompositeData> list = new ArrayList<CompositeData>();

    
    public CompositeData[] getData() {
        return list.toArray(new CompositeData[list.size()]);
    }
    
    public void consume(String path, String[] sheets, String value, String type) {
        CompositeData cd = Types.composite(Types.GET_DATA, 
                new String[] { "path", "sheets", "value", "type" },
                new Object[] { path, sheets, value, type });
        list.add(cd);
    }


}
