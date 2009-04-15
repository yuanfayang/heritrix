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
 * Checkpointer.java
 *
 * Created on Mar 8, 2007
 *
 * $Id:$
 */

package org.archive.settings;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import org.archive.settings.file.Checkpointable;
import org.archive.util.IoUtils;

/**
 * Executes checkpoints and recovers.
 * 
 * @author pjack
 */
public class Checkpointer {

    final public static String ACTIONS_FILE = "actions.serialized";
    
    final public static String OBJECT_GRAPH_FILE = "object_graph.serialized";
    
    private Checkpointer() {
    }


    public static void checkpoint(/*SheetManager*/Object mgr, File dir) 
    throws IOException {
        List<RecoverAction> actions = new ArrayList<RecoverAction>();
//        for (Checkpointable c: mgr.getCheckpointables()) {
//            c.checkpoint(dir, actions);
//        }
        
        writeObject(new File(dir, ACTIONS_FILE), actions);
        writeObject(new File(dir, OBJECT_GRAPH_FILE), mgr);
    }

    
    private static void writeObject(File f, Object o) 
    throws IOException {
        ObjectOutputStream oout = null;
        try {
            oout = new ObjectOutputStream(new FileOutputStream(f));
            oout.writeObject(o);
        } finally {
            IoUtils.close(oout);
        }
        
    }
    

    private static List<RecoverAction> readActions(File dir) 
    throws IOException {
        File actionsFile = new File(dir, ACTIONS_FILE);
        ObjectInputStream oinp = null;
        try {
            oinp = new ObjectInputStream(
                    new FileInputStream(actionsFile));
            @SuppressWarnings("unchecked")
            List<RecoverAction> actions = (List)oinp.readObject();
            return actions;
        } catch (ClassNotFoundException e) {
            IOException io = new IOException();
            io.initCause(e);
            throw io;
        } finally {
            IoUtils.close(oinp);
        }
    }

//    public static SheetManager recover(File dir, CheckpointRecovery recovery) 
//    throws IOException {
//        List<RecoverAction> actions = readActions(dir);
//        for (RecoverAction action: actions) try {
//            action.recoverFrom(dir, recovery);
//        } catch (Exception e) {
//            IOException io = new IOException();
//            io.initCause(e);
//            throw io;
//        }
//        
//        CheckpointInputStream cinp = null;
//        try {
//            File f = new File(dir, OBJECT_GRAPH_FILE);
//            cinp = new CheckpointInputStream(new FileInputStream(f), recovery);
//            SheetManager mgr = (SheetManager)cinp.readObject();
//            recovery.apply(mgr.getGlobalSheet());
//            return mgr;
//        } catch (ClassNotFoundException e) { 
//            IOException io = new IOException();
//            io.initCause(e);
//            throw io;
//        }finally {
//            IoUtils.close(cinp);
//        }
//    }

}
