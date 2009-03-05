<%@ page language="java" isErrorPage="true" pageEncoding="UTF-8" %>
<!DOCTYPE HTML PUBLIC "-//w3c//dtd html 4.0 transitional//en">
<html>
<head>
<title>ERROR</title>
</head>
<body bgcolor="#FFFFFF">

<h2>An error occured</h2>

<a href="javascript:history.back()">You may be able to recover by going back</a>

<pre><h3 style="color: red"><%= exception %></h3></pre>

<pre><% 

if (exception != null) { 

  while(exception != null) {
    %><%=exception.getMessage()%><p/><%
    exception.printStackTrace(new java.io.PrintWriter(out));
    exception = exception.getCause();
    %><hr/><%
  }
} else { %>

No exception to report.

<%}%>
</pre>
</body>
</html>
