/* CrawlURITest
 * 
 * Created on Jul 26, 2004
 *
 * Copyright (C) 2004 Internet Archive.
 * 
 * This file is part of the Heritrix web crawler (crawler.archive.org).
 * 
 * Heritrix is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * any later version.
 * 
 * Heritrix is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser Public License
 * along with Heritrix; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.archive.crawler.datamodel;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.archive.util.TmpDirTestCase;

/**
 * @author stack
 * @version $Revision$, $Date$
 */
public class CrawlURITest extends TmpDirTestCase {

    /**
     * Test serialization/deserialization works.
     * 
     * @throws IOException
     * @throws ClassNotFoundException
     */
    final public void testSerialization()
    		throws IOException, ClassNotFoundException {
        UURI uuri = UURIFactory.getInstance("http://www.dh.gov.uk/Home/fs/en");
        CrawlURI curi = new CrawlURI(uuri);
        curi.schedulingDirective = "Medium";
        curi.setIsSeed(true);
        // Force calc. of class key.
        curi.getClassKey();
        curi.setVia(uuri);
        // Write out the object.
        File serialize = new File(getTmpDir(), this.getClass().getName());
        FileOutputStream fos = new FileOutputStream(serialize);
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        oos.writeObject(curi);
        oos.close();
        // Read in the object.
        FileInputStream fis = new FileInputStream(serialize);
        ObjectInputStream ois = new ObjectInputStream(fis);
        CrawlURI deserializedCuri = (CrawlURI)ois.readObject();
        assertTrue("Deserialized not equal to original",
            curi.toString().equals(deserializedCuri.toString()));
    }
    
    /**
     * Test serialization into and out of a disk-backed queue.
     */
    
}
