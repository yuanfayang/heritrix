<%@include file="/include/secure.jsp"%>
<%@include file="/include/handler.jsp"%>

<%
	String title = "Crawl jobs";
	int tab = 1;
%>

<%@include file="/include/head.jsp"%>

<% if(request.getParameter("message")!=null && request.getParameter("message").length() >0){ %>
	<p>
		<font color="red"><b><%=request.getParameter("message")%></b></font>
<% } %>
<p>
<b>New jobs</b><br>
<ul>
	<li><a href="/admin/jobs/new.jsp">Create new crawl job</a>
	<li><a href="/admin/jobs/basedon.jsp">Create new crawl job based on a profile</a>
	<li><a href="/admin/jobs/basedon.jsp?type=jobs">Create new crawl job based on an existing job</a>
<%-- // DISABLED FOR NOW
	<li><a href="/admin/jobs/resumefromjob.jsp">Resume an existing job from a checkpoint</a>
--%>
</ul>

<% if(handler.isCrawling()){ %>
	<p>
	<b>Current job</b> - <i><%=handler.getCurrentJob().getJobName()%></i>
	<ul>
		<li><a href="/admin/reports/crawljob.jsp">View crawl report</a>
        <li><a href="/admin/reports/seeds.jsp">View seeds report</a>
        <li><a href="/admin/jobs/journal.jsp?job=<%=handler.getCurrentJob().getUID()%>">Journal</a>
		<li><a target="_blank" href="/admin/jobs/vieworder.jsp?job=<%=handler.getCurrentJob().getUID()%>">View crawl order (xml file)</a>
		<li><a href="/admin/jobs/configure.jsp?job=<%=handler.getCurrentJob().getUID()%>">Edit configuration</a>
	</ul>
<% } %>

<p>
<b><a href="/admin/jobs/pending.jsp">View pending jobs</a></b> (<%=handler.getPendingJobs().size()%>)<br>
<b><a href="/admin/jobs/completed.jsp">View completed jobs</a></b> (<%=handler.getCompletedJobs().size()%>)<br>


<%@include file="/include/foot.jsp"%>
