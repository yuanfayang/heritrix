<%@ page contentType="text/html; charset=ISO-8859-1" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN"
    "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
    <head>
        <title>Test for double-encoding</title>
        <meta http-equiv="content-type" content="text/html; charset=ISO-8859-1"/>
    </head>
    <body>
        <h1>URI Double Encoding Test</h1>
        <p>See sourceforge bug 
        "[ 966219 ] UURI doubly-encodes %XX sequences" to learn more
        about what this selftest is probing.</p>
            <%
                String param = (String)request.getParameter("parameter"); 
                if (param == null) {
                    param = "";
                }
     		%>
        <p><a href="index.jsp?parameter=%AD<%=param%>">Reference back to
            this page with encoded characters in query string</a>.</p>
        <p>Parameter is <%=param%></p>
    </body>
</html>
