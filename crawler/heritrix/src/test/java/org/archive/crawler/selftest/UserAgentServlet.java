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
 * UserAgentServlet.java
 *
 * Created on Apr 27, 2007
 *
 * $Id:$
 */
package org.archive.crawler.selftest;

import java.io.IOException;
import java.util.Enumeration;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 * @author pjack
 *
 */
public class UserAgentServlet extends HttpServlet {


    private static final long serialVersionUID = 1L;



    private String ua;
    private String from;
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) 
    throws ServletException, IOException {
        Enumeration e = req.getHeaderNames();
        while (e.hasMoreElements()) {
            String name = (String)e.nextElement();
            System.out.println(name + "=" + req.getHeader(name));
        }
        this.ua = req.getHeader("User-Agent");
        this.from = req.getHeader("From");
        resp.getWriter().println("This space intentionally left blank.");
        resp.getWriter().close();
    }
    
    
    public String getUserAgent() {
        return ua;
    }
    
    
    public String getFrom() {
        return from;
    }

}
