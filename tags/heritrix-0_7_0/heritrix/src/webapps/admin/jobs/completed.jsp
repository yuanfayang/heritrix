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

		<table border="0" cellspacing="0" cellpadding="1"> 
			<tr>
			    <th>
			        UID
			    </th>
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
			    boolean alt = true;
				for(int i=0 ; i	< jobs.size() ; i++)
				{
					CrawlJob job = (CrawlJob)jobs.get(i);
			%>		
					<tr <%=alt?"bgcolor='#EEEEFF'":""%>>
						<td>
                            <code><%=job.getUID()%></code>&nbsp;&nbsp;
						</td>
						<td>
							<%=job.getJobName()%>&nbsp;&nbsp;
						</td>
						<td>
							<i><%=job.getStatus()%></i>&nbsp;&nbsp;&nbsp;
						</td>
						<td>
							<a style="color: #003399;" class="underLineOnHover" target="_blank" href="/admin/jobs/vieworder.jsp?job=<%=job.getUID()%>">Crawl order</a>
							&nbsp;
                            <a style="color: #003399;" class="underLineOnHover" href="/admin/reports/crawljob.jsp?job=<%=job.getUID()%>&nav=3">Crawl report</a>
                            &nbsp;
							<a style="color: #003399;" class="underLineOnHover" href="/admin/reports/seeds.jsp?job=<%=job.getUID()%>&nav=3">Seeds report</a>
                            &nbsp;
                            <a style="color: #003399;" class="underLineOnHover" href="/admin/jobs/viewseeds.jsp?job=<%=job.getUID()%>">Seed file</a>
                            &nbsp;
                            <a style="color: #003399;" class="underLineOnHover" href="/admin/logs.jsp?job=<%=job.getUID()%>&nav=3">Logs</a>
                            &nbsp;
                            <a style="color: #003399;" class="underLineOnHover" href="/admin/jobs/journal.jsp?job=<%=job.getUID()%>">Journal</a>
                            &nbsp;
                            <a style="color: #003399;" class="underLineOnHover" href="/admin/jobs/completed.jsp?action=delete&job=<%=job.getUID()%>&nav=3">Delete</a>
                            &nbsp;
						</td>
					</tr>
					<% if(job.getErrorMessage()!=null){ %>
					<tr <%=alt?"bgcolor='#EEEEFF'":""%>>
						<td>
						</td>
						<td colspan="2">
							<pre><font color="red"><%=job.getErrorMessage()%></font></pre>
						</td>
					</tr>
					<% } %>
			<%
                    alt = !alt;
				}
			%>
		</table>
		
<%@include file="/include/foot.jsp"%>
