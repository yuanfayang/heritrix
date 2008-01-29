/* AuthFilter
 *
 * Created on Aug 14, 2007
 *
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
 */
package org.archive.crawler.webui;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Filter requiring simple authentication. 
 */
public class AuthFilter implements Filter {
    public static final String CONTINUE_URL = "continueUrl";
    public static final String IS_AUTHORIZED = "isAuthorized";
    
    private static String uiPassword;
    
    private FilterConfig filterConfig = null;

    public void init(FilterConfig config) {
        this.filterConfig = config;
    }

    public void doFilter(ServletRequest req, ServletResponse res,
            FilterChain chain)
    throws IOException, ServletException {
        if (this.filterConfig == null) {
            return;
        }
        if (req instanceof HttpServletRequest) {
            HttpServletRequest httpReq = (HttpServletRequest)req;
            String path = httpReq.getRequestURI();
            String authUrl = httpReq.getContextPath() +
                this.filterConfig.getInitParameter("authUrl");
            boolean isAuth = Boolean.TRUE.equals(httpReq.getSession(true).getAttribute(IS_AUTHORIZED));
            if (!isAuth && !authUrl.equals(path)) {
              // If there is a queryString, we need to append "?" +
              // queryString to the redir path.  Otherwise leave it
              // alone.
              if (httpReq.getQueryString() != null) {
                  StringBuffer pathBuf = new StringBuffer(path);
                  pathBuf.append("?").append( httpReq.getQueryString());
                  path = pathBuf.toString();
              }
              httpReq.getSession(true).setAttribute(CONTINUE_URL, path);
              // direct all requests that are unauth to auth
              ((HttpServletResponse)res).sendRedirect(authUrl);
              return;
            }
        }
        chain.doFilter(req, res);
    }

    public void destroy() {
        this.filterConfig = null;
    }
    
    /**
     * Returns the current web ui password.  If any value was set using 
     * {@link #setUIPassword(String)}, that password is returned.  Otherwise
     * the given context must contain an attribute named <code>uiPassword</code>,
     * and that attribute value is returned.
     * 
     * @param sc  The servlet context containing the initial configuration
     * @return   the current UI password
     */
    public static String getUIPassword(ServletContext sc) {
        if (uiPassword == null) {
            uiPassword = (String)sc.getAttribute("uiPassword");
        }
        return uiPassword;
    }
    
    /**
     * Sets the current password.
     * 
     * @param password   the new password
     */
    public static void setUIPassword(String password) {
        uiPassword = password;
    }
} 