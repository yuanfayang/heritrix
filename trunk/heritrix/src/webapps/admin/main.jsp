<%@include file="/include/secure.jsp"%>
<%@include file="/include/handler.jsp"%>
<%@ page import="org.archive.crawler.admin.CrawlJob" %>
<%@ page import="org.archive.crawler.admin.StatisticsTracker" %>
<%@ page import="org.archive.util.ArchiveUtils" %>

<%
	StatisticsTracker stats = null;

	if(handler.getCurrentJob() != null)
	{
		stats = (StatisticsTracker)handler.getCurrentJob().getStatisticsTracking(); //Assume that StatisticsTracker is being used.
	}

	String title = "Administrator Console";
	int tab = 0;
%>

<%@include file="/include/head.jsp"%>
	
	<script type="text/javascript">
		function doTerminateCurrentJob(){
			if(confirm("Are you sure you wish to terminate the job currently being crawled?")){
				document.location = '/admin/console/action.jsp?action=terminate';
			}
		}	
	</script>
	
	<table border="0" cellspacing="0" cellpadding="0"><tr><td>
	<fieldset style="width: 750px">
		<legend>Crawler status</legend>
		<table border="0" cellspacing="0" cellpadding="0" width="100%">
			<tr>
				<td valign="top" width="60%">
					<table border="0" cellspacing="0" cellpadding="0" >
						<tr>
							<td>
								<b>Crawler running:</b>&nbsp;
							</td>
							<td>
								<%=handler.isRunning()?"Yes":"No"%>
							</td>
						</tr>
						<tr>
							<td>
								<b>Current job:</b>&nbsp;
							</td>
							<td nowrap>
								<%=handler.isCrawling()?handler.getCurrentJob().getJobName():"None"%>
							</td>
						</tr>
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
					</table>
				</td>
				<td valign="top" width="40%">
					<table border="0" cellspacing="0" cellpadding="0" >
						<tr>
							<td>
								<b>Used memory:</b>&nbsp;
							</td>
							<td>
								<%=(Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory())/1024%> KB
							</td>
						</tr>
						<tr>
							<td>
								<b>Heap size:</b>&nbsp;
							</td>
							<td>
								<%=(Runtime.getRuntime().totalMemory())/1024%> KB
							</td>
						</tr>
						<tr>
							<td>
								<b>Max heap size:</b>&nbsp;
							</td>
							<td>
								<%=(Runtime.getRuntime().maxMemory())/1024%> KB
							</td>
						</tr>
						<tr>
							<td>
								<b>
									Alerts: 
								</b>
							</td>
							<td>
								<a style="color: #000000" href="/admin/console/alerts.jsp">
									<%=Heritrix.getAlerts().size()%> (<%=Heritrix.getNewAlerts()%> new)
								</a>
							</td>
					</table>
				</td>
			</tr>
			<%
				if(handler.isCrawling())
				{
			%>
					<tr>
						<td valign="top">
							<table border="0" cellspacing="0" cellpadding="0">
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
										<%=ArchiveUtils.doubleToString(stats.currentProcessedDocsPerSec(),2)%> (<%=ArchiveUtils.doubleToString(stats.processedDocsPerSec(),2)%>)
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
							</table>
						</td>
						<td valign="top">
							<table border="0" cellspacing="0" cellpadding="0">
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
										<%=ArchiveUtils.formatBytesForDisplay(stats.totalBytesWritten())%>
									</td>
								</tr>
							</table>
						</td>
					</tr>
			<%	
				}
				if(handler.isCrawling())
				{
					long begin = stats.successfullyFetchedCount();
					long end = (stats.discoveredUriCount() - stats.failedFetchAttempts()) - stats.disregardedFetchAttempts();
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
	</fieldset>
	</td></tr>
	<tr><td>
	<%
		if(handler.isRunning())
		{
			out.println("<a href='/admin/console/action.jsp?action=stop'>Stop crawling pending jobs</a>");
		}
		else
		{
			out.println("<a href='/admin/console/action.jsp?action=start'>Start crawling pending jobs</a>");
		}

		if(handler.isCrawling())
		{
			out.println(" | <a href='javascript:doTerminateCurrentJob()'>Terminate current job</a> | ");
			if(handler.getCurrentJob().getStatus().equals(CrawlJob.STATUS_PAUSED) || handler.getCurrentJob().getStatus().equals(CrawlJob.STATUS_WAITING_FOR_PAUSE))
			{
                out.println("<a href='/admin/console/action.jsp?action=resume'>Resume current job</a>");
	            if(handler.getCurrentJob().getStatus().equals(CrawlJob.STATUS_PAUSED))
	            {
                    out.println(" | <a href='/admin/console/frontier.jsp'>Inspect frontier URIs</a> ");
                }
			}
			else
			{
				out.println("<a href='/admin/console/action.jsp?action=pause'>Pause current job</a> ");
			}
		}
	%> | <a href="/admin/main.jsp">Refresh</a>
	</td></tr>
	<tr><td>
		<p>
			&nbsp;
		<p>
			&nbsp;
	</td></tr>
	<tr><td>
		<a href="/admin/console/shutdown.jsp">Shut down Heritrix software</a>
	</td></tr></table>
<%@include file="/include/foot.jsp"%>
