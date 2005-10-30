/* LinkedQueueTest
*
* $Id$
*
* Created on Oct 29, 2005.
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
package org.archive.queue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Iterator;

import org.archive.util.TmpDirTestCase;

/**
 * @author stack
 */
public class LinkedQueueTest extends TmpDirTestCase {
    public void testSerialization()
    throws IOException, ClassNotFoundException, InterruptedException {
        LinkedQueue q = new LinkedQueue();
        byte [] b = new byte[1];
        b[0] = ' ';
        for (int i = 32 /*ASCII space.*/; i < 128 /*Last ASCII pos.*/; i++) {
            (b[0])++;
            q.put(new String(b));
        }
        File f = new File(getTmpDir(), this.getClass().getName() + ".ser");
        ObjectOutputStream oos =
            new ObjectOutputStream(new FileOutputStream(f));
        try {
            printContent(q);
            oos.writeObject(q);
        } finally {
            oos.close();
        }
        ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f));
        try {
            printContent((LinkedQueue)ois.readObject());
        } finally {
            ois.close();
        }
    }

    private void printContent(final LinkedQueue lq) {
        System.out.print("Count: " + Integer.toString(lq.count) + " ");
        for (final Iterator i = lq.iterator(); i.hasNext();) {
            System.out.print((i.next()).toString());
        }
        System.out.println();
    }
}
