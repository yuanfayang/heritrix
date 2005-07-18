/* IoUtils
 * 
 * $Id$
 *
 * Created on Jun 9, 2005
 * 
 * Copyright (C) 2003 Internet Archive.
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
 */
package org.archive.crawler.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import org.archive.net.UURIFactory;

/**
 * Logging utils.
 * @author stack
 */
public class IoUtils {
    public static InputStream getInputStream(String pathOrUrl) {
        return getInputStream(null, pathOrUrl);
    }
    
    /**
     * Get inputstream.
     * 
     * This method looks at passed string and tries to judge it a
     * filesystem path or an URL.  It then gets an InputStream on to
     * the file or URL.
     * 
     * <p>ASSUMPTION: Scheme on any url will probably only ever be 'file' 
     * or 'http'.
     * 
     * @param basedir If passed <code>fileOrUrl</code> is a file path and
     * it is not absolute, prefix with this basedir (May be null then
     * no prefixing will be done).
     * @param pathOrUrl Pass path to a file on disk or pass in a URL.
     * @return An input stream.
     */
    public static InputStream getInputStream(File basedir, String pathOrUrl) {
        InputStream is = null;
        if (UURIFactory.hasSupportedScheme(pathOrUrl)) {
            try {
                URL url = new URL(pathOrUrl);
                is = url.openStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            // Assume its not an URI or we failed the parse.
            // Try it as a file.
            File source = new File(pathOrUrl);
            if (!source.isAbsolute() && basedir != null) {
                source = new File(basedir, pathOrUrl);
            }
            try {
                is = new FileInputStream(source);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        return is;
    }
    
    /**
     * Takes a path or url string and returns a File object for a local
     * resource.  If passed <code>pathOrUrl</code> is judged remote
     * URL, we return a pointer to a local copy of the URL resource.
     * @param basedir If passed <code>fileOrUrl</code> is a file path and
     * it is not absolute, prefix with this basedir (May be null then
     * no prefixing will be done).
     * @param tmpdir If remote url -- not a file url -- then the directory
     * in which to store the local copy of the remote resource.
     * @param pathOrUrl Pass path to a file on disk or pass in a URL.
     * @return A File.
     */
    public static File getLocalFile(File basedir, File tmpdir, String pathOrUrl) {
        File result = null;
        if (UURIFactory.hasScheme(pathOrUrl)) {
            try {
                URL url = new URL(pathOrUrl);
                if (url.getProtocol().equals("file")) {
                    // Assume local reference.
                    result = new File(url.getPath());
                } else {
                    // Assume remote.  Bring a copy local.
                    throw new RuntimeException("Unimplemented");
                }
            } catch (MalformedURLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } else {
            // Assume its not an URI or we failed the parse.
            // Try it as a file.
            result = new File(pathOrUrl);
            if (!result.isAbsolute() && basedir != null) {
                result = new File(basedir, pathOrUrl);
            }
        }
        return result;
    }
}
