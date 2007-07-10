package org.archive.settings.file;

import java.io.IOException;
import java.io.Writer;

import org.archive.settings.path.StringPathListConsumer;

public class FilePathListConsumer extends StringPathListConsumer {


    final Writer writer;
    
    private char sheetsDelim = '\t';
    private char pathDelim = '=';
    private String typeDelim = ", ";
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
    
    
    public void setTypeDelim(String d) {
        this.typeDelim = d;
    }
    
    
    @Override
    protected void consume(
            String path, 
            String[] sheets, 
            String value, 
            String type) {
        try {
            consume2(path, sheets, value, type);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
    
    private void consume2(
            String path, 
            String[] sheets, 
            String value, 
            String type) 
    throws IOException {
        StringBuilder sb = new StringBuilder();
        if (includeSheets) {
            sb.append(sheets[0]);
            for (int i = 1; i < sheets.length; i++) {
                sb.append(',').append(sheets[i]);
            }
            sb.append(sheetsDelim);
        }
        sb.append(path).append(pathDelim);
        sb.append(type).append(typeDelim);
        sb.append(value); // FIXME: Escape special characters
    }

}
