<%@include file="/include/handler.jsp"%>
<%@include file="/include/secure.jsp"%>

<%@ page import="org.archive.crawler.admin.CrawlJob,org.archive.crawler.admin.StatisticsTracker,org.archive.crawler.admin.LongWrapper,java.util.*" %>
<%@ page import="org.archive.crawler.datamodel.UURI"%>

<%
	/**
	 *  Page allows user to view the information on seeds in the StatisticsTracker 
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
	
	String title = "Seeds report";
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

		<table>
			<tr>
				<th>
					Seeds for job '<%=cjob.getJobName()%>'
				</th>
				<th>
					Code
				</th>
				<th>
					Disposition
				</th>
			</tr>
			<%
				Iterator seeds = stats.getSeedsSortedByStatusCode();
				while(seeds.hasNext()){
					String UriString = (String)seeds.next();
					String disposition = stats.getSeedDisposition(UriString);
					int code = stats.getSeedStatusCode(UriString);
					String statusCode = code==0 ? "" : Integer.toString(code);
					String statusColor = "black";
					if(code<0 || code >=400){
						statusColor = "red";
					}else if(code == 200){
						statusColor = "green";
					}
			%>
					<tr>
						<td>
							<%=UriString%>
						</td>
						<td align="right">
							<font color="<%=statusColor%>"><%=statusCode%></font>
						</td>
						<td>
							<a href="/admin/logs.jsp?job=<%=cjob.getUID()%>&log=crawl.log&mode=regexpr&regexpr=^[^ ].*<%=UriString%>&grep=true" style="text-decoration: none;"><%=disposition%></a>
						</td>
					</tr>
			<%
				}
			%>
		</table>

<%
	}
%>

<%@include file="/include/foot.jsp"%>
