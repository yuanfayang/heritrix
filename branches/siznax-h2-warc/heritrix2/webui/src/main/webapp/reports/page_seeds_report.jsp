<%@ page pageEncoding="UTF-8" %> 

<%@ page import="org.archive.crawler.webui.CrawlJob"%>
<%@ page import="org.archive.crawler.webui.Crawler"%>
<%@ page import="org.archive.crawler.framework.StatisticsTracker"%>
<%@ page import="org.archive.crawler.datamodel.CrawlURI"%>
<%@ page import="javax.management.openmbean.CompositeData"%>

<html>
<head>
    <%@include file="/include/header.jsp"%>
    <title>Heritrix Crawl Report</title>
    <style>
        tr.headerrow {background-color: #ccccff }
        tr.totalrow {background-color: #ccccff }
        .percent {font-size: 70%}
    </style>
</head>
<body>

<%@include file="/include/nav.jsp"%>


<%
    Crawler crawler = (Crawler)Text.get(request, "crawler");
    CrawlJob crawljob = (CrawlJob)Text.get(request, "job");
    StatisticsTracker stats = (StatisticsTracker)Text.get(request, "stats");
    String qs = crawler.getQueryString() + "&job=" + crawljob.getName();

    // TODO: Handle crawl reports for completed jobs.

    if(!crawljob.hasReports())
    {
        // NO JOB SELECTED - ERROR
        // TODO: Make report available for completed jobs
%>
        <p>&nbsp;<p>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<b>Non active job selected</b>
<%
    } else {
%>
        <table cellspacing=0>
            <tr>
                <th style="border-bottom:solid 1px #666666;">
                    Status code
                    <br> and Disposition
                </th>
                <th style="border-bottom:solid 1px #666666;" align="left">
                    Seeds for job '<%=crawljob.getName()%>'
                </th>
            </tr>
            <%
                CompositeData[] seeds = stats.seedReport();

                for (CompositeData seed : seeds) {
                    int code = (Integer)seed.get("statusCode");
                    String statusCode = code==0?
                        "" : CrawlURI.fetchStatusCodesToString(code);
                    String statusColor = "black";
                    if (code<0 || code >= 400) {
                        statusColor = "red";
                    } else if(code == 200) {
                        statusColor = "green";
                    }
            %>
                    <tr >
                        <td style="border-bottom:solid 1px #666666;"
                            align="left"> 
             <%
                 if(code!=0) {
              %>
                            &nbsp;<font color="<%=statusColor%>"><%=statusCode%></font>&nbsp;<br>
             <%
                 }
             %>
                            <a href="<%=request.getContextPath()%>/logs/do_show_log.jsp?<%=qs%>&log=CRAWL&mode=REGEXPR&regexpr=^[^ ].*<%=seed.get("uri")%>&grep=true" style="text-decoration: none;">
                            <%=seed.get("disposition")%></a>
                        </td>
                        <td style="border-bottom:solid 1px #666666;" nowrap>
                            <%=seed.get("uri")%>
             <%
                if(seed.get("redirectUri")!=null) {
             %>
                        <br>&rarr; <a href="<%=seed.get("redirectUri")%>"><%=seed.get("redirectUri")%></a>
             <%
                }
             %>
                        </td>


                    </tr>
            <%
                }
            %>
        </table>

<%    

    }
%>
</body>
</html>
