<%@include file="/include/handler.jsp"%>

<%@ page import="org.archive.crawler.admin.StatisticsTracker" %>
<%
	int iTime = 10;
	
	try
	{
		iTime = Integer.parseInt(request.getParameter("time"));
	}
	catch(Exception e){}
%>

<html>
	<head>
		<meta http-equiv=Refresh content="<%=iTime%> URL=/admin/status.jsp?time=<%=iTime%>">
	</head>

	<body>
	
		Crawler running: <%=handler.shouldcrawl()?"Yes":"No"%>
		<br>
		Current job: <%=handler.isCrawling()?handler.getCurrentJob().getJobName():"None"%>
		<br>
		Result of last action: <%=handler.getStatusMessage()%>
		<br>
		Jobs pending: <%=handler.getPendingJobs().size()%><br>
		<%
			if(handler.isCrawling())
			{
				StatisticsTracker stats = handler.getStatistics();

				out.println("Active thread count: " + stats.activeThreadCount() + "<br>");
				out.println("Total bytes written: " + stats.getTotalBytesWritten() + "<br>");
				
				long begin = stats.uriFetchSuccessCount();
				long end = stats.urisEncounteredCount();
				if(end < 1)
					end = 1; 
				int ratio = (int) (100 * begin / end);
		%>
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
		<%
			}
		%>
	
	</body>

</html>