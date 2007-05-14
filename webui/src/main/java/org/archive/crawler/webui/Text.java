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

import javax.servlet.http.HttpServletRequest;

import org.archive.util.TextUtils;

/**
 * @author pjack
 *
 */
public class Text {

    
    public static String attr(Object s) {
        return TextUtils.escapeForMarkupAttribute(s.toString());
    }


    public static String html(Object s) {
        return TextUtils.escapeForHTML(s.toString());
    }
    
    
    public static Object get(HttpServletRequest request, String attr) {
        Object r = request.getAttribute(attr);
        if (r == null) {
            throw new IllegalStateException("Missing required request attribute: " + attr);
        }
        return r;
    }
    
    
    public static String parentPath(String path) {
        int p = path.indexOf('.');
        if (p < 0) {
            return "";
        } else {
            return path.substring(0, p);
        }
    }
    
    
    public static int countSegments(String path) {
        String[] all = path.split("\\.");
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
            r.append("&profile=").append(job);
        } else {
            r.append("&job=").append(job);
        }
        
        return r.toString();
    }

    
    public static String sheetQueryString(HttpServletRequest request) {
        String sheet = (String)get(request, "sheet");
        return jobQueryString(request) + "&sheet=" + sheet;
    }
}
