<%@include file="/include/handler.jsp"%>
<%@include file="/include/secure.jsp"%>
<%@ page import="org.archive.crawler.framework.CrawlJob,java.util.Vector" %>

<%
	Vector jobs = handler.getCompletedJobs();

	String title = "Completed job";
	int navigation = 3;
%>

<%@include file="/include/head.jsp"%>

		<table border="0" cellspacing="0" cellpadding="0">
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
			<tr>
				<td colspan="3" height="1" bgcolor="#000000">
				</td>
			</tr>
			<%
				boolean altern = false;
				for(int i=0 ; i	< jobs.size() ; i++)
				{
					CrawlJob job = (CrawlJob)jobs.get(i);
			%>		
					<tr>
						<td <%=altern?"bgcolor='#f0f0f0'":""%>>
							&nbsp;&nbsp;<%=job.getJobName()%>&nbsp;&nbsp;
						</td>
						<td <%=altern?"bgcolor='#f0f0f0'":""%>>
							&nbsp;&nbsp;<%=job.getStatus()%>&nbsp;&nbsp;
						</td>
						<td <%=altern?"bgcolor='#f0f0f0'":""%>>
							&nbsp;&nbsp;<a target="_blank" href="/admin/jobs/vieworder.jsp?job=<%=job.getUID()%>">View order</a>
							<a href="/admin/jobs/viewstatistics.jsp?job=<%=job.getUID()%>&nav=3">View statistics</a>&nbsp;&nbsp;
						</td>
					</tr>
			<%
					altern = !altern;
				}
			%>
		</table>
		
<%@include file="/include/foot.jsp"%>
