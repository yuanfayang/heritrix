package org.archive.settings.file;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

import org.archive.settings.Offline;
import org.archive.settings.Sheet;
import org.archive.settings.path.PathListConsumer;
import org.archive.state.KeyTypes;

public class FilePathListConsumer implements PathListConsumer {


    final Writer writer;
    
    
    public FilePathListConsumer(Writer writer) {
        this.writer = writer;
    }
    
    
    public void consume(String path, List<Sheet> sheets, Object value, 
            String seen) {
        try {
            consume2(path, sheets, value, seen);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
    
    private void consume2(String path, List<Sheet> sheets, Object value, 
            String seen) 
    throws IOException {
        StringBuilder sb = new StringBuilder();
        writeHeader(sb, path, sheets);
        if (value == null) {
            sb.append("reference:null");
        } else if (KeyTypes.isSimple(value.getClass())) {
            String tag = KeyTypes.getSimpleTypeTag(value.getClass());
            String s = KeyTypes.toString(value);
            sb.append(tag).append('\t').append(s);
        } else if (seen != null) {
            sb.append("reference").append('\t').append(seen);
        } else {
            Class c = Offline.getType(value);
            sb.append("object").append('\t').append(c.getName());
        }
        sb.append('\n');
        writer.write(sb.toString());
    }
    
    
    private void writeHeader(StringBuilder sb, String path, List<Sheet> sheets) {
        sb.append(sheets.get(0).getName());
        for (int i = 1; i < sheets.size(); i++) {
            sb.append(',').append(sheets.get(i).getName());
        }
        sb.append('\t').append(path).append('\t');
    }

}
