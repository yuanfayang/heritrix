<%@include file="/include/handler.jsp"%>
<%@include file="/include/secure.jsp"%>
<%@ page import="org.archive.crawler.datamodel.CrawlOrder,org.archive.crawler.admin.CrawlJob,java.util.Vector" %>

<%
	String sAction = request.getParameter("action");

	if(sAction != null)
	{
		// Need to handle an action	
		if(sAction.equalsIgnoreCase("delete"))
		{
			try
			{
				handler.deleteJob(request.getParameter("job"));
			}
			catch(java.lang.NumberFormatException e){}
		}
		
	}	

	Vector jobs = handler.getPendingJobs();
	String title = "Pending crawl jobs";
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
					Actions
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
							<a target="_blank" href="/admin/jobs/vieworder.jsp?job=<%=job.getUID()%>">View order</a>
							<a href="/admin/jobs/configure.jsp?job=<%=job.getUID()%>">Modify</a>
							<a href="/admin/jobs/poweredit.jsp?job=<%=job.getUID()%>">Power edit</a>
							<a href="/admin/jobs/pending.jsp?action=delete&job=<%=job.getUID()%>">Delete</a>
						</td>
					</tr>
			<%
				}
			%>
		</table>
		
<%@include file="/include/foot.jsp"%>
