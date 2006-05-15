/* $Id$
 *
 * Created on Dec 12, 2005
 *
 * Copyright (C) 2005 Internet Archive.
 *  
 * This file is part of the Heritrix Cluster Controller (crawler.archive.org).
 *  
 * HCC is free software; you can redistribute it and/or modify
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
 */
package org.archive.hcc.client;

public enum JobStatus {
    RUNNING("RUNNING", false), PAUSED("PAUSED", false), COMPLETED_NORMAL(
            "COMPLETED_NORMAL", true), USER_TERMINATED("USER_TERMINATED", true),
            SYSTEM_TIME_LIMIT(
            "SYSTEM_TIME_LIMIT", true), SYSTEM_URL_LIMIT("SYSTEM_URL_LIMIT",
            true), SYSTEM_DATA_LIMIT("SYSTEM_DATA_LIMIT", true), FAILED(
            "FAILED", true);

    private final String key;

    private boolean endState;

    JobStatus(String key, boolean endState) {
        this.key = key;
        this.endState = endState;
    }

    // public String getLocalizedName(){
    // if(this.localizedName == null){
    // ResourceBundle rb =
    // ResourceBundle.getBundle("org.archive.archiveit.domain.Resource");
    // this.localizedName = rb.getString(key);
    // }
    //
    // return this.localizedName;
    // }

    @Override
    public String toString() {
        return key;
    }

    public boolean isEndState() {
        return this.endState;
    }
}