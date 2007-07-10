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
 * Text.java
 *
 * Created on May 7, 2007
 *
 * $Id:$
 */

package org.archive.crawler.webui;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspWriter;

import org.archive.settings.path.PathValidator;
import org.archive.util.TextUtils;

/**
 * @author pjack
 *
 */
public class Text {


    /**
     * Convenience for escaping a string to appear in an attribute of an
     * html tag.  See {@link TextUtils#escapeForMarkupAttribute}.
     * 
     * @param s  the object whose toString to escape
     * @return   the escaped string
     */
    public static String attr(Object s) {
        if (s == null) {
            return "null"; // FIXME
        }
        return TextUtils.escapeForMarkupAttribute(s.toString());
    }


    /**
     * Convenience for escaping a string to appear in an attribute of an
     * html tag.  See {@link TextUtils#escapeForHTML(String)}.
     * 
     * @param s  the object whose toString to escape
     * @return   the escaped string
     */
    public static String html(Object s) {
        return TextUtils.escapeForHTML(s.toString());
    }
    
    
    /**
     * Convenience for escaping a string to appear in a query string of a
     * URL.  See {@link java.net.URLEncoder}.
     * 
     * @param s   the object whose toString to escape
     * @return    the escape string
     */
    public static String query(Object s) {
        try {
            return URLEncoder.encode(s.toString(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
    
    
    /**
     * Retrieve a required attribute from the requeset.
     * 
     * @param request   the request whose attribute to return
     * @param attr      the name of the attribute to return
     * @return   the attribute
     * @excetion  IllegalStateException   if the request does not contain 
     *                                    that attribute.
     */
    public static Object get(HttpServletRequest request, String attr) {
        Object r = request.getAttribute(attr);
        if (r == null) {
            throw new IllegalStateException("Missing required request attribute: " + attr);
        }
        return r;
    }
    
    
    public static String parentPath(String path) {
        int p = path.lastIndexOf(PathValidator.DELIMITER);
        if (p < 0) {
            return "";
        } else {
            return path.substring(0, p);
        }
    }
    
    
    public static String lastPath(String path) {
        int p = path.lastIndexOf(PathValidator.DELIMITER);
        if (p < 0) {
            return path;
        } else {
            return path.substring(p + 1);
        }
    }
    
    
    public static String baseName(String className) {
        int p = className.lastIndexOf('.');
        if (p < 0) {
            return className;
        } else {
            return className.substring(p + 1);
        }        
    }
    
    
    public static int countSegments(String path) {
        String[] all = path.split("\\" + PathValidator.DELIMITER);
        return all.length;
    }

    
    
    public static String jobQueryString(HttpServletRequest request) {
        StringBuilder r = new StringBuilder();
        Crawler c = (Crawler)get(request, "crawler");
        r.append(c.getQueryString());
        
        String job = (String)request.getAttribute("job");
        if (job == null) {
            job = (String)request.getAttribute("profile");
            if (job == null) {
                throw new IllegalStateException(
                        "Must specify job or profile in request.");
            }
            r.append("&profile=").append(query(job));
        } else {
            r.append("&job=").append(query(job));
        }
        
        return r.toString();
    }

    
    public static void printJobFormFields(
            HttpServletRequest request, 
            JspWriter out) 
    throws IOException {      
        Crawler c = (Crawler)get(request, "crawler");
        c.printFormFields(out);

        String job = (String)request.getAttribute("job");
        if (job == null) {
            job = (String)request.getAttribute("profile");
            if (job == null) {
                throw new IllegalStateException(
                        "Must specify job or profile in request.");
            }
            hidden(out, "profile", job);
        } else {
            hidden(out, "job", job);
        }
    }

    
    public static void printSheetFormFields(
            HttpServletRequest request, 
            JspWriter out) 
    throws IOException {      
        printJobFormFields(request, out);
        hidden(out, "sheet", (String)request.getAttribute("sheet"));
    }

    
    private static void hidden(JspWriter out, String name, String value) 
    throws IOException {
        out.print("<input type=\"hidden\" name=\"");
        out.print(attr(name));
        out.print("\" value=\"");
        out.print(attr(value));
        out.println("\"/>");
    }
    
    public static String sheetQueryString(HttpServletRequest request) {
        String sheet = (String)get(request, "sheet");
        return jobQueryString(request) + "&sheet=" + sheet;
    }
}
