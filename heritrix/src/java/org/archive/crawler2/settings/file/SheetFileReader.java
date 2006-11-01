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
 * $Header$
 */
package org.archive.crawler2.settings.file;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.archive.crawler2.settings.SingleSheet;
import org.archive.crawler2.settings.path.PathValidator;
import org.archive.openmbeans.annotations.OpenTypes;
import org.archive.state.Key;
import org.archive.state.KeyManager;

public class SheetFileReader {

    
    final private BufferedReader reader;
    final private SingleSheet sheet;
    
    public SheetFileReader(SingleSheet sheet, BufferedReader br) {
        this.sheet = sheet;
        this.reader = br;
    }
    
    
    public void read() throws IOException {
        for (String s = reader.readLine(); s != null; s = reader.readLine()) {
            if (!s.startsWith("#")) {
                processLine(s);
            }
        }
    }
    
    
    private void processLine(String line) throws IOException {
        System.out.println(line);
        int p = line.indexOf('=');
        if (p < 0) {
            throw new IOException("Can't parse " + line);
        }
        String path = line.substring(0, p).trim();
        String value = line.substring(p + 1);
        if (path.endsWith("._impl")) {
            processObject(path, value);
            return;
        }
        
        p = path.lastIndexOf('.');
        if (p < 0) {
            throw new IOException("Root objects can't be primitive: " + line);
        }
        String previous = path.substring(0, p);
        String lastToken = path.substring(p + 1);
        Object processor = PathValidator.validate(sheet, previous);
        Map<String,Key<Object>> keys = KeyManager.getKeys(processor.getClass());
        Key<Object> key = keys.get(lastToken);
        if (key == null) {
            throw new IOException("No such key: " + lastToken);
        }
        Object v;
        try {
            v = OpenTypes.parse(key.getType(), value);
        } catch (Exception e) {
            throw new IOException("Could not parse value: " + line);
        }
        sheet.set(processor, key, v);
    }
    
    
    private void processObject(String path, String className)
    throws IOException {
        // Instantiate the new object.
        // FIXME: Somehow allow constructors?
        className = className.trim();
        Object object;
        try {
            object = Class.forName(className).newInstance();
        } catch (Exception e) {
            IOException io = new IOException();
            io.initCause(e);
            throw io;
        }

        // Now we need to figure out where to store the object.
        // There are three possibilities:
        // (1) The newly minted object is a root object, and goes in the
        //     Sheet Manager.
        // (2) The newly minted object is a member of a List.
        // (3) The newly minted object is the value of some other object's
        //     Key field.
        
        // Eliminate the trailing "._impl" from the given path
        path = path.substring(0, path.length() - 6);

        // If the path is now 1 token long, then it's a root object.
        // (Eg, if given path was X._impl, then it defined root object X.
        int p = path.lastIndexOf('.');
        if (p < 0) {
            sheet.getSheetManager().addRoot(path, object);
            return;
        }
        
        String previous = path.substring(0, p);
        Object processor = PathValidator.validate(sheet, previous);
        String lastToken = path.substring(p + 1);
        
        // Check to see if we're appending to a list.
        // If so, the last token will be an integer.
        // Eg, we were passed X.0._impl
        int index = index(lastToken);
        if (index >= 0) {
            if (!(processor instanceof List)) {
                throw new IOException("Not a list: " + previous);
            }
            @SuppressWarnings("unchecked")
            List<Object> list = (List)processor;
            if (index != list.size()) {
                throw new IOException("Incorrect index: " + path + " (expected " + list.size());
            }
            list.add(object);
            return;
        }

        // Not a root object, not a list member.
        // Must be a value for some other processor's Key.
        // The lastToken is therefore a Key field name.
        // Check the KeyManager for that key, then set its value in the sheet.
        Map<String,Key<Object>> keys = KeyManager.getKeys(processor.getClass());
        Key<Object> key = keys.get(lastToken);
        if (key == null) {
            throw new IOException("No such key: " + path);
        }
        sheet.set(processor, key, object);
    }

    
    private static int index(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
