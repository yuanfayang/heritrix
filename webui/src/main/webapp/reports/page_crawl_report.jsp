
<%@ page import="org.archive.crawler.webui.CrawlJob"%>
<%@ page import="org.archive.crawler.webui.Crawler"%>
<%@ page import="org.archive.crawler.framework.StatisticsTracker"%>
<%@ page import="org.archive.crawler.framework.JobStage"%>
<%@ page import="org.archive.util.ArchiveUtils"%>
<%@ page import="org.archive.crawler.framework.CrawlController"%>
<%@ page import="java.util.TreeSet"%>
<%@ page import="org.archive.crawler.webui.ReportLine"%>
<%@ page import="org.archive.crawler.datamodel.CrawlURI"%>

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
    Crawler crawler = (Crawler)request.getAttribute("crawler");
    CrawlJob crawljob = (CrawlJob)request.getAttribute("job");
    StatisticsTracker stats = (StatisticsTracker)request.getAttribute("stats");
    String qs = crawler.getQueryString() + "&job=" + crawljob.getName();
    String qsStatus = "&statusorder=" + request.getAttribute("statusorder");
    String qsFiletype = "&fileorder=" + request.getAttribute("fileorder");
    String qsHosts = "&hostsorder=" + request.getAttribute("hostsorder");

    // TODO: Handle crawl reports for completed jobs.

    if(!crawljob.hasReports())
    {
        // NO JOB SELECTED - ERROR
%>
        <p>&nbsp;<p>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<b>Can only display report for ongoing crawls.</b>
<%
} else {
%>
    <hr />
        <table border="0">
            <tr>
                <td valign="top">
                    <table border="0" cellspacing="0" cellpadding="0" >
                        <tr>
                            <td>
                                <b>Job name:</b>&nbsp;
                            </td>
                            <td>
                                <%=crawljob.getName()%>
                            </td>
                        </tr>
                        <tr>
                            <td>
                                <b>Status:</b>&nbsp;
                            </td>
                            <td>
                                <%=crawljob.getCrawlStatus()%>
                            </td>
                        </tr>
                        <tr>
                            <td>
                                <b>Time:</b>&nbsp;
                            </td>
                            <td>
                                <%
                                                                    long time = (stats.getCrawlerTotalElapsedTime())/1000;
                                                                    if(time>3600)
                                                                    {
                                                                        //got hours.
                                                                        out.println(time/3600 + " h., ");
                                                                        time = time % 3600;
                                                                    }
                                                                    
                                                                    if(time > 60)
                                                                    {
                                                                        out.println(time/60 + " min. and ");
                                                                        time = time % 60;
                                                                    }

                                                                    out.println(time + " sec.");
                                %>
                            </td>
                        </tr>
                    </table>
                </td>
                <td>
                    &nbsp;&nbsp;&nbsp;
                </td>
                <td valign="top">
                    <table border="0" cellspacing="0" cellpadding="0" >
                        <tr>
                            <td>
                                <b>Processed docs/sec:</b>&nbsp;
                            </td>
                            <td>
                                <%
                                                                    if(crawljob.getCrawlStatus().equalsIgnoreCase("RUNNING")){
                                                                    // Show current and overall stats.
                                %>
                                    <%=ArchiveUtils.doubleToString(stats.currentProcessedDocsPerSec(),2)%> (<%=ArchiveUtils.doubleToString(stats.processedDocsPerSec(),2)%>)
                                <%
                                                                        } else {
                                                                        // Only show overall stats.
                                    %>
                                    <%=ArchiveUtils.doubleToString(stats.processedDocsPerSec(),2)%>
                                <%
                                }
                                %>
                            </td>
                        </tr>
                        <tr>
                            <td>
                                <b>Processed KB/sec:</b>&nbsp;
                            </td>
                            <td>
                                <%
                                                                    if(crawljob.getCrawlStatus().equalsIgnoreCase("RUNNING")){
                                                                    // Show current and overall stats.
                                %>
                                    <%=ArchiveUtils.doubleToString(stats.currentProcessedKBPerSec(),2)%> (<%=ArchiveUtils.doubleToString(stats.processedKBPerSec(),2)%>)
                                <%
                                    } else {
                                    // Only show overall stats.
                                %>
                                    <%=ArchiveUtils.doubleToString(stats.processedKBPerSec(),2)%>
                                <%
                                }
                                %>
                            </td>
                        </tr>
                        <tr>
                            <td>
                                <b>Total data crawled:</b>&nbsp;
                            </td>
                            <td>
                                <%=ArchiveUtils.formatBytesForDisplay(stats.totalBytesCrawled())%>
                            </td>
                        </tr>
                    </table>
                </td>
            </tr>
        </table>
        
        <p>
        
        <table width="400">
            <tr>
                <td colspan="6">
                    <table>
                        <tr>
                            <td valign="center" ><img 
                            src="<%=request.getContextPath()%>/images/blue.jpg" height="1" width="40"></td>
                            <td align="center"><i>URIs</i></td>
                            <td valign="center" ><img 
                            src="<%=request.getContextPath()%>/images/blue.jpg" height="1" width="300"></td>
                        </tr>
                    </table>
                </td>
            </tr>
            <tr>
                <td nowrap>
                    <b>Discovered:</b>
                </td>
                <td align="right">
                    <%=stats.discoveredUriCount()%>
                </td>
                <td colspan="3">
                    &nbsp;<a class='help' href="javascript:alert('URIs that the crawler has discovered and confirmed to be within scope. \nNOTE: Because the same URI can be fetched multiple times this number may be lower then the number of queued, in process and finished URIs.')">?</a>
                </td>
            </tr>
            <tr>
                <td nowrap>
                    &nbsp;&nbsp;<b>Queued:</b>
                </td>
                <td align="right">
                    <%=stats.queuedUriCount()%>
                </td>
                <td colspan="3">
                    &nbsp;<a class='help' href="javascript:alert('URIs that are waiting to be processed. \nThat is all URI that have been discovered (or should be revisited) that are waiting for processing.')">?</a>
                </td>
            </tr>
            <tr>
                <td nowrap>
                    &nbsp;&nbsp;<b>In progress:</b>
                </td>
                <td align="right">
                    <%=stats.activeThreadCount()%>
                </td>
                <td colspan="3">
                    &nbsp;<a class='help' href="javascript:alert('Number of URIs being processed at the moment. \nThis is based on the number of active threads.')">?</a>
                </td>
            </tr>
            <tr>
                <td>
                </td>
                <td align="right" width="1" nowrap>
                    &nbsp;&nbsp;<i>Total</i>
                </td>
                <td align="right" width="1" nowrap>
                    &nbsp;&nbsp;<i>Successfully</i>
                </td>
                <td align="right" width="1" nowrap>
                    &nbsp;&nbsp;<i>Failed</i>
                </td>
                <td align="right" width="1" nowrap>
                    &nbsp;&nbsp;<i>Disregarded</i>
                </td>
            </tr>
            <tr>
                <td nowrap>
                    &nbsp;&nbsp;<b>Finished:</b>
                </td>
                <td align="right">
                    <%=stats.finishedUriCount()%>
                </td>
                <td align="right">
                    <%=stats.successfullyFetchedCount()%>
                </td>
                <td align="right">
                    <%=stats.failedFetchAttempts()%>
                </td>
                <td align="right">
                    <%=stats.disregardedFetchAttempts()%>
                </td>
            </tr>
        </table>
        
        <hr />

        <table cellspacing="0">
            <tr>
                <th>
                    <a href="<%=request.getContextPath()%>/reports/do_show_crawl_report.jsp?<%=qs+qsFiletype+qsHosts%>&statusorder=name">Status code</a>
                </th>
                <th width="200">
                    <a href="<%=request.getContextPath()%>/reports/do_show_crawl_report.jsp?<%=qs+qsFiletype+qsHosts%>&statusorder=uris">Documents</a>
                </th>
            </tr>
            <%
                boolean alt = true;
                TreeSet<ReportLine> status = 
                    (TreeSet<ReportLine>)request.getAttribute("statuscode");

                for (ReportLine rl : status) {
                    long count = rl.numberOfURIS;
                    long displaybarwidth = 0;
                    if(stats.successfullyFetchedCount()/6>0){
                       displaybarwidth = count*100/(stats.successfullyFetchedCount()/6);
                    } 
                    if(displaybarwidth==0){
                       displaybarwidth=1;
                    }
            %>
                <tr <%=alt?"bgcolor=#EEEEFF":""%>>
                    <td nowrap>
                        <a style="text-decoration: none;" href="<%=request.getContextPath()%>/logs/do_show_log.jsp?<%=qs%>&log=CRAWL&mode=REGEXPR&regexpr=^.{24}\s*<%=rl.legend%>&grep=true">
                            <%=CrawlURI.fetchStatusCodesToString(Integer.parseInt(rl.legend))%>
                        </a>&nbsp;
                    </td>
                    <td nowrap>
                        <img src="<%=request.getContextPath()%>/images/blue.jpg" height="10" width="<%=displaybarwidth%>"> <%=count%> &nbsp;
                    </td>
                </tr>
            <%
                    alt = !alt;
                }
            %>                
            <tr>
                <td>&nbsp;</td>
            </tr>
            <tr>
                <th width="100">
                    <a href="<%=request.getContextPath()%>/reports/do_show_crawl_report.jsp?<%=qs+qsStatus+qsHosts%>&fileorder=name">File type</a>
                </th>
                <th width="200">
                    <a href="<%=request.getContextPath()%>/reports/do_show_crawl_report.jsp?<%=qs+qsStatus+qsHosts%>&fileorder=uris">Documents</a>
                </th>
                <th>
                    <a href="<%=request.getContextPath()%>/reports/do_show_crawl_report.jsp?<%=qs+qsStatus+qsHosts%>&fileorder=bytes">Data</a>
                </th>
            </tr>
            <%
                alt = true;
                TreeSet<ReportLine> filetypes = 
                    (TreeSet<ReportLine>)request.getAttribute("filetypes");

                for (ReportLine rl : filetypes) {
                    long count = rl.numberOfURIS;
                    long displaybarwidth = 0;
                    if(stats.successfullyFetchedCount()/6>0){
                       displaybarwidth = count*100/(stats.successfullyFetchedCount()/6);
                    } 
                    if(displaybarwidth==0){
                       displaybarwidth=1;
                    }
            %>
                <tr <%=alt?"bgcolor=#EEEEFF":""%>>
                    <td nowrap>
                        <a style="text-decoration: none;" href="<%=request.getContextPath()%>/logs/do_show_log.jsp?<%=qs%>&log=CRAWL&mode=REGEXPR&regexpr=^.{24}.*<%=rl.legend%>&grep=true">
                            <%=rl.legend%>
                        </a>&nbsp;
                    </td>
                    <td nowrap>
                        <img src="<%=request.getContextPath()%>/images/blue.jpg" height="10" width="<%=displaybarwidth%>"> <%=count%> &nbsp;
                    </td>
                    <td align="right">
                        <%=ArchiveUtils.formatBytesForDisplay(rl.bytes)%>
                    </td>
                </tr>
            <%
                    alt = !alt;
                }
            %>                
            <tr>
                <td>&nbsp;</td>
            </tr>
            <tr>
                <th>
                    Hosts&nbsp;
                </th>
                <th>
                    <a href="<%=request.getContextPath()%>/reports/do_show_crawl_report.jsp?<%=qs+qsStatus+qsFiletype%>&hostsorder=uris">Documents</a>
                </th>
                <th>
                    <a href="<%=request.getContextPath()%>/reports/do_show_crawl_report.jsp?<%=qs+qsStatus+qsFiletype%>&hostsorder=bytes">Data</a>
                </th>
                <th>
                    <a href="<%=request.getContextPath()%>/reports/do_show_crawl_report.jsp?<%=qs+qsStatus+qsFiletype%>&hostsorder=lastactive">Time since last URI finished</a>
                </th>
            </tr>
            <%
                alt = true;
                TreeSet<ReportLine> hosts = 
                    (TreeSet<ReportLine>)request.getAttribute("hosts");

                for (ReportLine rl : hosts) {
                    long count = rl.numberOfURIS;
                    long displaybarwidth = 0;
                    if(stats.successfullyFetchedCount()/6>0){
                       displaybarwidth = count*100/(stats.successfullyFetchedCount()/6);
                    } 
                    if(displaybarwidth==0){
                       displaybarwidth=1;
                    }
            %>
                <tr <%=alt?"bgcolor=#EEEEFF":""%>>
                    <td nowrap>
                        <a style="text-decoration: none;" href="<%=request.getContextPath()%>/logs/do_show_log.jsp?<%=qs%>&log=CRAWL&mode=REGEXPR&regexpr=^.{24}.*<%=rl.legend%>&grep=true">
                            <%=rl.legend%>
                        </a>&nbsp;
                    </td>
                    <td nowrap>
                        <img src="<%=request.getContextPath()%>/images/blue.jpg" height="10" width="<%=displaybarwidth%>"> <%=count%> &nbsp;
                    </td>
                    <td align="right">
                        <%=ArchiveUtils.formatBytesForDisplay(rl.bytes)%>
                    </td>
                    <td align="right">
                        <%=ArchiveUtils.formatMillisecondsToConventional(System.currentTimeMillis()-rl.lastActive)%>
                    </td>
                </tr>
            <%
                    alt = !alt;
                }
            %>                
                <tr>
                    <td colspan="4">Showing top <%=hosts.size() %> hosts only</td>
                </tr>
        </table>
<%
    } 
%>
</body>
</html>
