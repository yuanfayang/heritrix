<%@include file="/include/handler.jsp"%>
<%@include file="/include/secure.jsp"%>

<%@ page import="org.archive.crawler.framework.CrawlJob,org.archive.crawler.admin.StatisticsTracker,java.util.*" %>
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
		stats = (StatisticsTracker)cjob.getStatisticsTracking();
		
	}
	else
	{
		// Assume current job.
		cjob = handler.getCurrentJob();
		stats = (StatisticsTracker)cjob.getStatisticsTracking();
	}
	
	String title = "View job statistics";
	
%>

<%@include file="/include/head.jsp"%>

		<%
			if(cjob == null)
			{
				// NO JOB SELECTED - ERROR
		%>
				<b>Invalid job selected</b>
		<%
			}
			else if(stats == null)
			{
				out.println("No statistics associated with job.</b>  Job status: " + cjob.getStatus());			
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
										<%=stats.urisInFrontierCount()%>
									</td>
								</tr>
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
				</table>
				&nbsp;<br>

				<table>
					<tr>
						<th>
							Status code
						</th>
						<th width="200">
							Number of occurances
						</th>
						<th>
						</th>
					</tr>
					<%
						HashMap statusCodeDistribution = stats.getStatusCodeDistribution();
						Iterator statusCodes = statusCodeDistribution.keySet().iterator();
						
						
						while(statusCodes.hasNext())
						{
							Object code = statusCodes.next();
							long count = Integer.parseInt(statusCodeDistribution.get(code).toString());
							double percent = ((double)count/stats.successfulFetchAttempts())*100;
					%>
							<tr>
								<td>
									<%=code%>
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
						<th width="200">
							Number of occurances
						</th>
						<th>
						</th>
					</tr>
					<%
						HashMap fileDistribution = stats.getFileDistribution();
						Iterator files = fileDistribution.keySet().iterator();
						while(files.hasNext())
						{
							Object file = files.next();
							long count = Integer.parseInt(fileDistribution.get(file).toString());
							double percent = ((double)count/stats.successfulFetchAttempts())*100;
					%>
							<tr>
								<td nowrap>
									<%=file%>
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
					Number of occurances
				</th>
			</tr>
			<%
				HashMap hostsDistribution = stats.getHostsDistribution();
				Iterator hosts = hostsDistribution.keySet().iterator();
				int i=0;
				while(hosts.hasNext())
				{
					i++;
					Object host = hosts.next();
			%>
					<tr>
						<td>
							<%=host%>
						</td>
						<td>
							<%=hostsDistribution.get(host)%>
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
