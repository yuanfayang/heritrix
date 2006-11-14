package org.archive.crawler2.settings.file;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

import org.archive.crawler2.settings.Sheet;
import org.archive.crawler2.settings.path.PathListConsumer;
import org.archive.crawler2.settings.path.Paths;

public class FilePathListConsumer implements PathListConsumer {


    final Writer writer;
    
    
    public FilePathListConsumer(Writer writer) {
        this.writer = writer;
    }
    
    
    public void consume(String path, List<Sheet> sheets, Object value) {
        try {
            consume2(path, sheets, value);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
    
    public void consume2(String path, List<Sheet> sheets, Object value) 
    throws IOException {
        if (Paths.isSimple(value.getClass())) {
            writer.write(path + "=" + value + '\n'); // FIXME: Escape special characters
        } else {
            writer.write(path + "._impl=" + value.getClass().getName() + '\n');
        }
    }

}
