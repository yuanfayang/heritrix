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
 * DirectoryModule.java
 *
 * Created on Mar 20, 2007
 *
 * $Id:$
 */

package org.archive.processors;

import java.io.File;

/**
 * @author pjack
 *
 */
public interface DirectoryModule {

    
    File getDirectory();
        
    
    /**
     * Makes a possibly relative path absolute.  If the given path is 
     * relative, then the returned string will be an absolute path 
     * relative to getDirectory().
     * 
     * <p>If the given path is already absolute, then it is returned unchanged.
     * 
     * @param path   the path to make absolute
     * @return   the absolute path
     */
    String toAbsolutePath(String path);

    
    /**
     * Makes a possibly absolute path relative.  If the given path is 
     * relative, then it is returned unchanged.  Otherwise, if the given 
     * absolute path represents a subdirectory of getDirectory, then 
     * the subdirectory path is returned as a relative path.
     * 
     * Eg, if getDirectory() returns <code>/foo/bar</code>, then:
     * 
     * <ul>
     * <li><code>toRelativePath("/foo/bar/baz/snafu")</code> should return
     * <codeE>baz/snafu</code>.</li>
     * <li><code>toRelativePath("/fnord/x")</code> should return 
     * <code>/fnord/x</code>.</li>
     * <li><code>toRelativePath("a/b/c")</code> should return <code>a/b/c</code>.
     * </ul>
     * 
     * @param path
     * @return
     */
    String toRelativePath(String path);

}
