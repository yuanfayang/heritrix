<%@include file="/include/handler.jsp"%>
<%@include file="/include/secure.jsp"%>
<%@ page import="org.archive.crawler.datamodel.CrawlOrder,org.archive.crawler.admin.SimpleCrawlJob,java.io.File" %>

<%
	/**
	 * This page enables customized jobs to be scheduled.
	 * (order.xml is used for default values)
	 */

	if(request.getParameter(handler.XP_CRAWL_ORDER_NAME) != null)
	{
		// Got something in the request.  Let's update!
		String newUID = handler.getNextJobUID();
		String filename = "jobs"+File.separator+newUID+File.separator+"job-"+request.getParameter(handler.XP_CRAWL_ORDER_NAME)+".xml";
		String seedfile = "seeds-"+request.getParameter(handler.XP_CRAWL_ORDER_NAME)+".txt";
		File f = new File("jobs"+File.separator+newUID);
		f.mkdirs();
		handler.createCrawlOrderFile(request,filename,seedfile,true);
		handler.addJob(new SimpleCrawlJob(newUID, request.getParameter(handler.XP_CRAWL_ORDER_NAME),filename,SimpleCrawlJob.PRIORITY_AVERAGE));
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
		<h2>New job</h2>

		<%@include file="/include/jobconfig.jsp"%>
		
		<input type="submit" value="Submit job">
		
		</form>
		
		<a href="/admin/main.jsp">Back to main page</a>
		
	</body>
</html>