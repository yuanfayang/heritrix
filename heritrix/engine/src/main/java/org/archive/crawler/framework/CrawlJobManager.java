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
 * CrawlJobManager.java
 *
 * Created on Jan 24, 2007
 *
 * $Id:$
 */

package org.archive.crawler.framework;

import java.io.Closeable;
import java.io.IOException;

import javax.management.ObjectName;

import org.archive.openmbeans.annotations.Attribute;
import org.archive.openmbeans.annotations.Operation;
import org.archive.openmbeans.annotations.Parameter;

/**
 * 
 * 
 * @author pjack
 */
public interface CrawlJobManager extends Closeable {

    
    

    @Operation(desc="Lists all profiles, and all ready, active and completed jobs.")
    public String[] listJobs();

    @Operation(desc="Copies a job or profile to a new profile or a new ready job.")
    public void copy(
            
            @Parameter(name="oldJob", desc="stage-name of the profile to copy.")
            String origName, 
            
            @Parameter(name="newJob", desc="stage-name of the copied job.")
            String copiedName) throws IOException;
    
    
    @Operation(desc="Launches a job.")
    public void launchJob(
            
            @Parameter(name="job", desc="The stage-name of the job to launch.")
            String profile) throws IOException;

    
    @Operation(desc="Loads a SheetManager for editing.  If a SheetManager " +
                "for the given job already exists, it will be re-used.")
    public ObjectName getSheetManagerStub(
            
            @Parameter(name="job", desc="The stage-name of the job whose " +
                        "SheetManager to load and return.")
            String profile
            
            ) throws IOException;

    
    @Operation(desc="Closes an open SheetManager.")
    public void closeSheetManagerStub(
            
            @Parameter(name="job", desc="The stage-name of the job whose" +
                        " SheetManager to close.")
            String profile
            
            );


    @Operation(desc="Loads the logs for a job.")
    public ObjectName getLogs(
            
            @Parameter(name="job", desc="The stage-name of the job.")
            String job
            
            ) throws IOException;



    @Operation(desc="Lists available checkpoints for the given job.")
    String[] listCheckpoints(
            @Parameter(name="job", desc="The stage-name of the job.")
            String job);

    
    @Operation(desc="Recovers a checkpoint.")
    void recoverCheckpoint(
            
            @Parameter(name="completedJob", desc="The stage-name of a completed job to recover.")
            String completedJob,
            
            @Parameter(name="recoverJob", desc="The stage-name for the new recovered job.")
            String recoverJob,
            
            @Parameter(name="checkpointPath", desc="The checkpoint to recover from.")
            String checkpoint, 
            
            @Parameter(name="oldPaths", desc="Old path prefixes to replace.")
            String[] oldPaths, 
            
            @Parameter(name="newPaths", desc="New path prefixes to replace.")
            String[] newPaths);

    @Operation(desc="Deregisters this CrawlJobManager from the MBeanServer.")
    void close();

    @Operation(desc="Invokes System.exit to terminate the JVM.")
    void systemExit();

    
    @Operation(desc="Reads lines from a text file.")
    String readLines(
            @Parameter(name="job", desc="The job whose file to read.")
            String job,
            
            @Parameter(name="filename", desc="An optional filename if the settingsPath resolves to a directory.")
            String filename,

            @Parameter(name="settingsPath", desc="The settings path to a FileModule in that job.")
            String settingsPath,
            
            @Parameter(name="startLine", desc="The starting line number to read.")
            int startLine,
            
            @Parameter(name="lineCount", desc="The number of lines to read.")
            int lineCount) throws IOException;

    
    @Operation(desc="Writes lines to a text file.")
    void writeLines(
            @Parameter(name="job", desc="The job whose file to read.")
            String job,
            
            @Parameter(name="settingsPath", desc="The settings path to a FileModule in that job.")
            String settingsPath,

            @Parameter(name="filename", desc="An optional filename if the settingsPath resolves to a directory.")
            String filename,

            @Parameter(name="startLine", desc="The starting line number to write.")
            int startLine,

            @Parameter(name="lineCount", desc="The number of lines in a page.")
            int lineCount,
            
            @Parameter(name="lines", desc="The new text to write.")
            String lines) throws IOException;

    @Attribute(desc="The version of Heritrix.", def="Unknown")
    String getHeritrixVersion();

    @Operation(desc="Returns some helpful information.")
    String help();


    @Operation(desc="Returns a filesystem path based on a settings path to a FileModule.")
    String getFilePath(
            
            @Parameter(name="job", desc="The stage-name of the job whose settings to examine.")
            String job, 
            
            @Parameter(name="settingsPath", desc="The settings path to the FileModule.")
            String settingsPath);

     @Operation(desc="Lists files in a directory.")
     String[] listFiles(
             @Parameter(name="job", desc="The stage-name of the job whose directory to read.")
             String job,
             
             @Parameter(name="settings", desc="The settings path to the FileModule representing the directory.")
             String settingsPath,
             
             @Parameter(name="regex", desc="A regex filename filter.  Only filenames that match the regex will be included in the result.")
             String regex);

}
