package org.archive.crawler.writer;

import java.util.Collections;
import java.util.List;

public class EmptyMetadataProvider implements MetadataProvider {


    public List<String> getMetadata() {
        return Collections.emptyList();
    }
    
    
}
