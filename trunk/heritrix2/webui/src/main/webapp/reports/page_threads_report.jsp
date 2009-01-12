<%@ page pageEncoding="UTF-8" %> 

<%@ page import="org.archive.crawler.webui.CrawlJob"%>
<%@ page import="org.archive.crawler.webui.Crawler"%>
<%@ page import="org.archive.crawler.framework.CrawlController"%>

<html>
<head>
    <%@include file="/include/header.jsp"%>
    <title>Heritrix Threads Report</title>
</head>
<body>

<%@include file="/include/nav.jsp"%>
<%
    Crawler crawler = (Crawler)request.getAttribute("crawler");
    CrawlJob crawljob = (CrawlJob)request.getAttribute("job");
    CrawlController controller = (CrawlController)request.getAttribute("controller");
    String qs = crawler.getQueryString() + "&job=" + crawljob.getName();
%>

<% if(request.getAttribute("message")!=null){ %>
    <p class="flashMessage"><%=request.getAttribute("message") %></p>
<% } %>

    <hr />

    <pre><%=controller.getToeThreadReport() %></pre>

    <hr />

    <form name="frmThread" method="GET" action="do_kill_thread.jsp" accept-charset='UTF-8'>
        <input type="hidden" name="host" value="<%=crawler.getHost() %>">
        <input type="hidden" name="port" value="<%=crawler.getPort() %>">
        <input type="hidden" name="id" value="<%=crawler.getIdentityHashCode() %>">
        <input type="hidden" name="job" value="<%=crawljob.getName() %>">
        <b>Thread number:</b> <input name="threadNumber" size="3"> <input type="checkbox" name="replace" value="replace">Replace thread <input type="submit" value="Kill thread">
    </form>

</body>
</html>
