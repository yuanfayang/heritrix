<%@ page import="org.archive.crawler.webui.Text" %>
<%

String error = (String)request.getAttribute("error");

%>
<html>
<head>
<%@include file="/include/header.jsp"%>
<title>Heritrix Crawler List</title>
</head>
<body>

<a border="0" href="<%=request.getContextPath()%>/index.jsp">
<img border="0" src="<%=request.getContextPath()%>/images/logo.gif" height="37" width="145">
</a>


<h3>Enter JMX host, port, username and password for the new crawler:</h3>

<% if (error != null) { %>
<div class="error">
<%=error%>
</error>
<% } %>

<form class="nospace" action="do_add_crawler.jsp" method="post">

<table>
<tr>
<td>Host:</td>
<td>
<input type="text" name="host">
</td>
</tr>
<tr>
<td>Username:</td>
<td>
<input type="text" name="port" value="8849">
</td>
</tr>
<tr>
<td>Username:</td>
<td>
<input type="text" name="username" value="controlRole">
</td>
</tr>

<tr>
<td>
Password:
</td>
<td>
<input type="password" name="password">
</td>
</tr>
</table>
<input class="nospace" type="submit" value="Submit"></form><form class="nospace" action="index.jsp"><input class="nospace" type="submit" value="Cancel"></form>

</body>
</html>