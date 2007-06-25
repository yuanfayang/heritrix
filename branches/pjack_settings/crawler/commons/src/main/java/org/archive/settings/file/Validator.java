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
 * Validator.java
 *
 * Created on Jun 20, 2007
 *
 * $Id:$
 */

package org.archive.settings.file;

import java.io.File;

import org.archive.settings.path.PathChange;
import org.archive.settings.path.PathChangeException;

/**
 * Command-line tool for validating a group of sheets.
 * 
 * @author pjack
 *
 */
public class Validator {

    
    public static void main(String[] args) throws Exception {
        String mainConfigPath;
        if (args.length == 0) {
            mainConfigPath = "config.txt";
        } else {
            mainConfigPath = args[0];
        }
        
        File main = new File(mainConfigPath);
        FileSheetManager fsm = new FileSheetManager(main, false);

        int count = 0;
        for (String sheet: fsm.getProblemSingleSheetNames()) {
            for (PathChangeException pce: fsm.getSingleSheetProblems(sheet)) {
                PathChange pc = pce.getPathChange();
                System.out.print(pc.getPath());
                System.out.print("=");
                System.out.print(pc.getType());
                System.out.print(", ");
                System.out.println(pc.getValue());
                System.out.print(" # ^^^ ");
                System.out.println(pce.getMessage());
                count++;
            }            
        }
        
        System.out.println("There were " + count + " problems.");
    }
}
