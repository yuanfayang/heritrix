<%@include file="/include/handler.jsp"%>
<%@include file="/include/secure.jsp"%>
<%@ page import="org.archive.crawler.datamodel.CrawlOrder,org.archive.crawler.framework.CrawlController,org.archive.crawler.admin.SimpleCrawlJob,java.io.File" %>

<%
	/**
	 * This page enables customized jobs to be scheduled.
	 * (order.xml is used for default values)
	 */
	String message = "";

	if(request.getParameter(SimpleHandler.XP_CRAWL_ORDER_NAME) != null)
	{
		// Got something in the request.  Let's update!
		String newUID = handler.getNextJobUID();
		String filename = "jobs"+File.separator+newUID+File.separator+"job-"+request.getParameter(SimpleHandler.XP_CRAWL_ORDER_NAME)+"-1.xml";
		String seedfile = "seeds-"+request.getParameter(SimpleHandler.XP_CRAWL_ORDER_NAME)+".txt";
		File f = new File("jobs"+File.separator+newUID);
		f.mkdirs();
		handler.createCrawlOrderFile(request,filename,seedfile,true);
		CrawlJob newjob = new SimpleCrawlJob(newUID, request.getParameter(SimpleHandler.XP_CRAWL_ORDER_NAME),filename,SimpleCrawlJob.PRIORITY_AVERAGE);
		if(CrawlController.checkUserAgentAndFrom(newjob.getCrawlOrder()))
		{
			handler.addJob(newjob);
			response.sendRedirect("/admin/jobs.jsp?message=New crawl job created");
			return;
		}
		else
		{
			message = "<pre>You must set the User-Agent and From HTTP header values " +
								"to acceptable strings before proceeding. \n" +
								" User-Agent: [software-name](+[info-url])[misc]\n" +
								" From: [email-address]</pre>";
		}
	}


	CrawlOrder crawlOrder = handler.getDefaultCrawlOrder();
	int iInputSize = 50;
	String title = "New crawl job";
	int tab = 1;
%>

<%@include file="/include/head.jsp"%>

		<p><font color="red"><%=message%></font>
		
		<form xmlns:java="java" xmlns:ext="http://org.archive.crawler.admin.TextUtils" name="frmConfig" method="post" action="new.jsp">

		<%@include file="/include/jobconfig.jsp"%>
		
		<input type="submit" value="Submit job">
		
		</form>
		
<%@include file="/include/foot.jsp"%>
