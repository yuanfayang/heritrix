package org.archive.configuration;

import java.io.IOException;
import java.util.Iterator;

public interface Store {
    /**
     * System property key of where on disk to persist on disk.
     */
    public static final String STORE_DIR_KEY =
        "org.archive.configuration.store.dir";
  
    public void save(final Iterator<StoreElement> i)
    throws IOException;
    public Iterator<StoreElement> load()
    throws IOException;
    public Iterator<StoreElement> load(final String domain)
    throws IOException;
}
