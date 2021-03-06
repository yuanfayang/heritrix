<%@ page pageEncoding="UTF-8" %> 

<%@ page import="org.archive.crawler.webui.CrawlJob"%>
<%@ page import="org.archive.crawler.webui.Crawler"%>
<%@ page import="org.archive.crawler.framework.CrawlController"%>

<html>
<head>
    <%@include file="/include/header.jsp"%>
    <title>Heritrix Frontier Report</title>
</head>
<body>

<%@include file="/include/nav.jsp"%>
<%
    Crawler crawler = (Crawler)request.getAttribute("crawler");
    CrawlJob crawljob = (CrawlJob)request.getAttribute("job");
    CrawlController controller = (CrawlController)request.getAttribute("controller");
    String qs = crawler.getQueryString() + "&job=" + crawljob.getName();
%>

    <hr />

    <pre><%=controller.getFrontierReport() %></pre>

    <hr />

</body>
</html>
