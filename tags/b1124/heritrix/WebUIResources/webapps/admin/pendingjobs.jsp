<%@include file="/include/handler.jsp"%>
<%@ page import="org.archive.crawler.datamodel.CrawlOrder,org.archive.crawler.admin.CrawlJob,java.util.ArrayList" %>

<%
	ArrayList jobs = handler.getPendingJobs();
%>

<html>
	<head>
		<title>Pending jobs</title>
	</head>
	<body>
		<strong>Pending jobs</strong>
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
							<a href="editjob.jsp?name=<%=job.getJobName()%>">Edit</a>
							<a href="pendingjobs.jsp?action=delete&name=<%=job.getJobName()%>">Delete</a>
						</td>
					</tr>
			<%
				}
			%>
		</table>
		
		<p>
			<a href="newjob.jsp">New job</a>
		<p>
			<a href="main.jsp">Main page</a>
	</body>
</html>