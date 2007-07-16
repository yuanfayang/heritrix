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

import org.archive.openmbeans.annotations.Attribute;
import org.archive.openmbeans.annotations.Operation;
import org.archive.openmbeans.annotations.Parameter;

/**
 * 
 * 
 * @author pjack
 */
public interface CrawlJobManager extends Closeable {

    

    @Operation(desc="Lists all jobs.")
    public String[] listAllJobs();

    @Operation(desc="Lists active jobs.")
    public String[] listActiveJobs();

    @Operation(desc="Lists completed jobs.")
    public String[] listCompletedJobs();

    
    @Operation(desc="Lists all profiles.")
    public String[] listProfiles();
    
    
    @Operation(desc="Copies a profile.")
    public void copyProfile(
            
            @Parameter(name="origName", desc="The name of the profile to copy.")
            String origName, 
            
            @Parameter(name="copiedName", desc="The name for the new profile.")
            String copiedName) throws IOException;
    
    
    @Operation(desc="Launches a profile into a new, pending job.")
    public void launchProfile(
            
            @Parameter(name="profile", desc="The name of the profile to launch.")
            String profile,
            
            @Parameter(name="job", desc="The name for the new launched job.")
            String job) throws IOException;

    
    @Operation(desc="Loads a profile for editing.")
    public String getProfile(
            
            @Parameter(name="profile", desc="The name of the profile to load.")
            String profile
            
            ) throws IOException;

    
    @Operation(desc="Closes an open profile.")
    public void closeProfile(
            
            @Parameter(name="profile", desc="The name of the open profile to close.")
            String profile
            
            ) throws IOException;


    @Operation(desc="Loads the logs for a job.")
    public String getLogs(
            
            @Parameter(name="job", desc="The name of the job.")
            String job
            
            ) throws IOException;



    @Operation(desc="Lists all available checkpoints.")
    String[] listCheckpoints();

    
    @Operation(desc="Recovers a checkpoint.")
    void recoverCheckpoint(
            
            @Parameter(name="checkpointPath", desc="The path to the checkpoint directory.")
            String checkpoint, 
            
            @Parameter(name="oldPaths", desc="Old path prefixes to replace.")
            String[] oldPaths, 
            
            @Parameter(name="newPaths", desc="New path prefixes to replace.")
            String[] newPaths) throws IOException;

    @Operation(desc="Deregisters this CrawlJobManager from the MBeanServer.")
    void close();

    @Operation(desc="Invokes System.exit to terminate the JVM.")
    void systemExit();

    
    @Operation(desc="Reads lines from a text file.")
    String readLines(
            @Parameter(name="filename", desc="The name of the file to read.")
            String fileName,
            
            @Parameter(name="startLine", desc="The starting line number to read.")
            int startLine,
            
            @Parameter(name="lineCount", desc="The number of lines to read.")
            int lineCount) throws IOException;

    
    @Operation(desc="Writes lines to a text file.")
    void writeLines(
            @Parameter(name="filename", desc="The name of the file to write.")
            String fileName,
            
            @Parameter(name="startLine", desc="The starting line number to write.")
            int startLine,

            @Parameter(name="lineCount", desc="The number of lines in a page.")
            int lineCount,
            
            @Parameter(name="lines", desc="The new text to write.")
            String lines) throws IOException;

    @Attribute(desc="The version of Heritrix.", def="Unknown")
    String getHeritrixVersion();
}
