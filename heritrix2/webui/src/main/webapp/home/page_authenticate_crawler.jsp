<%@ page pageEncoding="UTF-8" %> 
<%@ page import="java.util.List" %>
<%@ page import="org.archive.crawler.webui.Crawler" %>
<%@ page import="org.archive.crawler.webui.Text" %>
<%

String host = (String)request.getAttribute("host");
int port = (Integer)request.getAttribute("port");
int id = (Integer)request.getAttribute("id");

%>
<html>
<head>
<%@include file="/include/header.jsp"%>
<title>Heritrix Crawler List</title>
</head>
<body>

<%@include file="/include/nav.jsp"%>


<h3>Enter JMX Credentials for <%=host%>:<%=port%>#<%=id%>.</h3>

<form class="nospace" action="do_authenticate_crawler.jsp" method="post" accept-charset='UTF-8'>

<input type="hidden" name="host" value="<%=Text.attr(host)%>">
<input type="hidden" name="port" value="<%=Text.attr(port)%>">
<input type="hidden" name="id" value="<%=Text.attr(id)%>">

<table>
<tr>
<td>Username:</td>
<td>
<input type="text" name="username">
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
<input class="nospace" type="submit" value="Submit"></form><form class="nospace" action="index.jsp" accept-charset='UTF-8'><input class="nospace" type="submit" value="Cancel"></form>

</body>
</html>
