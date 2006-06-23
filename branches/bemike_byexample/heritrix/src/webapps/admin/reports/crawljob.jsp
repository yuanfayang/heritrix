<%@include file="/include/handler.jsp"%>

<%@ page import="java.util.*" %>
<%@ page import="org.archive.util.LongWrapper"%>
<%@ page import="org.archive.crawler.datamodel.CrawlURI"%>
<%@ page import="org.archive.util.ArchiveUtils" %>
<%
    /**
     * Page allows user to view the information on seeds in the
     * StatisticsTracker for a completed job.
     * Parameter: job - UID for the job.
     */
    String job = request.getParameter("job");
    CrawlJob cjob = (job != null)? handler.getJob(job): handler.getCurrentJob();
    
    String title = "Crawl job report"; 
    int tab = 4;
    
%>

<%@include file="/include/head.jsp"%>

<%
    if(cjob == null)
    {
        // NO JOB SELECTED - ERROR
%>
        <p>&nbsp;<p>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<b>Invalid job selected</b>
<%
    }
    else if(stats == null)
    {
        out.println("<b>No statistics associated with job.</b><p><b>Job status:</b> " + cjob.getStatus());            
        if(cjob.getErrorMessage()!=null){
            out.println("<p><pre><font color='red'>"+cjob.getErrorMessage()+"</font></pre>");
        }
    }
    else
    {
%>
        <table border="0">
            <tr>
                <td valign="top">
                    <table border="0" cellspacing="0" cellpadding="0" >
                        <tr>
                            <td>
                                <b>Job name:</b>&nbsp;
                            </td>
                            <td>
                                <%=cjob.getJobName()%>
                            </td>
                        </tr>
                        <tr>
                            <td>
                                <b>Status:</b>&nbsp;
                            </td>
                            <td>
                                <%=cjob.getStatus()%>
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
                                    if(cjob.getStatus().equalsIgnoreCase(CrawlJob.STATUS_RUNNING))
                                    {
                                        // Show current and overall stats.
                                %>
                                        <%=ArchiveUtils.doubleToString(stats.currentProcessedDocsPerSec(),2)%> (<%=ArchiveUtils.doubleToString(stats.processedDocsPerSec(),2)%>)
                                <%
                                    }
                                    else
                                    {
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
                                    if(cjob.getStatus().equalsIgnoreCase(CrawlJob.STATUS_RUNNING))
                                    {
                                        // Show current and overall stats.
                                %>
                                        <%=ArchiveUtils.doubleToString(stats.currentProcessedKBPerSec(),2)%> (<%=ArchiveUtils.doubleToString(stats.processedKBPerSec(),2)%>)
                                <%
                                    }
                                    else
                                    {
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
                                <b>Total data written:</b>&nbsp;
                            </td>
                            <td>
                                <%=ArchiveUtils.formatBytesForDisplay(stats.totalBytesWritten())%>
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
        
        <p>

        <table cellspacing="0">
            <tr>
                <th>
                    Status code
                </th>
                <th width="200" colspan="2">
                    Documents
                </th>
            </tr>
            <%
                boolean alt = true;
                TreeMap scd = stats.getReverseSortedCopy(stats.
                    getStatusCodeDistribution());
                for (Iterator i = scd.keySet().iterator();
                        i.hasNext();) {
                    Object key = i.next();
                    long count = ((LongWrapper)scd.get(key)).longValue;
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
                            <a style="text-decoration: none;" href="<%=request.getContextPath()%>/logs.jsp?job=<%=cjob.getUID()%>&log=crawl.log&mode=regexpr&regexpr=\d{17}\s*<%=(String)key%>&grep=true">
                                <%=CrawlURI.fetchStatusCodesToString(Integer.parseInt((String)key))%>
                            </a>&nbsp;
                        </td>
                        <td colspan="2" nowrap>
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
                    File type
                </th>
                <th width="200">
                    Documents
                </th>
                <th>
                    Data
                </th>
            </tr>
            <%
                alt=true;
                TreeMap fd = stats.getReverseSortedCopy(stats.
                        getFileDistribution());
                for (Iterator i = fd.keySet().iterator(); i.hasNext();) {
                    Object key = i.next();
                    long count = ((LongWrapper)fd.get(key)).longValue;
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
                            <a style="text-decoration: none;" href="<%=request.getContextPath()%>/logs.jsp?job=<%=cjob.getUID()%>&log=crawl.log&mode=regexpr&regexpr=^[^ ].*<%=(String)key%>&grep=true"><%=key%></a>&nbsp;&nbsp;
                        </td>
                        <td nowrap>
                            <img src="<%=request.getContextPath()%>/images/blue.jpg" height="10" width="<%=displaybarwidth%>"> <%=count%> &nbsp;&nbsp;
                        </td>
                        <td align="right" nowrap>
                            <%=ArchiveUtils.formatBytesForDisplay(stats.getBytesPerFileType((String)key))%>
                        </td>
                    </tr>
            <%
                    alt = !alt;
                }
            %>                
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
                <% if (cjob.getStatus().equals(CrawlJob.STATUS_RUNNING) ||
                         cjob.getStatus().equals(CrawlJob.STATUS_PAUSED) ||
                         cjob.getStatus().equals(CrawlJob.STATUS_WAITING_FOR_PAUSE)){ %>
                    <th>
                        Time since last URI finished
                    </th>
                <% } %>
            </tr>
            <%
                alt = true;
                SortedMap hd = stats.getReverseSortedHostsDistribution();
                for (Iterator i = hd.keySet().iterator(); i.hasNext();) {
                    Object key = i.next();
            %>
                    <tr <%=alt?"bgcolor=#EEEEFF":""%>>
                        <td nowrap>
                            <a style="text-decoration: none;" href="<%=request.getContextPath()%>/logs.jsp?job=<%=cjob.getUID()%>&log=crawl.log&mode=regexpr&regexpr=^[^ ].*<%=(String)key%>&grep=true"><%=(String)key%></a>&nbsp;
                        </td>
                        <td nowrap>
                            <% LongWrapper lw = ((LongWrapper)hd.get(key)); %>
                            <%=(lw == null) ?
                                "null": Long.toString(lw.longValue)%>&nbsp;
                        </td>
                        <td align="right" nowrap>
                            <%=ArchiveUtils.formatBytesForDisplay(stats.getBytesPerHost((String)key))%>&nbsp;
                        </td>
                        <% if (cjob.getStatus().equalsIgnoreCase(CrawlJob.STATUS_RUNNING) ||
                                 cjob.getStatus().equals(CrawlJob.STATUS_PAUSED) ||
                                 cjob.getStatus().equals(CrawlJob.STATUS_WAITING_FOR_PAUSE)){ %>
                            <td align="right">
                                <%=ArchiveUtils.formatMillisecondsToConventional(System.currentTimeMillis()-stats.getHostLastFinished((String)key))%>
                            </td>
                        <% } %>
                    </tr>
            <%
                    alt = !alt;
                }
            %>                
        </table>
<%
    } // End if(cjob==null)else clause
%>
<%@include file="/include/foot.jsp"%>
