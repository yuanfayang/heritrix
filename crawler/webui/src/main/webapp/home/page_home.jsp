<%@ page import="java.util.Collection" %>
<%@ page import="org.archive.crawler.webui.Crawler" %>
<%@ page import="org.archive.crawler.webui.Text" %>
<%

Collection<Crawler> crawlers = (Collection)request.getAttribute("crawlers");
boolean jndiWarning = (Boolean)request.getAttribute("jndiWarning");

%>
<html>
<head>
<%@include file="/include/header.jsp"%>
<title>Heritrix Crawler List</title>
</head>
<body>

<%@include file="/include/nav.jsp"%>

<h3>Known Crawlers:</h3>

<table class="info">
<tr>
<th class="info">Crawler</th>
<th class="info">Status</th>
<th class="info">Actions</th>
</tr>

<% int row = 1; %>
<% for (Crawler crawler: crawlers) { %>
<% row = -row + 1; %>

<tr>

<td class="info<%=row%>">
<% if (crawler.getError() == null) { %>
    <a href="<%=request.getContextPath()%>/crawler_area/do_show_crawler.jsp?<%=crawler.getQueryString()%>">
<% } %>
<%=Text.html(crawler.getLegend())%>
<% if (crawler.getError() == null) { %>
    </a>
<% } %>

</td>
<td class="info<%=row%>">
<% if (crawler.getError() != null) { %>
<%=crawler.getError()%>
<% } %>
</td>


<td class="info<%=row%>">

<a href="do_show_authenticate_crawler.jsp?<%=crawler.getQueryString()%>">
Authenticate
</a>

<% if (crawler.getError() == null) { %>
<a href="do_stop_crawler.jsp?<%=crawler.getQueryString()%>">
| Stop
</a>
<% } %>


<% if (crawler.getSource() == Crawler.Source.MANUAL) { %>
<a href="do_remove_crawler.jsp?<%=crawler.getQueryString()%>">
| Remove
</a>
<% } %>


</td>
</tr>

<% } %>

</table>

<a href="do_show_add_crawler.jsp">Add</a>
|
<a href="do_crawler_refresh.jsp">Refresh</a>

<% if (jndiWarning) { %>
Note: No JNDI server was configured for the servlet container JVM.
<% } %>