<%@include file="/include/secure.jsp"%>
<%@include file="/include/handler.jsp"%>
<%@ page import="org.archive.crawler.framework.CrawlJob,java.util.Vector" %>

<%
	String title = "Reports";
	int tab = 4;
%>

<%@include file="/include/head.jsp"%>

<%
	boolean crawling = handler.isCrawling();
	Vector jobs = handler.getCompletedJobs();
%>

<p>
<b>Reports on ongoing and finished crawl jobs</b><br>
<% if(crawling == false && jobs.size() == 0){ %>
		&nbsp;No crawl jobs have been started
<% } else { %>
	<ul>
		<% if(crawling){ %>
				<li><a href="/admin/reports/crawljob.jsp?job=<%=handler.getCurrentJob().getUID()%>"><%=handler.getCurrentJob().getJobName()%></a> <i><%=handler.getCurrentJob().getStatus()%></i>
		<% 
			}
			for(int i=jobs.size()-1; i>=0; i--){ 
				CrawlJob job = (CrawlJob)jobs.get(i);
		%>
				<li><a href="/admin/reports/crawljob.jsp?job=<%=job.getUID()%>"><%=job.getJobName()%></a> <i><%=job.getStatus()%></i>
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
<ul>
<% } %>

<%@include file="/include/foot.jsp"%>
