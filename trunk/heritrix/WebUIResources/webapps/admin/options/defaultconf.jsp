<%@include file="/include/handler.jsp"%>
<%@ page import="org.archive.crawler.datamodel.CrawlOrder" %>

<%
	/**
	 * This page enables changes to the default crawl job settings.
	 * (order.xml is rewritten)
	 */

	if(request.getParameter(handler.XP_HTTP_USER_AGENT) != null)
	{
		// Got something in the request.  Let's update!
		handler.updateDefaultCrawlOrder(request);
		response.sendRedirect("/admin/main.jsp");
	}


	CrawlOrder crawlOrder = handler.getDefaultCrawlOrder();
	int iInputSize = 50;
%>

<html>
	<head>
		<title>Heritrix: Default configuration</title>
	</head>
	<body>
		<form xmlns:java="java" xmlns:ext="http://org.archive.crawler.admin.TextUtils" name="frmConfig" method="post" action="defaultconf.jsp">
		<strong>Heritrix: Default configurations for a crawl job</strong>

		<%@include file="/include/jobconfig.jsp"%>
		
		<input type="submit" value="Apply changes">
		
		</form>
		
		<a href="/admin/main.jsp">Back to main page</a>
		
	</body>
</html>