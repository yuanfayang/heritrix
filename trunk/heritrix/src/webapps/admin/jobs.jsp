<%@include file="/include/secure.jsp"%>
<%@include file="/include/handler.jsp"%>

<%
	String title = "Crawl jobs";
	int tab = 1;
%>

<%@include file="/include/head.jsp"%>


<p>
<b>New jobs</b><br>
<ul>
	<li><a href="/admin/jobs/new.jsp">Create new crawl job</a>
</ul>

<% if(handler.isCrawling()){ %>
	<p>
	<b>Current job</b> - <i><%=handler.getCurrentJob().getJobName()%></i>
	<ul>
		<li><a href="/admin/reports/crawljob.jsp">View crawl report</a>
		<li><a target="_blank" href="/admin/jobs/vieworder.jsp?job=<%=handler.getCurrentJob().getUID()%>">View crawl order (xml file)</a>
		<li><a href="/admin/jobs/jobconfig.jsp">Modify crawl order</a>
	</ul>
<% } %>

<p>
<b><a href="/admin/jobs/pending.jsp">View pending jobs</a></b> (<%=handler.getPendingJobs().size()%>)<br>
<b><a href="/admin/jobs/completed.jsp">View completed jobs</a></b> (<%=handler.getCompletedJobs().size()%>)<br>


<%@include file="/include/foot.jsp"%>
