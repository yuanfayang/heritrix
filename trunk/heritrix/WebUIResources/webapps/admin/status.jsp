<%@include file="/include/handler.jsp"%>

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
		stats = handler.getStatistics();
	}

%>

<html>
	<head>
		<meta http-equiv=Refresh content="<%=iTime%> URL=/admin/status.jsp?time=<%=iTime%>">
	</head>

	<body>
		<table border="0" cellspacing="0" cellpadding="0" width="100%">
			<tr>
				<td valign="top">
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
										<b>Overall processed docs/sec:</b>&nbsp;
									</td>
									<td>
										<%=stats.processedDocsPerSec()%>
									</td>
								</tr>
								<tr>
									<td>
										<b>Current processed docs/sec:</b>&nbsp;
									</td>
									<td>
										<%=stats.currentProcessedDocsPerSec()%>
									</td>
								</tr>
						<%	
							}
						%>
					</table>
				</td>
				<td valign="top">
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
										<%=stats.activeThreadCount()%>
									</td>
								</tr>
								<tr>
									<td>
										<b>Total bytes written:</b>&nbsp;
									</td>
									<td>
										<%=stats.getTotalBytesWritten()%>
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
			<%
				}
			%>
		</table>
	</body>

</html>