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
 * JobStage.java
 *
 * Created on Jul 31, 2007
 *
 * $Id:$
 */

package org.archive.crawler.framework;

/**
 * The stages of a crawl job.
 * 
 * @author pjack
 */
public enum JobStage {

    /**
     * Profile stage.  Profiles may be fully edited, but cannot be launched into 
     * running crawls.  They can be copied into new profiles, or into a "ready"
     * job.
     */
    PROFILE("profile-", "Profile"),
    
    /**
     * Ready stage.  May be fully edited, and can be launched.  A typical
     * use case is to copy a profile to a new ready job, edit the new job's
     * seeds, and then launch that ready job.  A ready job can be copied into
     * a profile or into another "ready" job.
     */
    READY("ready-", "Ready Job"),
    
    /**
     * Active stage.  Entered when a "ready" job is launched.  An active job
     * has a JMX-accessible CrawlController.  The settings of an active job 
     * are only partially editable (any setting marked &at;Immutable, for 
     * instance, can't be altered in the active stage).
     */
    ACTIVE("active-", "Active Job"),
    
    /**
     * Completed stage.  Entered when an "active" job finishes, either by 
     * operator request or by natural causes.  A completed job can be copied
     * into a profile or into a ready job.
     */
    COMPLETED("completed-", "Completed Job");

    final private String prefix;
    final private String label;
    final public static char DELIMITER = '-';
    
    JobStage(String prefix, String label) {
        this.prefix = prefix;
        this.label = label;
    }
    
    
    public String getLabel() {
        return label;
    }
    
    /**
     * The prefix for this JobStage.  A crawl job is actually identified by 
     * two pieces of information, its stage and its name.  The API in 
     * {@link CrawlJobManager} expects jobs to specified in this manner (eg,
     * "profile-basic" for a profile named "basic".
     * 
     * @return  the prefix for this stage
     */
    public String getPrefix() {
        return prefix;
    }
    
    
    public static String encode(JobStage stage, String jobName) {
        return stage.getPrefix() + jobName;
    }


    public static String getJobName(String encoded) {
        int p = encoded.indexOf(DELIMITER);
        if (p < 0) {
            throw new IllegalArgumentException();
        }
        return encoded.substring(p + 1);
    }

    
    public static JobStage getJobStage(String encoded) {
        int p = encoded.indexOf(DELIMITER);
        if (p < 0) {
            throw new IllegalArgumentException();
        }
        String stage = encoded.substring(0, p + 1);        
        for (JobStage js: JobStage.values()) {
            if (js.prefix.equals(stage)) {
                return js;
            }
        }
        throw new IllegalArgumentException(encoded + 
                " has no valid JobStage prefix.");
    }
}
