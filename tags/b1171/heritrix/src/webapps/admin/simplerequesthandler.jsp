<%@include file="/include/handler.jsp"%>
<%@ page import="org.archive.crawler.datamodel.CrawlOrder,org.archive.crawler.admin.SimpleCrawlJob,java.io.File" %>

<%
	SimpleCrawlJob thejob = null;
	if(request.getParameter(handler.XP_CRAWL_ORDER_NAME) != null)
	{
		// Got something in the request.  Let's update!
		String newUID = handler.getNextJobUID();
		String filename = "jobs"+File.separator+newUID+File.separator+"job-"+request.getParameter(handler.XP_CRAWL_ORDER_NAME)+".xml";
		String seedfile = "seeds-"+request.getParameter(handler.XP_CRAWL_ORDER_NAME)+".txt";
		File f = new File("jobs"+File.separator+newUID);
		f.mkdirs();
		handler.createCrawlOrderFile(request,filename,seedfile,true);
		thejob = new SimpleCrawlJob(newUID, request.getParameter(handler.XP_CRAWL_ORDER_NAME),filename,SimpleCrawlJob.PRIORITY_LOW);
		handler.addJob(thejob);
	}
	else
	{
		// Got nothing, send to schedule page
		response.sendRedirect("/admin/simplerequest.jsp");
	}
%>

<html>
	<head>
		<title>Heritrix</title>
	</head>
	<body>
		<p>Your job has been scheduled.
		<p>It's name is: <%=thejob==null?"NO JOB SUBMITTED":thejob.getJobName()%>
	</body>
</html>