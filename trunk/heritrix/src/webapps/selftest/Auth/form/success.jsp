<%@ page import="javax.servlet.*" %>
<%@ page import="javax.servlet.http.*" %>

<%
    String login = request.getParameter("login");
    String password = request.getParameter("password");
    if (login == null || !login.equals("login") ||
            password == null || !password.equals("password")) {

        response.sendRedirect("error.html");
    }
%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
    <head>
        <title>Successful Login Page</title>
        <meta name="author" content="Debian User,,," >
        <meta name="generator" content="screem 0.8.2" >
        <meta name="keywords" content="" >
        <meta http-equiv="content-type" content="text/html; charset=UTF-8" >
        <meta http-equiv="Content-Script-Type" content="text/javascript" >
        <meta http-equiv="Content-Style-Type" content="text/css" >
    </head>
    <body>
            <h1>Successful Login Page</h1>
            <p>You get this page if a successful login.</p>
</form>
            
</form>
    </body>
</html>
