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

<%@include file="/include/nav.jsp"%>


<h3>Enter JMX host, port, username and password for crawl engine to administer:</h3>

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
<td>Port:</td>
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
<input class="nospace" type="submit" value="Submit"></form>
</body>
</html>