<%@include file="/include/handler.jsp"%>

<%@ page import="org.archive.crawler.framework.CrawlJob,org.archive.crawler.admin.StatisticsTracker,java.util.Vector" %>
<%
	/**
	 *  Page allows user to view the information in the StatisticsTracker 
	 *  for a completed job.
	 *	Parameter: job - UID for the job.
	 */
	 
	String job = request.getParameter("job");
	CrawlJob cjob = null;

	StatisticsTracker stats = null;
	
	if(job != null)
	{
		Vector jobs = handler.getCompletedJobs();
		for(int i=0 ; i < jobs.size() ; i++)
		{
			cjob = (CrawlJob)jobs.get(i);
			if(cjob.getUID().equals(job))
			{
				// Found it!
				stats = cjob.getStatisticsTracker();
			}
		}
	}
	
%>

<html>
	<head>
		<title>View statistics</title>
	</head>

	<body>
		<%
			if(cjob == null)
			{
				// NO JOB SELECTED - ERROR
		%>
				<b>No job selected</b>
		<%
			}
			else
			{
		%>
				<table border="0" cellspacing="0" cellpadding="0" width="600">
					<tr>
						<td valign="top">
							<table border="0" cellspacing="0" cellpadding="0" >
								<tr>
									<td>
										<b>Job name:</b>&nbsp;
									</td>
									<td>
										<%=cjob.getJobName()%>
									</td>
								</tr>
								<tr>
									<td>
										<b>Status:</b>&nbsp;
									</td>
									<td>
										<%=cjob.getStatus()%>
									</td>
								</tr>
								<tr>
									<td height="5" colspan="2">
									</td>
								</tr>
								<tr>
									<td>
										<b>Time:</b>&nbsp;
									</td>
									<td>
										<%
											long time = (stats.getCrawlEndTime()-stats.getCrawlStartTime())/1000;
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
										%>
									</td>
								</tr>
								<tr>
									<td>
										<b>Processed docs/sec:</b>&nbsp;
									</td>
									<td>
										<%=stats.processedDocsPerSec()%>
									</td>
								</tr>
							</table>
						</td>
						<td valign="top">
							<table border="0" cellspacing="0" cellpadding="0" >
								<tr>
									<td>
										<b>Discovered:</b>&nbsp;
									</td>
									<td>
										<%=stats.urisEncounteredCount()%>
									</td>
								</tr>
								<tr>
									<td>
										<b>Pending:</b>&nbsp;
									</td>
									<td>

									</td>
								</tr>
								<tr>
									<td>
										<b>Downloaded:</b>&nbsp;
									</td>
									<td>
										<%=stats.uriFetchSuccessCount()%>
									</td>
								</tr>
								<tr>
									<td>
										<b>Total megabytes written:</b>&nbsp;
									</td>
									<td>
										<%=(stats.getTotalBytesWritten()/1048576)%>
									</td>
								</tr>
							</table>
						</td>
					</tr>
					<%
						long begin = stats.uriFetchSuccessCount();
						long end = stats.urisEncounteredCount();
						if(end < 1)
							end = 1; 
						int ratio = (int) (100 * begin / end);
					%>
					<tr>
						<td colspan="2" height="5">
						</td>
					</tr>
					<tr>
						<td colspan="2">
							<center>
							<table border=1 width="500">
							<tr>
							<td><center><b><u>DOWNLOADED/DISCOVERED DOCUMENT RATIO</u></b><br>
							<table border="0" cellpadding="0" cellspacing= "0" width="100%"> 
								<tr>
									<td width="20%"></td>
									<td bgcolor="darkorange" width="<%= (int) (ratio/2) %>%" align="right">
										<strong><%= ratio %></strong>%
									</td>
									<td bgcolor="lightblue" align="right" width="<%= (int) ((100-ratio)/2) %>%"></td>
									<td nowrap>&nbsp;&nbsp;(<%= begin %> of <%= end %>)</td>
								</tr>
							</table>		
							</td>
							</tr>
							</table>
							</center>
						</td>
					</tr>
				</table>
				&nbsp;<br>
				<a href="completedjobs.jsp">Back</a><br>
		<%
			} // End if(cjob==null)else clause
		%>
		<a href="main.jsp">Main page</a>
	</body>

</html>