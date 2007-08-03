
<%@ page import="org.archive.crawler.webui.CrawlJob"%>
<%@ page import="org.archive.crawler.webui.Crawler"%>
<%@ page import="org.archive.crawler.framework.JobController"%>

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
    JobController controller = (JobController)Text.get(request, "controller");
    String qs = crawler.getQueryString() + "&job=" + crawljob.getName();
%>

<% if(request.getAttribute("message")!=null){ %>
    <p class="flashMessage"><%=request.getAttribute("message") %></p>
<% } %>

<p>
<b>Reports for <%=crawljob.getName()%> (<i><%=crawljob.getCrawlStatus()%></i>)</b><br>
<ul>
    <% if (crawljob.hasReports()) { %>
    <li><a href="<%=request.getContextPath()%>/reports/do_show_crawl_report.jsp?<%=qs%>">Crawl report</a></li>
    <li><a href="<%=request.getContextPath()%>/reports/do_show_seeds_report.jsp?<%=qs%>">Seed report</a></li>
    <% } else { %>
    <li>Crawl report (unavailable)</li>
    <li>Seed report (unavailable)</li>
    <% } %>
    <% if (crawljob.hasReports()) { %>
    <li><a href="<%=request.getContextPath()%>/reports/do_show_frontier_report.jsp?<%=qs%>">Frontier report</a><br>
    <%=controller.getFrontierReportShort()%></li>
    <% } else { %>
    <li>Frontier report (unavailable)</li>
    <% } %>
    <% if (crawljob.hasReports()) { %>
    <li><a href="<%=request.getContextPath()%>/reports/do_show_processors_report.jsp?<%=qs%>">Processors report</a></li>
    <% } else { %>
    <li>Processors report (unavailable)</li>
    <% } %>
    <% if (crawljob.hasReports()) { %>
    <li><a href="<%=request.getContextPath()%>/reports/do_show_threads_report.jsp?<%=qs%>">ToeThread report</a><br>
    <%=controller.getToeThreadReportShort()%></li>
    <% } else { %>
    <li>ToeThread report (unavailable)</li>
    <% } %>
</ul>
<% if (crawljob.hasReports()) { %>
<p>The crawler generates reports when it finishes a job.  Clicking here on <a href="<%=request.getContextPath()%>/reports/do_force_reports.jsp?<%=qs%>">Force generation of end-of-crawl Reports</a> will force the writing of reports to disk.  Clicking this link will return you to this page. Look to the disk for the generated reports.  Each click overwrites previously generated reports. Use this facility when the crawler has hung threads that can't be interrupted.</p>
<% } %>
</body>
</html>

