<%@include file="/include/secure.jsp"%>
<%@include file="/include/handler.jsp"%>
<%@ page import="org.archive.crawler.admin.CrawlJob,java.util.List" %>

<%
	String title = "Reports";
	int tab = 4;
%>

<%@include file="/include/head.jsp"%>

<%
	boolean crawling = handler.isCrawling();
	List jobs = handler.getCompletedJobs();
%>

<p>
<table border="0" cellspacing="0" cellpadding="1">
    <tr>
        <td colspan="3">
            <b>Reports on ongoing and finished crawl jobs</b> (newest to oldest)
        </td>
	<% if(crawling == false && jobs.size() == 0){ %>
        <tr>
            <td>&nbsp;No crawl jobs have been started</td>
        </tr>
	<% } else { %>
		<%  if(crawling){ %>
	            <tr bgcolor="#EEEEFF">
	                <td>
					   <%=handler.getCurrentJob().getJobName()%>&nbsp;
					</td>
					<td>
					   <i><%=handler.getCurrentJob().getStatus()%></i>&nbsp;&nbsp;
					</td>
					<td>
						<a href="/admin/reports/crawljob.jsp?job=<%=handler.getCurrentJob().getUID()%>">Crawl report</a>
						&nbsp;
	       				<a href="/admin/reports/seeds.jsp?job=<%=handler.getCurrentJob().getUID()%>">Seed report</a>
	       				&nbsp;
	                </td>
	            </tr>
		<% 
			}
			boolean alt = !crawling;
			for(int i=jobs.size()-1; i>=0; i--){ 
				CrawlJob job = (CrawlJob)jobs.get(i);
		%>
	            <tr <%=alt?"bgcolor='#EEEEFF'":""%>>
	                <td>
						<%=job.getJobName()%>&nbsp;&nbsp;
	                </td>
	                <td>
						<i><%=job.getStatus()%></i>&nbsp;&nbsp;
	                </td>
	                <td>
						<a href="/admin/reports/crawljob.jsp?job=<%=job.getUID()%>">Crawl report</a>
						&nbsp;
						<a href="/admin/reports/seeds.jsp?job=<%=job.getUID()%>">Seed report</a>
	                </td>
	            </tr>
		<%
                alt = !alt; 
			} 
		%>
	<% } %>
</table>

<% if(crawling){ %>
<p>
<b>Internal reports on ongoing crawl</b><br>
<ul>
	<li><a href="/admin/reports/frontier.jsp">Frontier report</a>
	<li><a href="/admin/reports/threads.jsp">Thread report</a>
	<li><a href="/admin/reports/processors.jsp">Processors report</a>
<ul>
<% } %>

<%@include file="/include/foot.jsp"%>
