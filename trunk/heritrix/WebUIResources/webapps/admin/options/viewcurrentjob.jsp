<%@include file="/include/handler.jsp"%>
<%@include file="/include/secure.jsp"%>
<%@ page import="org.archive.crawler.framework.CrawlJob,org.archive.crawler.admin.StatisticsTracker,java.util.*" %>


	<html>
		<head>
			<title>Crawl in progress</title>
		</head>
		<body>

		<fieldset style="width: 600px" align="top">
			<legend>Crawler status</legend>
			<iframe name="frmStatus" src="/admin/status.jsp?time=10" width="730" height="170" frameborder="0" ></iframe>
		</fieldset><a href="/admin/main.jsp">Main page</a>
		<fieldset style="width: 600px" align="top">
			<legend>Frontier data</legend>
			<iframe name="frmFrontier" src="/admin/options/viewfrontier.jsp" width="730" height="300" frameborder="0" ></iframe>
		</fieldset><a href="viewfrontier.jsp">View frontier report in fullscreen</a>
		<fieldset style="width: 600px" align="top">
			<legend>Thread data</legend>
			<iframe name="frmThreads" src="/admin/options/viewthreads.jsp" width="730" height="300" frameborder="0" ></iframe>
		</fieldset><a href="viewthreads.jsp">View threads report in fullscreen</a>
		
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
						StatisticsTracker stats = (StatisticsTracker)handler.getStatistics(); //Assume that StatisticsTracker is being used.
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
												&nbsp;<%=count%> (<%=Double.toString(percent).substring(0,Double.toString(percent).indexOf(".")+3)%>%)
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
		<%=i%>
		
		</body>
	</html>
