<%@ page import="java.util.Collection" %>
<%@ page import="org.archive.crawler.webui.Crawler" %>
<%@ page import="org.archive.crawler.webui.Text" %>
<%@ page import="org.archive.crawler.webui.Misc"%>
<%@ page import="org.archive.crawler.webui.Remote"%>
<%@ page import="org.archive.crawler.webui.CrawlJob"%>
<%

Crawler crawler = (Crawler)request.getAttribute("crawler");
Collection<CrawlJob> active = (Collection)Text.get(request, "active");
Collection<CrawlJob> ready = (Collection)Text.get(request, "ready");
Collection<CrawlJob> profiles = (Collection)Text.get(request, "profiles");
Collection<CrawlJob> completed = (Collection)Text.get(request, "completed");

String copyUrl = request.getContextPath() + "/crawler_area/do_show_copy.jsp";

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
    <% String jqs = Text.jobQueryString(request, job); %>
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

<h2>Ready Jobs:</h2>

<% if (ready.isEmpty()) { %>
    <span class="placeholder">There are no ready jobs on 
    <%=Text.html(crawler.getLegend())%>.</span>
<% } else { %>
    <% for (CrawlJob job: ready) { %>
    <div class="multilineItem">
    <% String jqs = Text.jobQueryString(request, job); %>
    <span class="label">Job:</span>
        <%=job.getName()%>
    <div class="itemDetails">
        <a 
           class="rowLink" 
           title="View or edit this job's settings sheets."
           href="<%=request.getContextPath()%>/sheets/do_show_sheets.jsp?<%=jqs%>">Sheets</a>
        <a 
           class="rowLink" 
           title="View or edit this jobs's seeds."
           href="<%=request.getContextPath()%>/seeds/do_show_seeds.jsp?<%=jqs%>">Seeds</a>
        <a 
           class="rowLink" 
           title="Duplicate this job to a profile or another job."
           href="<%=copyUrl%>?<%=jqs%>">Copy</a>
           
        <a 
           class="rowLink" 
           title="Begin crawling!"
           href="<%=request.getContextPath()%>/crawler_area/do_launch.jsp?<%=jqs%>">Launch</a>
    </div>
    </div>
    <% } %>
<% } %>


<h2>Profiles:</h2>

<% for (CrawlJob profile: profiles) { %>
    <% String jqs = Text.jobQueryString(request, profile); %>
    <div class="multilineItem">
    <span class="label">Profile:</span> <%=profile.getName()%>
    <div class="itemDetails">
        <a class="rowLink" title="View or edit this profile's settings sheets."
           href="<%=request.getContextPath()%>/sheets/do_show_sheets.jsp?<%=jqs%>">Sheets</a>
        <a class="rowLink" title="View or edit this profile's seeds."
           href="<%=request.getContextPath()%>/seeds/do_show_seeds.jsp?<%=jqs%>">Seeds</a>
        <a class="rowLink" title="Duplicate this profile to another profile or a new job."
           href="<%=copyUrl%>?<%=jqs%>">Copy</a>
    </div>
    </div>
<% } %>

<h2>Completed Jobs:</h2>
<% if (completed.isEmpty()) { %>
    <span class="placeholder">There are no completed jobs.</span>
<% } else { %>
    <% for (CrawlJob job: completed) { %>
        <div class="multilineItem">
        <% String jqs = Text.jobQueryString(request, job); %>
        <span class="label">Job:</span> <%=job.getName()%>
        <div class="itemDetails">
            <a class="rowLink" href="javascript:alert('not yet implemented')">Sheets</a>
            <a class="rowLink" href="javascript:alert('not yet implemented')">Seeds</a> 
            <a 
               class="rowLink" 
               title="Copy this job to a profile or a new ready job."
               href="<%=copyUrl%>?<%=jqs%>">Copy</a>
            
            <a class="rowLink" href="javascript:alert('not yet implemented')">Reports</a>
            <a class="rowLink" title="View logs for this job."
               href="<%=request.getContextPath()%>/logs/do_show_log.jsp?<%=jqs%>">Logs</a>
               
            <a 
               class="rowLink" 
               title="Recover from checkpoint or recover.gz file."
               href="<%=request.getContextPath()%>/crawler_area/do_show_recover.jsp?<%=jqs%>">
               Recover</a>
            
        </div>
        </div>
    <% } %>
    </table>
<% } %>

</body>
</html>

