<%@page import="org.archive.crawler.admin.auth.User,java.net.URLEncoder" %><%  
    // One page (/iframes/xml.jsp) breaks in certain browsers (Mozilla) if
    // there are any empty lines before it's content.  Since it includes
    // this file it is important that it create no empty lines.
    //
    User user = User.getUser((User)session.getAttribute("user"), request);
    if (user == null || user.authenticate() != User.ADMINISTRATOR) {
        // Not logged in.  Send to login page.
        response.sendRedirect(request.getContextPath() +
            "/login.jsp?action=redirect&back=" +
            URLEncoder.encode(request.getRequestURL().toString(), "UTF-8"));
        // Without this return below the rest of the code on the page would
        // be executed before the redirect is done.
        return; 
    }
%>
