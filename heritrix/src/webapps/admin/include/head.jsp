<%@ page errorPage="/error.jsp" %>
<%@ page import="java.text.SimpleDateFormat" %>
<%@ page import="org.archive.crawler.Heritrix" %>
<%@ page import="org.archive.crawler.admin.CrawlJob" %>
<%@ page import="org.archive.crawler.admin.StatisticsTracker" %>
<%
    /**
     * An include file that handles the "look" and navigation of a web page. 
     * Include at top (where you would normally begin the HTML code).
     * If used, the include "foot.jsp" should be included at the end of the HTML
     * code. It will close any table, body and html tags left open in this one.
     * Any custom HTML code is thus placed between the two.
     *
     * The following variables must exist prior to this file being included:
     *
     * String title - Title of the web page
     * int tab - Which to display as 'selected'.
     *           0 - Console
     *           1 - Jobs
     *           2 - Profiles
     *           3 - Logs
     *           4 - Reports
     *           5 - About
     *           6 - Help
     *
     * SimpleHandler handler - In general this is provided by the include
     *                         page 'handler.jsp' which should be included
     *                         prior to this one.
     *
     * @author Kristinn Sigurdsson
     */
%>
<%@include file="stats.jsp"%>

<html>
    <head>
        <title>Heritrix: <%=title%></title>
        <link rel="stylesheet" 
            href="<%=request.getContextPath()%>/css/heritrix.css">
    </head>

    <body>
        <table border="0" cellspacing="0" cellpadding="0" width="100%">
            <tr>
                <td>
                    <table border="0" cellspacing="0" cellpadding="0" height="100%">
                        <tr>
                            <td height="60" width="155" valign="top" nowrap>
                                <table border="0" width="155" cellspacing="0" cellpadding="0" height="60">
                                    <tr>
                                        <td align="center" height="40" valign="bottom">
                                            <a border="0" 
                                            href="<%=request.getContextPath()%>/index.jsp"><img border="0" src="<%=request.getContextPath()%>/images/logo.gif" height="37" width="145"></a>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td class="subheading">
                                            <%=title%>
                                        </td>
                                    </tr>
                                </table>
                            </td>
                            <td width="5" nowrap>
                                &nbsp;&nbsp;
                            </td>
                            <td width="460" align="left" nowrap>
                                <table border="0" cellspacing="0" cellpadding="0" height="60">
                                    <tr>
                                        <td colspan="2" nowrap>
                                            <%
                                                SimpleDateFormat sdf = new SimpleDateFormat("MMM. d, yyyy HH:mm:ss");
                                                sdf.setTimeZone(java.util.TimeZone.getTimeZone("GMT"));
                                            %>
                                            <b>
                                                Status of crawler as of <a style="color: #000000" href="<%=request.getRequestURL()%>"><%=sdf.format(new java.util.Date())%> GMT</a>
                                            </b>
                                            &nbsp;&nbsp;&nbsp;
                                            <b>
                                                Alerts: 
                                            </b>
                                            <% if(Heritrix.getAlerts().size()==0) { %>
                                                <a style="color: #000000; text-decoration: none" href="<%=request.getContextPath()%>/console/alerts.jsp">no alerts</a>
                                            <% } else if(Heritrix.getNewAlerts()>0){ %>
                                                <b><a href="<%=request.getContextPath()%>/console/alerts.jsp"><%=Heritrix.getAlerts().size()%> (<%=Heritrix.getNewAlerts()%> new)</a></b>
                                            <% } else { %>
                                                <a style="color: #000000" href="<%=request.getContextPath()%>/console/alerts.jsp"><%=Heritrix.getAlerts().size()%> (<%=Heritrix.getNewAlerts()%> new)</a>
                                            <% } %>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td nowrap>
                                            <%=handler.isRunning()?"Crawler is running":"Crawler is not running"%>
                                        </td>
                                        <td nowrap>
                                            <%
                                                if(handler.isRunning() || handler.isCrawling())
                                                {
                                                    if(handler.isCrawling())
                                                    {
                                                        out.println("<b>Current job:</b> " + handler.getCurrentJob().getJobName());
                                                    }
                                                    else
                                                    {
                                                        out.println("No job ready for crawling <a href=\"");
                                                        out.println(request.getContextPath());
                                                        out.println("/jobs/new.jsp\" style='color: #000000'>(create new)</a>");
                                                    }
                                                }
                                            %>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td nowrap>
                                            <%=handler.getPendingJobs().size()%>
                                            jobs
                                            <a style="color: #000000" href="<%=request.getContextPath()%>/jobs/pending.jsp">pending</a>,
                                            <%=handler.getCompletedJobs().size()%>
                                            <a style="color: #000000" href="<%=request.getContextPath()%>/jobs/completed.jsp">completed</a>
                                            &nbsp;
                                        </td>
                                        <td nowrap>
                                            <% if(handler.isCrawling()){ %>
                                                    Downloaded <%=stats.successfullyFetchedCount()%> documents in 
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
                                                } 
                                            %>
                                        </td>
                                    </tr>
                                </table>
                            </td>
                        </tr>
                    </table>
                </td>
                <td width="100%" nowrap>
                    &nbsp;
                </td>
            </tr>
            <tr>
                <td bgcolor="#0000FF" height="1" colspan="4">
                </td>
            </tr>
            <tr>
                <td colspan="4" height="20">
                    <table border="0" cellspacing="0" cellpadding="0" width="100%" height="20">
                        <tr>
                            <td class="tab_seperator">&nbsp;</td>
                            <td class="tab<%=tab==0?"_selected":""%>">
                                <a href="<%=request.getContextPath()%>/index.jsp" class="tab_text<%=tab==0?"_selected":""%>">Console</a>
                            </td>
                            <td class="tab_seperator">&nbsp;</td>
                            <td class="tab<%=tab==1?"_selected":""%>">
                                <a href="<%=request.getContextPath()%>/jobs.jsp" class="tab_text<%=tab==1?"_selected":""%>">Jobs</a>
                            </td>
                            <td class="tab_seperator">&nbsp;</td>
                            <td class="tab<%=tab==2?"_selected":""%>">
                                <a href="<%=request.getContextPath()%>/profiles.jsp" class="tab_text<%=tab==2?"_selected":""%>">Profiles</a>
                            </td>
                            <td class="tab_seperator">&nbsp;</td>
                            <td class="tab<%=tab==3?"_selected":""%>">
                                <a href="<%=request.getContextPath()%>/logs.jsp" class="tab_text<%=tab==3?"_selected":""%>">Logs</a>
                            </td>
                            <td class="tab_seperator">&nbsp;</td>
                            <td class="tab<%=tab==4?"_selected":""%>">
                                <a href="<%=request.getContextPath()%>/reports.jsp" class="tab_text<%=tab==4?"_selected":""%>">Reports</a>
                            </td>
                            <td class="tab_seperator">&nbsp;</td>
                            <td class="tab<%=tab==5?"_selected":""%>">
                                <a href="<%=request.getContextPath()%>/about.jsp" class="tab_text<%=tab==5?"_selected":""%>">About</a>
                            </td>
                            <td class="tab_seperator">&nbsp;</td>
                            <td class="tab<%=tab==6?"_selected":""%>">
                                <a href="<%=request.getContextPath()%>/help.jsp" class="tab_text<%=tab==6?"_selected":""%>">Help</a>
                            </td>
                            <td width="100%">
                            </td>
                        </tr>
                    </table>
                </td>
            </tr>
            <tr>
                <td bgcolor="#0000FF" height="1" colspan="4">
                </td>
            </tr>
         </table>
                    <!-- MAIN BODY -->
