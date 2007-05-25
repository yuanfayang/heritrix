/* Copyright (C) 2007 Internet Archive.
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
 * SettingsSAX.java
 * Created on January 23, 2007
 *
 * $Header:$
 */
package org.archive.settings.file;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.archive.settings.path.PathChange;
import org.archive.state.KeyTypes;
import org.archive.util.CollectionUtils;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class SettingsSAX extends DefaultHandler {

    
    final private static String INDEX = "name";
    final private static String VALUE = "value";
    
    final private static String ROOT = "root";
    final private static String REFERENCE = "reference";
    final private static String OBJECT = "object";
    final private static String MAP = "map";
    final private static String LIST = "list";
    
    final private static Set<String> COMPLEX = 
        CollectionUtils.constantSet(OBJECT, MAP, LIST);
    
    final private static String DEPENDENCIES = "dependencies";
    
    
    private static class Setting {
        String path;
        String type;
        String value;
        List<PathChange> dependencies = new ArrayList<PathChange>();
    }
    
    
    private static enum State { NORMAL, DEP_DETECT, DEP_STORE };
    
    
    private Setting pending = new Setting();    
    private State state = State.NORMAL;
    private String path = "";
    final private PathChangeListener listener;
    
    
    public SettingsSAX(PathChangeListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener may not be null");
        }
        this.listener = listener;
    }

    @Override
    public void startElement(
            String uri, 
            String localName, 
            String qName, 
            Attributes attr)
    throws SAXException {
        if (qName.equals(ROOT)) {
            return;
        }
        switch (state) {
            case NORMAL:
                handleNormal(qName, attr);
                break;
            case DEP_DETECT:
                handleDepDetect(qName, attr);
                break;
            case DEP_STORE:
                handleDepStore(qName, attr);
                break;
        }
    }
    
    
    private void handleNormal(String tag, Attributes attr) 
    throws SettingsSAXException {
        if (!isDataTag(tag)) {
            throw new SettingsSAXException("Invalid data tag: " + tag);
        }
        
        String index = attr.getValue(INDEX);
        String value = attr.getValue(VALUE);
        validate(tag, index, value);

        if (tag.equals(OBJECT)) {
            this.state = State.DEP_DETECT;
            this.path = appendPath(index);
            pending.path = this.path;
            pending.value = value;
            return;
            
        }

        String npath = appendPath(index);
        PathChange pc = new PathChange(npath, tag, value);
        change(pc);

        if (isComplex(tag)) {
            this.path = npath;
        }
    }
    
    
    private void handleDepDetect(String tag, Attributes attr) 
    throws SettingsSAXException {
        if (tag.equals(DEPENDENCIES)) {
            state = State.DEP_STORE;
            return;
        }

        // We were expecting a <dependencies> tag but didn't find one.
        // We need to construct the object that was waiting on its dependencies.
        constructPending();

        handleNormal(tag, attr);
    }
    
    
    private void handleDepStore(String tag, Attributes attr) 
    throws SettingsSAXException {
        if (!isDataTag(tag)) {
            throw new SettingsSAXException("Invalid data tag: " + tag);
        }

        // We are processing the tags listed in <dependencies>...</dependencies>
        String index = attr.getValue(INDEX);
        String value = attr.getValue(VALUE);
        validate(tag, index, value);
        pending.dependencies.add(new PathChange(index, tag, value));
    }
    
    

    @Override
    public void endElement(String uri, String local, String tag) 
    throws SettingsSAXException {
        if (tag.equals(ROOT)) {
            
            return;
        }
        if (tag.equals(DEPENDENCIES)) {
            constructPending();
            return;
        }
        
        if (tag.equals(OBJECT) && (state != State.NORMAL)) {
            constructPending();
        }
        if (isComplex(tag)) {
            truncatePath();
        }
    }
    
    
    private void constructPending() {
        PathChange pc = new PathChange(
                pending.path, 
                OBJECT, 
                pending.value); 
        change(pc);
        pending.path = null;
        pending.value = null;
        pending.dependencies.clear();
        state = State.NORMAL;
    }
    
    
    private void truncatePath() {
        int p = path.lastIndexOf('.');
        if (p < 0) {
            path = "";
        } else {
            path = path.substring(0, p);
        }
    }

    
    private String appendPath(String n) {
        if (path.equals("")) {
            return n;
        } else {
            return path + "." + n;
        }
    }
    
    
    private static boolean isComplex(String tag) {
        return COMPLEX.contains(tag);
    }


    private void change(PathChange pc) {
        listener.change(pc);
    }

    private boolean isDataTag(String tag) {
        if (COMPLEX.contains(tag)) {
            return true;
        }
        
        if (tag.equals(REFERENCE)) {
            return true;
        }
        
        Class c = KeyTypes.getSimpleType(tag);
        return c != null;
    }


    private static void validate(String tag, String index, String value) 
    throws SettingsSAXException {
        if (index == null) {
            throw new SettingsSAXException(tag + " requires a " + INDEX + 
                    " attribute.");
        }
        
        if (value == null) {
            throw new SettingsSAXException(tag + " requires a " + VALUE +
                    " attribute.");
        }        
    }

    
    public static void main(String args[]) throws Exception {
        SettingsSAX sax = new SettingsSAX(new PathChangeListener() {
            public void change(PathChange p) {
                System.out.println(p.getPath() + "=" + p.getType() + ":" + p.getValue());
            }
        });
        
        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser parser = factory.newSAXParser();
        String path = "testdata/selftest/AuthSelfTest/profile/sheets/default.single";
        parser.parse(new File(path), sax);
    }

}
