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
    
    private char sheetsDelim = '\t';
    private char pathDelim = '=';
    private char typeDelim = ':';
    private boolean includeSheets = false;
    
    public FilePathListConsumer(Writer writer) {
        this.writer = writer;
    }
    
    
    public void setIncludeSheets(boolean b) {
        this.includeSheets = b;
    }
    
    public void setSheetsDelim(char ch) {
        this.sheetsDelim = ch;
    }
    
    
    public void setPathDelim(char ch) {
        this.pathDelim = ch;
    }
    
    
    public void setTypeDelim(char ch) {
        this.typeDelim = ch;
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
            sb.append("reference").append(typeDelim).append("null");
        } else if (KeyTypes.isSimple(value.getClass())) {
            String tag = KeyTypes.getSimpleTypeTag(value.getClass());
            String s = KeyTypes.toString(value);
            sb.append(tag).append(typeDelim).append(s);
        } else if (seen != null) {
            sb.append("reference").append(typeDelim).append(seen);
        } else {
            Class c = Offline.getType(value);
            sb.append("object").append(typeDelim).append(c.getName());
        }
        sb.append('\n');
        writer.write(sb.toString());
    }
    
    
    private void writeHeader(StringBuilder sb, String path, List<Sheet> sheets) {
        if (includeSheets) {
            sb.append(sheets.get(0).getName());
            for (int i = 1; i < sheets.size(); i++) {
                sb.append(',').append(sheets.get(i).getName());
            }
            sb.append(sheetsDelim);
        }
        sb.append(path).append(pathDelim);
    }

}
