<%@ page pageEncoding="UTF-8" %> 

<%@ page import="org.archive.crawler.webui.CrawlJob"%>
<%@ page import="org.archive.crawler.webui.Crawler"%>
<%@ page import="java.util.List"%>

<html>
<head>
    <%@include file="/include/header.jsp"%>
    <title>Heritrix Reports</title>
</head>
<body>

<%@include file="/include/nav.jsp"%>
<%
    Crawler crawler = (Crawler)Text.get(request, "crawler");
    CrawlJob crawljob = (CrawlJob)Text.get(request, "job");
    String url = request.getContextPath() + "/reports/do_show_completed_report.jsp?" 
        + Text.jobQueryString(request);
    List<String> reports = (List)Text.get(request, "reports");
%>

<% if(request.getAttribute("message")!=null){ %>
    <p class="flashMessage"><%=request.getAttribute("message") %></p>
<% } %>

<p>
<b>Reports for <%=crawljob.getName()%> (completed)</b><br>
<ul>

<% for (String s: reports) { %>
<li>
<a href="<%=url%>&report=<%=Text.query(s)%>">
<%=Text.html(s)%>
</a>
</li>

<% } %>

</ul>

</body>
</html>

