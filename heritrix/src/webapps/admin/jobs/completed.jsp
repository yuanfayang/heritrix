<%@include file="/include/handler.jsp"%>
<%@include file="/include/secure.jsp"%>
<%@ page import="org.archive.crawler.framework.CrawlJob,java.util.Vector" %>

<%
	Vector jobs = handler.getCompletedJobs();

	String title = "Completed crawl jobs";
	int tab = 1;
%>

<%@include file="/include/head.jsp"%>

		<table border="0">
			<tr>
				<th>
					Job name
				</th>
				<th>
					Status
				</th>
				<th>
					View
				</th>
			</tr>
			<%
				for(int i=0 ; i	< jobs.size() ; i++)
				{
					CrawlJob job = (CrawlJob)jobs.get(i);
			%>		
					<tr>
						<td>
							<%=job.getJobName()%>
						</td>
						<td>
							<%=job.getStatus()%>
						</td>
						<td>
							<a target="_blank" href="/admin/jobs/vieworder.jsp?job=<%=job.getUID()%>">Crawl order</a>
							<a href="/admin/reports/crawljob.jsp?job=<%=job.getUID()%>&nav=3">Crawl report</a>
						</td>
					</tr>
			<%
				}
			%>
		</table>
		
<%@include file="/include/foot.jsp"%>
