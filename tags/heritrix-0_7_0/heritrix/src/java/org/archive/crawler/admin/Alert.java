/* Alert
 * 
 * $Id$
 * 
 * Created on Feb 20, 2004
 *
 * Copyright (C) 2004 Internet Archive.
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
 */
package org.archive.crawler.admin;

import java.io.StringWriter;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.archive.util.ArchiveUtils;

/**
 * A simple data class that captures one alert. 
 * 
 * <p>An alert is a message to a human operator created by Heritrix when 
 * exceptional conditions occur that can not be reported in any other way except 
 * through logs. 
 * 
 * <p>In general all condition that cause alerts should also cause similar info 
 * to be written to an appropriate log. To aid this the class offers a method 
 * that will dump the alert to a specified log. Also each alert has a Level
 * attribute, same as log entries.
 * 
 * @author Kristinn Sigurdsson
 * 
 * @see org.archive.crawler.Heritrix#addAlert(Alert)
 */
public class Alert {
    private boolean newAlert;
    private String alertTitle;
    private String alertBody;
    private String ID;
    private Date time;
    
    private Level level;
    
    private static int nextID = 0;
    
    /**
     * Create a new alert
     * @param title short descriptive string that represents a title for the alert
     * @param body the alert message
     * @param level
     *            The severity level of the alert
     */
    public Alert(String title, String body, Level level){
        alertTitle = title;
        alertBody = body;
        newAlert = true;
        ID = ArchiveUtils.get17DigitDate()+(nextID++); //Gurantee uniqueness.
        time = new Date();
        this.level = level;
    }


    /**
     * Create a new alert
     * 
     * @param title
     *            short descriptive string that represents a title for the alert
     * @param body
     *            the alert message
     * @param error 
     *            an error associated with the alert. It's content will be
     *            written to the body
     * @param level
     *            The severity level of the alert
     */
    public Alert(String title, String body, Throwable error, Level level) {
        this(title, body, level);
        StringWriter message = new StringWriter();
        message.write("\n\n");
        message.write("Associated Throwable: " + error + "\n\n");
        if (error.getMessage() != null) {
            message.write("  Message:\n");
            message.write("    " + error.getMessage() + "\n\n");
        }
        if (error.getCause() != null) {
            message.write("  Cause:\n");
            message.write("    " + error.getCause() + "\n\n");
        }
        message.write("  Stacktrace:\n");
        error.printStackTrace(new java.io.PrintWriter(message));
        alertBody = alertBody + message.toString();
    }

    /**
     * A string containing the alert message.
     * @return the alert message
     */
    public String getBody() {
        return alertBody;
    }

    /**
     * Get a short descriptive string that represents a title for the alert.
     * @return a short descriptive string that represents a title for the alert
     */
    public String getTitle() {
        return alertTitle;
    }

    /**
     * Is alert new? 
     * @return True if the alert is new (setAlertSeen() has not been invoked)
     */
    public boolean isNew() {
        return newAlert;
    }

    /**
     * Mark alert as seen (That is, isNew() no longer returns true).
     */
    public void setAlertSeen() {
        newAlert = false;
    }

    /**
     * Get the alert's unique ID
     * @return the alert's unique ID
     */
    public String getID() {
        return ID;
    }
    
    public boolean equals(Alert compare){
        return compare.getID().equals(ID);
    }
    /**
     * Returns the time when the alert was created.
     * @return the time when the alert was created.
     */
    public Date getTimeOfAlert() {
        return time;
    }

    /**
     * @return Returns the level.
     */
    public Level getLevel() {
        return level;
    }


    /**
     * Write the alert to a log
     * @param logger The log to write to
     */
    public void print(Logger logger) {
        logger.log(level,alertBody);        
    }
}
