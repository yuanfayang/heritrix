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
 * Actions.java
 *
 * Created on May 7, 2007
 *
 * $Id:$
 */

package org.archive.crawler.webui;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Utility for including a brief last-action or background-action 
 * message atop all WUI pages. 
 * 
 * @author gojomo
 *
 */
public class Flash {
    /** usual types */
    public enum Kind {ACK, NACK, ADVISORY}
    
    static protected String DEFAULT_PATH = "/include/flash.jsp";
    
    /** the jsp/servlet to include; usually "/include/flash.jsp" */
    protected String path;
    /** the message to show, if any  */
    protected String message;
    /** kind of flash, ACK NACK or ADVISORY */
    protected Kind kind;
    
    /**
     * Create an ACK flash of default styling with the given message.
     * 
     * @param message
     */
    public Flash(String message) {
        this(Kind.ACK, message, DEFAULT_PATH);
    }
    
    /**
     * Create a Flash of the given kind, message with default styling.
     * 
     * @param kind
     * @param message
     * @param path
     */
    public Flash(Kind kind, String message) {
        this(kind, message, DEFAULT_PATH);
    }
    
    /**
     * Create a Flash of the given kind, message, and styling.
     * 
     * @param kind
     * @param message
     * @param path
     */
    public Flash(Kind kind, String message, String path) {
        this.kind = kind;
        this.message = message; 
        this.path = path;
    }
    
    /**
     * Utility method for dynamically inserting any outstanding Flash
     * messages in the current response (for execution in a JSP or 
     * servlet that has already written part of the response). 
     * 
     * @param request
     * @param response
     */
    public static void writeAllFromSession(HttpServletRequest request, 
            HttpServletResponse response) {
        
       HttpSession sess = request.getSession();
       if(sess==null) {
           return;
       }
       LinkedList<Flash> flashes = (LinkedList<Flash>) sess.getAttribute("flashes");
       if(flashes==null) {
           return;
       }
       Iterator<Flash> iter = flashes.iterator();
       while(iter.hasNext()) {
           Flash flash = iter.next();
            try {
                flash.insert(request,response);
                if(flash.isExpired()) {
                    iter.remove();
                }
            } catch (ServletException e) {
                throw new IllegalStateException(e);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
       }
    }

    /**
     * Add a Flash to the given request's session, so that it
     * may appear on a subsequently composed response. 
     * 
     * @param request
     */
    public void addToSession(HttpServletRequest request) {
        HttpSession sess = request.getSession(true);
        LinkedList<Flash> flashes = 
            (LinkedList<Flash>) sess.getAttribute("flashes");
        if(flashes==null) {
            flashes = new LinkedList<Flash>();
            sess.setAttribute("flashes", flashes);
        }
        flashes.addFirst(this);
    }
    
    /**
     * Indicate whether the Flash should persist. The usual and 
     * default case is that a Flash displays once and then expires.
     * 
     * @return boolean whether to discard Flash
     */
    public boolean isExpired() {
        return true;
    }

    /**
     * Dynamically insert current Flash output into in-progress Response.
     * 
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     */
    public void insert(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
        Object prev = request.getAttribute("flash");
        request.setAttribute("flash", this);
        // dynamically include (write) the configured flash servlet/jsp output
        request.getRequestDispatcher(getPath()).include(request, response);
        request.setAttribute("flash",prev);
    }

    public String getPath() {
        return this.path;
    }

    public String getMessage() {
        return this.message;
    }

    public Kind getKind() {
        return this.kind;
    }
}
