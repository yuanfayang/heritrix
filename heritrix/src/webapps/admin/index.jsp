<%@include file="/include/handler.jsp"%>
<%@ page import="org.archive.crawler.admin.CrawlJob" %>
<%@ page import="org.archive.crawler.Heritrix" %>
<%@ page import="org.archive.crawler.admin.StatisticsTracker" %>
<%@ page import="org.archive.util.ArchiveUtils" %>
<%@ page import="javax.servlet.jsp.JspWriter" %>
<%!
	private void printTime(final JspWriter out,long time)
    throws java.io.IOException {
	    if(time > 3600) {
	        //got hours.
	        out.println(time/3600 + " h., ");
	        time = time % 3600;
	    }
	    
	    if(time > 60) {
	        out.println(time/60 + " min. and ");
	        time = time % 60;
	    }
	
	    out.println(time + " sec.");
	}
%>
<%

    String sAction = request.getParameter("action");
    if(sAction != null) {
        if(sAction.equalsIgnoreCase("logout")) {
            // Logging out.
            session = request.getSession();
            if (session != null) {
                session.invalidate();
                // Redirect back to here and we'll get thrown to the login
                // page.
                response.sendRedirect(request.getContextPath() + "/index.jsp"); 
            }
        }
    }

    String title = "Administrator Console";
    int tab = 0;
%>

<%@include file="/include/head.jsp"%>
    
    <script type="text/javascript">
        function doTerminateCurrentJob(){
            if(confirm("Are you sure you wish to terminate the job currently being crawled?")){
                document.location = '<%out.print(request.getContextPath());%>/console/action.jsp?action=terminate';
            }
        }    
    </script>
    
    <table border="0" cellspacing="0" cellpadding="0"><tr><td>
    <fieldset style="width: 750px">
        <legend>Crawler status</legend>
        <table border="0" cellspacing="0" cellpadding="0" width="100%">
            <tr>
                <td valign="top" width="60%">
                    <table border="0" cellspacing="0" cellpadding="0" >
                        <tr>
                            <td>
                                <b>Crawler running:</b>&nbsp;
                            </td>
                            <td>
                                <%=handler.isRunning()?"Yes":"No"%>
                            </td>
                        </tr>
                        <tr>
                            <td>
                                <b>Current job:</b>&nbsp;
                            </td>
                            <td nowrap>
                                <%=handler.getCurrentJob()!=null?handler.getCurrentJob().getJobName():"None"%>
                            </td>
                        </tr>
                        <tr>
                            <td>
                                <b>Jobs pending:</b>&nbsp;
                            </td>
                            <td>
                                <%=handler.getPendingJobs().size()%>
                            </td>
                        </tr>
                        <tr>
                            <td>
                                <b>Jobs completed:</b>&nbsp;
                            </td>
                            <td>
                                <%=handler.getCompletedJobs().size()%>
                            </td>
                        </tr>
                    </table>
                </td>
                <td valign="top" width="40%">
                    <table border="0" cellspacing="0" cellpadding="0" >
                        <tr>
                            <td>
                                <b>Used memory:</b>&nbsp;
                            </td>
                            <td>
                                <%=(Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory())/1024%> KB
                            </td>
                        </tr>
                        <tr>
                            <td>
                                <b>Heap size:</b>&nbsp;
                            </td>
                            <td>
                                <%=(Runtime.getRuntime().totalMemory())/1024%> KB
                            </td>
                        </tr>
                        <tr>
                            <td>
                                <b>Max heap size:</b>&nbsp;
                            </td>
                            <td>
                                <%=(Runtime.getRuntime().maxMemory())/1024%> KB
                            </td>
                        </tr>
                        <tr>
                            <td>
                                <b>
                                    Alerts: 
                                </b>
                            </td>
                            <td>
                                <a style="color: #000000" 
                                    href="<%=request.getContextPath()%>/console/alerts.jsp">
                                    <%=Heritrix.getAlerts().size()%> (<%=Heritrix.getNewAlerts()%> new)
                                </a>
                            </td>
                    </table>
                </td>
            </tr>
            <%
            	long begin, end;
	            if(stats != null) {
	                begin = stats.successfullyFetchedCount();
	                end = stats.totalCount();
	                if(end < 1) {
	                    end = 1;
	                }
	            } else {
                    begin = 0;
                    end = 1;
	            }
                
                if(handler.getCurrentJob() != null)
                {
                    final long timeElapsed, timeRemain;
                    if(stats == null) {
                        timeElapsed= 0;
                        timeRemain = -1;
                    } else {
	                    timeElapsed = (stats.getCrawlerTotalElapsedTime())/1000;
	                    if(begin == 0) {
	                        timeRemain = -1;
	                    } else {
	                        timeRemain = ((long)(timeElapsed*end/(double)begin))-timeElapsed;
	                    }
                    }
            %>
                    <tr>
                        <td valign="top">
                            <table border="0" cellspacing="0" cellpadding="0">
                                <tr>
                                    <td height="5" colspan="2">
                                    </td>
                                </tr>
                                <tr>
                                    <td>
                                        <b>Status:</b>&nbsp;
                                    </td>
                                    <td>
                                        <%=handler.getCurrentJob().getStatus()%>
                                    </td>
                                </tr>
                                <%
                                  if(handler.isCrawling() && stats != null)
                                  {
                                %>
                                <tr>
                                    <td>
                                        <b>Processed docs/sec:</b>&nbsp;
                                    </td>
                                    <td>
                                        <%=ArchiveUtils.doubleToString(stats.currentProcessedDocsPerSec(),2)%> (<%=ArchiveUtils.doubleToString(stats.processedDocsPerSec(),2)%>)
                                        &nbsp;&nbsp;&nbsp;
                                        <b>KB/sec:</b>&nbsp;<%=stats.currentProcessedKBPerSec()%> (<%=stats.processedKBPerSec()%>)
                                    </td>
                                </tr>
                                <tr>
                                    <td>
                                        <b>Run time:</b>&nbsp;
                                    </td>
                                    <td>
                                        <%
                                        	printTime(out,timeElapsed);
                                        %>
                                    </td>
                                </tr>
                                <%
                                   if(timeRemain != -1)
                                   {
                                %>
                                <tr>
                                    <td>
                                        <b>Remaining (estimated):</b>&nbsp;
                                    </td>
                                    <td>
                                        <%
                                        	printTime(out,timeRemain);
                                        %>
                                    </td>
                                </tr>
                                <%
                                   }
                                %>
                                <%
                                }
                                %>
                            </table>
                        </td>
                        <td valign="top">
                        	<%
                                  if(stats != null)
                                  {
                            %>
                            <table border="0" cellspacing="0" cellpadding="0">
                                <tr>
                                    <td height="5" colspan="2">
                                    </td>
                                </tr>
                                <tr>
                                    <td>
                                        <b>Active thread count:</b>&nbsp;
                                    </td>
                                    <td>
                                        <%=stats.activeThreadCount()%> of <%=stats.threadCount()%>
                                    </td>
                                </tr>
                                <tr>
                                    <td>
                                        <b>Total data received:</b>&nbsp;
                                    </td>
                                    <td>
                                        <%=ArchiveUtils.formatBytesForDisplay(stats.totalBytesWritten())%>
                                    </td>
                                </tr>
                            </table>
                        	<%
                                  }
                            %>
                        </td>
                    </tr>
            <%    
                }
                if(stats != null)
                {
	                int ratio = (int) (100 * begin / end);
            %>
                    <tr>
                        <td colspan="2" height="5"> 
                        </td>
                    </tr>
                    <tr>
                        <td colspan="2">
                            <center>
                            <table border=1 width="500">
                            <tr>
                            <td><center><b><u>DOWNLOADED/QUEUED DOCUMENT RATIO</u></b><br>
                            <table border="0" cellpadding="0" cellspacing= "0" width="100%"> 
                                <tr>
                                    <td width="20%"></td>
                                    <td bgcolor="darkorange" width="<%= (int) (ratio/2) %>%" align="right">
                                        <strong><%= ratio %></strong>%
                                    </td>
                                    <td bgcolor="lightblue" align="right" width="<%= (int) ((100-ratio)/2) %>%"></td>
                                    <td nowrap>&nbsp;&nbsp;(<%= begin %> of <%= end %>)</td>
                                </tr>
                            </table>        
                            </td>
                            </tr>
                            </table>
                            </center>
                        </td>
                    </tr>
            <%
                }
            %>
        </table>
    </fieldset>
    </td></tr>
    <tr><td>
    <%
        if(handler.isRunning()) {
            out.println("<a href=\"");
            out.println(request.getContextPath());
            out.println("/console/action.jsp?action=stop\">Stop crawling pending jobs</a>");
        } else {
            out.println("<a href=\"");
            out.println(request.getContextPath());
            out.println("/console/action.jsp?action=start\">Start crawling pending jobs</a>");
        }

        if(handler.isCrawling()) {
            out.println(" | <a href='javascript:doTerminateCurrentJob()'>Terminate current job</a> | ");
            if(handler.getCurrentJob().getStatus().equals(CrawlJob.STATUS_PAUSED) || handler.getCurrentJob().getStatus().equals(CrawlJob.STATUS_WAITING_FOR_PAUSE)) {
                out.println("<a href='/console/action.jsp?action=resume'>Resume current job</a>");
                if(handler.getCurrentJob().getStatus().equals(CrawlJob.STATUS_PAUSED))
                {
                    out.println(" | <a href=\"");
                    out.println(request.getContextPath());
                    out.println("/console/frontier.jsp\">View or Edit Frontier URIs</a> ");

                }
            } else {
                out.println("<a href=\"");
                out.println(request.getContextPath());
                out.println("/console/action.jsp?action=pause\">Pause current job</a> ");
            }
        }
    %> | <a href="<%=request.getContextPath()%>/">Refresh</a>
    </td></tr>
    <tr><td>
        <p>
            &nbsp;
        <p>
            &nbsp;
    </td></tr>
    <tr><td>
        <% if (Heritrix.isCommandLine()) {  
            // Print the shutdown only if we were started from command line.
            // It makes no sense when in webcontainer mode.
         %>
        <a href="<%=request.getContextPath()%>/console/shutdown.jsp">Shut down Heritrix software</a> |
        <% } %>
        <a href="<%=request.getContextPath()%>/index.jsp?action=logout">Logout</a>
    </td></tr></table>
<%@include file="/include/foot.jsp"%>
