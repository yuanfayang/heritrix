<%@include file="/include/handler.jsp"%>
<%@ page import="org.archive.crawler.framework.URIFrontier,org.archive.crawler.basic.Frontier" %>


	<html>
		<head>
			<title>Crawl in progress</title>
		</head>
		<body>

		<fieldset style="width: 600px">
			<legend>Crawler status</legend>
			<iframe name="frmStatus" src="/admin/status.jsp?time=10" width="730" height="170" frameborder="0" ></iframe>
		</fieldset><br>
		<fieldset style="width: 600px">
			<legend>Frontier data</legend>
			<pre><%=handler.getFrontierReport().replaceAll(" ","&nbsp;")%></pre>
		</fieldset>
		<p>
			<a href="/admin/main.jsp">Main page</a>
		</body>
	</html>
