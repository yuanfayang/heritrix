<%@include file="/include/handler.jsp"%>
<%@include file="/include/secure.jsp"%>

<%@ page import="org.archive.crawler.admin.StatisticsTracker" %>
<%
	int iTime = 10;
	
	try
	{
		iTime = Integer.parseInt(request.getParameter("time"));
	}
	catch(Exception e){}
	
	StatisticsTracker stats = null;

	if(handler.isCrawling())
	{
		stats = (StatisticsTracker)handler.getStatistics(); //Assume that StatisticsTracker is being used.
	}

%>

<html>
	<head>
		<meta http-equiv=Refresh content="<%=iTime%> URL=/admin/status.jsp?time=<%=iTime%>">
	</head>

	<body>
		<table border="0" cellspacing="0" cellpadding="0" width="100%">
			<tr>
				<td valign="top" width="60%">
					<table border="0" cellspacing="0" cellpadding="0" >
						<tr>
							<td>
								<b>Crawler running:</b>&nbsp;
							</td>
							<td>
								<%=handler.shouldcrawl()?"Yes":"No"%>
							</td>
						</tr>
						<tr>
							<td>
								<b>Current job:</b>&nbsp;
							</td>
							<td>
								<%=handler.isCrawling()?handler.getCurrentJob().getJobName():"None"%>
							</td>
						</tr>
						<%
							if(handler.isCrawling())
							{
						%>
								<tr>
									<td height="5" colspan="2">
									</td>
								</tr>
								<tr>
									<td>
										<b>Status:</b>&nbsp;
									</td>
									<td>
										<%=handler.getCurrentJob().getStatus()%>
									</td>
								</tr>
								<tr>
									<td>
										<b>Processed docs/sec:</b>&nbsp;
									</td>
									<td>
										<%=stats.currentProcessedDocsPerSec()%> (<%=stats.processedDocsPerSec()%>)
										&nbsp;&nbsp;&nbsp;
										<b>KB/sec:</b>&nbsp;<%=stats.currentProcessedKBPerSec()%> (<%=stats.processedKBPerSec()%>)
									</td>
								</tr>
								<tr>
									<td>
										<b>Run time:</b>&nbsp;
									</td>
									<td>
										<%
											long time = (stats.getCrawlerTotalElapsedTime())/1000;
											
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
						<%	
							}
						%>
					</table>
				</td>
				<td valign="top" width="40%">
					<table border="0" cellspacing="0" cellpadding="0" >
						<tr>
							<td>
								<b>Jobs pending:</b>&nbsp;
							</td>
							<td>
								<%=handler.getPendingJobs().size()%>
							</td>
						</tr>
						<tr>
							<td>
								<b>Jobs completed:</b>&nbsp;
							</td>
							<td>
								<%=handler.getCompletedJobs().size()%>
							</td>
						</tr>
						<%
							if(handler.isCrawling())
							{
						%>
								<tr>
									<td height="5" colspan="2">
									</td>
								</tr>
								<tr>
									<td>
										<b>Active thread count:</b>&nbsp;
									</td>
									<td>
										<%=stats.activeThreadCount()%> of <%=stats.threadCount()%>
									</td>
								</tr>
								<tr>
									<td>
										<b>Total data written:</b>&nbsp;
									</td>
									<td>
										<%=(stats.getTotalBytesWritten()/1048576)%> MB
									</td>
								</tr>
						<%	
							}
						%>
					</table>
				</td>
			</tr>
			<%
				if(handler.isCrawling())
				{
					long begin = stats.successfulFetchAttempts();
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
			<%
				}
			%>
		</table>
	</body>

</html>