<%@include file="/include/handler.jsp"%>
<%@include file="/include/secure.jsp"%>
<%@ page import="org.archive.crawler.datamodel.CrawlOrder,org.archive.crawler.framework.CrawlJob,java.util.Vector" %>

<%
	Vector jobs = handler.getCompletedJobs();

	String title = "Completed job";
	int navigation = 3;
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
							<a target="_blank" href="viewjob.jsp?job=<%=job.getUID()%>">View order</a>
							<a href="viewstatistics.jsp?job=<%=job.getUID()%>&nav=3">View statistics</a>
						</td>
					</tr>
			<%
				}
			%>
		</table>
		
<%@include file="/include/foot.jsp"%>
