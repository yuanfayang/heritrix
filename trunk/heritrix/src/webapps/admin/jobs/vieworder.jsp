<%@include file="/include/secure.jsp"%>
<%@include file="/include/handler.jsp"%>

<%@ page import="org.archive.crawler.framework.CrawlJob,org.archive.crawler.admin.StatisticsTracker,java.util.*" %>
<%
 
	String job = request.getParameter("job");
	CrawlJob cjob = null;

	StatisticsTracker stats = null;
	
	if(job != null)
	{
		cjob = handler.getJob(job);
		stats = (StatisticsTracker)cjob.getStatisticsTracking();
		
	}
	else
	{
		// Assume current job.
		cjob = handler.getCurrentJob();
		stats = (StatisticsTracker)cjob.getStatisticsTracking();
	}
	
	
%>
<html>
	<head>
		<title>Heritrix: View crawl order</title>
		<link rel="stylesheet" href="/admin/css/heritrix.css">
	</head>
	
	<body>
		<%
			if(cjob == null)
			{
				// NO JOB SELECTED - ERROR
		%>
				<b>Invalid job selected</b>
		<%
			}
			else
			{
		%>
			<iframe name="frmStatus" src="/admin/iframes/xml.jsp?file=<%=cjob.getCrawlOrderFile()%>" width="100%" height="100%" frameborder="0" ></iframe>
		<%
			} // End if(cjob==null)else clause
		%>
	</body>
</html>