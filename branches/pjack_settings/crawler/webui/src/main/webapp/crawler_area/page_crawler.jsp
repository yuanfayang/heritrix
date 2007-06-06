<%@ page import="java.util.Collection" %>
<%@ page import="org.archive.crawler.webui.Crawler" %>
<%@ page import="org.archive.crawler.webui.Text" %>
<%

Crawler crawler = (Crawler)request.getAttribute("crawler");
Collection<String> profiles = (Collection)request.getAttribute("profiles");
Collection<String> active = (Collection)request.getAttribute("active");
Collection<String> completed = (Collection)request.getAttribute("completed");
int row = 1;

%>
<html>
<head>
<%@include file="/include/header.jsp"%>
<title>Heritrix <%=Text.html(crawler.getLegend())%></title>
</head>
<body>

<%@include file="/include/nav.jsp"%>

<% if (active.isEmpty()) { %>
<p>There are no active jobs on <%=Text.html(crawler.getLegend())%>.
<% } else { %>

<h3>Active Jobs:</h3>

<table class="info">
<% for (String job: active) { %>
<% row = -row + 1; %>
<% String jqs = crawler.getQueryString() + "&job=" + job; %>
<tr>
<td class="info<%=row%>">
<%=job%>
</td>
<td class="info<%=row%>">
<a 
   title="View and control the current status for this job."
   href="<%=request.getContextPath()%>/console/do_show_job_console.jsp?<%=jqs%>">
   Console
</a>
|
Sheets | Seeds | Reports | Logs
</td>
</tr>
<% } %>
</table>
<% } %>

<h3>Profiles:</h3>

<table class="info">
<% for (String job: profiles) { %>
<% row = -row + 1; %>
<tr>
<td class="info<%=row%>">
<%=job%>
</td>
<td class="info<%=row%>">
<% String pqs = crawler.getQueryString() + "&profile=" + job; %>

<a 
   title="View or edit this profile's settings sheets."
   href="<%=request.getContextPath()%>/sheets/do_show_sheets.jsp?<%=pqs%>">
Sheets 
</a>
| 
Seeds 
| 
Copy 
| 
<a 
   title="Launch a new job based on this profile." 
   href="do_show_launch_profile.jsp?<%=pqs%>">
Launch
</a>
</td>
</tr>
<% } %>
</table>

<% if (completed.isEmpty()) { %>
<p>There are no completed jobs.
<% } else { %>
<h3>Completed Jobs:</h3>

<table class="info">
<% for (String job: completed) { %>
<% row = -row + 1; %>
<tr>
<td class="info<%=row%>">
<%=job%>
</td>
<td class="info<%=row%>">
Sheets | Seeds | Reports | Logs
</td>
</tr>
<% } %>
</table>
<% } %>


</body>
</html>