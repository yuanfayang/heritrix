<%@include file="/include/handler.jsp"%>
<%@include file="/include/secure.jsp"%>
<%@ page import="org.archive.crawler.datamodel.CrawlOrder,org.archive.crawler.admin.SimpleCrawlJob,java.io.File" %>

<%
	/**
	 * This page enables customized jobs to be scheduled.
	 * (order.xml is used for default values)
	 */

	if(request.getParameter(SimpleHandler.XP_CRAWL_ORDER_NAME) != null)
	{
		// Got something in the request.  Let's update!
		String newUID = handler.getNextJobUID();
		String filename = "jobs"+File.separator+newUID+File.separator+"job-"+request.getParameter(SimpleHandler.XP_CRAWL_ORDER_NAME)+"-1.xml";
		String seedfile = "seeds-"+request.getParameter(SimpleHandler.XP_CRAWL_ORDER_NAME)+".txt";
		File f = new File("jobs"+File.separator+newUID);
		f.mkdirs();
		handler.createCrawlOrderFile(request,filename,seedfile,true);
		handler.addJob(new SimpleCrawlJob(newUID, request.getParameter(SimpleHandler.XP_CRAWL_ORDER_NAME),filename,SimpleCrawlJob.PRIORITY_AVERAGE));
		response.sendRedirect("/admin/main.jsp");
	}


	CrawlOrder crawlOrder = handler.getDefaultCrawlOrder();
	int iInputSize = 50;
	String title = "New job";
	int navigation = 1;
%>

<%@include file="/include/head.jsp"%>

		<form xmlns:java="java" xmlns:ext="http://org.archive.crawler.admin.TextUtils" name="frmConfig" method="post" action="new.jsp">

		<%@include file="/include/jobconfig.jsp"%>
		
		<input type="submit" value="Submit job">
		
		</form>
		
<%@include file="/include/foot.jsp"%>
