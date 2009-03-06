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
import java.util.logging.Logger;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Filter requiring simple authentication. 
 */
public class AuthFilter implements Filter {
    private static final Logger logger =
        Logger.getLogger(AuthFilter.class.getName());

    private static final String IS_AUTHORIZED = "isAuthorized";
    private static final String LOGIN_FAILURES = "loginFailures";
    private static final String LAST_ATTEMPT = "lastAttempt";
    private static final int MIN_MS_BETWEEN_ATTEMPTS = 3000;
    
    private static String uiPassword;
    
    private String authUrl;

    public void init(FilterConfig config) {
        this.authUrl = config.getInitParameter("authUrl");
        if (this.authUrl == null) {
            this.authUrl = "/auth.jsp";
            logger.info("parameter authUrl not provided in AuthFilter config "
                    + "in web.xml, using default: " + this.authUrl);
        }
    }

    public void doFilter(ServletRequest req, ServletResponse res,
            FilterChain chain)
    throws IOException, ServletException {
        if (!(req instanceof HttpServletRequest)
                || !(res instanceof HttpServletResponse)) {
            chain.doFilter(req, res);
            return;
        }
        
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        
        HttpSession session = request.getSession(true); 
        ServletContext sc = session.getServletContext(); 
        
        if (Boolean.TRUE.equals(session.getAttribute(IS_AUTHORIZED))) {
            // already logged in
            chain.doFilter(req, res);
            return;
        }

        String enteredPassword = request.getParameter("enteredPassword");
        if (enteredPassword != null && !enteredPassword.equals("")) {

            // throttle
            synchronized (sc) {
                // XXX can an attacker keep getting the lock and prevent another
                // user from logging in?
                Long lastAttempt = (Long) sc.getAttribute(LAST_ATTEMPT);
                if (lastAttempt == null) {
                    lastAttempt = 0l;
                }

                long now = System.currentTimeMillis();
                if (now - lastAttempt < MIN_MS_BETWEEN_ATTEMPTS) {
                    long sleepMs = MIN_MS_BETWEEN_ATTEMPTS - (now - lastAttempt);
                    logger.info("only " + (now - lastAttempt)
                            + "ms since last login attempt, sleeping for "
                            + sleepMs + "ms");
                    try {
                        Thread.sleep(sleepMs);
                    } catch (InterruptedException e) {
                    }
                }

                sc.setAttribute(LAST_ATTEMPT, System.currentTimeMillis());
            }

            if (enteredPassword.equals(getUIPassword(sc))) {
                // login success
                session.setAttribute(AuthFilter.IS_AUTHORIZED, true);
                chain.doFilter(req, res);
                return;
            } else {
                // login failure
                Integer failures = (Integer) sc.getAttribute(LOGIN_FAILURES);
                if (failures == null) {
                    failures = 0;
                }
                failures++;

                if (failures > 5) {
                    logger.warning(failures + " failed login attempts");
                }
                sc.setAttribute(LOGIN_FAILURES, failures);
                request.getRequestDispatcher(authUrl).forward(request, response);
                return;
            }
        } else {
            // no password supplied
            request.getRequestDispatcher(authUrl).forward(request, response);
            return;
        }
    }

    public void destroy() {
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
    /* XXX why does it work like this with the ServletContext? */
    public static String getUIPassword(ServletContext sc) {
        if (uiPassword == null) {
            uiPassword = (String) sc.getAttribute("uiPassword");
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