<%@include file="/include/handler.jsp"%>
<%@include file="/include/secure.jsp"%>
<%@ page import="org.archive.crawler.admin.CrawlJob,java.util.List" %>

<%
    String sAction = request.getParameter("action");
    if(sAction != null){
        // Need to handle an action 
        if(sAction.equalsIgnoreCase("delete")){
            handler.deleteJob(request.getParameter("job"));
        }
    }   

	List jobs = handler.getCompletedJobs();

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
					Options
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
							<a href="/admin/reports/seeds.jsp?job=<%=job.getUID()%>&nav=3">Seeds report</a>
                            <a href="/admin/jobs/viewseeds.jsp?job=<%=job.getUID()%>">Seed file</a>
                            <a href="/admin/logs.jsp?job=<%=job.getUID()%>&nav=3">Logs</a>
                            <a href="/admin/jobs/completed.jsp?action=delete&job=<%=job.getUID()%>&nav=3">Delete</a>
						</td>
					</tr>
					<% if(job.getErrorMessage()!=null){ %>
					<tr>
						<td>
						</td>
						<td colspan="2">
							<pre><font color="red"><%=job.getErrorMessage()%></font></pre>
						</td>
					</tr>
					<% } %>
			<%
				}
			%>
		</table>
		
<%@include file="/include/foot.jsp"%>
