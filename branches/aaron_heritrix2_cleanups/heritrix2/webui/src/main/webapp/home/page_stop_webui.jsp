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

<h3>Stop the web UI Java VM process?</h3>

Any engines in the same Java VM will also be stopped.<p>

You will have to restart it from the command line.

 <form method="post" action="do_stop_webui.jsp">
 <input type="submit" value="Stop Web UI JVM">
 </form>

</body>
</html>