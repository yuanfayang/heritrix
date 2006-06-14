package org.archive.configuration.store;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Iterator;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.MBeanInfo;
import javax.management.ObjectName;

import org.archive.configuration.Configuration;
import org.archive.configuration.Store;
import org.archive.configuration.StoreElement;

public class SerializeStore implements Store {
    private final File storeDir;
    
    /**
     * Shutdown constructor.
     */
    private SerializeStore() {
        this((File)null);
    }
    
    public SerializeStore(final File sd) {
        this.storeDir = sd;
    }

    public void save(final Iterator<StoreElement> i)
    throws IOException {
        while (i.hasNext()) {
            StoreElement se = i.next();
            AttributeList al = se.getConfiguration().getAttributes();
            for (final Iterator k = al.iterator(); k.hasNext();) {
                Attribute a = (Attribute) k.next();
                System.out.println(a.getName() + " " + a.getValue());
            }
            File f = new File(this.storeDir,
                se.getObjectName().getCanonicalName());
            if (f.exists()) {
                f.delete();
            }
            ObjectOutputStream oos =
                new ObjectOutputStream(new FileOutputStream(f));
            try {
                oos.writeObject(se.getObjectName());
                oos.writeObject(se.getConfiguration().getMBeanInfo());
                oos.writeObject(al);
            } finally {
                oos.close();
            }
        }
    }
    
    public Iterator<StoreElement> load()
    throws IOException {
        // TODO: For now, just load up all the files in this.storeDir.
        File [] files = this.storeDir.listFiles();
        ArrayList<StoreElement> al =
            new ArrayList<StoreElement>(files.length);
        for (int i = 0; i < files.length; i++) {
            al.add(load(files[i]));
        }
        return al.iterator();
    }

    public Iterator<StoreElement> load(
        @SuppressWarnings("unused") String domain)
    throws IOException {
        throw new IOException("Unimplemented");
    }
            
    protected synchronized StoreElement load(final File f)
    throws IOException {
        ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f));
        ObjectName on = null;
        Configuration configuration = null;
        try {
            // File has two objects: The ObjectName and the Configuration
            // AttributeList.
            on = (ObjectName) ois.readObject();
            MBeanInfo mi = (MBeanInfo)ois.readObject();
            AttributeList al = (AttributeList) ois.readObject();
            // Configuration is abstract.  Change that?
            configuration = new Configuration(mi) {};
            configuration.setAttributes(al);
        } catch (ClassNotFoundException e) {
            throw new IOException(e.toString()); 
        } catch (SecurityException e) {
            throw new IOException(e.toString());
        }
        return new StoreElement(configuration, on);
    }
}
