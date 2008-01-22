/* Copyright (C) 2006 Internet Archive.
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
 * SheetFileReader.java
 * Created on October 24, 2006
 *
 * $Header: /cvsroot/archive-crawler/ArchiveOpenCrawler/src/java/org/archive/settings/file/Attic/SheetFileReader.java,v 1.1.2.2 2006/12/15 22:09:33 paul_jack Exp $
 */
package org.archive.settings.file;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;

import org.archive.settings.path.PathChange;
import org.archive.util.iterator.LineReadingIterator;
import org.archive.util.iterator.TransformingIteratorWrapper;

public class SheetFileReader 
extends TransformingIteratorWrapper<String,PathChange> {

    
    public SheetFileReader(InputStream input) {
        Reader r;
        try {
            r = new InputStreamReader(input, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError();
        }
        BufferedReader br = new BufferedReader(r);
        this.inner = new LineReadingIterator(br);
    }
    
    
    public PathChange transform(String line) {
        if (line.startsWith("#")) {
            return null;
        }
        
        if (line.trim().equals("")) {
            return null;
        }
        
        int p = line.indexOf('=');
        if (p < 0) {
            throw new IllegalStateException("Invalid line: " + line);
        }
        String path = line.substring(0,p);
        String value = line.substring(p + 1);
        
        p = value.indexOf(", ");
        if (p < 0) {
            throw new IllegalStateException("Invalid line: " + line);
        }
        
        String type = value.substring(0, p);
        value = value.substring(p + 2);
        return new PathChange(path, type, value);
    }
    
}
