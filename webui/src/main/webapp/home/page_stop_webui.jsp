<%@ page import="java.util.List" %>
<%@ page import="org.archive.crawler.webui.Crawler" %>
<%@ page import="org.archive.crawler.webui.Text" %>
<%

%>
<html>
<head>
<%@include file="/include/header.jsp"%>
<title>Heritrix Copy Job</title>
</head>
<body>

<%@include file="/include/nav.jsp"%>

<h3>Stop the web UI?</h3>

You will have to restart it from the command line.

<table>
<tr>
<td>
 <form method="get" action="do_show_home.jsp">
 <input type="submit" value="Cancel">
 </form>
</td>
<td>
 <form method="post" action="do_stop_webui.jsp">
 <input type="submit" value="Stop">
 </form>
</td>
</tr>
</table>

</body>
</html>