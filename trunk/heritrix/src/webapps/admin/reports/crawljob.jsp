<%@include file="/include/handler.jsp"%>
<%@include file="/include/secure.jsp"%>

<%@ page import="org.archive.crawler.admin.CrawlJob,org.archive.crawler.admin.StatisticsTracker,org.archive.crawler.admin.LongWrapper,java.util.*" %>
<%@ page import="org.archive.util.ArchiveUtils" %>
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
		cjob = handler.getJob(job);
		if(cjob != null){
			stats = (StatisticsTracker)cjob.getStatisticsTracking();
		}
	}
	else
	{
		// Assume current job.
		cjob = handler.getCurrentJob();
		stats = (StatisticsTracker)cjob.getStatisticsTracking();
	}
	
	String title = "Crawl job report";
	int tab = 4;
	
%>

<%@include file="/include/head.jsp"%>

		<%
			if(cjob == null)
			{
				// NO JOB SELECTED - ERROR
		%>
				<p>&nbsp;<p>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<b>Invalid job selected</b>
		<%
			}
			else if(stats == null)
			{
				out.println("<b>No statistics associated with job.</b><p><b>Job status:</b> " + cjob.getStatus());			
				if(cjob.getErrorMessage()!=null){
					out.println("<p><pre><font color='red'>"+cjob.getErrorMessage()+"</font></pre>");
				}
			}
			else
			{
		%>
				<table border="0" cellspacing="0" cellpadding="0">
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
										<%
											if(cjob.getStatus().equalsIgnoreCase(CrawlJob.STATUS_RUNNING))
											{
												// Show current and overall stats.
										%>
												<%=ArchiveUtils.doubleToString(stats.currentProcessedDocsPerSec(),2)%> (<%=ArchiveUtils.doubleToString(stats.processedDocsPerSec(),2)%>)
												&nbsp;&nbsp;&nbsp;
												<b>KB/sec:</b>&nbsp;<%=stats.currentProcessedKBPerSec()%> (<%=stats.processedKBPerSec()%>)
										<%
											}
											else
											{
												// Only show overall stats.
										%>
												<%=stats.processedDocsPerSec()%>
												&nbsp;&nbsp;&nbsp;
												<b>KB/sec:</b>&nbsp;<%=stats.processedKBPerSec()%>
										<%
											}
										%>
									</td>
								</tr>
							</table>
						</td>
						<td>
							&nbsp;&nbsp;
						</td>
						<td valign="top">
							<table border="0" cellspacing="0" cellpadding="0" >
								<tr>
									<td>
										<b>Downloaded:</b>&nbsp;
									</td>
									<td>
										<%=stats.successfulFetchAttempts()%>
									</td>
								</tr>
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
										<%=stats.urisInFrontierCount()%>
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
				</table>
				&nbsp;<br>

				<table>
					<tr>
						<th>
							Status code
						</th>
						<th width="200">
							Documents
						</th>
						<th>
						</th>
					</tr>
					<%
						TreeSet statusCodeDistribution = stats.getSortedByValue(stats.getStatusCodeDistribution());
						Iterator statusCodes = statusCodeDistribution.iterator();
						
						
						while(statusCodes.hasNext())
						{
							Map.Entry entry = (Map.Entry)statusCodes.next();
							long count = ((LongWrapper)(entry.getValue())).longValue;
							long percent = count*100/stats.successfulFetchAttempts();
					%>
							<tr>
								<td>
									<a style="text-decoration: none;" href="/admin/logs.jsp?job=<%=cjob.getUID()%>&log=crawl.log&mode=regexpr&regexpr=\d{17}\s*<%=entry.getKey()%>&grep=true"><%=entry.getKey()%></a>
								</td>
								<td width="600" colspan="2">
									<table width="600" cellspacing="0" cellpadding="0" border="0">
										<tr>
											<td bgcolor="blue" width="<%=percent%>%">&nbsp;
											</td>
											<td nowrap>
												&nbsp;<%=count%>
											</td>
											<td width="<%=100-percent%>%"></td>
										</tr>
									</table>
								</td>
							</tr>
					<%
						}
					%>				
				</table>

				<table>
					<tr>
						<th width="100">
							File type
						</th>
						<th width="200" colspan="2">
							Documents
						</th>
						<th>
                            Kilobytes
						</th>
					</tr>
					<%
						TreeSet fileDistribution = stats.getSortedByValue(stats.getFileDistribution());
						Iterator files = fileDistribution.iterator();
						while(files.hasNext())
						{
							Map.Entry file = (Map.Entry)files.next();
							long count = ((LongWrapper)file.getValue()).longValue;
							long percent = count*100/stats.successfulFetchAttempts();
					%>
							<tr>
								<td nowrap>
									<a style="text-decoration: none;" href="/admin/logs.jsp?job=<%=cjob.getUID()%>&log=crawl.log&mode=regexpr&regexpr=^[^ ].*<%=file.getKey()%>&grep=true"><%=file.getKey()%></a>
								</td>
								<td width="600" colspan="2">
									<table width="600" cellspacing="0" cellpadding="0" border="0">
										<tr>
											<td bgcolor="blue" width="<%=percent%>%">&nbsp;
											</td>
											<td>
												&nbsp;<%=count%>
											</td>
											<td width="<%=100-percent%>%"></td>
										</tr>
									</table>
								</td>
								<td align="right">
								    <%=(stats.getBytesPerFileType((String)file.getKey())/1024)%> Kb
								</td>
							</tr>
					<%
						}
					%>				
				</table>
		<table>
			<tr>
				<th>
					Hosts
				</th>
				<th>
					Documents&nbsp;
				</th>
				<th>
				    Kilobytes
				</th>
			</tr>
			<%
				TreeSet hostsDistribution = stats.getSortedByValue(stats.getHostsDistribution());
				Iterator hosts = hostsDistribution.iterator();
				while(hosts.hasNext())
				{
					Map.Entry host = (Map.Entry)hosts.next();
			%>
					<tr>
						<td>
							<a style="text-decoration: none;" href="/admin/logs.jsp?job=<%=cjob.getUID()%>&log=crawl.log&mode=regexpr&regexpr=^[^ ].*<%=host.getKey()%>&grep=true"><%=host.getKey()%></a>
						</td>
						<td>
							<%=((LongWrapper)host.getValue()).longValue%>
						</td>
						<td align="right">
                            <%=(stats.getBytesPerHost((String)host.getKey())/1024)%> Kb
						</td>
					</tr>
			<%
				}
			%>				
		</table>
		<%
			} // End if(cjob==null)else clause
		%>
<%@include file="/include/foot.jsp"%>
