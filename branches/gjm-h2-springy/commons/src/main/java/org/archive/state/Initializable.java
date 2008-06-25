/* 
 * Copyright (C) 2007 Internet Archive.
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
 *
 * Initializable.java
 *
 * Created on Mar 16, 2007
 *
 * $Id:$
 */
package org.archive.state;

import org.springframework.beans.factory.InitializingBean;

/**
 * Implemented by modules that have custom initialization needs.  A Module that
 * implements this interface should have its {@link #initialTasks} method 
 * invoked after its settings have been applied to a {@link StateProvider} 
 * that represents the global context. 
 * 
 * <p>For instance, {@link FileModule} is Initializable.  If you wanted to 
 * manually construct two FileModules representing <code>/tmp</code> 
 * and <code>/tmp/foo.txt</code>, you would have to:
 * 
 * <pre>
 * DefaultStateProvider state = new DefaultStateProvider();
 * FileModule parent = new FileModule();
 * sp.set(parent, FileModule.PARENT, null);
 * sp.set(parent, FileModule.PATH, "/tmp");
 * parent.initialTasks(state);
 * 
 * FileModule file = new FileModule();
 * sp.set(file, FileModule.PARENT, parent);
 * sp.set(file, FileModule.PATH, "foo.txt");
 * parent.initialTasks(state);
 * </pre>
 * 
 * <p>The {@link org.archive.settings} system would automate the above steps as
 * the modules were read from a configuration file (a sheet).
 * 
 * <p>Frequently this interface is implemented to allow {@link Immutable} key
 * fields to be cached in Java instance fields.  FileModule implements
 * Initializable for exactly that purpose:
 * 
 * <pre>
 * public class FileModule implements Initializable ... {
 * 
 *     final public static Key&lt;FileModule&gt; PARENT = ...; 
 *     final public static Key&lt;FileModule&gt; PATH = ...; 
 * 
 *     private FileModule parent;
 *     private String path;
 *     
 *     public FileModule() {}
 *     
 *     public void intialTasks(StateProvider sp) {
 *         this.parent = sp.get(this, PARENT);
 *         this.path = sp.get(this, PATH);
 *     }
 * 
 * }
 * </pre>
 * 
 * <p>It is wise to cache Immutable fields in this manner for two reasons:
 * 
 * <ol>
 * <li>The {@link StateProvider#get} operation is potentially expensive.</li>
 * <li>It may be desirable to use configuration values as instance fields for
 * serialization purposes.</li>
 * </ol>
 * 
 * @author pjack
 */
public interface Initializable extends InitializingBean {
    
    
    /**
     * Initialize this module based on global configuration.
     */
    void afterPropertiesSet();

}
