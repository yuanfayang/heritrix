package org.archive.settings.jmx;

import java.util.ArrayList;
import java.util.List;

import javax.management.openmbean.CompositeData;

import org.archive.settings.Offline;
import org.archive.settings.Sheet;
import org.archive.settings.path.PathListConsumer;
import org.archive.state.KeyTypes;

public class JMXPathListConsumer implements PathListConsumer {

    final private List<CompositeData> list = new ArrayList<CompositeData>();

    
    public CompositeData[] getData() {
        return list.toArray(new CompositeData[list.size()]);
    }
    
    public void consume(String path, List<Sheet> sheets, Object value,
            String seen) {
        String[] snames = new String[sheets.size()];
        for (int i = 0; i < snames.length; i++) {
            snames[i] = sheets.get(i).getName();
        }
        
        if (KeyTypes.isSimple(value.getClass())) {
            addSimple(path, snames, value);
        } else {
            addSimple(path, snames, Offline.getType(value).getName());
        }
    }
    
    
    private void addSimple(String path, String[] sheets, Object v) {
        CompositeData cd = Types.composite(Types.GET_DATA, 
                new String[] { "path", "sheets", "value" },
                new Object[] { path, sheets, v.toString() });
        list.add(cd);
    }


}
