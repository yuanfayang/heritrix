/* CheckpointObjectInputStream
*
* $Id$
*
* Created on Apr 28, 2004
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
package org.archive.crawler.checkpoint;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.Iterator;
import java.util.LinkedList;

import org.archive.util.FileUtils;


/**
 * @author gojomo
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class ObjectPlusFilesInputStream extends ObjectInputStream {
    LinkedList auxiliaryDirectoryStack = new LinkedList();
    LinkedList postRestoreTasks = new LinkedList();
    
    /**
     * @throws IOException
     * @throws SecurityException
     */
    protected ObjectPlusFilesInputStream() throws IOException, SecurityException {
        super();
        // TODO Auto-generated constructor stub
    }
    
    /**
     * @param in
     * @throws java.io.IOException
     */
    public ObjectPlusFilesInputStream(InputStream in) throws IOException {
        super(in);
        // TODO Auto-generated constructor stub
    }
    
    /**
     * @param dir
     */
    public void pushAuxiliaryDirectory(File dir) {
        auxiliaryDirectoryStack.addFirst(dir);
    }
    
    /**
     * 
     */
    public void popAuxiliaryDirectory() {
        auxiliaryDirectoryStack.removeFirst();
    }
    
    /**
     * @return
     */
    public File getAuxiliaryDirectory() {
        return (File)auxiliaryDirectoryStack.getFirst();
    }

    /**
     * @param destination
     * @throws IOException
     */
    public void restoreFile(File destination) throws IOException {
        String nameAsStored = readUTF();
        long lengthAtStoreTime = readLong();
        File storedFile = new File(getAuxiliaryDirectory(),nameAsStored);
        FileUtils.copyFile(storedFile,destination,lengthAtStoreTime);
    }

    /**
     * register a task to be done 
     * @param task
     */
    public void registerFinishTask(Runnable task) {
        postRestoreTasks.addFirst(task);
    }
    
    private void doFinishTasks() {
    	Iterator iter = postRestoreTasks.iterator();
    	while(iter.hasNext()) {
    		((Runnable)iter.next()).run();   
        }
    }
    
    /** (non-Javadoc)
     * @see java.io.InputStream#close()
     */
    public void close() throws IOException {
        super.close();
        doFinishTasks();
    }
}
