
<%@ page import="org.archive.crawler.webui.CrawlJob"%>
<%@ page import="org.archive.crawler.webui.Crawler"%>
<%@ page import="org.archive.crawler.framework.StatisticsTracking"%>
<%@ page import="org.archive.util.ArchiveUtils"%>
<%@ page import="org.archive.crawler.framework.CrawlController"%>

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
System.out.println("crawler " + (crawler!=null));    
    CrawlJob crawljob = (CrawlJob)request.getAttribute("crawljob");
System.out.println("crawljob " + (crawljob!=null));    
    StatisticsTracking stats = (StatisticsTracking)request.getAttribute("stats");
System.out.println("stats " + (stats!=null));    
    String qs = crawler.getQueryString() + "&job=" + crawljob.getName();

    // TODO: Handle crawl reports for completed jobs.

    if(crawljob.getState()!=CrawlJob.State.ACTIVE)
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
                                <%=crawljob.getCrawlState()%>
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
                                <%  if(crawljob.getCrawlState().equalsIgnoreCase("RUNNING")){
                                        // Show current and overall stats.
                                %>
                                        <%=ArchiveUtils.doubleToString(stats.currentProcessedDocsPerSec(),2)%> (<%=ArchiveUtils.doubleToString(stats.processedDocsPerSec(),2)%>)
                                <%  } else {
                                        // Only show overall stats.
                                %>
                                        <%=ArchiveUtils.doubleToString(stats.processedDocsPerSec(),2)%>
                                <%  }  %>
                            </td>
                        </tr>
                        <tr>
                            <td>
                                <b>Processed KB/sec:</b>&nbsp;
                            </td>
                            <td>
                                <%  if(crawljob.getCrawlState().equalsIgnoreCase("RUNNING")){
                                        // Show current and overall stats.
                                %>
                                        <%=ArchiveUtils.doubleToString(stats.currentProcessedKBPerSec(),2)%> (<%=ArchiveUtils.doubleToString(stats.processedKBPerSec(),2)%>)
                                <%  } else {
                                        // Only show overall stats.
                                %>
                                        <%=ArchiveUtils.doubleToString(stats.processedKBPerSec(),2)%>
                                <%  }  %>
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
                    Status code
                </th>
                <th width="200" colspan="2">
                    Documents
                </th>
            </tr>
            <% // TODO: Implement status code report %>
            <tr>
                <td>&nbsp;</td>
            </tr>
            <tr>
                <th width="100">
                    File type
                </th>
                <th width="200">
                    Documents
                </th>
                <th>
                    Data
                </th>
            </tr>
            <% // TODO: Implement file type report %>
        </table>
        
        <p>
        
        <table cellspacing="0">
            <tr>
                <th>
                    Hosts&nbsp;
                </th>
                <th>
                    Documents&nbsp;
                </th>
                <th>
                    Data&nbsp;
                </th>
                    <th>
                        Time since last URI finished
                    </th>
            </tr>
            <% // TODO: Implement hosts report %>
        </table>
<%
    } 
%>
</body>
</html>
