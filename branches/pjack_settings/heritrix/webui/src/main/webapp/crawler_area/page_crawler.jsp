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

<h2>Active Jobs:</h2>
<% if (active.isEmpty()) { %>
    <span class="placeholder">There are no active jobs on 
    <%=Text.html(crawler.getLegend())%>.</span>
<% } else { %>
    <% for (CrawlJob job: active) { %>
    <div class="multilineItem">
    <% String jqs = crawler.getQueryString() + "&job=" + job.getName(); %>
    <span class="label">Job:</span>
        <%=job.getName()%>
    <div class="itemDetails">
        <a 
           class="rowLink" 
           title="View and control the current status for this job."
           href="<%=request.getContextPath()%>/console/do_show_job_console.jsp?<%=jqs%>">Console</a>
        <a 
           class="rowLink" 
           title="View or edit this job's settings sheets."
           href="<%=request.getContextPath()%>/sheets/do_show_sheets.jsp?<%=jqs%>">Sheets</a>
        <a 
           class="rowLink" 
           title="View or edit this jobs's seeds."
           href="javascript:alert('not yet implemented')">Seeds</a>
        <a 
           class="rowLink" 
           title="View reports for this job."
           href="<%=request.getContextPath()%>/reports/do_show_reports.jsp?<%=jqs%>">Reports</a>
        <a 
           class="rowLink" 
           title="View logs for this job."
           href="<%=request.getContextPath()%>/logs/do_show_log.jsp?<%=jqs%>">Logs</a>
    </div>
    </div>
    <% } %>
<% } %>

<h2>Profiles:</h2>

<% for (CrawlJob profile: profiles) { %>
    <div class="multilineItem">
    <span class="label">Profile:</span> <%=profile.getName()%>
    <div class="itemDetails">
    <% String pqs = crawler.getQueryString() + "&profile=" + profile.getName(); %>
        <a class="rowLink" title="View or edit this profile's settings sheets."
           href="<%=request.getContextPath()%>/sheets/do_show_sheets.jsp?<%=pqs%>">Sheets</a>
        <a class="rowLink" title="View or edit this profile's seeds."
           href="<%=request.getContextPath()%>/seeds/do_show_seeds.jsp?<%=pqs%>">Seeds</a>
        <a class="rowLink" title="Duplicate to another profile name."
           href="javascript:alert('not yet implemented')">Duplicate</a>
        <a class="rowLink" title="Launch a new job based on this profile." 
           href="do_show_launch_profile.jsp?<%=pqs%>">Launch as Job</a>
    </div>
    </div>
<% } %>

<h2>Completed Jobs:</h2>
<% if (completed.isEmpty()) { %>
    <span class="placeholder">There are no completed jobs.</span>
<% } else { %>
    <% for (CrawlJob job: completed) { %>
        <div class="multilineItem">
        <% String jqs = crawler.getQueryString() + "&job=" + job.getName(); %>
        <span class="label">Job:</span> <%=job.getName()%>
        <div class="itemDetails">
            <a class="rowLink" href="javascript:alert('not yet implemented')">Sheets</a>
            <a class="rowLink" href="javascript:alert('not yet implemented')">Seeds</a> 
            <a class="rowLink" href="javascript:alert('not yet implemented')">Reports</a>
            <a class="rowLink" title="View logs for this job."
               href="<%=request.getContextPath()%>/logs/do_show_log.jsp?<%=jqs%>">Logs</a>
        </div>
        </div>
    <% } %>
    </table>
<% } %>

</body>
</html>