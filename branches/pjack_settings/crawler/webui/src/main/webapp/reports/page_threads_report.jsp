
<%@ page import="org.archive.crawler.webui.CrawlJob"%>
<%@ page import="org.archive.crawler.webui.Crawler"%>
<%@ page import="org.archive.crawler.framework.JobController"%>

<html>
<head>
    <%@include file="/include/header.jsp"%>
    <title>Heritrix Threads Report</title>
</head>
<body>

<%@include file="/include/nav.jsp"%>
<%
    Crawler crawler = (Crawler)request.getAttribute("crawler");
    CrawlJob crawljob = (CrawlJob)request.getAttribute("crawljob");
    JobController controller = (JobController)request.getAttribute("controller");
    String qs = crawler.getQueryString() + "&job=" + crawljob.getName();
%>

    <hr />

    <pre><%=controller.getToeThreadReport() %></pre>

    <hr />

</body>
</html>