<%@include file="/include/handler.jsp"%>
<%@ page import="org.archive.crawler.datamodel.CrawlOrder,org.archive.crawler.admin.CrawlJob" %>

<%
	/**
	 * This page enables changes to the default crawl job settings.
	 * (order.xml is rewritten)
	 */

	if(request.getParameter(handler.XP_CRAWL_ORDER_NAME) != null)
	{
		// Got something in the request.  Let's update!
		String filename = "job-"+request.getParameter(handler.XP_CRAWL_ORDER_NAME)+".xml";
		handler.createCrawlOrderFile(request,filename);
		handler.addJob(new CrawlJob(request.getParameter(handler.XP_CRAWL_ORDER_NAME),filename,handler.WEB_APP_PATH));
		response.sendRedirect("/admin/main.jsp");
	}


	CrawlOrder crawlOrder = handler.getDefaultCrawlOrder();
	int iInputSize = 50;
%>

<html>
	<head>
		<title>New job</title>
	</head>
	<body>
		<form xmlns:java="java" xmlns:ext="http://org.archive.crawler.admin.TextUtils" name="frmConfig" method="post" action="newjob.jsp">
		<strong>New job</strong>

		<%@include file="/include/jobconfig.jsp"%>
		
		<input type="submit" value="Submit job">
		
		</form>
		
		<a href="/admin/main.jsp">Back to main page</a>
		
	</body>
</html>