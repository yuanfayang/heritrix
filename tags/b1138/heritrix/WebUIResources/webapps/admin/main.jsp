<%@include file="/include/secure.jsp"%>
<%@include file="/include/handler.jsp"%>
<%@ page import="org.archive.crawler.framework.CrawlJob" %>

<%
	String sAction = request.getParameter("action");

	if(sAction != null)
	{
		// Need to handle an action	
		if(sAction.equalsIgnoreCase("start"))
		{
			// Tell handler to start crawl job
			handler.startCrawler();
		}
		else if(sAction.equalsIgnoreCase("stop"))
		{
			// Tell handler to stop crawl job
			handler.stopCrawler();
		}
		else if(sAction.equalsIgnoreCase("terminate"))
		{
			// Tell handler to stop crawl job
			handler.terminateJob();
		}
		else if(sAction.equalsIgnoreCase("pause"))
		{
			// Tell handler to pause crawl job
			handler.pauseJob();
		}
		else if(sAction.equalsIgnoreCase("resume"))
		{
			// Tell handler to resume crawl job
			handler.resumeJob();
		}
		else if(sAction.equalsIgnoreCase("gc"))
		{
			// Tell handler to stop crawl job
			System.gc();
		}
		
	}	
%>

<html>

	<head>
		<title>Heritrix: Administrator console</title>
	</head>
	
	<body>
	
	<p>
		<fieldset style="width: 600px">
			<legend>Crawler status</legend>
			<iframe name="frmStatus" src="/admin/status.jsp?time=5" width="730" height="185" frameborder="0" ></iframe>
		</fieldset><br>
		<%
			if(handler.shouldcrawl())
			{
				out.println("<a href='main.jsp?action=stop'>Stop crawling</a>");
			}
			else
			{
				out.println("<a href='main.jsp?action=start'>Start crawling</a>");
			}
		%>
		|
		<%
			if(handler.isCrawling())
			{
				out.println("<a href='main.jsp?action=terminate'>Terminate current job</a> | <a href='options/viewcurrentjob.jsp'>View current job</a> | <a href='updatejob.jsp'>Update current job</a> | ");
				if(handler.getCurrentJob().getStatus().equals(CrawlJob.STATUS_PAUSED) || handler.getCurrentJob().getStatus().equals(CrawlJob.STATUS_WAITING_FOR_PAUSE))
				{
					out.println("<a href='main.jsp?action=resume'>Resume current job</a> | ");
				}
				else
				{
					out.println("<a href='main.jsp?action=pause'>Pause current job</a> |  ");
				}
			}
		%>
		<a href="newjob.jsp">New job</a>
	<p> OPTIONS
	
		<ul>
			<li><a href="pendingjobs.jsp">View pending jobs</a></li>
			<li><a href="completedjobs.jsp">View completed jobs</a></li>
			<li><a href="/admin/options/defaultconf.jsp">Change default job configurations</a></li>
			<li><a href="/admin/options/viewlogs.jsp">View logs</a></li>
			<li><a href="#">Update access tool</a></li>
			<li><a href="main.jsp?action=gc">Garbage collect</a></li>
		</ul>
	
	</body>

</html>