<%@ page pageEncoding="UTF-8" %> 
<%@ page import="org.archive.crawler.webui.Text" %>
<%@ page import="org.archive.crawler.webui.Crawler" %>
<%@ page import="org.archive.crawler.framework.StatisticsTracker" %>
<%@ page import="org.archive.crawler.framework.CrawlController" %>
<%@ page import="org.archive.util.ArchiveUtils" %>
<%@ page import="org.archive.crawler.webui.CrawlJob"%>
<%@ page import="javax.management.openmbean.CompositeData"%>

<% 

Crawler crawler = (Crawler)Text.get(request, "crawler");
CompositeData memory = (CompositeData)Text.get(request, "memory"); 
CrawlJob job = (CrawlJob)Text.get(request, "job"); 

String qs = crawler.getQueryString() + "&job=" + job.getName();


%>

<html>
<head>
    <%@include file="/include/header.jsp"%>
    <title>Heritrix Console</title>
</head>
<body>

<%@include file="/include/nav.jsp"%>
    <table border="0" cellspacing="0" cellpadding="0"><tr><td>
    <fieldset style="width: 750px">
        <legend> 
        <b>
            <span class="legendTitle">Crawler:</span> 
            <span class="status crawling"><%=crawler.getLegend()%></span>
        </b>
        </legend>
        <div style="float:right;padding-right:50px;">
            <b>Memory</b><br>
            <div style="padding-left:20px">
                <%=((Long)memory.get("used"))/1024L%> KB 
                used<br>
                <%=((Long)memory.get("committed"))/1024L%> KB
                committed heap<br>
                <%=((Long)memory.get("max"))/1024L%> KB
                max heap
            </div>
        </div>
        <b>Jobs</b>
        <div style="padding-left:20px">
            <%=job.getCrawlStatus()%>: <i><%=job.getName()%></i>
            <!-- TODO: Consider pending jobs -->
        </div>

        <b>Alerts:</b> 0 (0 new) <!-- FIXME
            <a style="color: #000000" 
                href="<=request.getContextPath()>/console/alerts.jsp">
                <=heritrix.getAlertsCount()> (<=heritrix.getNewAlertsCount()> new)
            </a> -->
            
         </fieldset>

    
    <a href="<%=request.getContextPath()%>/console/do_show_job_console.jsp?<%=qs%>">Refresh</a>
    </td></tr>
    <tr><td>
        <p>
            &nbsp;
        <p>
            &nbsp;
    </td></tr>
    <tr><td>

    </td></tr></table>

</body>
</html>
