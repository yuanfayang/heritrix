<%@include file="/include/handler.jsp"%>
<%@include file="/include/secure.jsp"%>

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
			<iframe name="frmStatus" src="/admin/status.jsp?time=10" width="730" height="170" frameborder="0" ></iframe>
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
				out.println("<a href='main.jsp?action=terminate'>Terminate current job</a> | <a href='options/viewcurrentjob.jsp'>View current job</a> |");
			}
		%>
		<a href="newjob.jsp">New job</a>
		| <a href="pendingjobs.jsp">View pending jobs</a> 
		| <a href="completedjobs.jsp">View completed jobs</a>
		| <a href="main.jsp?action=gc">Garbage collect</a>
	<p> OPTIONS
	
		<ul>
			<li><a href="/admin/options/defaultconf.jsp">Change default job configurations</a></li>
			<li><a href="/admin/options/viewlogs.jsp">View logs</a></li>
			<li><a href="#">Update access tool</a></li>
		</ul>
	
	</body>

</html>