<%@include file="/include/handler.jsp"%>
<%@include file="/include/secure.jsp"%>
<%@ page import="org.archive.crawler.datamodel.CrawlOrder,org.archive.crawler.framework.CrawlJob,java.util.Vector" %>

<%
	String sAction = request.getParameter("action");

	if(sAction != null)
	{
		// Need to handle an action	
		if(sAction.equalsIgnoreCase("delete"))
		{
			try
			{
				handler.removeJob(request.getParameter("job"));
			}
			catch(java.lang.NumberFormatException e){}
		}
		
	}	

	Vector jobs = handler.getPendingJobs();
	String title = "Pending jobs";
	int navigation = 2;
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
							<a target="_blank" href="/admin/jobs/vieworder.jsp?job=<%=job.getUID()%>">View</a>
							<a href="/admin/jobs/pending.jsp?action=delete&job=<%=job.getUID()%>">Delete</a>
						</td>
					</tr>
			<%
				}
			%>
		</table>
		
<%@include file="/include/foot.jsp"%>
