<%@include file="/include/secure.jsp"%>
<%@include file="/include/handler.jsp"%>
<%@ page import="org.archive.crawler.datamodel.CrawlOrder,org.archive.crawler.framework.CrawlJob,java.io.File" %>

<%
	/**
	 * This page enables customized jobs to be scheduled.
	 * (order.xml is used for default values)
	 */
	CrawlJob job = handler.getCurrentJob(); // This page is only intended for updating the current job.  Warning: This may be unsafe if the current job terminates while edits are being made.

	if(request.getParameter(handler.XP_CRAWL_ORDER_NAME) != null)
	{
		// Got something in the request.  Let's update!
		handler.createCrawlOrderFile(request,job.getCrawlOrderFile(),"seeds-"+request.getParameter(handler.XP_CRAWL_ORDER_NAME)+".txt",true);
		handler.updateCrawlOrder();
		response.sendRedirect("/admin/main.jsp");
		return;
	}


	CrawlOrder crawlOrder = job.getCrawlOrder();
	int iInputSize = 50;
%>

<html>
	<head>
		<title>Update job</title>
	</head>
	<body>
		<form xmlns:java="java" xmlns:ext="http://org.archive.crawler.admin.TextUtils" name="frmConfig" method="post" action="updatejob.jsp">
		<h2>Update job</h2>

		<%@include file="/include/jobconfig.jsp"%>
		
		<input type="submit" value="Submit changes">
		
		</form>
		
		<a href="/admin/main.jsp">Back to main page</a>
		
	</body>
</html>