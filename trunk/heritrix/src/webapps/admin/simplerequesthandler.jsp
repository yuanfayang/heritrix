<%@include file="/include/secure_limited.jsp"%>
<%@include file="/include/handler.jsp"%>
<%@ page import="org.archive.crawler.datamodel.CrawlOrder,org.archive.crawler.admin.SimpleCrawlJob,java.io.File" %>

<%
	SimpleCrawlJob thejob = null;
	boolean schedule = false;
	boolean lookup = false;
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
		schedule = true;
	}
	else if(request.getParameter("lookup") != null)
	{
		String uid = request.getParameter("lookup");
		thejob = (SimpleCrawlJob)handler.getJob(uid);
		lookup = true;
	}
%>

<html>
	<head>
		<title>Heritrix</title>
	</head>
	<body>
		<% if(schedule){ %>
			<p>Your job has been scheduled.
			<p>It's unique ID is: <%=thejob.getUID()%> (you'll need this ID to check on your job's progress)
		<% } %>
		<% if(lookup){ %>
			<% if(thejob!=null){ %>
				
				<p><b>Job found</b><br>
					<b>Job name:</b> <%=thejob.getJobName()%><br>
					<b>Status:</b> <%=thejob.getStatus()%><br>
				<% if(thejob.getStatisticsTracker() != null) { %>
					<b>Progress:</b>
					<%=thejob.getStatisticsTracker().successfulFetchAttempts()%>/<%=thejob.getStatisticsTracker().urisEncounteredCount()%> @ 
					<%
						long time = (thejob.getStatisticsTracker().getCrawlerTotalElapsedTime())/1000;
	
						if(time>3600)
						{
							//got hours.
							out.println(time/3600 + " h., ");
							time = time % 3600;
						}
	
						if(time > 60)
						{
							out.println(time/60 + " min. and ");
							time = time % 60;
						}
	
						out.println(time + " sec.");
					%><br>
				<%}%>
			<%}else{%>
				<p><b>Job not found</b>
			<%}%>
		<% } %>
		<form method="post" action="simplerequesthandler.jsp">
			<p>Lookup job by UID: <input name="lookup"> <input type="submit" value="Lookup">
		</form>
		<p>
			<a href="simplerequest.jsp">Submit new job</a>
	</body>
</html>