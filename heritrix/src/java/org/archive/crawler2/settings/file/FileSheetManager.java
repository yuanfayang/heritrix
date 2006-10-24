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
 * FileSheetManager.java
 * Created on October 24, 2006
 *
 * $Header$
 */
package org.archive.crawler2.settings.file;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.archive.crawler2.settings.NamedObject;
import org.archive.crawler2.settings.Sheet;
import org.archive.crawler2.settings.SheetManager;
import org.archive.crawler2.settings.SingleSheet;
import org.archive.util.LRU;


/**
 * Simple sheet manager that stores settings in a directory hierarchy.
 * 
 * @author pjack
 */
public class FileSheetManager extends SheetManager {


    final private File root;

    final private Map<String,Sheet> sheets;
    
    
    public FileSheetManager(File root, int maxSheets, int maxAssoc) {
        sheets = new LRU<String,Sheet>(maxSheets);
        this.root = root;
    }

    @Override
    public void addRoot(String name, Object root) {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected void addSheet(Sheet sheet, String name) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void associate(Sheet sheet, Iterable<String> strings) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void disassociate(Sheet sheet, Iterable<String> strings) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public Sheet getAssociation(String context) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public SingleSheet getDefault() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<NamedObject> getRoots() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Sheet getSheet(String sheetName) throws IllegalArgumentException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Set<String> getSheetNames() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void moveRootDown(String rootName) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void moveRootUp(String rootName) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void removeRoot(String name) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void removeSheet(String sheetName) throws IllegalArgumentException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void renameSheet(String oldName, String newName) {
        // TODO Auto-generated method stub
        
    }

}
