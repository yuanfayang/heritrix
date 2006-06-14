package org.archive.configuration.store;

import java.io.File;
import java.util.Iterator;

import javax.management.ObjectName;

import org.archive.configuration.Configuration;
import org.archive.configuration.Store;
import org.archive.configuration.StoreElement;

import junit.framework.TestCase;

public class SerializeStoreTest extends TestCase {
    private static final File TMPDIR = new File(
        System.getProperty(Store.STORE_DIR_KEY,
            System.getProperty("java.io.tmpdir", "/tmp")));
    
    private final File storeDir = new File(TMPDIR,
        this.getClass().getName());

    protected void setUp() throws Exception {
        super.setUp();
        storeDir.mkdirs();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }
    
    public void testReadWriteStore() throws Exception {
        SerializeStore ss = new SerializeStore(this.storeDir);
        final StoreElement se =
            new StoreElement(new Configuration("Some description") {},
                new ObjectName("org.archive:name=x,type=y"));
        
        ss.save(new Iterator<StoreElement> () {
                private boolean hasNext = true;
                public boolean hasNext() {
                return this.hasNext;
                }

                public StoreElement next() {
                    this.hasNext = false;
                    return se;
                }

                public void remove() {
                    throw new UnsupportedOperationException("Unimplemented");
                }
            }
        );
        
        Iterator<StoreElement> i = ss.load();
        i.hasNext();
        StoreElement seLoaded = i.next();
        assertEquals(se.getObjectName().getCanonicalName(),
            seLoaded.getObjectName().getCanonicalName());
    }
}
