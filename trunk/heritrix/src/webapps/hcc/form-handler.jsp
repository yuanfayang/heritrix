<%--This JSP does form handling only. Doesn't draw any HTML--%>
<%
    // Process cluster login stuff.
    if (request.getMethod() != null &&
            request.getMethod().equalsIgnoreCase("post") &&
            request.getParameter("cluster-login-submit") != null) {
        // Then its the cluster login form that was posted.  Put login values
        // into the user's session.
        String clusterLogin = request.getParameter("login");
        session.putValue("clusterLogin", clusterLogin);
        String clusterPassword = request.getParameter("password");
        session.putValue("clusterPassword", clusterPassword);
        response.sendRedirect(request.getContextPath());
        return;
    }
%>
<%--For now, always forward to home page.--%>
<jsp:forward page="index.jsp" />
