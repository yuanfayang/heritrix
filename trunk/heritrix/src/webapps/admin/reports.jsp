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
<b>Reports on ongoing and finished crawl jobs</b><br>
<% if(crawling == false && jobs.size() == 0){ %>
		&nbsp;No crawl jobs have been started
<% } else { %>
	<ul>
		<%  if(crawling){ %>
				<li>
					<%=handler.getCurrentJob().getJobName()%>
					<i><%=handler.getCurrentJob().getStatus()%></i> 
					-
					<a href="/admin/reports/crawljob.jsp?job=<%=handler.getCurrentJob().getUID()%>">Crawl report</a>
					<a href="/admin/reports/seeds.jsp?job=<%=handler.getCurrentJob().getUID()%>">Seed report</a>
		<% 
			}
			for(int i=jobs.size()-1; i>=0; i--){ 
				CrawlJob job = (CrawlJob)jobs.get(i);
		%>
				<li>
					<%=job.getJobName()%>
					<i><%=job.getStatus()%></i> 
					-
					<a href="/admin/reports/crawljob.jsp?job=<%=job.getUID()%>">Crawl report</a>
					<a href="/admin/reports/seeds.jsp?job=<%=job.getUID()%>">Seed report</a>
		<% 
			} 
		%>
	</ul>
<% } %>

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
