<%@ page import="java.util.Collection" %>
<%@ page import="org.archive.crawler.webui.Crawler" %>
<%@ page import="org.archive.crawler.webui.Text" %>
<%@ page import="javax.management.remote.JMXConnector"%>
<%@ page import="java.util.Set"%>
<%@ page import="javax.management.ObjectName"%>
<%@ page import="org.archive.crawler.webui.Misc"%>
<%@ page import="javax.management.MBeanServerConnection"%>
<%@ page import="java.lang.management.MemoryMXBean"%>
<%@ page import="org.archive.crawler.webui.Remote"%>
<%@ page import="javax.management.openmbean.CompositeData"%>
<%@ page import="org.archive.crawler.webui.CrawlJob"%>
<%@ page import="org.archive.crawler.webui.CrawlJob.State"%>
<%

Crawler crawler = (Crawler)request.getAttribute("crawler");

Collection<CrawlJob> profiles = crawler.getJobs(State.PROFILE);
Collection<CrawlJob> active = crawler.getJobs(State.ACTIVE);
Collection<CrawlJob> completed = crawler.getJobs(State.COMPLETED);

%>
<html>
<head>
    <%@ include file="/include/header.jsp"%>
    <title>Heritrix <%=Text.html(crawler.getLegend())%></title>
</head>
<body>

<%@include file="/include/nav.jsp"%>

<% if (active.isEmpty()) { %>
    <p>There are no active jobs on <%=Text.html(crawler.getLegend())%>.
<% } else { %>

    <h3>Active Jobs:</h3>
    
    <table class="info">
    <% boolean alt = false; %>
    <% for (CrawlJob job: active) { %>
    <% String jqs = crawler.getQueryString() + "&job=" + job.getName(); %>
    <tr <%=alt?"class=\"infoalt\"":""%>>
    <td>
        <%=job.getName()%>
    </td>
    <td>
        <a 
           title="View and control the current status for this job."
           href="<%=request.getContextPath()%>/console/do_show_job_console.jsp?<%=jqs%>">Console</a>
        |
        Sheets | Seeds | 
        <a 
           title="View logs for this job."
           href="<%=request.getContextPath()%>/reports/do_show_reports.jsp?<%=jqs%>">Reports</a>
        | 
        <a 
           title="View logs for this job."
           href="<%=request.getContextPath()%>/logs/do_show_log.jsp?<%=jqs%>">Logs</a>
    </td>
    </tr>
    <% alt = !alt; %>
    <% } %>
    </table>
<% } %>

<h3>Profiles:</h3>

<table class="info">
<% boolean alt = false; %>
<% for (CrawlJob job: profiles) { %>
    <tr <%=alt?"class=\"infoalt\"":""%>>
    <td class="info">
        <%=job.getName()%>
    </td>
    <% String pqs = crawler.getQueryString() + "&profile=" + job.getName(); %>
    <td class="info">
        <a title="View or edit this profile's settings sheets."
           href="<%=request.getContextPath()%>/sheets/do_show_sheets.jsp?<%=pqs%>">Sheets</a>
        | 
        <a title="View or edit this profile's settings sheets."
           href="<%=request.getContextPath()%>/seeds/do_show_seeds.jsp?<%=pqs%>">Seeds</a>
        | 
        Copy 
        | 
        <a title="Launch a new job based on this profile." 
           href="do_show_launch_profile.jsp?<%=pqs%>">Launch</a>
    </td>
    </tr>
    <% alt = !alt; %>
<% } %>
</table>


<% if (completed.isEmpty()) { %>
    <p>There are no completed jobs.
<% } else { %>
    <h3>Completed Jobs:</h3>
    
    <table class="info">
    <% alt = false; %>
    <% for (CrawlJob job: completed) { %>
        <% String jqs = crawler.getQueryString() + "&job=" + job.getName(); %>
        <tr <%=alt?"class=\"infoalt\"":""%>>
        <td class="info">
            <%=job.getName()%>
        </td>
        <td class="info">
            Sheets | Seeds | Reports | 
            <a title="View logs for this job."
               href="<%=request.getContextPath()%>/logs/do_show_log.jsp?<%=jqs%>">Logs</a>
        </td>
        </tr>
        <% alt = !alt; %>
    <% } %>
    </table>
<% } %>

</body>
</html>